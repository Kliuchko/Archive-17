package com.kliuchko.archive17.domain.model

data class LocalBook(
    val id: String,
    val title: String,
    val author: String?,
    val identifier: String?,
    val filePath: String,
    val coverPath: String?,
    val progressionJson: String?,
    val readingStatus: ReadingStatus,
    val addedAt: Long,
    val updatedAt: Long,
)
