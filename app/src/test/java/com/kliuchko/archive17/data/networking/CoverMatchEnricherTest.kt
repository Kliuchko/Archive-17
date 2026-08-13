package com.kliuchko.archive17.data.networking

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.Work
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverMatchEnricherTest {
    private val enricher = CoverMatchEnricher()

    @Test
    fun `reuses free book cover for exact normalized title and author`() {
        val withoutCover = freeBook(
            workId = "wikisource-1",
            title = "Братья Карамазовы",
            author = "Фёдор Достоевский",
        )
        val donor = freeBook(
            workId = "OL1W",
            title = "Братья, Карамазовы!",
            author = "Федор Достоевский",
            coverId = 42,
        )

        val result = enricher.enrichFreeBooks(listOf(withoutCover, donor))

        assertEquals(42, result.first().coverId)
    }

    @Test
    fun `does not reuse cover when author differs`() {
        val result = enricher.enrichFreeBooks(
            listOf(
                freeBook("local-1", "Одно название", "Первый автор"),
                freeBook("local-2", "Одно название", "Другой автор", coverId = 42),
            ),
        )

        assertNull(result.first().coverId)
    }

    @Test
    fun `reuses cover from an earlier result by work id`() {
        enricher.enrichFreeBooks(
            listOf(freeBook("OL1W", "Original title", "Author", coverUrl = "https://cover")),
        )

        val result = enricher.enrichFreeBooks(
            listOf(freeBook("ol1w", "Localized title", "Localized author")),
        )

        assertEquals("https://cover", result.single().coverUrl)
    }

    @Test
    fun `enriches works with same strict matching rules`() {
        val result = enricher.enrichWorks(
            listOf(
                work("different-id", "Jane Eyre", "Charlotte Brontë"),
                work("OL2W", "Jane Eyre", "Charlotte Bronte", coverId = 91),
            ),
        )

        assertEquals(91, result.first().coverId)
    }

    private fun freeBook(
        workId: String,
        title: String,
        author: String,
        coverId: Int? = null,
        coverUrl: String? = null,
    ) = FreeBook(
        workId = workId,
        editionId = "$workId-edition",
        title = title,
        authors = listOf(author),
        coverId = coverId,
        coverUrl = coverUrl,
        firstPublishYear = null,
        languageCode = "rus",
    )

    private fun work(
        id: String,
        title: String,
        author: String,
        coverId: Int? = null,
    ) = Work(
        id = id,
        title = title,
        authors = listOf(author),
        coverId = coverId,
        firstPublishYear = null,
        editionCount = null,
        editionLanguages = emptyList(),
        description = null,
        subjects = emptyList(),
        lastUpdatedAt = null,
    )
}
