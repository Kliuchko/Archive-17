package com.kliuchko.archive17.data.networking

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.Work
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Reuses cover metadata that was already received for the same book. This never performs network
 * requests. A title match is accepted only together with an exact normalized author match.
 */
internal class CoverMatchEnricher {
    private val freeCoversByWorkId = ConcurrentHashMap<String, FreeCover>()
    private val freeCoversByTitleAndAuthor = ConcurrentHashMap<BookKey, FreeCover>()
    private val workCoversById = ConcurrentHashMap<String, Int>()
    private val workCoversByTitleAndAuthor = ConcurrentHashMap<BookKey, Int>()

    fun enrichFreeBooks(books: List<FreeBook>): List<FreeBook> {
        books.filter { it.hasCover() }.forEach(::remember)
        return books.map { book ->
            if (book.hasCover()) {
                book
            } else {
                findFreeCover(book)?.let { cover ->
                    book.copy(coverId = cover.coverId, coverUrl = cover.coverUrl)
                } ?: book
            }
        }.also { enriched -> enriched.filter { it.hasCover() }.forEach(::remember) }
    }

    fun enrichWorks(works: List<Work>): List<Work> {
        works.filter { it.coverId != null }.forEach(::remember)
        return works.map { work ->
            if (work.coverId != null) {
                work
            } else {
                findWorkCover(work)?.let { coverId -> work.copy(coverId = coverId) } ?: work
            }
        }.also { enriched -> enriched.filter { it.coverId != null }.forEach(::remember) }
    }

    private fun remember(book: FreeBook) {
        val cover = FreeCover(book.coverId, book.coverUrl?.takeIf(String::isNotBlank))
        book.workId.normalizedId()?.let { freeCoversByWorkId.putIfAbsent(it, cover) }
        book.bookKeys().forEach { key -> freeCoversByTitleAndAuthor.putIfAbsent(key, cover) }
    }

    private fun remember(work: Work) {
        val coverId = work.coverId ?: return
        work.id.normalizedId()?.let { workCoversById.putIfAbsent(it, coverId) }
        work.bookKeys().forEach { key -> workCoversByTitleAndAuthor.putIfAbsent(key, coverId) }
    }

    private fun findFreeCover(book: FreeBook): FreeCover? =
        book.workId.normalizedId()?.let(freeCoversByWorkId::get)
            ?: book.bookKeys().firstNotNullOfOrNull(freeCoversByTitleAndAuthor::get)

    private fun findWorkCover(work: Work): Int? =
        work.id.normalizedId()?.let(workCoversById::get)
            ?: work.bookKeys().firstNotNullOfOrNull(workCoversByTitleAndAuthor::get)

    private fun FreeBook.hasCover(): Boolean = coverId != null || !coverUrl.isNullOrBlank()

    private fun FreeBook.bookKeys(): List<BookKey> = bookKeys(title, authors)

    private fun Work.bookKeys(): List<BookKey> = bookKeys(title, authors)

    private fun bookKeys(title: String, authors: List<String>): List<BookKey> {
        val normalizedTitle = title.normalizedMetadata().takeIf(String::isNotBlank)
            ?: return emptyList()
        return authors
            .map { author -> author.normalizedMetadata() }
            .filter(String::isNotBlank)
            .distinct()
            .map { author -> BookKey(normalizedTitle, author) }
    }

    private fun String.normalizedId(): String? = trim()
        .lowercase(Locale.ROOT)
        .takeIf(String::isNotBlank)

    private fun String.normalizedMetadata(): String = Normalizer
        .normalize(lowercase(Locale.ROOT).replace('ё', 'е'), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace(NON_ALPHANUMERIC, " ")
        .trim()
        .replace(WHITESPACE, " ")

    private data class FreeCover(val coverId: Int?, val coverUrl: String?)

    private data class BookKey(val title: String, val author: String)

    private companion object {
        val COMBINING_MARKS = Regex("\\p{M}+")
        val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
        val WHITESPACE = Regex("\\s+")
    }
}
