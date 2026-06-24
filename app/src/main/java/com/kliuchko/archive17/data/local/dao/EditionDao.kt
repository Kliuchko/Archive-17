package com.kliuchko.archive17.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kliuchko.archive17.data.local.entity.EditionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EditionDao {
    @Query("SELECT * FROM editions WHERE workId = :workId ORDER BY languageCode ASC")
    fun observeEditionsForWork(workId: String): Flow<List<EditionEntity>>

    @Query("SELECT * FROM editions WHERE workId = :workId ORDER BY languageCode ASC")
    suspend fun getEditionsForWork(workId: String): List<EditionEntity>

    @Upsert
    suspend fun upsertEditions(editions: List<EditionEntity>)

    @Query("DELETE FROM editions WHERE workId = :workId")
    suspend fun deleteEditionsForWork(workId: String)
}
