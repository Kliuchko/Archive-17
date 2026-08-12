package com.kliuchko.archive17.domain.repository

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookDetails
import com.kliuchko.archive17.domain.model.LocalBook

interface FreeBookRepository {
    suspend fun searchBooks(
        query: String,
        languageCode: String,
        page: Int = 1,
    ): RepositoryResult<List<FreeBook>>

    suspend fun keepDownloadableBooks(
        books: List<FreeBook>,
        languageCode: String,
    ): RepositoryResult<List<FreeBook>>

    suspend fun getBookDetails(editionId: String): RepositoryResult<FreeBookDetails>

    suspend fun downloadToShelf(book: FreeBook): RepositoryResult<LocalBook>
}
