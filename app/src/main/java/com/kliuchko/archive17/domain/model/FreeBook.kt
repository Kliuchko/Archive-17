package com.kliuchko.archive17.domain.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class FreeBook(
    val workId: String,
    val editionId: String,
    val title: String,
    val authors: List<String>,
    val coverId: Int?,
    val coverUrl: String? = null,
    val firstPublishYear: Int?,
    val languageCode: String,
    val archiveIdentifier: String = "",
    val epubFileName: String? = null,
    val epubSizeBytes: Long? = null,
    val source: FreeBookSource = FreeBookSource.OPEN_LIBRARY,
    val sourcePageTitle: String? = null,
    val sourcePageUrl: String? = null,
    val epubDownloadUrl: String? = null,
) {
    val sourceName: String
        get() = when (source) {
            FreeBookSource.OPEN_LIBRARY -> "Open Library · Internet Archive"
            FreeBookSource.WIKISOURCE -> "Викитека · Wikimedia"
            FreeBookSource.STANDARD_EBOOKS -> "Standard Ebooks"
            FreeBookSource.AUTHORIZED_PUBLISHER -> "Официальный сайт книги"
        }

    val sourceUrl: String
        get() = when (source) {
            FreeBookSource.OPEN_LIBRARY -> "https://openlibrary.org/books/$editionId"
            FreeBookSource.WIKISOURCE -> {
                val page = URLEncoder.encode(
                    sourcePageTitle.orEmpty().replace(' ', '_'),
                    StandardCharsets.UTF_8.name(),
                ).replace("+", "%20")
                "https://ru.wikisource.org/wiki/$page"
            }
            FreeBookSource.STANDARD_EBOOKS -> sourcePageUrl.orEmpty()
            FreeBookSource.AUTHORIZED_PUBLISHER -> sourcePageUrl.orEmpty()
        }

    val isDownloadable: Boolean
        get() = epubFileName != null || epubDownloadUrl != null

    val catalogTitleKey: String
        get() {
            val normalized = title
                .lowercase(Locale.ROOT)
                .replace(NON_TITLE_CHARACTER_PATTERN, " ")
                .trim()
                .replace(WHITESPACE_PATTERN, " ")
            return ENGLISH_ARTICLES.firstNotNullOfOrNull { article ->
                normalized.removePrefix("$article ").takeIf { it != normalized }
            } ?: normalized
        }
}

enum class FreeBookSource {
    OPEN_LIBRARY,
    WIKISOURCE,
    STANDARD_EBOOKS,
    AUTHORIZED_PUBLISHER,
}

private val NON_TITLE_CHARACTER_PATTERN = Regex("[^\\p{L}\\p{N}]+")
private val WHITESPACE_PATTERN = Regex("\\s+")
private val ENGLISH_ARTICLES = listOf("the", "an", "a")

data class FreeBookDetails(
    val book: FreeBook,
    val description: String?,
    val subjects: List<String>,
)

data class TemporaryBook(
    val editionId: String,
    val title: String,
    val filePath: String,
)

data class DownloadedBookMetadata(
    val title: String,
    val author: String?,
    val identifier: String,
    val languageCode: String,
    val sourceName: String,
    val sourceUrl: String,
    val isPublicAccess: Boolean,
)
