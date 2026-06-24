package com.kliuchko.archive17.domain.model

data class LibraryEntry(
    val workId: String,
    val readingStatus: ReadingStatus,
    val savedAt: Long,
    val updatedAt: Long,
)
