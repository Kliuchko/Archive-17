package com.kliuchko.archive17.data.networking.mapper

import com.google.gson.JsonElement
import com.kliuchko.archive17.data.networking.dto.OpenLibraryEditionSearchDto
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchDocDto
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchResponseDto
import com.kliuchko.archive17.data.networking.dto.OpenLibraryWorkDto
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.FreeBook
import java.text.Normalizer

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
        val title = localizedTitle(
            editionTitle = edition.title,
            workTitle = document.title,
            languageCode = expectedLanguageCode,
        ) ?: return@mapNotNull null
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
            coverId = document.preferredCoverId(edition),
            firstPublishYear = document.firstPublishYear,
            languageCode = languageCode,
            editionYear = edition.publishDate.toEditionYear(),
            translator = edition.contributors.extractTranslator(),
            publisher = edition.publishers.normalizeList().firstOrNull(),
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
        coverId = preferredCoverId(),
        firstPublishYear = firstPublishYear,
        editionCount = editionCount,
        editionLanguages = languages.normalizeList(),
        description = null,
        subjects = emptyList(),
        lastUpdatedAt = lastUpdatedAt,
    )
}

private fun OpenLibrarySearchDocDto.preferredCoverId(
    selectedEdition: OpenLibraryEditionSearchDto? = null,
): Int? = coverId
    ?: selectedEdition?.coverId
    ?: editions?.docs.orEmpty().firstNotNullOfOrNull { it.coverId }

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

internal fun localizedTitle(
    editionTitle: String?,
    workTitle: String?,
    languageCode: String?,
): String? {
    val edition = editionTitle.normalize()
        ?.withoutCombiningMarks()
        ?.withoutTrailingRomanization(languageCode)
    val work = workTitle.normalize()
        ?.withoutCombiningMarks()
        ?.withoutTrailingRomanization(languageCode)

    if (languageCode != RUSSIAN_LANGUAGE) return edition ?: work

    return sequenceOf(edition, work)
        .filterNotNull()
        .firstOrNull(String::containsCyrillic)
        ?: edition
            ?.takeIf(String::looksLikeRussianRomanization)
            ?.transliterateRussianTitle()
        ?: work
        ?: edition
}

private fun String.withoutCombiningMarks(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace(COMBINING_MARKS_PATTERN, "")

private fun String.withoutTrailingRomanization(languageCode: String?): String =
    if (languageCode == RUSSIAN_LANGUAGE && containsCyrillic()) {
        replace(TRAILING_LATIN_PARENTHESIS_PATTERN, "").trim()
    } else {
        this
    }

private fun String.containsCyrillic(): Boolean = any { it in '\u0400'..'\u04FF' }

private fun String.looksLikeRussianRomanization(): Boolean {
    if (containsCyrillic() || !any(Char::isLetter)) return false
    val words = lowercase().split(NON_LETTER_PATTERN).filter(String::isNotBlank)
    return words.none { it in COMMON_ENGLISH_TITLE_WORDS }
}

private fun String.transliterateRussianTitle(): String {
    val source = lowercase()
    val result = StringBuilder(source.length)
    var index = 0
    while (index < source.length) {
        val match = RUSSIAN_TRANSLITERATION.keys
            .firstOrNull { key -> source.regionMatches(index, key, 0, key.length) }
        if (match == null) {
            result.append(source[index])
            index += 1
        } else {
            result.append(RUSSIAN_TRANSLITERATION.getValue(match))
            index += match.length
        }
    }
    return result.toString().replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase() else first.toString()
    }
}

private fun List<String>?.normalizeList(): List<String> =
    orEmpty()
        .mapNotNull { it.normalize() }
        .distinct()

private fun String?.toEditionYear(): Int? = this
    ?.let { value -> EDITION_YEAR_PATTERN.find(value)?.value }
    ?.toIntOrNull()

private fun List<String>?.extractTranslator(): String? = normalizeList()
    .firstOrNull { contributor -> TRANSLATOR_MARKER.containsMatchIn(contributor) }
    ?.replace(TRANSLATOR_MARKER, "")
    ?.trim(' ', ',', ';', ':', '(', ')')
    ?.takeIf(String::isNotBlank)

private fun JsonElement?.toDescription(): String? {
    if (this == null || isJsonNull) return null

    return when {
        isJsonPrimitive -> asJsonPrimitive.takeIf { it.isString }?.asString.normalize()
        isJsonObject -> asJsonObject.get("value")?.takeIf { it.isJsonPrimitive }?.asString.normalize()
        else -> null
    }
}

private const val PUBLIC_ACCESS = "public"
private const val RUSSIAN_LANGUAGE = "rus"
private val COMBINING_MARKS_PATTERN = Regex("\\p{M}+")
private val EDITION_YEAR_PATTERN = Regex("\\b(?:1[0-9]{3}|20[0-2][0-9])\\b")
private val TRANSLATOR_MARKER = Regex(
    "(?:translator|translated by|перевод(?:чик|чики)?|пер\\.)",
    RegexOption.IGNORE_CASE,
)
private val NON_LETTER_PATTERN = Regex("[^\\p{L}]+")
private val TRAILING_LATIN_PARENTHESIS_PATTERN = Regex("\\s*\\([^)]*[A-Za-z][^)]*\\)\\s*$")
private val COMMON_ENGLISH_TITLE_WORDS = setOf(
    "a",
    "an",
    "and",
    "for",
    "in",
    "of",
    "or",
    "the",
    "to",
)
private val RUSSIAN_TRANSLITERATION = linkedMapOf(
    "shch" to "щ",
    "yo" to "ё",
    "yu" to "ю",
    "ya" to "я",
    "zh" to "ж",
    "kh" to "х",
    "ts" to "ц",
    "ch" to "ч",
    "sh" to "ш",
    "ye" to "е",
    "a" to "а",
    "b" to "б",
    "v" to "в",
    "g" to "г",
    "d" to "д",
    "e" to "е",
    "z" to "з",
    "i" to "и",
    "y" to "й",
    "k" to "к",
    "l" to "л",
    "m" to "м",
    "n" to "н",
    "o" to "о",
    "p" to "п",
    "r" to "р",
    "s" to "с",
    "t" to "т",
    "u" to "у",
    "f" to "ф",
    "h" to "х",
    "c" to "к",
    "j" to "дж",
    "q" to "к",
    "w" to "в",
    "x" to "кс",
)
