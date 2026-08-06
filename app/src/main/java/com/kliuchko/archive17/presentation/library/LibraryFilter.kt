package com.kliuchko.archive17.presentation.library

import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.presentation.details.displayName

enum class LibraryFilter(
    val readingStatus: ReadingStatus?,
) {
    ALL(readingStatus = null),
    WANT_TO_READ(readingStatus = ReadingStatus.WANT_TO_READ),
    READING(readingStatus = ReadingStatus.READING),
    FINISHED(readingStatus = ReadingStatus.FINISHED),
}

fun LibraryFilter.displayName(): String =
    readingStatus?.displayName() ?: "Все"
