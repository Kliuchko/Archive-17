package com.kliuchko.archive17.domain.model

import java.text.Normalizer
import java.util.Locale

/** Groups records that differ only by printing, binding, publisher, or another carrier detail. */
fun List<PublicationEdition>.groupMeaningfulVariants(
    preferredLanguageCode: String,
): List<PublicationEdition> = groupBy(PublicationEdition::meaningfulVariantKey)
    .values
    .map { records ->
        val representative = records.maxWithOrNull(editionRepresentativeComparator())
            ?: records.first()
        representative.copy(
            accessOptions = records
                .flatMap(PublicationEdition::accessOptions)
                .distinctBy { access ->
                    listOf(
                        access.mode.name,
                        access.availability.name,
                        access.providerName.orEmpty(),
                        access.actionUrl.orEmpty(),
                    ).joinToString("|")
                },
        )
    }
    .sortedWith(
        compareByDescending<PublicationEdition> { edition ->
            edition.languageCode == preferredLanguageCode
        }.thenByDescending { edition ->
            edition.accessOptions.any { access ->
                access.availability == EditionAvailability.AVAILABLE
            }
        }.thenByDescending { edition ->
            when (edition.textEditionType) {
                TextEditionType.MODERN_ORTHOGRAPHY -> 3
                TextEditionType.UNSPECIFIED -> 2
                TextEditionType.HISTORICAL_ORTHOGRAPHY -> 1
            }
        }.thenByDescending { edition -> edition.translator != null },
    )

private fun PublicationEdition.meaningfulVariantKey(): String = listOf(
    languageCode.lowercase(Locale.ROOT),
    translator.toVariantKey().ifBlank { UNKNOWN_TRANSLATOR_KEY },
    textEditionType.name,
    label.toVariantKey(),
).joinToString("|")

private fun editionRepresentativeComparator() =
    compareBy<PublicationEdition> { edition ->
        edition.accessOptions.any { access ->
            access.availability == EditionAvailability.AVAILABLE
        }
    }.thenBy { edition -> edition.translator != null }
        .thenBy { edition -> edition.publisher != null }
        .thenBy { edition -> edition.coverId != null || edition.coverUrl != null }
        .thenBy { edition -> edition.publishedYear ?: Int.MIN_VALUE }

private fun String?.toVariantKey(): String = Normalizer
    .normalize(orEmpty(), Normalizer.Form.NFKD)
    .lowercase(Locale.ROOT)
    .replace(COMBINING_MARKS, "")
    .replace(NON_ALPHANUMERIC, "")

private const val UNKNOWN_TRANSLATOR_KEY = "unknown"
private val COMBINING_MARKS = Regex("\\p{M}+")
private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
