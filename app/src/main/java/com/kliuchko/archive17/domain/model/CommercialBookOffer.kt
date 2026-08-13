package com.kliuchko.archive17.domain.model

data class CommercialBookOffer(
    val offerId: String,
    val providerName: String,
    val workId: String,
    val editionId: String,
    val title: String,
    val authors: List<String>,
    val languageCode: String,
    val translator: String? = null,
    val publisher: String? = null,
    val publishedYear: Int? = null,
    val coverUrl: String? = null,
    val purchaseMode: CommercialPurchaseMode,
    val priceMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val actionUrl: String? = null,
    val territoryCodes: Set<String> = emptySet(),
) {
    fun isAvailableIn(territoryCode: String): Boolean = territoryCodes.isEmpty() ||
        territoryCode.uppercase() in territoryCodes.map(String::uppercase)

    fun toPublicationEdition(): PublicationEdition = PublicationEdition(
        id = editionId,
        workId = workId,
        title = title,
        authors = authors,
        languageCode = languageCode,
        translator = translator,
        publishedYear = publishedYear,
        publisher = publisher,
        coverUrl = coverUrl,
        accessOptions = listOf(
            EditionAccessOption(
                mode = when (purchaseMode) {
                    CommercialPurchaseMode.IN_APP -> EditionAccessMode.PURCHASE
                    CommercialPurchaseMode.PARTNER -> EditionAccessMode.PARTNER_PURCHASE
                    CommercialPurchaseMode.SUBSCRIPTION -> EditionAccessMode.SUBSCRIPTION
                },
                availability = EditionAvailability.AVAILABLE,
                providerName = providerName,
                actionUrl = actionUrl,
                priceMinorUnits = priceMinorUnits,
                currencyCode = currencyCode,
            ),
        ),
    )
}

enum class CommercialPurchaseMode {
    IN_APP,
    PARTNER,
    SUBSCRIPTION,
}

data class CommercialCatalogRequest(
    val workId: String,
    val title: String,
    val authors: List<String>,
    val preferredLanguageCode: String,
    val territoryCode: String,
)
