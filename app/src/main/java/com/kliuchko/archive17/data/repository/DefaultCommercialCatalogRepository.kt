package com.kliuchko.archive17.data.repository

import com.kliuchko.archive17.domain.model.CommercialBookOffer
import com.kliuchko.archive17.domain.model.CommercialCatalogRequest
import com.kliuchko.archive17.domain.repository.CommercialCatalogProvider
import com.kliuchko.archive17.domain.repository.CommercialCatalogRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class DefaultCommercialCatalogRepository(
    private val providers: List<CommercialCatalogProvider>,
) : CommercialCatalogRepository {
    override suspend fun findOffers(
        request: CommercialCatalogRequest,
    ): RepositoryResult<List<CommercialBookOffer>> = coroutineScope {
        if (providers.isEmpty()) return@coroutineScope RepositoryResult.Success(emptyList())
        val results = providers.map { provider ->
            async {
                try {
                    provider.findOffers(request)
                } catch (exception: Throwable) {
                    if (exception is CancellationException) throw exception
                    null
                }
            }
        }.awaitAll()
        if (results.all { it == null }) {
            RepositoryResult.Error("Коммерческий каталог временно недоступен.")
        } else {
            RepositoryResult.Success(
                results.filterNotNull()
                    .flatten()
                    .filter { offer -> offer.workId == request.workId }
                    .filter { offer -> offer.isAvailableIn(request.territoryCode) }
                    .distinctBy { offer -> offer.offerId }
                    .sortedWith(offerPreference(request.preferredLanguageCode)),
            )
        }
    }

    private fun offerPreference(preferredLanguageCode: String) =
        compareByDescending<CommercialBookOffer> { offer ->
            offer.languageCode == preferredLanguageCode
        }.thenBy { offer -> offer.priceMinorUnits ?: Long.MAX_VALUE }
}
