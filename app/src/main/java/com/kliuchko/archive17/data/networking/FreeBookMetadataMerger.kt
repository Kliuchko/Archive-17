package com.kliuchko.archive17.data.networking

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookSource
import com.kliuchko.archive17.domain.repository.BookQueryResolver
import java.text.Normalizer
import java.util.Locale

/**
 * Joins the same readable work across catalog providers without relying on a per-title allowlist.
 * Direct metadata matches are preferred; translated title or author labels are requested only for
 * plausible cross-provider pairs.
 */
internal class FreeBookMetadataMerger(
    private val queryResolver: BookQueryResolver,
    private val coverMatchEnricher: CoverMatchEnricher,
) {
    suspend fun merge(books: List<FreeBook>): List<FreeBook> {
        val result = mutableListOf<FreeBook>()
        coverMatchEnricher.enrichFreeBooks(books).forEach { candidate ->
            val existingIndex = result.indexOfFirst { existing ->
                areEquivalent(existing, candidate)
            }
            if (existingIndex < 0) {
                result += candidate
            } else {
                val existing = result[existingIndex]
                val sharedWorkId = sharedWorkId(existing, candidate)
                val enrichedExisting = existing
                    .withCoverFrom(candidate)
                    .copy(workId = sharedWorkId)
                val enrichedCandidate = candidate
                    .withCoverFrom(existing)
                    .copy(workId = sharedWorkId)
                result[existingIndex] = enrichedExisting
                if (candidate.editionId != existing.editionId) {
                    result += enrichedCandidate
                } else {
                    result[existingIndex] = mergeSameEdition(enrichedExisting, enrichedCandidate)
                }
            }
        }
        return coverMatchEnricher.enrichFreeBooks(result)
    }

    private suspend fun areEquivalent(left: FreeBook, right: FreeBook): Boolean {
        if (left.editionId == right.editionId) return true
        if (left.workId.isNotBlank() && left.workId == right.workId) return true
        if (left.source == FreeBookSource.OPEN_LIBRARY && right.source == FreeBookSource.OPEN_LIBRARY) {
            return false
        }

        val titlesMatch = left.title.toTitleKey() == right.title.toTitleKey()
        val authorsMatch = authorsMatch(left.authors, right.authors)
        if (titlesMatch && authorsMatch) return true
        if (
            titlesMatch &&
            left.source == right.source &&
            (left.authors.isEmpty() || right.authors.isEmpty())
        ) {
            return true
        }
        if (left.authors.isEmpty() || right.authors.isEmpty()) return false

        return when {
            titlesMatch -> resolvedValues(left.authors.first(), left.languageCode)
                .any { alias -> right.authors.any { author -> namesMatch(alias, author) } }

            authorsMatch -> resolvedValues(left.title, left.languageCode)
                .any { alias -> alias.toTitleKey() == right.title.toTitleKey() }

            isCrossProviderCoverCandidate(left, right) -> {
                val directBook = if (left.source != FreeBookSource.OPEN_LIBRARY) left else right
                val metadataBook = if (directBook === left) right else left
                val translatedTitleMatches = resolvedValues(
                    directBook.title,
                    directBook.languageCode,
                ).any { alias -> alias.toTitleKey() == metadataBook.title.toTitleKey() }
                val translatedAuthorMatches = resolvedValues(
                    directBook.authors.first(),
                    directBook.languageCode,
                ).any { alias ->
                    metadataBook.authors.any { author -> namesMatch(alias, author) }
                }
                translatedTitleMatches && translatedAuthorMatches
            }

            else -> false
        }
    }

    private fun isCrossProviderCoverCandidate(left: FreeBook, right: FreeBook): Boolean =
        left.source != right.source &&
            (left.source == FreeBookSource.OPEN_LIBRARY || right.source == FreeBookSource.OPEN_LIBRARY) &&
            sequenceOf(left, right).any { it.coverId != null || !it.coverUrl.isNullOrBlank() } &&
            sequenceOf(left, right).any { it.isDownloadable }

    private fun mergeSameEdition(left: FreeBook, right: FreeBook): FreeBook {
        val preferred = when {
            left.isDownloadable && !right.isDownloadable -> left
            right.isDownloadable && !left.isDownloadable -> right
            left.source != FreeBookSource.OPEN_LIBRARY && right.source == FreeBookSource.OPEN_LIBRARY -> left
            right.source != FreeBookSource.OPEN_LIBRARY && left.source == FreeBookSource.OPEN_LIBRARY -> right
            else -> left
        }
        return preferred.withCoverFrom(if (preferred === left) right else left)
    }

    private fun FreeBook.withCoverFrom(other: FreeBook): FreeBook = copy(
        coverId = coverId ?: other.coverId,
        coverUrl = coverUrl ?: other.coverUrl,
    )

    private fun sharedWorkId(left: FreeBook, right: FreeBook): String = when {
        left.source == FreeBookSource.OPEN_LIBRARY -> left.workId
        right.source == FreeBookSource.OPEN_LIBRARY -> right.workId
        else -> left.workId
    }

    private suspend fun resolvedValues(value: String, languageCode: String): List<String> =
        queryResolver.resolve(value, languageCode).ifEmpty { listOf(value) }

    private fun authorsMatch(left: List<String>, right: List<String>): Boolean =
        left.any { first -> right.any { second -> namesMatch(first, second) } }

    private fun namesMatch(left: String, right: String): Boolean {
        val leftKey = left.toMetadataKey()
        val rightKey = right.toMetadataKey()
        if (leftKey.isBlank() || rightKey.isBlank()) return false
        if (leftKey == rightKey) return true
        val leftSurname = leftKey.substringAfterLast(' ')
        val rightSurname = rightKey.substringAfterLast(' ')
        return leftSurname.length >= MIN_SURNAME_LENGTH && leftSurname == rightSurname
    }
}

private fun String.toTitleKey(): String {
    val normalized = toMetadataKey()
    return ENGLISH_ARTICLES.firstNotNullOfOrNull { article ->
        normalized.removePrefix("$article ").takeIf { it != normalized }
    } ?: normalized
}

private fun String.toMetadataKey(): String = Normalizer
    .normalize(lowercase(Locale.ROOT).replace('ё', 'е'), Normalizer.Form.NFD)
    .replace(COMBINING_MARKS, "")
    .replace(NON_ALPHANUMERIC, " ")
    .trim()
    .replace(WHITESPACE, " ")

private const val MIN_SURNAME_LENGTH = 3
private val ENGLISH_ARTICLES = listOf("the", "an", "a")
private val COMBINING_MARKS = Regex("\\p{M}+")
private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
private val WHITESPACE = Regex("\\s+")
