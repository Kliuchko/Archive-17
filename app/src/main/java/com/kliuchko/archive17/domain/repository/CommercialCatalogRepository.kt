package com.kliuchko.archive17.domain.repository

import com.kliuchko.archive17.domain.model.CommercialBookOffer
import com.kliuchko.archive17.domain.model.CommercialCatalogRequest

interface CommercialCatalogProvider {
    val providerId: String

    suspend fun findOffers(request: CommercialCatalogRequest): List<CommercialBookOffer>
}

interface CommercialCatalogRepository {
    suspend fun findOffers(
        request: CommercialCatalogRequest,
    ): RepositoryResult<List<CommercialBookOffer>>
}
