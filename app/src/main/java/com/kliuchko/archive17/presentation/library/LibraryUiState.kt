package com.kliuchko.archive17.presentation.library

import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LocalBook

data class LibraryUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val books: List<LibraryBook> = emptyList(),
    val localBooks: List<LocalBook> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val message: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && books.isEmpty() && localBooks.isEmpty()

    val bookCount: Int
        get() = books.size + localBooks.size
}
