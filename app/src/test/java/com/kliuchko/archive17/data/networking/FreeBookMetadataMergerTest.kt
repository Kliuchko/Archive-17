package com.kliuchko.archive17.data.networking

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookSource
import com.kliuchko.archive17.domain.repository.BookQueryResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeBookMetadataMergerTest {
    private val resolver = FakeBookQueryResolver(
        mapOf(
            "Война и мир" to listOf("Война и мир", "War and Peace"),
            "Чарльз Диккенс" to listOf("Чарльз Диккенс", "Charles Dickens"),
            "Дон Кихот" to listOf("Дон Кихот", "Don Quixote"),
            "Мигель де Сервантес" to listOf("Мигель де Сервантес", "Miguel de Cervantes"),
        ),
    )
    private val merger = FreeBookMetadataMerger(resolver, CoverMatchEnricher())

    @Test
    fun `keeps translated editions and reuses cover for the readable edition`() = runBlocking {
        val readable = book(
            editionId = "wikisource-war",
            title = "Война и мир",
            author = "Лев Николаевич Толстой",
            source = FreeBookSource.WIKISOURCE,
            downloadUrl = "https://ws-export.wmcloud.org/war.epub",
        )
        val metadata = book(
            editionId = "openlibrary-war",
            title = "War and Peace",
            author = "Лев Толстой",
            source = FreeBookSource.OPEN_LIBRARY,
            coverId = 12621906,
        )

        val result = merger.merge(listOf(readable, metadata))

        assertEquals(2, result.size)
        val readableResult = result.single { it.editionId == "wikisource-war" }
        assertEquals(12621906, readableResult.coverId)
        assertEquals("openlibrary-war", readableResult.workId)
        assertTrue(readableResult.isDownloadable)
    }

    @Test
    fun `merges translated author names for the same title`() = runBlocking {
        val readable = book(
            editionId = "wikisource-oliver",
            title = "Оливер Твист",
            author = "Чарльз Диккенс",
            source = FreeBookSource.WIKISOURCE,
            downloadUrl = "https://ws-export.wmcloud.org/oliver.epub",
        )
        val metadata = book(
            editionId = "openlibrary-oliver",
            title = "Оливер Твист",
            author = "Charles Dickens",
            source = FreeBookSource.OPEN_LIBRARY,
            coverId = 9280636,
        )

        val result = merger.merge(listOf(readable, metadata))

        assertEquals(2, result.size)
        assertEquals(9280636, result.single { it.editionId == "wikisource-oliver" }.coverId)
    }

    @Test
    fun `does not merge books with the same title by different authors`() = runBlocking {
        val first = book(
            editionId = "first",
            title = "Дом",
            author = "Первый Автор",
            source = FreeBookSource.WIKISOURCE,
            downloadUrl = "https://ws-export.wmcloud.org/first.epub",
        )
        val second = book(
            editionId = "second",
            title = "Дом",
            author = "Другой Писатель",
            source = FreeBookSource.OPEN_LIBRARY,
            coverId = 42,
        )

        assertEquals(2, merger.merge(listOf(first, second)).size)
    }

    @Test
    fun `uncertain source page is retained beside a full edition`() = runBlocking {
        val indexPage = book(
            editionId = "wikisource-index",
            title = "Оливер Твист",
            author = "",
            source = FreeBookSource.WIKISOURCE,
        )
        val fullEdition = book(
            editionId = "wikisource-full",
            title = "Оливер Твист",
            author = "Чарльз Диккенс",
            source = FreeBookSource.WIKISOURCE,
            downloadUrl = "https://ws-export.wmcloud.org/oliver.epub",
        )

        val result = merger.merge(listOf(indexPage, fullEdition))

        assertEquals(2, result.size)
        assertTrue(result.single { it.editionId == "wikisource-full" }.isDownloadable)
    }

    @Test
    fun `merges when both title and author use another writing system`() = runBlocking {
        val readable = book(
            editionId = "wikisource-quixote",
            title = "Дон Кихот",
            author = "Мигель де Сервантес",
            source = FreeBookSource.WIKISOURCE,
            downloadUrl = "https://ws-export.wmcloud.org/quixote.epub",
        )
        val metadata = book(
            editionId = "openlibrary-quixote",
            title = "Don Quixote",
            author = "Miguel de Cervantes",
            source = FreeBookSource.OPEN_LIBRARY,
            coverId = 77,
        )

        val result = merger.merge(listOf(metadata, readable))

        assertEquals(2, result.size)
        assertEquals(77, result.single { it.editionId == "wikisource-quixote" }.coverId)
    }

    @Test
    fun `all source editions inherit the canonical work id discovered later`() = runBlocking {
        val firstTranslation = book(
            editionId = "wikisource-1841",
            title = "Оливер Твист",
            author = "Чарльз Диккенс",
            source = FreeBookSource.WIKISOURCE,
            downloadUrl = "https://ws-export.wmcloud.org/1841.epub",
        )
        val secondTranslation = book(
            editionId = "wikisource-1909",
            title = "Оливер Твист",
            author = "Чарльз Диккенс",
            source = FreeBookSource.WIKISOURCE,
            downloadUrl = "https://ws-export.wmcloud.org/1909.epub",
        )
        val canonicalWork = book(
            editionId = "openlibrary-oliver",
            title = "Оливер Твист",
            author = "Charles Dickens",
            source = FreeBookSource.OPEN_LIBRARY,
            coverId = 9280636,
        )

        val result = merger.merge(listOf(firstTranslation, secondTranslation, canonicalWork))

        assertEquals(3, result.size)
        assertTrue(result.all { it.workId == "openlibrary-oliver" })
    }

    private fun book(
        editionId: String,
        title: String,
        author: String,
        source: FreeBookSource,
        coverId: Int? = null,
        downloadUrl: String? = null,
    ) = FreeBook(
        workId = editionId,
        editionId = editionId,
        title = title,
        authors = listOf(author).filter(String::isNotBlank),
        coverId = coverId,
        firstPublishYear = null,
        languageCode = "rus",
        source = source,
        epubDownloadUrl = downloadUrl,
    )
}

private class FakeBookQueryResolver(
    private val values: Map<String, List<String>>,
) : BookQueryResolver {
    override suspend fun resolve(query: String, preferredLanguageCode: String?): List<String> =
        values[query].orEmpty().ifEmpty { listOf(query) }
}
