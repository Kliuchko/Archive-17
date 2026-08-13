package com.kliuchko.archive17.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PublicationEditionTest {
    @Test
    fun `downloadable free book becomes available free access`() {
        val edition = freeBook(downloadUrl = "https://example.org/book.epub")
            .toPublicationEdition()

        assertEquals(EditionAccessMode.FREE, edition.accessOptions.single().mode)
        assertEquals(EditionAvailability.AVAILABLE, edition.accessOptions.single().availability)
    }

    @Test
    fun `unverified source edition stays visible as external access`() {
        val edition = freeBook(downloadUrl = null).toPublicationEdition()

        assertEquals(EditionAvailability.EXTERNAL_ONLY, edition.accessOptions.single().availability)
    }

    private fun freeBook(downloadUrl: String?) = FreeBook(
        workId = "work-1",
        editionId = "edition-1",
        title = "Book",
        authors = listOf("Author"),
        coverId = null,
        firstPublishYear = 1900,
        languageCode = "eng",
        source = FreeBookSource.WIKISOURCE,
        sourcePageTitle = "Book",
        epubDownloadUrl = downloadUrl,
    )
}
