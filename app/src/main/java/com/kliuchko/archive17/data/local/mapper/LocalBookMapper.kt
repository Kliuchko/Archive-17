package com.kliuchko.archive17.data.local.mapper

import com.kliuchko.archive17.data.local.entity.LocalBookEntity
import com.kliuchko.archive17.domain.model.LocalBook

fun LocalBookEntity.toDomain(): LocalBook = LocalBook(
    id = id,
    workId = workId,
    title = title,
    author = author,
    identifier = identifier,
    contentHash = contentHash,
    filePath = filePath,
    coverPath = coverPath,
    progressionJson = progressionJson,
    readingStatus = readingStatus,
    addedAt = addedAt,
    updatedAt = updatedAt,
    languageCode = languageCode,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    isPublicAccess = isPublicAccess,
)

fun LocalBook.toEntity(): LocalBookEntity = LocalBookEntity(
    id = id,
    workId = workId,
    title = title,
    author = author,
    identifier = identifier,
    contentHash = contentHash,
    filePath = filePath,
    coverPath = coverPath,
    progressionJson = progressionJson,
    readingStatus = readingStatus,
    addedAt = addedAt,
    updatedAt = updatedAt,
    languageCode = languageCode,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    isPublicAccess = isPublicAccess,
)
