package com.kliuchko.archive17.data.local.mapper

import com.kliuchko.archive17.data.local.entity.EditionEntity
import com.kliuchko.archive17.data.local.entity.LibraryEntryEntity
import com.kliuchko.archive17.data.local.entity.WorkEntity
import com.kliuchko.archive17.data.local.relation.LibraryEntryWithWorkEntity
import com.kliuchko.archive17.domain.model.Edition
import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LibraryEntry
import com.kliuchko.archive17.domain.model.Work

fun Work.toEntity(now: Long): WorkEntity =
    WorkEntity(
        id = id,
        title = title,
        authors = authors,
        coverId = coverId,
        firstPublishYear = firstPublishYear,
        editionCount = editionCount,
        editionLanguages = editionLanguages,
        description = description,
        subjects = subjects,
        lastUpdatedAt = lastUpdatedAt ?: now,
    )

fun WorkEntity.toDomain(): Work =
    Work(
        id = id,
        title = title,
        authors = authors,
        coverId = coverId,
        firstPublishYear = firstPublishYear,
        editionCount = editionCount,
        editionLanguages = editionLanguages,
        description = description,
        subjects = subjects,
        lastUpdatedAt = lastUpdatedAt,
    )

fun Edition.toEntity(): EditionEntity =
    EditionEntity(
        id = id ?: buildEditionId(workId, languageCode, title),
        workId = workId,
        title = title,
        languageCode = languageCode,
    )

fun EditionEntity.toDomain(): Edition =
    Edition(
        id = id,
        workId = workId,
        title = title,
        languageCode = languageCode,
    )

fun LibraryEntry.toEntity(): LibraryEntryEntity =
    LibraryEntryEntity(
        workId = workId,
        readingStatus = readingStatus,
        savedAt = savedAt,
        updatedAt = updatedAt,
    )

fun LibraryEntryEntity.toDomain(): LibraryEntry =
    LibraryEntry(
        workId = workId,
        readingStatus = readingStatus,
        savedAt = savedAt,
        updatedAt = updatedAt,
    )

fun LibraryEntryWithWorkEntity.toDomain(): LibraryBook =
    LibraryBook(
        work = work.toDomain(),
        entry = entry.toDomain(),
    )

private fun buildEditionId(
    workId: String,
    languageCode: String?,
    title: String?,
): String = listOf(workId, languageCode.orEmpty(), title.orEmpty()).joinToString(":")
