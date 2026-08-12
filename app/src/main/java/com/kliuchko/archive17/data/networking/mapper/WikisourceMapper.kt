package com.kliuchko.archive17.data.networking.mapper

import com.kliuchko.archive17.data.networking.dto.WikisourceSearchResponseDto
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookSource
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun WikisourceSearchResponseDto.toFreeBooks(languageCode: String): List<FreeBook> =
    query?.search.orEmpty()
        .asSequence()
        .filter { it.ns == MAIN_NAMESPACE }
        .filter { result -> result.title?.contains('/') == false }
        .mapNotNull { result ->
            val pageId = result.pageid ?: return@mapNotNull null
            val pageTitle = result.title?.trim()?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val snippet = result.snippet.toPlainText()
            val author = snippet.extractAuthor() ?: return@mapNotNull null
            FreeBook(
                workId = "wikisource-ru-$pageId",
                editionId = "wikisource-ru-$pageId",
                title = pageTitle.withoutAuthorSuffix(),
                authors = listOf(author),
                coverId = null,
                firstPublishYear = snippet.extractYear(),
                languageCode = languageCode,
                source = FreeBookSource.WIKISOURCE,
                sourcePageTitle = pageTitle,
                epubDownloadUrl = buildWikisourceEpubUrl(pageTitle),
            )
        }
        .distinctBy { it.title.lowercase() to it.authors.first().lowercase() }
        .toList()

fun curatedRussianWikisourceBooks(page: Int, pageSize: Int = 4): List<FreeBook> =
    RUSSIAN_STARTER_BOOKS
        .drop((page.coerceAtLeast(1) - 1) * pageSize)
        .take(pageSize)
        .mapIndexed { index, book ->
            val stableId = (page.coerceAtLeast(1) - 1) * pageSize + index + 1
            FreeBook(
                workId = "wikisource-ru-starter-$stableId",
                editionId = "wikisource-ru-starter-$stableId",
                title = book.title,
                authors = listOf(book.author),
                coverId = null,
                firstPublishYear = book.year,
                languageCode = RUSSIAN_LANGUAGE,
                source = FreeBookSource.WIKISOURCE,
                sourcePageTitle = book.pageTitle,
                epubDownloadUrl = buildWikisourceEpubUrl(book.pageTitle),
            )
        }

internal fun buildWikisourceEpubUrl(pageTitle: String): String {
    val encodedPage = URLEncoder.encode(pageTitle.replace(' ', '_'), StandardCharsets.UTF_8.name())
        .replace("+", "%20")
    return "$WS_EXPORT_BASE_URL?lang=ru&format=epub&page=$encodedPage"
}

private fun String?.toPlainText(): String = orEmpty()
    .replace(HTML_TAG_PATTERN, " ")
    .replace("&nbsp;", " ")
    .replace("&#160;", " ")
    .replace("&quot;", "\"")
    .replace("&amp;", "&")
    .replace(WHITESPACE_PATTERN, " ")
    .trim()

private fun String.extractAuthor(): String? = AUTHOR_PATTERN
    .find(this)
    ?.groupValues
    ?.getOrNull(1)
    ?.trim(' ', '.', ',', ';')
    ?.takeIf(String::isNotEmpty)

private fun String.extractYear(): Int? = YEAR_PATTERN
    .find(this)
    ?.value
    ?.toIntOrNull()

private fun String.withoutAuthorSuffix(): String = replace(AUTHOR_SUFFIX_PATTERN, "").trim()

private data class StarterBook(
    val title: String,
    val author: String,
    val year: Int,
    val pageTitle: String,
)

private const val MAIN_NAMESPACE = 0
private const val RUSSIAN_LANGUAGE = "rus"
private const val WS_EXPORT_BASE_URL = "https://ws-export.wmcloud.org/"
private val HTML_TAG_PATTERN = Regex("<[^>]+>")
private val WHITESPACE_PATTERN = Regex("\\s+")
private val AUTHOR_PATTERN = Regex(
    "(?:^|\\s)автор\\s+(.+?)(?=\\s(?:[12][0-9]{3}|←|→)|$)",
    RegexOption.IGNORE_CASE,
)
private val YEAR_PATTERN = Regex("\\b(?:1[0-9]{3}|20[0-2][0-9])\\b")
private val AUTHOR_SUFFIX_PATTERN = Regex("\\s+\\([^()]+\\)$")

private val RUSSIAN_STARTER_BOOKS = listOf(
    StarterBook(
        title = "Преступление и наказание",
        author = "Фёдор Михайлович Достоевский",
        year = 1866,
        pageTitle = "Преступление и наказание (Достоевский)",
    ),
    StarterBook(
        title = "Война и мир",
        author = "Лев Николаевич Толстой",
        year = 1869,
        pageTitle = "Война и мир (Толстой)",
    ),
    StarterBook(
        title = "Анна Каренина",
        author = "Лев Николаевич Толстой",
        year = 1878,
        pageTitle = "Анна Каренина (Толстой)",
    ),
    StarterBook(
        title = "Идиот",
        author = "Фёдор Михайлович Достоевский",
        year = 1869,
        pageTitle = "Идиот (Достоевский)",
    ),
    StarterBook(
        title = "Братья Карамазовы",
        author = "Фёдор Михайлович Достоевский",
        year = 1880,
        pageTitle = "Братья Карамазовы (Достоевский)",
    ),
    StarterBook(
        title = "Отцы и дети",
        author = "Иван Сергеевич Тургенев",
        year = 1862,
        pageTitle = "Отцы и дети (Тургенев)",
    ),
    StarterBook(
        title = "Герой нашего времени",
        author = "Михаил Юрьевич Лермонтов",
        year = 1840,
        pageTitle = "Герой нашего времени (Лермонтов)",
    ),
    StarterBook(
        title = "Евгений Онегин",
        author = "Александр Сергеевич Пушкин",
        year = 1833,
        pageTitle = "Евгений Онегин (Пушкин)",
    ),
    StarterBook(
        title = "Капитанская дочка",
        author = "Александр Сергеевич Пушкин",
        year = 1836,
        pageTitle = "Капитанская дочка (Пушкин)",
    ),
    StarterBook(
        title = "Мёртвые души",
        author = "Николай Васильевич Гоголь",
        year = 1842,
        pageTitle = "Мёртвые души (Гоголь)",
    ),
    StarterBook(
        title = "Обломов",
        author = "Иван Александрович Гончаров",
        year = 1859,
        pageTitle = "Обломов (Гончаров)",
    ),
    StarterBook(
        title = "Вишнёвый сад",
        author = "Антон Павлович Чехов",
        year = 1904,
        pageTitle = "Вишнёвый сад (Чехов)",
    ),
)
