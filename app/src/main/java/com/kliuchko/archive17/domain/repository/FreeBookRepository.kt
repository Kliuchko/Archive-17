package com.kliuchko.archive17.domain.repository

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookDetails
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.PublicationEdition
import com.kliuchko.archive17.domain.model.TemporaryBook

interface FreeBookRepository {
    suspend fun searchBooks(
        query: String,
        languageCode: String,
        page: Int = 1,
    ): RepositoryResult<List<FreeBook>>

    suspend fun refreshStarterCatalog(
        languageCode: String,
        page: Int = 1,
    ): RepositoryResult<List<FreeBook>>

    suspend fun keepDownloadableBooks(
        books: List<FreeBook>,
        languageCode: String,
    ): RepositoryResult<List<FreeBook>>

    suspend fun getBookDetails(editionId: String): RepositoryResult<FreeBookDetails>

    suspend fun getRelatedEditions(
        editionId: String,
    ): RepositoryResult<List<PublicationEdition>>

    suspend fun downloadToShelf(book: FreeBook): RepositoryResult<LocalBook>

    suspend fun downloadForReading(book: FreeBook): RepositoryResult<TemporaryBook>
}
