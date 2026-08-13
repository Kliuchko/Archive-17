package com.kliuchko.archive17.domain.repository

import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.PublicationEdition
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.WorkDetails
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    suspend fun searchBooks(query: String, page: Int = 1): RepositoryResult<List<Work>>

    suspend fun cacheCatalogWorks(works: List<Work>): RepositoryResult<Unit>

    fun observeWorkDetails(workId: String): Flow<WorkDetails>

    suspend fun refreshWorkDetails(workId: String): RepositoryResult<Work>

    suspend fun getPublicationEditions(
        work: Work,
        preferredLanguageCode: String,
    ): RepositoryResult<List<PublicationEdition>>

    fun observeLibrary(status: ReadingStatus? = null): Flow<List<LibraryBook>>

    suspend fun saveWorkToLibrary(
        work: Work,
        readingStatus: ReadingStatus,
    ): RepositoryResult<LibraryBook>

    suspend fun removeFromLibrary(workId: String): RepositoryResult<Unit>
}

sealed interface RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>

    data class Cached<T>(
        val data: T,
        val message: String,
    ) : RepositoryResult<T>

    data class Error(
        val message: String,
    ) : RepositoryResult<Nothing>
}
