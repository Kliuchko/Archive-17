package com.kliuchko.archive17.data.remote.mapper

import com.google.gson.JsonParser
import com.kliuchko.archive17.data.remote.dto.OpenLibrarySearchDocDto
import com.kliuchko.archive17.data.remote.dto.OpenLibrarySearchResponseDto
import com.kliuchko.archive17.data.remote.dto.OpenLibraryWorkDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLibraryMapperTest {
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
