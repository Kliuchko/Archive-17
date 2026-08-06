package com.kliuchko.archive17.domain.repository

import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.ReadingStatus
import kotlinx.coroutines.flow.Flow

interface LocalBookRepository {
    fun observeLocalBooks(status: ReadingStatus? = null): Flow<List<LocalBook>>

    suspend fun getLocalBook(id: String): LocalBook?

    suspend fun importBook(sourceUri: String): RepositoryResult<LocalBook>

    suspend fun saveProgression(id: String, progressionJson: String)
}
