package com.kliuchko.archive17.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kliuchko.archive17.data.local.entity.WorkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * FROM works WHERE id = :workId")
    fun observeWork(workId: String): Flow<WorkEntity?>

    @Query("SELECT * FROM works WHERE id = :workId")
    suspend fun getWork(workId: String): WorkEntity?

    @Upsert
    suspend fun upsertWork(work: WorkEntity)

    @Upsert
    suspend fun upsertWorks(works: List<WorkEntity>)

    @Query("DELETE FROM works WHERE id = :workId")
    suspend fun deleteWork(workId: String)
}
