package com.kliuchko.archive17.presentation.search

import com.kliuchko.archive17.domain.model.FreeBook
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUiStateTest {
    @Test
    fun `shows minimum query state for non blank one character query`() {
        val state = SearchUiState(query = "a")

        assertTrue(state.showMinimumQueryState)
    }

    @Test
    fun `does not show minimum query state for blank query`() {
        val state = SearchUiState(query = "")

        assertFalse(state.showMinimumQueryState)
    }

    @Test
    fun `shows empty state only after completed search with no books`() {
        val state = SearchUiState(
            query = "austen",
            isLoading = false,
            hasSearched = true,
            books = emptyList(),
        )

        assertTrue(state.showEmptyState)
    }

    @Test
    fun `does not show empty state while loading`() {
        val state = SearchUiState(
            query = "austen",
            isLoading = true,
            hasSearched = true,
            books = emptyList(),
        )

        assertFalse(state.showEmptyState)
    }

    @Test
    fun `starts in loading state while starter catalog is requested`() {
        val state = SearchUiState()

        assertTrue(state.isLoading)
        assertFalse(state.showEmptyState)
    }

    @Test
    fun `keeps catalog visible when next page fails`() {
        val state = SearchUiState(
            isLoading = false,
            hasSearched = true,
            freeBooks = listOf(sampleFreeBook()),
            nextPageError = "Offline",
            canLoadMore = true,
        )

        assertFalse(state.showEmptyState)
        assertTrue(state.canLoadMore)
    }

    private fun sampleFreeBook() = FreeBook(
        workId = "work",
        editionId = "edition",
        title = "Book",
        authors = listOf("Author"),
        coverId = null,
        firstPublishYear = 1900,
        languageCode = "eng",
    )
}
