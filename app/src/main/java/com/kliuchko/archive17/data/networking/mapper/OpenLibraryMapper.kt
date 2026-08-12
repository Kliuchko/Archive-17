package com.kliuchko.archive17.data.networking.mapper

import com.google.gson.JsonElement
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchDocDto
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchResponseDto
import com.kliuchko.archive17.data.networking.dto.OpenLibraryWorkDto
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.FreeBook

fun OpenLibrarySearchResponseDto.toDomain(): List<Work> = docs.mapNotNull { it.toDomain() }

fun OpenLibrarySearchResponseDto.toFreeBooks(expectedLanguageCode: String? = null): List<FreeBook> =
    docs.mapNotNull { document ->
        val workId = document.key.toWorkId() ?: return@mapNotNull null
        val edition = document.editions
            ?.docs
            .orEmpty()
            .firstOrNull {
                it.ebookAccess == PUBLIC_ACCESS &&
                    !it.key.isNullOrBlank() &&
                    !it.archiveIdentifiers.isNullOrEmpty() &&
                    (expectedLanguageCode == null || it.languages.orEmpty().contains(expectedLanguageCode))
            }
            ?: return@mapNotNull null
        val title = edition.title.normalize() ?: document.title.normalize() ?: return@mapNotNull null
        val editionId = edition.key
            ?.removePrefix("/books/")
            ?.takeIf(String::isNotBlank)
            ?: return@mapNotNull null
        val archiveIdentifier = edition.archiveIdentifiers
            ?.firstOrNull()
            ?.normalize()
            ?: return@mapNotNull null
        val languageCode = expectedLanguageCode
            ?: edition.languages.normalizeList().firstOrNull()
            ?: return@mapNotNull null

        FreeBook(
            workId = workId,
            editionId = editionId,
            title = title,
            authors = document.authorNames.normalizeList(),
            coverId = document.coverId,
            languageCode = languageCode,
            archiveIdentifier = archiveIdentifier,
        )
    }

fun OpenLibrarySearchDocDto.toDomain(lastUpdatedAt: Long? = null): Work? {
    val workId = key.toWorkId() ?: return null
    val normalizedTitle = title.normalize() ?: return null

    return Work(
        id = workId,
        title = normalizedTitle,
        authors = authorNames.normalizeList(),
        coverId = coverId,
        firstPublishYear = firstPublishYear,
        editionCount = editionCount,
        editionLanguages = languages.normalizeList(),
        description = null,
        subjects = emptyList(),
        lastUpdatedAt = lastUpdatedAt,
    )
}

fun OpenLibraryWorkDto.toDomain(
    fallback: Work? = null,
    lastUpdatedAt: Long? = null,
): Work? {
    val workId = key.toWorkId() ?: fallback?.id ?: return null
    val normalizedTitle = title.normalize() ?: fallback?.title ?: return null

    return Work(
        id = workId,
        title = normalizedTitle,
        authors = fallback?.authors.orEmpty(),
        coverId = fallback?.coverId,
        firstPublishYear = fallback?.firstPublishYear,
        editionCount = fallback?.editionCount,
        editionLanguages = fallback?.editionLanguages.orEmpty(),
        description = description.toDescription(),
        subjects = subjects.normalizeList(),
        lastUpdatedAt = lastUpdatedAt ?: fallback?.lastUpdatedAt,
    )
}

internal fun String?.toWorkId(): String? {
    val normalized = normalize() ?: return null
    return normalized.removePrefix("/works/").takeIf { it.isNotBlank() }
}

private fun String?.normalize(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun List<String>?.normalizeList(): List<String> =
    orEmpty()
        .mapNotNull { it.normalize() }
        .distinct()

private fun JsonElement?.toDescription(): String? {
    if (this == null || isJsonNull) return null

    return when {
        isJsonPrimitive -> asJsonPrimitive.takeIf { it.isString }?.asString.normalize()
        isJsonObject -> asJsonObject.get("value")?.takeIf { it.isJsonPrimitive }?.asString.normalize()
        else -> null
    }
}

private const val PUBLIC_ACCESS = "public"
