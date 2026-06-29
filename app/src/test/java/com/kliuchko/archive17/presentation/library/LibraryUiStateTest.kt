package com.kliuchko.archive17.presentation.library

import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LibraryEntry
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryUiStateTest {
    @Test
    fun `maps filters to reading statuses`() {
        assertEquals(null, LibraryFilter.ALL.readingStatus)
        assertEquals(ReadingStatus.WANT_TO_READ, LibraryFilter.WANT_TO_READ.readingStatus)
        assertEquals(ReadingStatus.READING, LibraryFilter.READING.readingStatus)
        assertEquals(ReadingStatus.FINISHED, LibraryFilter.FINISHED.readingStatus)
    }

    @Test
    fun `maps filter labels`() {
        assertEquals("All", LibraryFilter.ALL.displayName())
        assertEquals("Want to read", LibraryFilter.WANT_TO_READ.displayName())
        assertEquals("Reading", LibraryFilter.READING.displayName())
        assertEquals("Finished", LibraryFilter.FINISHED.displayName())
    }

    @Test
    fun `is empty after loading when no books exist`() {
        val state = LibraryUiState(
            isLoading = false,
            books = emptyList(),
        )

        assertTrue(state.isEmpty)
    }

    @Test
    fun `is not empty while loading`() {
        val state = LibraryUiState(
            isLoading = true,
            books = emptyList(),
        )

        assertFalse(state.isEmpty)
    }

    @Test
    fun `is not empty when books exist`() {
        val state = LibraryUiState(
            isLoading = false,
            books = listOf(sampleLibraryBook()),
        )

        assertFalse(state.isEmpty)
    }

    private fun sampleLibraryBook(): LibraryBook =
        LibraryBook(
            work = Work(
                id = "OL1W",
                title = "Book",
                authors = listOf("Author"),
                coverId = null,
                firstPublishYear = 2001,
                editionCount = 2,
                editionLanguages = listOf("eng"),
                description = null,
                subjects = emptyList(),
                lastUpdatedAt = 100L,
            ),
            entry = LibraryEntry(
                workId = "OL1W",
                readingStatus = ReadingStatus.READING,
                savedAt = 100L,
                updatedAt = 200L,
            ),
        )
}
