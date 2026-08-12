package com.kliuchko.archive17.data.local.mapper

import com.kliuchko.archive17.data.local.entity.LocalBookEntity
import com.kliuchko.archive17.domain.model.LocalBook

fun LocalBookEntity.toDomain(): LocalBook = LocalBook(
    id = id,
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
)

fun LocalBook.toEntity(): LocalBookEntity = LocalBookEntity(
    id = id,
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
)
