package com.kliuchko.archive17.data.networking.mapper

import com.kliuchko.archive17.data.networking.dto.WikisourceSearchResponseDto
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookSource
import com.kliuchko.archive17.domain.model.FreeBookRights
import com.kliuchko.archive17.domain.model.FreeAccessBasis
import com.kliuchko.archive17.domain.model.TextEditionType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun WikisourceSearchResponseDto.toFreeBooks(languageCode: String): List<FreeBook> =
    query?.search.orEmpty()
        .asSequence()
        .filter { it.ns == MAIN_NAMESPACE }
        .mapNotNull { result ->
            val pageId = result.pageid ?: return@mapNotNull null
            val pageTitle = result.title?.trim()?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val snippet = result.snippet.toPlainText()
            val author = snippet.extractAuthor()
            val editionYear = snippet.extractYear() ?: pageTitle.extractYear()
            val isLikelyChapter = pageTitle.isLikelyChapter()
            val editionType = pageTitle.toTextEditionType()
            FreeBook(
                workId = "wikisource-ru-$pageId",
                editionId = "wikisource-ru-$pageId",
                title = pageTitle.toCatalogTitle(),
                authors = listOfNotNull(author),
                coverId = null,
                firstPublishYear = editionYear,
                languageCode = languageCode,
                editionYear = editionYear,
                translator = snippet.extractTranslator(),
                source = FreeBookSource.WIKISOURCE,
                sourcePageTitle = pageTitle,
                epubDownloadUrl = if (author != null && !isLikelyChapter) {
                    buildWikisourceEpubUrl(pageTitle)
                } else {
                    null
                },
                rights = WIKISOURCE_RIGHTS,
                textEditionType = editionType,
                editionLabel = pageTitle.toEditionLabel(),
            )
        }
        .distinctBy { it.title.lowercase() to it.authors.firstOrNull().orEmpty().lowercase() }
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
                rights = WIKISOURCE_RIGHTS,
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

private fun String.extractTranslator(): String? = TRANSLATOR_PATTERN
    .find(this)
    ?.groupValues
    ?.getOrNull(1)
    ?.trim(' ', '.', ',', ';')
    ?.takeIf(String::isNotEmpty)

private fun String.toCatalogTitle(): String = substringBefore('/')
    .replace(AUTHOR_SUFFIX_PATTERN, "")
    .trim()

private fun String.isLikelyChapter(): Boolean {
    val childPage = substringAfterLast('/', missingDelimiterValue = "")
    return childPage.isNotBlank() && (
        CHAPTER_MARKER_PATTERN.containsMatchIn(childPage) ||
            CHAPTER_NUMBER_PATTERN.matches(childPage.trim())
        )
}

private fun String.toTextEditionType(): TextEditionType = when {
    MODERN_ORTHOGRAPHY_PATTERN.containsMatchIn(this) -> TextEditionType.MODERN_ORTHOGRAPHY
    HISTORICAL_ORTHOGRAPHY_PATTERN.containsMatchIn(this) ->
        TextEditionType.HISTORICAL_ORTHOGRAPHY
    else -> TextEditionType.UNSPECIFIED
}

private fun String.toEditionLabel(): String? = substringAfter('/', missingDelimiterValue = "")
    .replace(ORTHOGRAPHY_MARKER_PATTERN, "")
    .trim(' ', '-', '—')
    .takeIf(String::isNotBlank)

private data class StarterBook(
    val title: String,
    val author: String,
    val year: Int,
    val pageTitle: String,
)

private const val MAIN_NAMESPACE = 0
private const val RUSSIAN_LANGUAGE = "rus"
private const val WS_EXPORT_BASE_URL = "https://ws-export.wmcloud.org/"
private val WIKISOURCE_RIGHTS = FreeBookRights(
    basis = FreeAccessBasis.OPEN_LICENSE,
    licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0/",
)
private val HTML_TAG_PATTERN = Regex("<[^>]+>")
private val WHITESPACE_PATTERN = Regex("\\s+")
private val AUTHOR_PATTERN = Regex(
    "(?:^|\\s)автор(?:ъ|ом)?\\s+(.+?)(?=\\s*,?\\s*(?:пер\\.|перевод|[12][0-9]{3}|←|→)|$)",
    RegexOption.IGNORE_CASE,
)
private val YEAR_PATTERN = Regex("\\b(?:1[0-9]{3}|20[0-2][0-9])\\b")
private val TRANSLATOR_PATTERN = Regex(
    "(?:пер\\.|перевод(?:чик|чики|а)?)\\s*[:.]?\\s*(.+?)" +
        "(?=\\s*,?\\s*(?:[12][0-9]{3}|←|→)|$)",
    RegexOption.IGNORE_CASE,
)
private val AUTHOR_SUFFIX_PATTERN = Regex("\\s+\\([^()]+\\)$")
private val CHAPTER_MARKER_PATTERN = Regex(
    "(?:^|\\s)(?:глава|chapter|сцена|scene|песнь|song)\\b",
    RegexOption.IGNORE_CASE,
)
private val CHAPTER_NUMBER_PATTERN = Regex("(?:[IVXLCDM]+|[0-9]+)[.]?", RegexOption.IGNORE_CASE)
private val MODERN_ORTHOGRAPHY_PATTERN = Regex("\\(СО\\)", RegexOption.IGNORE_CASE)
private val HISTORICAL_ORTHOGRAPHY_PATTERN = Regex("\\(ДО\\)", RegexOption.IGNORE_CASE)
private val ORTHOGRAPHY_MARKER_PATTERN = Regex("\\s*\\((?:СО|ДО)\\)\\s*", RegexOption.IGNORE_CASE)

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
