package com.kliuchko.archive17.presentation.details

import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailsUiStateTest {
    @Test
    fun `uses neutral language fallback when metadata is missing`() {
        val state = BookDetailsUiState(
            workId = "OL1W",
            work = sampleWork(editionLanguages = emptyList()),
        )

        assertEquals("Not specified", state.languageLabel)
    }

    @Test
    fun `joins edition language metadata`() {
        val state = BookDetailsUiState(
            workId = "OL1W",
            work = sampleWork(editionLanguages = listOf("eng", "fre")),
        )

        assertEquals("eng, fre", state.languageLabel)
    }

    @Test
    fun `can save when work exists and refresh is not running`() {
        val state = BookDetailsUiState(
            workId = "OL1W",
            work = sampleWork(),
            isRefreshing = false,
        )

        assertTrue(state.canSave)
    }

    @Test
    fun `cannot save while refresh is running`() {
        val state = BookDetailsUiState(
            workId = "OL1W",
            work = sampleWork(),
            isRefreshing = true,
        )

        assertFalse(state.canSave)
    }

    @Test
    fun `maps reading status labels`() {
        assertEquals("Want to read", ReadingStatus.WANT_TO_READ.displayName())
        assertEquals("Reading", ReadingStatus.READING.displayName())
        assertEquals("Finished", ReadingStatus.FINISHED.displayName())
    }

    private fun sampleWork(
        editionLanguages: List<String> = listOf("eng"),
    ): Work =
        Work(
            id = "OL1W",
            title = "Book",
            authors = listOf("Author"),
            coverId = null,
            firstPublishYear = 2001,
            editionCount = 2,
            editionLanguages = editionLanguages,
            description = null,
            subjects = emptyList(),
            lastUpdatedAt = 100L,
        )
}
