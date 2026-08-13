package com.kliuchko.archive17.data.repository

import com.kliuchko.archive17.domain.model.CommercialBookOffer
import com.kliuchko.archive17.domain.model.CommercialCatalogRequest
import com.kliuchko.archive17.domain.model.CommercialPurchaseMode
import com.kliuchko.archive17.domain.repository.CommercialCatalogProvider
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCommercialCatalogRepositoryTest {
    @Test
    fun `keeps offers for the work and territory and prioritizes device language`() = runBlocking {
        val repository = DefaultCommercialCatalogRepository(
            providers = listOf(
                FakeProvider(
                    listOf(
                        offer("english", "work", "eng", setOf("IT"), 500),
                        offer("russian", "work", "rus", setOf("IT"), 700),
                        offer("wrong-work", "other", "rus", setOf("IT"), 100),
                        offer("wrong-region", "work", "rus", setOf("US"), 100),
                    ),
                ),
            ),
        )

        val result = repository.findOffers(
            CommercialCatalogRequest(
                workId = "work",
                title = "Book",
                authors = listOf("Author"),
                preferredLanguageCode = "rus",
                territoryCode = "IT",
            ),
        )

        require(result is RepositoryResult.Success)
        assertEquals(listOf("russian", "english"), result.data.map(CommercialBookOffer::offerId))
    }

    @Test
    fun `empty provider list is a valid catalog without offers`() = runBlocking {
        val result = DefaultCommercialCatalogRepository(emptyList()).findOffers(
            CommercialCatalogRequest("work", "Book", emptyList(), "eng", "IT"),
        )

        assertEquals(RepositoryResult.Success(emptyList<CommercialBookOffer>()), result)
    }

    private fun offer(
        id: String,
        workId: String,
        languageCode: String,
        territories: Set<String>,
        price: Long,
    ) = CommercialBookOffer(
        offerId = id,
        providerName = "Partner",
        workId = workId,
        editionId = "edition-$id",
        title = "Book",
        authors = listOf("Author"),
        languageCode = languageCode,
        purchaseMode = CommercialPurchaseMode.PARTNER,
        priceMinorUnits = price,
        currencyCode = "EUR",
        actionUrl = "https://example.com/$id",
        territoryCodes = territories,
    )

    private class FakeProvider(
        private val offers: List<CommercialBookOffer>,
    ) : CommercialCatalogProvider {
        override val providerId: String = "fake"

        override suspend fun findOffers(
            request: CommercialCatalogRequest,
        ): List<CommercialBookOffer> = offers
    }
}
