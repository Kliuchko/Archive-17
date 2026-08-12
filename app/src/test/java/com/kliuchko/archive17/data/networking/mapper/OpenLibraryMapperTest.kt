package com.kliuchko.archive17.data.networking.mapper

import com.google.gson.JsonParser
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchDocDto
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchResponseDto
import com.kliuchko.archive17.data.networking.dto.OpenLibraryWorkDto
import com.kliuchko.archive17.data.networking.dto.OpenLibraryEditionSearchDto
import com.kliuchko.archive17.data.networking.dto.OpenLibraryEditionsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLibraryMapperTest {
    @Test
    fun `maps only public editions with archive files to free books`() {
        val response = OpenLibrarySearchResponseDto(
            docs = listOf(
                OpenLibrarySearchDocDto(
                    key = "/works/OL1W",
                    title = "Work title",
                    authorNames = listOf("Author"),
                    coverId = 10,
                    firstPublishYear = 1900,
                    editionCount = 3,
                    languages = listOf("eng"),
                    ebookAccess = "public",
                    editions = OpenLibraryEditionsDto(
                        docs = listOf(
                            OpenLibraryEditionSearchDto(
                                key = "/books/OL1M",
                                title = "Edition title",
                                languages = listOf("eng"),
                                ebookAccess = "public",
                                archiveIdentifiers = listOf("archive-id"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val result = response.toFreeBooks()

        assertEquals(1, result.size)
        assertEquals("OL1W", result.first().workId)
        assertEquals("OL1M", result.first().editionId)
        assertEquals("Edition title", result.first().title)
        assertEquals("archive-id", result.first().archiveIdentifier)
        assertEquals(1900, result.first().firstPublishYear)
    }

    @Test
    fun `uses cyrillic work title when russian edition title is romanized`() {
        assertEquals(
            "Идиот",
            localizedTitle(
                editionTitle = "Idot",
                workTitle = "Идиот",
                languageCode = "rus",
            ),
        )
    }

    @Test
    fun `removes catalog accents from russian title`() {
        assertEquals(
            "Братья Карамазовы",
            localizedTitle(
                editionTitle = "Бра́тья Карама́зовы",
                workTitle = "Братья Карамазовы",
                languageCode = "rus",
            ),
        )
    }

    @Test
    fun `transliterates romanized russian edition title when localized title is absent`() {
        assertEquals(
            "Макбет",
            localizedTitle(
                editionTitle = "Makbet",
                workTitle = "Macbeth",
                languageCode = "rus",
            ),
        )
    }

    @Test
    fun `removes duplicate romanized title from russian edition`() {
        assertEquals(
            "Казаки",
            localizedTitle(
                editionTitle = "Казаки (Kazaki)",
                workTitle = "The Cossacks",
                languageCode = "rus",
            ),
        )
    }

    @Test
    fun `maps search documents to domain works`() {
        val response = OpenLibrarySearchResponseDto(
            docs = listOf(
                OpenLibrarySearchDocDto(
                    key = "/works/OL45883W",
                    title = "  Pride and Prejudice ",
                    authorNames = listOf("Jane Austen", "Jane Austen", " "),
                    coverId = 12645118,
                    firstPublishYear = 1813,
                    editionCount = 428,
                    languages = listOf("eng", "fre", "eng"),
                ),
            ),
        )

        val result = response.toDomain()

        assertEquals(1, result.size)
        assertEquals("OL45883W", result.first().id)
        assertEquals("Pride and Prejudice", result.first().title)
        assertEquals(listOf("Jane Austen"), result.first().authors)
        assertEquals(12645118, result.first().coverId)
        assertEquals(1813, result.first().firstPublishYear)
        assertEquals(428, result.first().editionCount)
        assertEquals(listOf("eng", "fre"), result.first().editionLanguages)
    }

    @Test
    fun `skips search documents without required fields`() {
        val response = OpenLibrarySearchResponseDto(
            docs = listOf(
                OpenLibrarySearchDocDto(
                    key = null,
                    title = "Missing key",
                    authorNames = null,
                    coverId = null,
                    firstPublishYear = null,
                    editionCount = null,
                    languages = null,
                ),
                OpenLibrarySearchDocDto(
                    key = "/works/OL1W",
                    title = " ",
                    authorNames = null,
                    coverId = null,
                    firstPublishYear = null,
                    editionCount = null,
                    languages = null,
                ),
            ),
        )

        assertEquals(emptyList<Any>(), response.toDomain())
    }

    @Test
    fun `maps work details and preserves search fallback fields`() {
        val fallback = OpenLibrarySearchDocDto(
            key = "/works/OL45883W",
            title = "Pride and Prejudice",
            authorNames = listOf("Jane Austen"),
            coverId = 12645118,
            firstPublishYear = 1813,
            editionCount = 428,
            languages = listOf("eng"),
        ).toDomain()

        val details = OpenLibraryWorkDto(
            key = "/works/OL45883W",
            title = "Pride and Prejudice",
            description = JsonParser.parseString("""{"value":"A classic novel."}"""),
            subjects = listOf("Love stories", "Social classes"),
        )

        val result = details.toDomain(fallback = fallback, lastUpdatedAt = 123L)

        requireNotNull(result)
        assertEquals("OL45883W", result.id)
        assertEquals(listOf("Jane Austen"), result.authors)
        assertEquals(12645118, result.coverId)
        assertEquals("A classic novel.", result.description)
        assertEquals(listOf("Love stories", "Social classes"), result.subjects)
        assertEquals(123L, result.lastUpdatedAt)
    }

    @Test
    fun `maps string work description`() {
        val details = OpenLibraryWorkDto(
            key = "/works/OL1W",
            title = "Book",
            description = JsonParser.parseString("\"Plain description\""),
            subjects = null,
        )

        assertEquals("Plain description", details.toDomain()?.description)
    }

    @Test
    fun `returns null when details have no title and no fallback`() {
        val details = OpenLibraryWorkDto(
            key = "/works/OL1W",
            title = null,
            description = null,
            subjects = null,
        )

        assertNull(details.toDomain())
    }
}
