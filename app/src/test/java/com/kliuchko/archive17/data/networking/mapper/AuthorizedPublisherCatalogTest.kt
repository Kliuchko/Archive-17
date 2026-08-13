package com.kliuchko.archive17.data.networking.mapper

import com.kliuchko.archive17.domain.model.FreeBookSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.HttpUrl.Companion.toHttpUrl

class AuthorizedPublisherCatalogTest {
    @Test
    fun `official AllatRa book is included on first Russian catalog page`() {
        val book = curatedAuthorizedRussianBooks(query = "", page = 1).single()

        assertEquals("АллатРа", book.title)
        assertEquals(listOf("Анастасия Новых"), book.authors)
        assertEquals(FreeBookSource.AUTHORIZED_PUBLISHER, book.source)
        assertEquals("rus", book.languageCode)
        assertEquals(
            "https://allatra-book.org/books/getfile/epub/1/ru",
            book.epubDownloadUrl,
        )
        assertTrue(book.isDownloadable)
    }

    @Test
    fun `AllatRa aliases and author find official book`() {
        listOf(
            "Аллатра",
            "аЛлАтРа",
            "Аллат Ра",
            "AllatRa",
            "Allat Ra",
            "Анастасия Новых",
            "Anastasiia Novykh",
        ).forEach { query ->
            assertEquals(query, 1, curatedAuthorizedRussianBooks(query, page = 1).size)
        }
    }

    @Test
    fun `official book is not duplicated on later pages or unrelated searches`() {
        assertTrue(curatedAuthorizedRussianBooks("", page = 2).isEmpty())
        assertTrue(curatedAuthorizedRussianBooks("Достоевский", page = 1).isEmpty())
    }

    @Test
    fun `only exact allowlisted publisher EPUB URL is trusted`() {
        assertTrue(
            isTrustedAuthorizedPublisherEpub(
                "https://allatra-book.org/books/getfile/epub/1/ru".toHttpUrl(),
            ),
        )
        assertTrue(
            !isTrustedAuthorizedPublisherEpub(
                "http://allatra-book.org/books/getfile/epub/1/ru".toHttpUrl(),
            ),
        )
        assertTrue(
            !isTrustedAuthorizedPublisherEpub(
                "https://evil.allatra-book.org/books/getfile/epub/1/ru".toHttpUrl(),
            ),
        )
        assertTrue(
            !isTrustedAuthorizedPublisherEpub(
                "https://allatra-book.org/books/getfile/epub/1/ru?redirect=evil".toHttpUrl(),
            ),
        )
    }
}
