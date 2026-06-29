package com.kliuchko.archive17.presentation.search

import com.kliuchko.archive17.domain.model.Work

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val books: List<Work> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
) {
    val showMinimumQueryState: Boolean
        get() = query.isNotBlank() && query.trim().length < MIN_QUERY_LENGTH

    val showEmptyState: Boolean
        get() = hasSearched && !isLoading && errorMessage == null && books.isEmpty()

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
