package com.kliuchko.archive17.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.model.BookLanguage
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.PublicationEdition
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.toPublicationEdition
import com.kliuchko.archive17.domain.model.groupMeaningfulVariants
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.domain.repository.BookQueryResolver
import com.kliuchko.archive17.domain.repository.FreeBookRepository
import com.kliuchko.archive17.domain.repository.LanguageSettingsRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.res.Resources
import java.util.Locale

class BookDetailsViewModel(
    private val workId: String,
    private val repository: BookRepository,
    private val freeBookRepository: FreeBookRepository,
    private val languageSettingsRepository: LanguageSettingsRepository,
    private val queryResolver: BookQueryResolver,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailsUiState(workId = workId))
    val uiState: StateFlow<BookDetailsUiState> = _uiState.asStateFlow()
    private var editionsWorkId: String? = null

    init {
        observeDetails()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    errorMessage = null,
                    message = null,
                )
            }

            when (val result = repository.refreshWorkDetails(workId)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = null,
                            errorMessage = null,
                        )
                    }
                }

                is RepositoryResult.Cached -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = result.message,
                            errorMessage = null,
                        )
                    }
                }

                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = result.message,
                            message = null,
                        )
                    }
                }
            }
        }
    }

    fun saveStatus(status: ReadingStatus) {
        val work = uiState.value.work ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    message = null,
                    errorMessage = null,
                )
            }

            when (val result = repository.saveWorkToLibrary(work, status)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            selectedStatus = result.data.entry.readingStatus,
                            message = "Книга сохранена: ${result.data.entry.readingStatus.displayName()}.",
                        )
                    }
                }

                is RepositoryResult.Cached -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            selectedStatus = result.data.entry.readingStatus,
                            message = result.message,
                        )
                    }
                }

                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun readNow(book: FreeBook) {
        if (uiState.value.readingEditionId != null || uiState.value.downloadingEditionId != null) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(readingEditionId = book.editionId, message = null) }
            when (val result = freeBookRepository.downloadForReading(book)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(readingEditionId = null, temporaryBook = result.data)
                }
                is RepositoryResult.Cached -> _uiState.update {
                    it.copy(readingEditionId = null, message = result.message)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(readingEditionId = null, errorMessage = result.message)
                }
            }
        }
    }

    fun addEditionToShelf(book: FreeBook) {
        if (uiState.value.readingEditionId != null || uiState.value.downloadingEditionId != null) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingEditionId = book.editionId, message = null) }
            when (val result = freeBookRepository.downloadToShelf(book)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(downloadingEditionId = null, downloadedBookId = result.data.id)
                }
                is RepositoryResult.Cached -> _uiState.update {
                    it.copy(downloadingEditionId = null, message = result.message)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(downloadingEditionId = null, errorMessage = result.message)
                }
            }
        }
    }

    fun onDownloadedBookHandled() {
        _uiState.update { it.copy(downloadedBookId = null) }
    }

    fun onTemporaryBookHandled() {
        _uiState.update { it.copy(temporaryBook = null) }
    }

    fun showAllEditionVariants() {
        _uiState.update { it.copy(showAllEditionVariants = true) }
    }

    private fun observeDetails() {
        viewModelScope.launch {
            repository.observeWorkDetails(workId).collect { details ->
                _uiState.update {
                    it.copy(
                        work = details.work,
                        selectedStatus = details.libraryEntry?.readingStatus,
                        isLoading = false,
                        isCached = details.isCached,
                        isStale = details.isStale,
                        errorMessage = if (details.work == null) it.errorMessage else null,
                    )
                }
                details.work?.let { work ->
                    if (editionsWorkId != work.id) {
                        editionsWorkId = work.id
                        loadEditions(work)
                    }
                }
            }
        }
    }

    private fun loadEditions(work: com.kliuchko.archive17.domain.model.Work) {
        viewModelScope.launch {
            val preferredBookLanguageCode =
                languageSettingsRepository.preferredBookLanguage.value.toCatalogCode()
            val deviceLanguageCode = deviceLanguageCode()
            _uiState.update { it.copy(isLoadingEditions = true) }
            val resolvedOriginalLanguageCode = queryResolver.resolveOriginalLanguage(
                query = work.title,
                preferredLanguageCode = preferredBookLanguageCode,
            )
            val (metadataResult, freeResult) = coroutineScope {
                val metadata = async {
                    repository.getPublicationEditions(
                        work = work,
                        preferredLanguageCode = deviceLanguageCode,
                        originalLanguageCode = resolvedOriginalLanguageCode,
                    )
                }
                val free = async {
                    loadFreeEditions(work.title, work.id, preferredBookLanguageCode)
                }
                metadata.await() to free.await()
            }
            val metadata = metadataResult.dataOrEmpty()
            val freeBooks = freeResult
            val freeEditions = freeBooks.map(FreeBook::toPublicationEdition)
            val originalLanguageCode = resolvedOriginalLanguageCode
                ?: inferOriginalLanguage(work, freeEditions + metadata)
            val editions = (freeEditions + metadata)
                .distinctBy(PublicationEdition::id)
                .groupMeaningfulVariants(deviceLanguageCode, originalLanguageCode)
            _uiState.update {
                it.copy(
                    editions = editions,
                    originalLanguageCode = originalLanguageCode,
                    freeBooksByEditionId = freeBooks.associateBy(FreeBook::editionId),
                    isLoadingEditions = false,
                    showAllEditionVariants = false,
                )
            }
        }
    }

    private suspend fun loadFreeEditions(
        title: String,
        expectedWorkId: String,
        languageCode: String,
    ): List<FreeBook> {
        val candidates = freeBookRepository.searchBooks(title, languageCode).dataOrEmpty()
            .filter { book -> book.workId == expectedWorkId }
        if (candidates.isEmpty()) return emptyList()
        return freeBookRepository.keepDownloadableBooks(candidates, languageCode).dataOrEmpty()
    }

    private fun <T> RepositoryResult<List<T>>.dataOrEmpty(): List<T> = when (this) {
        is RepositoryResult.Success -> data
        is RepositoryResult.Cached -> data
        is RepositoryResult.Error -> emptyList()
    }

    private fun BookLanguage.toCatalogCode(): String = when (this) {
        BookLanguage.RUSSIAN -> "rus"
        BookLanguage.ENGLISH -> "eng"
        BookLanguage.DEVICE -> runCatching {
            val locale = Resources.getSystem().configuration.locales[0] ?: Locale.ENGLISH
            locale.isO3Language.toOpenLibraryLanguageCode()
        }.getOrDefault("eng")
    }

    private fun deviceLanguageCode(): String = runCatching {
        val locale = Resources.getSystem().configuration.locales[0] ?: Locale.ENGLISH
        locale.isO3Language.toOpenLibraryLanguageCode()
    }.getOrDefault("eng")

    private fun String.toOpenLibraryLanguageCode(): String = when (this) {
        "deu" -> "ger"
        "fra" -> "fre"
        "zho" -> "chi"
        "ces" -> "cze"
        "nld" -> "dut"
        else -> this
    }

    private fun inferOriginalLanguage(
        work: com.kliuchko.archive17.domain.model.Work,
        editions: List<PublicationEdition>,
    ): String? {
        val available = work.editionLanguages.toSet()
        val scriptLanguage = when {
            work.title.any { it in "іїєґІЇЄҐ" } -> "ukr"
            work.title.any { it in '\u0400'..'\u04FF' } -> "rus"
            work.title.any { it in '\u0600'..'\u06FF' } -> "ara"
            work.title.any { it in '\u3040'..'\u30FF' } -> "jpn"
            work.title.any { it in '\u4E00'..'\u9FFF' } -> "chi"
            else -> null
        }
        if (scriptLanguage != null && scriptLanguage in available) return scriptLanguage
        return editions
            .filter { edition -> edition.publishedYear != null }
            .minByOrNull { edition -> edition.publishedYear ?: Int.MAX_VALUE }
            ?.languageCode
            ?: "eng".takeIf(available::contains)
            ?: available.firstOrNull()
    }
}
