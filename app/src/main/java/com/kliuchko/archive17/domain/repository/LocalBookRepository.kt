package com.kliuchko.archive17.domain.repository

import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.DownloadedBookMetadata
import com.kliuchko.archive17.domain.model.ReadingStatus
import kotlinx.coroutines.flow.Flow

interface LocalBookRepository {
    fun observeLocalBooks(status: ReadingStatus? = null): Flow<List<LocalBook>>

    fun observeLocalBook(id: String): Flow<LocalBook?>

    suspend fun getLocalBook(id: String): LocalBook?

    suspend fun getLocalBookByIdentifier(identifier: String): LocalBook?

    suspend fun importBook(sourceUri: String): RepositoryResult<LocalBook>

    suspend fun importDownloadedBook(
        sourceFilePath: String,
        metadata: DownloadedBookMetadata,
    ): RepositoryResult<LocalBook>

    suspend fun updateMetadata(id: String, title: String, author: String?): RepositoryResult<LocalBook>

    suspend fun updateReadingStatus(id: String, status: ReadingStatus): RepositoryResult<LocalBook>

    suspend fun deleteLocalBook(id: String): RepositoryResult<Unit>

    suspend fun saveProgression(id: String, progressionJson: String)
}
