package com.kliuchko.archive17.presentation.details

import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.toPublicationEdition
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

        assertEquals("Не указан", state.languageLabel)
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
        assertEquals("Отложено", ReadingStatus.WANT_TO_READ.displayName())
        assertEquals("Читаю", ReadingStatus.READING.displayName())
        assertEquals("Прочитано", ReadingStatus.FINISHED.displayName())
    }

    @Test
    fun `preferred free edition follows saved selection`() {
        val russian = freeBook("ru-edition", "rus")
        val english = freeBook("en-edition", "eng")
        val state = BookDetailsUiState(
            workId = "OL1W",
            selectedEditionId = russian.editionId,
            freeBooksByEditionId = listOf(english, russian).associateBy(FreeBook::editionId),
            editions = listOf(english, russian).map { it.toPublicationEdition() },
        )

        assertEquals("ru-edition", state.preferredFreeBook?.editionId)
    }

    private fun freeBook(id: String, language: String) = FreeBook(
        workId = "OL1W",
        editionId = id,
        title = "Book",
        authors = listOf("Author"),
        coverId = null,
        firstPublishYear = 2001,
        languageCode = language,
        epubDownloadUrl = "https://example.com/$id.epub",
    )

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
