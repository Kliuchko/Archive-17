package com.kliuchko.archive17.data.networking.mapper

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookDetails
import com.kliuchko.archive17.domain.model.FreeBookSource
import com.kliuchko.archive17.domain.model.FreeBookRights
import com.kliuchko.archive17.domain.model.FreeAccessBasis
import java.text.Normalizer
import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

fun curatedAuthorizedRussianBooks(query: String, page: Int): List<FreeBook> {
    if (page.coerceAtLeast(1) != 1) return emptyList()
    val normalizedQuery = query.toSearchKey()
    return AUTHORIZED_RUSSIAN_BOOKS
        .asSequence()
        .filter { book ->
            normalizedQuery.isEmpty() || book.searchTerms.any { term ->
                term.toSearchKey().contains(normalizedQuery)
            }
        }
        .map(AuthorizedBook::toFreeBook)
        .toList()
}

fun authorizedPublisherDetails(book: FreeBook): FreeBookDetails? {
    val record = AUTHORIZED_RUSSIAN_BOOKS.firstOrNull { it.editionId == book.editionId }
        ?: return null
    return FreeBookDetails(
        book = book,
        description = record.description,
        subjects = record.subjects,
    )
}

internal fun isTrustedAuthorizedPublisherEpub(url: HttpUrl): Boolean =
    url.isHttps && AUTHORIZED_RUSSIAN_BOOKS.any { it.epubDownloadUrl.toHttpUrl() == url }

private fun AuthorizedBook.toFreeBook() = FreeBook(
    workId = workId,
    editionId = editionId,
    title = title,
    authors = listOf(author),
    coverId = null,
    coverUrl = coverUrl,
    firstPublishYear = year,
    languageCode = RUSSIAN_LANGUAGE,
    source = FreeBookSource.AUTHORIZED_PUBLISHER,
    sourcePageUrl = sourcePageUrl,
    epubDownloadUrl = epubDownloadUrl,
    rights = FreeBookRights(FreeAccessBasis.RIGHTS_HOLDER_PERMISSION),
)

private fun String.toSearchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFKD)
    .lowercase(Locale.ROOT)
    .replace(COMBINING_MARKS, "")
    .replace(NON_ALPHANUMERIC, "")

private data class AuthorizedBook(
    val workId: String,
    val editionId: String,
    val title: String,
    val author: String,
    val year: Int,
    val coverUrl: String,
    val sourcePageUrl: String,
    val epubDownloadUrl: String,
    val aliases: List<String>,
    val description: String,
    val subjects: List<String>,
) {
    val searchTerms: List<String>
        get() = listOf(title, author) + aliases
}

private const val RUSSIAN_LANGUAGE = "rus"
private val COMBINING_MARKS = Regex("\\p{M}+")
private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

private val AUTHORIZED_RUSSIAN_BOOKS = listOf(
    AuthorizedBook(
        workId = "authorized-allatra-allatra",
        editionId = "authorized-allatra-allatra-ru",
        title = "АллатРа",
        author = "Анастасия Новых",
        year = 2013,
        coverUrl = "https://allatra-book.org/assets/uploads/images/65ec1-8.jpg",
        sourcePageUrl = "https://allatra-book.org/ru/kniga-allatra",
        epubDownloadUrl = "https://allatra-book.org/books/getfile/epub/1/ru",
        aliases = listOf(
            "Аллатра",
            "Аллат Ра",
            "AllatRa",
            "Allatra",
            "Allat Ra",
            "Anastasia Novykh",
            "Anastasiia Novykh",
        ),
        description = "Книга о внутреннем мире человека, выборе и ответственности. " +
            "Электронное издание предоставлено официальным сайтом книги.",
        subjects = listOf("Современная проза", "Философия", "Самопознание"),
    ),
)
