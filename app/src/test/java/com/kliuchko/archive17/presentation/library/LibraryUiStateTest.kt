package com.kliuchko.archive17.presentation.library

import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LibraryEntry
import com.kliuchko.archive17.domain.model.LocalBook
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
        assertEquals("Все", LibraryFilter.ALL.displayName())
        assertEquals("Отложено", LibraryFilter.WANT_TO_READ.displayName())
        assertEquals("Читаю", LibraryFilter.READING.displayName())
        assertEquals("Прочитано", LibraryFilter.FINISHED.displayName())
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

    @Test
    fun `counts local and catalog books on one shelf`() {
        val state = LibraryUiState(
            isLoading = false,
            books = listOf(sampleLibraryBook()),
            localBooks = listOf(sampleLocalBook()),
        )

        assertEquals(2, state.bookCount)
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

    private fun sampleLocalBook(): LocalBook = LocalBook(
        id = "local-1",
        title = "Local book",
        author = "Author",
        identifier = null,
        filePath = "/books/local-1.epub",
        coverPath = null,
        progressionJson = null,
        readingStatus = ReadingStatus.WANT_TO_READ,
        addedAt = 100L,
        updatedAt = 100L,
    )
}
