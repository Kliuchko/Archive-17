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
import kotlinx.coroutines.Job
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
    private var activeRequest: SearchRequest? = null
    private var nextPage = 2
    private var paginationJob: Job? = null

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
        paginationJob?.cancel()
        activeRequest = null
        query.value = value
        val normalizedLength = value.trim().length
        _uiState.update {
            it.copy(
                query = value,
                isLoading = value.isBlank() || normalizedLength >= SearchUiState.MIN_QUERY_LENGTH,
                isCheckingFreeBooks = false,
                isLoadingNextPage = false,
                canLoadMore = false,
                nextPageError = null,
                errorMessage = null,
                actionMessage = null,
                isFreeCatalogFallback = false,
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
        if (selectedMode.value == mode) return
        paginationJob?.cancel()
        activeRequest = null
        selectedMode.value = mode
        _uiState.update {
            it.copy(
                selectedMode = mode,
                isLoading = it.query.isBlank() ||
                    it.query.trim().length >= SearchUiState.MIN_QUERY_LENGTH,
                isCheckingFreeBooks = false,
                isLoadingNextPage = false,
                canLoadMore = false,
                nextPageError = null,
                books = emptyList(),
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                errorMessage = null,
                actionMessage = null,
                isFreeCatalogFallback = false,
                hasSearched = false,
            )
        }
    }

    fun downloadBook(book: FreeBook) {
        if (
            _uiState.value.downloadingBookId != null ||
            _uiState.value.readingBookId != null ||
            !book.isDownloadable
        ) {
            return
        }
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

    fun readNow(book: FreeBook) {
        if (
            _uiState.value.downloadingBookId != null ||
            _uiState.value.readingBookId != null ||
            !book.isDownloadable
        ) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(readingBookId = book.editionId, actionMessage = null)
            }
            when (val result = freeBookRepository.downloadForReading(book)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(readingBookId = null, temporaryBook = result.data)
                }
                is RepositoryResult.Cached -> _uiState.update {
                    it.copy(readingBookId = null, actionMessage = result.message)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(readingBookId = null, actionMessage = result.message)
                }
            }
        }
    }

    fun onTemporaryBookHandled() {
        _uiState.update { it.copy(temporaryBook = null) }
    }

    fun loadNextPage() {
        val request = activeRequest ?: return
        val state = _uiState.value
        if (
            state.isLoading ||
            state.isCheckingFreeBooks ||
            state.isLoadingNextPage ||
            !state.canLoadMore
        ) {
            return
        }

        val page = nextPage
        paginationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNextPage = true, nextPageError = null) }
            when (request.mode) {
                CatalogMode.FREE -> loadNextFreeBookPage(request, page)
                CatalogMode.ALL -> loadNextAllBooksPage(request, page)
            }
        }
    }

    fun retryNextPage() = loadNextPage()

    private suspend fun search(request: SearchRequest) {
        paginationJob?.cancel()
        activeRequest = null
        nextPage = 2
        val normalizedQuery = request.query.trim()
        if (normalizedQuery.isNotEmpty() && normalizedQuery.length < SearchUiState.MIN_QUERY_LENGTH) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCheckingFreeBooks = false,
                    isLoadingNextPage = false,
                    canLoadMore = false,
                    nextPageError = null,
                    books = emptyList(),
                    freeBooks = emptyList(),
                    otherFreeBooks = emptyList(),
                    errorMessage = null,
                    actionMessage = null,
                    isFreeCatalogFallback = false,
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
                isLoadingNextPage = false,
                canLoadMore = false,
                nextPageError = null,
                errorMessage = null,
                actionMessage = null,
                books = emptyList(),
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                isFreeCatalogFallback = false,
                selectedMode = request.mode,
                bookLanguageCode = request.languageCode,
            )
        }

        when (request.mode) {
            CatalogMode.FREE -> searchFreeBooks(normalizedQuery, request.languageCode)
            CatalogMode.ALL -> searchAllBooks(request.copy(query = normalizedQuery))
        }
    }

    private suspend fun searchFreeBooks(query: String, languageCode: String) {
        when (val result = freeBookRepository.searchBooks(query, languageCode)) {
            is RepositoryResult.Success -> {
                showFreeBookCandidates(result.data, languageCode)
                activatePagination(
                    request = SearchRequest(query, CatalogMode.FREE, languageCode),
                    canLoadMore = result.data.isNotEmpty(),
                )
                if (query.isBlank()) refreshStarterFreeBooks(result.data, languageCode)
                if (query.isNotBlank() && result.data.isEmpty()) {
                    showFullCatalogFallback(query)
                }
            }
            is RepositoryResult.Cached -> {
                showFreeBookCandidates(result.data, languageCode, result.message)
                activatePagination(
                    request = SearchRequest(query, CatalogMode.FREE, languageCode),
                    canLoadMore = result.data.isNotEmpty(),
                )
                if (query.isBlank()) refreshStarterFreeBooks(result.data, languageCode)
                if (query.isNotBlank() && result.data.isEmpty()) {
                    showFullCatalogFallback(query)
                }
            }
            is RepositoryResult.Error -> showSearchError(result.message)
        }
    }

    private suspend fun showFullCatalogFallback(query: String) {
        val books = when (val result = repository.searchBooks(query, page = 1)) {
            is RepositoryResult.Success -> result.data
            is RepositoryResult.Cached -> result.data
            is RepositoryResult.Error -> return
        }
        if (books.isEmpty()) return
        activeRequest = null
        _uiState.update {
            it.copy(
                isLoading = false,
                isCheckingFreeBooks = false,
                isLoadingNextPage = false,
                canLoadMore = false,
                books = books,
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                errorMessage = null,
                actionMessage = null,
                isFreeCatalogFallback = true,
                hasSearched = true,
            )
        }
    }

    private suspend fun refreshStarterFreeBooks(
        initialBooks: List<FreeBook>,
        languageCode: String,
    ) {
        val refreshed = when (val result = freeBookRepository.refreshStarterCatalog(languageCode)) {
            is RepositoryResult.Success -> result.data
            is RepositoryResult.Cached -> result.data
            is RepositoryResult.Error -> return
        }
        if (refreshed.map(FreeBook::editionId) == initialBooks.map(FreeBook::editionId)) return
        showFreeBookCandidates(refreshed, languageCode)
        activatePagination(
            request = SearchRequest("", CatalogMode.FREE, languageCode),
            canLoadMore = refreshed.isNotEmpty(),
        )
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
                isFreeCatalogFallback = false,
                hasSearched = true,
            )
        }

        if (candidates.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCheckingFreeBooks = false,
                    canLoadMore = false,
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

    private suspend fun loadClassifiedFreeBookPage(
        query: String,
        languageCode: String,
        page: Int,
    ): FreeCatalogPageResult {
        val candidates = when (
            val searchResult = freeBookRepository.searchBooks(query, languageCode, page)
        ) {
            is RepositoryResult.Success -> searchResult.data
            is RepositoryResult.Cached -> searchResult.data
            is RepositoryResult.Error -> return FreeCatalogPageResult.Error(searchResult.message)
        }
        val classified = when (
            val availability = freeBookRepository.keepDownloadableBooks(candidates, languageCode)
        ) {
            is RepositoryResult.Success -> candidates.classify(availability.data)
            is RepositoryResult.Cached -> candidates.classify(availability.data)
            is RepositoryResult.Error -> ClassifiedFreeBooks(other = candidates)
        }
        return FreeCatalogPageResult.Success(
            books = classified,
            hasMore = candidates.isNotEmpty(),
        )
    }

    private fun List<FreeBook>.classify(verified: List<FreeBook>): ClassifiedFreeBooks {
        val verifiedIds = verified.mapTo(mutableSetOf(), FreeBook::editionId)
        return ClassifiedFreeBooks(
            verified = verified,
            other = filterNot { it.editionId in verifiedIds },
        )
    }

    private suspend fun searchAllBooks(request: SearchRequest) {
        val effectiveQuery = request.query.ifBlank { STARTER_ALL_BOOKS_QUERY }
        val result = repository.searchBooks(effectiveQuery, page = 1)
        when (result) {
            is RepositoryResult.Success -> {
                showAllBooks(result.data)
                activatePagination(
                    request = request.copy(query = effectiveQuery),
                    canLoadMore = result.data.isNotEmpty(),
                )
            }

            is RepositoryResult.Cached -> {
                showAllBooks(result.data, result.message)
                activatePagination(
                    request = request.copy(query = effectiveQuery),
                    canLoadMore = result.data.isNotEmpty(),
                )
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
                isLoadingNextPage = false,
                books = books,
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                errorMessage = null,
                actionMessage = notice,
                isFreeCatalogFallback = false,
                hasSearched = true,
            )
        }
    }

    private fun showSearchError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isCheckingFreeBooks = false,
                isLoadingNextPage = false,
                canLoadMore = false,
                nextPageError = null,
                books = emptyList(),
                freeBooks = emptyList(),
                otherFreeBooks = emptyList(),
                errorMessage = message,
                actionMessage = null,
                isFreeCatalogFallback = false,
                hasSearched = true,
            )
        }
    }

    private fun activatePagination(request: SearchRequest, canLoadMore: Boolean) {
        activeRequest = request
        nextPage = 2
        _uiState.update {
            it.copy(
                canLoadMore = canLoadMore,
                isLoadingNextPage = false,
                nextPageError = null,
            )
        }
    }

    private suspend fun loadNextFreeBookPage(request: SearchRequest, page: Int) {
        when (
            val result = loadClassifiedFreeBookPage(
                query = request.query,
                languageCode = request.languageCode,
                page = page,
            )
        ) {
            is FreeCatalogPageResult.Success -> {
                _uiState.update { state -> state.append(result.books, result.hasMore) }
                if (result.hasMore) nextPage = page + 1
            }
            is FreeCatalogPageResult.Error -> _uiState.update {
                it.copy(
                    isLoadingNextPage = false,
                    nextPageError = result.message,
                )
            }
        }
    }

    private suspend fun loadNextAllBooksPage(request: SearchRequest, page: Int) {
        when (val result = repository.searchBooks(request.query, page)) {
            is RepositoryResult.Success -> appendAllBooksPage(result.data, page)
            is RepositoryResult.Cached -> appendAllBooksPage(result.data, page, result.message)
            is RepositoryResult.Error -> _uiState.update {
                it.copy(
                    isLoadingNextPage = false,
                    nextPageError = result.message,
                )
            }
        }
    }

    private fun appendAllBooksPage(books: List<Work>, page: Int, notice: String? = null) {
        _uiState.update { state ->
            state.copy(
                books = (state.books + books).distinctBy(Work::id),
                isLoadingNextPage = false,
                canLoadMore = books.isNotEmpty(),
                nextPageError = null,
                actionMessage = notice ?: state.actionMessage,
            )
        }
        if (books.isNotEmpty()) nextPage = page + 1
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 350L
        const val STARTER_ALL_BOOKS_QUERY = "subject:fiction"
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

    private sealed interface FreeCatalogPageResult {
        data class Success(
            val books: ClassifiedFreeBooks,
            val hasMore: Boolean,
        ) : FreeCatalogPageResult

        data class Error(val message: String) : FreeCatalogPageResult
    }

    private fun SearchUiState.append(
        page: ClassifiedFreeBooks,
        hasMore: Boolean,
    ): SearchUiState {
        val verified = (freeBooks + page.verified).distinctBy(FreeBook::catalogTitleKey)
        val verifiedIds = verified.mapTo(mutableSetOf(), FreeBook::editionId)
        val verifiedTitles = verified.mapTo(mutableSetOf(), FreeBook::catalogTitleKey)
        val other = (otherFreeBooks + page.other)
            .distinctBy(FreeBook::catalogTitleKey)
            .filterNot { it.editionId in verifiedIds }
            .filterNot { it.catalogTitleKey in verifiedTitles }
        return copy(
            freeBooks = verified,
            otherFreeBooks = other,
            isLoadingNextPage = false,
            canLoadMore = hasMore,
            nextPageError = null,
        )
    }

    private fun BookLanguage.toCatalogCode(): String = when (this) {
        BookLanguage.RUSSIAN -> "rus"
        BookLanguage.ENGLISH -> "eng"
        BookLanguage.DEVICE -> runCatching {
            val systemLocale = Resources.getSystem().configuration.locales[0] ?: Locale.ENGLISH
            systemLocale.isO3Language
        }.getOrDefault("eng")
    }
}
