package com.kliuchko.archive17.presentation.library

import com.kliuchko.archive17.domain.model.LibraryBook

data class LibraryUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val books: List<LibraryBook> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean
        get() = !isLoading && books.isEmpty()
}
