package com.kliuchko.archive17.data.networking.mapper

import com.kliuchko.archive17.data.networking.dto.WikisourceQueryDto
import com.kliuchko.archive17.data.networking.dto.WikisourceSearchResponseDto
import com.kliuchko.archive17.data.networking.dto.WikisourceSearchResultDto
import com.kliuchko.archive17.domain.model.FreeBookSource
import com.kliuchko.archive17.domain.model.TextEditionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WikisourceMapperTest {
    @Test
    fun `maps top-level work with author to downloadable epub`() {
        val response = WikisourceSearchResponseDto(
            query = WikisourceQueryDto(
                search = listOf(
                    WikisourceSearchResultDto(
                        pageid = 6074,
                        ns = 0,
                        title = "Преступление и наказание (Достоевский)",
                        snippet = "<span>Преступление и наказание</span> автор " +
                            "Фёдор Михайлович Достоевский 1866",
                    ),
                ),
            ),
        )

        val book = response.toFreeBooks("rus").single()

        assertEquals("Преступление и наказание", book.title)
        assertEquals(listOf("Фёдор Михайлович Достоевский"), book.authors)
        assertEquals(1866, book.firstPublishYear)
        assertEquals(FreeBookSource.WIKISOURCE, book.source)
        assertTrue(book.isDownloadable)
        assertTrue(book.epubDownloadUrl.orEmpty().contains("format=epub"))
        assertTrue(book.sourceUrl.contains("%D0%9F%D1%80%D0%B5%D1%81%D1%82%D1%83%D0%BF"))
        assertFalse(book.sourceUrl.contains(' '))
    }

    @Test
    fun `keeps uncertain pages but does not mark chapters as downloadable books`() {
        val response = WikisourceSearchResponseDto(
            query = WikisourceQueryDto(
                search = listOf(
                    WikisourceSearchResultDto(
                        pageid = 1,
                        ns = 0,
                        title = "Роман/Глава I",
                        snippet = "Роман автор Автор",
                    ),
                    WikisourceSearchResultDto(
                        pageid = 2,
                        ns = 0,
                        title = "Значения слова",
                        snippet = "Страница значений без метаданных",
                    ),
                ),
            ),
        )

        val books = response.toFreeBooks("rus")

        assertEquals(2, books.size)
        assertTrue(books.none { it.isDownloadable })
        assertTrue(books.any { it.authors.isEmpty() })
    }

    @Test
    fun `maps a full edition subpage with old spelling author metadata`() {
        val response = WikisourceSearchResponseDto(
            query = WikisourceQueryDto(
                search = listOf(
                    WikisourceSearchResultDto(
                        pageid = 1013142,
                        ns = 0,
                        title = "Оливер Твист (Диккенс; Волошинова, Ясинский)/ПСС 1909 (ДО)",
                        snippet = "Оливер Твист авторъ Чарльз Диккенс, пер. М. П. Волошинова",
                    ),
                ),
            ),
        )

        val book = response.toFreeBooks("rus").single()

        assertEquals("Оливер Твист", book.title)
        assertEquals(listOf("Чарльз Диккенс"), book.authors)
        assertEquals(1909, book.firstPublishYear)
        assertEquals(1909, book.editionYear)
        assertEquals("М. П. Волошинова", book.translator)
        assertEquals(TextEditionType.HISTORICAL_ORTHOGRAPHY, book.textEditionType)
        assertEquals("ПСС 1909", book.editionLabel)
        assertTrue(book.isDownloadable)
        assertTrue(book.epubDownloadUrl.orEmpty().contains("%2F"))
    }

    @Test
    fun `starter catalog contains real russian classics across pages`() {
        val firstPage = curatedRussianWikisourceBooks(page = 1)
        val thirdPage = curatedRussianWikisourceBooks(page = 3)

        assertEquals(4, firstPage.size)
        assertEquals("Преступление и наказание", firstPage.first().title)
        assertEquals(4, thirdPage.size)
        assertTrue(thirdPage.all { it.source == FreeBookSource.WIKISOURCE })
        assertFalse(firstPage.map { it.editionId }.intersect(thirdPage.map { it.editionId }.toSet()).isNotEmpty())
    }
}
