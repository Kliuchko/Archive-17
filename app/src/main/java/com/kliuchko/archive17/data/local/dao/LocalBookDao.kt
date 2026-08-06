package com.kliuchko.archive17.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kliuchko.archive17.data.local.entity.LocalBookEntity
import com.kliuchko.archive17.domain.model.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalBookDao {
    @Query(
        """
        SELECT * FROM local_books
        WHERE (:status IS NULL OR readingStatus = :status)
        ORDER BY updatedAt DESC
        """,
    )
    fun observeLocalBooks(status: ReadingStatus? = null): Flow<List<LocalBookEntity>>

    @Query("SELECT * FROM local_books WHERE id = :id")
    suspend fun getLocalBook(id: String): LocalBookEntity?

    @Upsert
    suspend fun upsertLocalBook(book: LocalBookEntity)

    @Query(
        """
        UPDATE local_books
        SET progressionJson = :progressionJson,
            readingStatus = :readingStatus,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateProgression(
        id: String,
        progressionJson: String,
        readingStatus: ReadingStatus,
        updatedAt: Long,
    )
}
