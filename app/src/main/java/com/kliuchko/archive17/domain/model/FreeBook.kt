package com.kliuchko.archive17.domain.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class FreeBook(
    val workId: String,
    val editionId: String,
    val title: String,
    val authors: List<String>,
    val coverId: Int?,
    val firstPublishYear: Int?,
    val languageCode: String,
    val archiveIdentifier: String = "",
    val epubFileName: String? = null,
    val epubSizeBytes: Long? = null,
    val source: FreeBookSource = FreeBookSource.OPEN_LIBRARY,
    val sourcePageTitle: String? = null,
    val epubDownloadUrl: String? = null,
) {
    val sourceName: String
        get() = when (source) {
            FreeBookSource.OPEN_LIBRARY -> "Open Library · Internet Archive"
            FreeBookSource.WIKISOURCE -> "Викитека · Wikimedia"
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
        }

    val isDownloadable: Boolean
        get() = epubFileName != null || epubDownloadUrl != null
}

enum class FreeBookSource {
    OPEN_LIBRARY,
    WIKISOURCE,
}

data class FreeBookDetails(
    val book: FreeBook,
    val description: String?,
    val subjects: List<String>,
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
