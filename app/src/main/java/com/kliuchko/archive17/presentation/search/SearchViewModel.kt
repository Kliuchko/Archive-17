package com.kliuchko.archive17.presentation.search

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.model.BookLanguage
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.domain.repository.FreeBookRepository
import com.kliuchko.archive17.domain.repository.LanguageSettingsRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: BookRepository,
    private val freeBookRepository: FreeBookRepository,
    languageSettingsRepository: LanguageSettingsRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedMode = MutableStateFlow(CatalogMode.FREE)
    private val _uiState = MutableStateFlow(SearchUiState())

    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                query.debounce(SEARCH_DEBOUNCE_MILLIS).distinctUntilChanged(),
                selectedMode,
                languageSettingsRepository.preferredBookLanguage,
            ) { query, mode, language -> SearchRequest(query, mode, language.toCatalogCode()) }
                .collectLatest(::search)
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        val normalizedLength = value.trim().length
        _uiState.update {
            it.copy(
                query = value,
                isLoading = value.isBlank() || normalizedLength >= SearchUiState.MIN_QUERY_LENGTH,
                isCheckingFreeBooks = false,
                errorMessage = null,
                actionMessage = null,
                hasSearched = false,
                freeBooks = if (normalizedLength in 1 until SearchUiState.MIN_QUERY_LENGTH) {
                    emptyList()
                } else {
                    it.freeBooks
                },
                otherFreeBooks = if (normalizedLength in 1 until SearchUiState.MIN_QUERY_LENGTH) {
                    emptyList()
                } else {
                    it.otherFreeBooks
                },
                books = if (normalizedLength in 1 until SearchUiState.MIN_QUERY_LENGTH) {
                    emptyList()
                } else {
                    it.books
                },
            )
        }
    }

    fun onModeSelected(mode: CatalogMode) {
        selectedMode.value = mode
        _uiState.update {
            it.copy(
                selectedMode = mode,
                isLoading = it.query.isBlank() ||
                    it.query.trim().length >= SearchUiState.MIN_QUERY_LENGTH,
                isCheckingFreeBooks = false,
                books = emptyList(),
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                errorMessage = null,
                actionMessage = null,
                hasSearched = false,
            )
        }
    }

    fun downloadBook(book: FreeBook) {
        if (_uiState.value.downloadingBookId != null || book.epubFileName == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadingBookId = book.editionId,
                    actionMessage = null,
                )
            }
            when (val result = freeBookRepository.downloadToShelf(book)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        downloadingBookId = null,
                        downloadedBookId = result.data.id,
                    )
                }
                is RepositoryResult.Cached -> _uiState.update {
                    it.copy(downloadingBookId = null, actionMessage = result.message)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(downloadingBookId = null, actionMessage = result.message)
                }
            }
        }
    }

    fun onDownloadedBookHandled() {
        _uiState.update { it.copy(downloadedBookId = null) }
    }

    private suspend fun search(request: SearchRequest) {
        val normalizedQuery = request.query.trim()
        if (normalizedQuery.isNotEmpty() && normalizedQuery.length < SearchUiState.MIN_QUERY_LENGTH) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCheckingFreeBooks = false,
                    books = emptyList(),
                    freeBooks = emptyList(),
                    otherFreeBooks = emptyList(),
                    errorMessage = null,
                    actionMessage = null,
                    hasSearched = false,
                    selectedMode = request.mode,
                    bookLanguageCode = request.languageCode,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                isCheckingFreeBooks = false,
                errorMessage = null,
                actionMessage = null,
                selectedMode = request.mode,
                bookLanguageCode = request.languageCode,
            )
        }

        when (request.mode) {
            CatalogMode.FREE -> searchFreeBooks(normalizedQuery, request.languageCode)
            CatalogMode.ALL -> searchAllBooks(normalizedQuery)
        }
    }

    private suspend fun searchFreeBooks(query: String, languageCode: String) {
        when (val result = freeBookRepository.searchBooks(query, languageCode)) {
            is RepositoryResult.Success -> {
                showFreeBookCandidates(result.data, languageCode)
                fillFreeBookCatalog(query, languageCode)
            }
            is RepositoryResult.Cached -> {
                showFreeBookCandidates(result.data, languageCode, result.message)
                fillFreeBookCatalog(query, languageCode)
            }
            is RepositoryResult.Error -> showSearchError(result.message)
        }
    }

    private suspend fun showFreeBookCandidates(
        candidates: List<FreeBook>,
        languageCode: String,
        notice: String? = null,
    ) {
        _uiState.update {
            it.copy(
                isLoading = it.freeBooks.isEmpty() && candidates.isNotEmpty(),
                isCheckingFreeBooks = candidates.isNotEmpty(),
                books = emptyList(),
                errorMessage = null,
                actionMessage = notice,
                hasSearched = true,
            )
        }

        if (candidates.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCheckingFreeBooks = false,
                    freeBooks = emptyList(),
                    otherFreeBooks = emptyList(),
                )
            }
            return
        }

        when (
            val availability = freeBookRepository.keepDownloadableBooks(
                books = candidates,
                languageCode = languageCode,
            )
        ) {
            is RepositoryResult.Success -> _uiState.update {
                val verifiedIds = availability.data.mapTo(mutableSetOf(), FreeBook::editionId)
                it.copy(
                    isLoading = false,
                    isCheckingFreeBooks = false,
                    freeBooks = availability.data,
                    otherFreeBooks = candidates.filterNot { book -> book.editionId in verifiedIds },
                )
            }
            is RepositoryResult.Cached -> _uiState.update {
                val verifiedIds = availability.data.mapTo(mutableSetOf(), FreeBook::editionId)
                it.copy(
                    isLoading = false,
                    isCheckingFreeBooks = false,
                    freeBooks = availability.data,
                    otherFreeBooks = candidates.filterNot { book -> book.editionId in verifiedIds },
                    actionMessage = availability.message,
                )
            }
            is RepositoryResult.Error -> _uiState.update {
                it.copy(
                    isLoading = false,
                    isCheckingFreeBooks = false,
                    freeBooks = emptyList(),
                    otherFreeBooks = candidates,
                    actionMessage = availability.message,
                )
            }
        }
    }

    private suspend fun fillFreeBookCatalog(query: String, languageCode: String) {
        if (_uiState.value.freeBooks.size >= CATALOG_TARGET_SIZE) return

        _uiState.update { it.copy(isCheckingFreeBooks = true) }
        val additionalPages = coroutineScope {
            (2..MAX_CATALOG_PAGES).map { page ->
                async { loadClassifiedFreeBookPage(query, languageCode, page) }
            }.awaitAll()
        }
        _uiState.update { state ->
            val verified = (state.freeBooks + additionalPages.flatMap { it.verified })
                .distinctBy(FreeBook::editionId)
                .take(CATALOG_TARGET_SIZE)
            val verifiedIds = verified.mapTo(mutableSetOf(), FreeBook::editionId)
            val other = (state.otherFreeBooks + additionalPages.flatMap { it.other })
                .distinctBy(FreeBook::editionId)
                .filterNot { it.editionId in verifiedIds }
                .take(OTHER_CATALOG_SIZE)
            state.copy(
                isCheckingFreeBooks = false,
                freeBooks = verified,
                otherFreeBooks = other,
            )
        }
    }

    private suspend fun loadClassifiedFreeBookPage(
        query: String,
        languageCode: String,
        page: Int,
    ): ClassifiedFreeBooks {
        val candidates = when (
            val searchResult = freeBookRepository.searchBooks(query, languageCode, page)
        ) {
            is RepositoryResult.Success -> searchResult.data
            is RepositoryResult.Cached -> searchResult.data
            is RepositoryResult.Error -> return ClassifiedFreeBooks()
        }
        return when (
            val availability = freeBookRepository.keepDownloadableBooks(candidates, languageCode)
        ) {
            is RepositoryResult.Success -> candidates.classify(availability.data)
            is RepositoryResult.Cached -> candidates.classify(availability.data)
            is RepositoryResult.Error -> ClassifiedFreeBooks(other = candidates)
        }
    }

    private fun List<FreeBook>.classify(verified: List<FreeBook>): ClassifiedFreeBooks {
        val verifiedIds = verified.mapTo(mutableSetOf(), FreeBook::editionId)
        return ClassifiedFreeBooks(
            verified = verified,
            other = filterNot { it.editionId in verifiedIds },
        )
    }

    private suspend fun searchAllBooks(query: String) {
        val effectiveQuery = query.ifBlank { STARTER_ALL_BOOKS_QUERY }
        val result = repository.searchBooks(effectiveQuery)
        when (result) {
            is RepositoryResult.Success -> {
                showAllBooks(result.data)
            }

            is RepositoryResult.Cached -> {
                showAllBooks(result.data, result.message)
            }

            is RepositoryResult.Error -> {
                showSearchError(result.message)
            }
        }
    }

    private fun showAllBooks(books: List<Work>, notice: String? = null) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isCheckingFreeBooks = false,
                books = books,
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                errorMessage = null,
                actionMessage = notice,
                hasSearched = true,
            )
        }
    }

    private fun showSearchError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isCheckingFreeBooks = false,
                books = emptyList(),
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                errorMessage = message,
                actionMessage = null,
                hasSearched = true,
            )
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 350L
        const val STARTER_ALL_BOOKS_QUERY = "subject:fiction"
        const val CATALOG_TARGET_SIZE = 10
        const val OTHER_CATALOG_SIZE = 20
        const val MAX_CATALOG_PAGES = 3
    }

    private data class SearchRequest(
        val query: String,
        val mode: CatalogMode,
        val languageCode: String,
    )

    private data class ClassifiedFreeBooks(
        val verified: List<FreeBook> = emptyList(),
        val other: List<FreeBook> = emptyList(),
    )

    private fun BookLanguage.toCatalogCode(): String = when (this) {
        BookLanguage.RUSSIAN -> "rus"
        BookLanguage.ENGLISH -> "eng"
        BookLanguage.DEVICE -> runCatching {
            val systemLocale = Resources.getSystem().configuration.locales[0] ?: Locale.ENGLISH
            systemLocale.isO3Language
        }.getOrDefault("eng")
    }
}
