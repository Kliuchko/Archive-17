package com.kliuchko.archive17.domain.model

data class FreeBook(
    val workId: String,
    val editionId: String,
    val title: String,
    val authors: List<String>,
    val coverId: Int?,
    val firstPublishYear: Int?,
    val languageCode: String,
    val archiveIdentifier: String,
    val epubFileName: String? = null,
    val epubSizeBytes: Long? = null,
) {
    val sourceUrl: String
        get() = "https://openlibrary.org/books/$editionId"
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
