package com.kliuchko.archive17.domain.model

data class LocalBook(
    val id: String,
    val workId: String? = null,
    val title: String,
    val author: String?,
    val identifier: String?,
    val contentHash: String?,
    val filePath: String,
    val coverPath: String?,
    val progressionJson: String?,
    val readingStatus: ReadingStatus,
    val addedAt: Long,
    val updatedAt: Long,
    val languageCode: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val isPublicAccess: Boolean = false,
)
