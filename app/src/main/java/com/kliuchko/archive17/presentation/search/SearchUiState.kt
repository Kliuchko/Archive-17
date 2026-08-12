package com.kliuchko.archive17.presentation.search

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.TemporaryBook

enum class CatalogMode {
    FREE,
    ALL,
}

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val isCheckingFreeBooks: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val canLoadMore: Boolean = false,
    val nextPageError: String? = null,
    val books: List<Work> = emptyList(),
    val freeBooks: List<FreeBook> = emptyList(),
    val otherFreeBooks: List<FreeBook> = emptyList(),
    val selectedMode: CatalogMode = CatalogMode.FREE,
    val bookLanguageCode: String = "eng",
    val downloadingBookId: String? = null,
    val readingBookId: String? = null,
    val downloadedBookId: String? = null,
    val temporaryBook: TemporaryBook? = null,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val hasSearched: Boolean = false,
) {
    val showMinimumQueryState: Boolean
        get() = query.isNotBlank() && query.trim().length < MIN_QUERY_LENGTH

    val showEmptyState: Boolean
        get() = hasSearched &&
            !isLoading &&
            errorMessage == null &&
            books.isEmpty() &&
            freeBooks.isEmpty() &&
            otherFreeBooks.isEmpty()

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
