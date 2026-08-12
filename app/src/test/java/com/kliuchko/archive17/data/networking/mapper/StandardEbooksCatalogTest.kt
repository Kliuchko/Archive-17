package com.kliuchko.archive17.data.networking.mapper

import com.kliuchko.archive17.domain.model.FreeBookSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StandardEbooksCatalogTest {
    @Test
    fun `starter catalog contains twelve downloadable english editions`() {
        val books = (1..3).flatMap { page ->
            curatedEnglishStandardEbooks(query = "", page = page)
        }

        assertEquals(12, books.size)
        assertEquals(12, books.map { it.editionId }.distinct().size)
        assertTrue(books.all { it.languageCode == "eng" })
        assertTrue(books.all { it.source == FreeBookSource.STANDARD_EBOOKS })
        assertTrue(books.all { it.isDownloadable })
        assertTrue(books.all { it.coverUrl?.endsWith("/downloads/cover.jpg") == true })
        assertTrue(books.all { it.epubDownloadUrl?.endsWith("?source=download") == true })
    }

    @Test
    fun `search matches title and author without a network catalog request`() {
        val byTitle = curatedEnglishStandardEbooks(query = "franken", page = 1)
        val byAuthor = curatedEnglishStandardEbooks(query = "austen", page = 1)

        assertEquals(listOf("Frankenstein"), byTitle.map { it.title })
        assertEquals(listOf("Pride and Prejudice"), byAuthor.map { it.title })
        assertTrue(curatedEnglishStandardEbooks(query = "missing", page = 1).isEmpty())
    }

    @Test
    fun `catalog record provides source page and editorial details`() {
        val book = curatedEnglishStandardEbooks(query = "Pride", page = 1).single()
        val details = standardEbookDetails(book)

        assertEquals("https://standardebooks.org/ebooks/jane-austen/pride-and-prejudice", book.sourceUrl)
        assertFalse(book.sourceUrl.contains(' '))
        assertNotNull(details?.description)
        assertTrue(details?.subjects.orEmpty().isNotEmpty())
    }

    @Test
    fun `catalog title key ignores articles punctuation and spacing`() {
        val book = curatedEnglishStandardEbooks(query = "Huckleberry", page = 1).single()

        assertEquals("adventures of huckleberry finn", book.catalogTitleKey)
        assertEquals(
            curatedEnglishStandardEbooks(query = "Moby", page = 1).single().catalogTitleKey,
            "moby dick",
        )
    }
}
