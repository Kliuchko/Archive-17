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
    fun observeLocalBook(id: String): Flow<LocalBookEntity?>

    @Query("SELECT * FROM local_books WHERE id = :id")
    suspend fun getLocalBook(id: String): LocalBookEntity?

    @Query("SELECT * FROM local_books WHERE contentHash = :contentHash LIMIT 1")
    suspend fun getLocalBookByContentHash(contentHash: String): LocalBookEntity?

    @Query("SELECT * FROM local_books WHERE identifier = :identifier LIMIT 1")
    suspend fun getLocalBookByIdentifier(identifier: String): LocalBookEntity?

    @Query("SELECT * FROM local_books WHERE contentHash IS NULL")
    suspend fun getLocalBooksWithoutContentHash(): List<LocalBookEntity>

    @Upsert
    suspend fun upsertLocalBook(book: LocalBookEntity)

    @Query(
        """
        UPDATE local_books
        SET title = :title,
            author = :author,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateMetadata(id: String, title: String, author: String?, updatedAt: Long)

    @Query("UPDATE local_books SET contentHash = :contentHash WHERE id = :id")
    suspend fun updateContentHash(id: String, contentHash: String)

    @Query(
        """
        UPDATE local_books
        SET readingStatus = :readingStatus,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateReadingStatus(id: String, readingStatus: ReadingStatus, updatedAt: Long)

    @Query("DELETE FROM local_books WHERE id = :id")
    suspend fun deleteLocalBook(id: String)

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
