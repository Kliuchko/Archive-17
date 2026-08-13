package com.kliuchko.archive17.domain.model

/**
 * A concrete text of a work. Access is modelled separately so the same edition can later be
 * offered for free, through a subscription, as a purchase, by a partner, or as the user's file.
 */
data class PublicationEdition(
    val id: String,
    val workId: String,
    val title: String,
    val authors: List<String>,
    val languageCode: String,
    val translator: String? = null,
    val publishedYear: Int? = null,
    val publisher: String? = null,
    val textEditionType: TextEditionType = TextEditionType.UNSPECIFIED,
    val label: String? = null,
    val coverId: Int? = null,
    val coverUrl: String? = null,
    val accessOptions: List<EditionAccessOption> = emptyList(),
)

data class EditionAccessOption(
    val mode: EditionAccessMode,
    val availability: EditionAvailability,
    val providerName: String? = null,
    val actionUrl: String? = null,
    val priceMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val rights: FreeBookRights? = null,
)

enum class EditionAccessMode {
    FREE,
    SUBSCRIPTION,
    PURCHASE,
    PARTNER_PURCHASE,
    OWNED_FILE,
}

enum class EditionAvailability {
    AVAILABLE,
    COMING_SOON,
    EXTERNAL_ONLY,
    REGION_RESTRICTED,
    UNAVAILABLE,
}

fun FreeBook.toPublicationEdition(): PublicationEdition = PublicationEdition(
    id = editionId,
    workId = workId,
    title = title,
    authors = authors,
    languageCode = languageCode,
    translator = translator,
    publishedYear = editionYear,
    publisher = publisher,
    textEditionType = textEditionType,
    label = editionLabel,
    coverId = coverId,
    coverUrl = coverUrl,
    accessOptions = listOf(
        EditionAccessOption(
            mode = EditionAccessMode.FREE,
            availability = if (isDownloadable) {
                EditionAvailability.AVAILABLE
            } else {
                EditionAvailability.EXTERNAL_ONLY
            },
            providerName = sourceName,
            actionUrl = sourceUrl,
            rights = rights,
        ),
    ),
)
