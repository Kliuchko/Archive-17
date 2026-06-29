package com.kliuchko.archive17.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kliuchko.archive17.data.local.entity.LibraryEntryEntity
import com.kliuchko.archive17.data.local.relation.LibraryEntryWithWorkEntity
import com.kliuchko.archive17.domain.model.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryEntryDao {
    @Query(
        """
        SELECT * FROM library_entries
        WHERE (:status IS NULL OR readingStatus = :status)
        ORDER BY updatedAt DESC
        """,
    )
    fun observeLibraryEntries(status: ReadingStatus? = null): Flow<List<LibraryEntryEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM library_entries
        WHERE (:status IS NULL OR readingStatus = :status)
        ORDER BY updatedAt DESC
        """,
    )
    fun observeLibraryEntriesWithWorks(
        status: ReadingStatus? = null,
    ): Flow<List<LibraryEntryWithWorkEntity>>

    @Query("SELECT * FROM library_entries WHERE workId = :workId")
    fun observeLibraryEntry(workId: String): Flow<LibraryEntryEntity?>

    @Query("SELECT * FROM library_entries WHERE workId = :workId")
    suspend fun getLibraryEntry(workId: String): LibraryEntryEntity?

    @Upsert
    suspend fun upsertLibraryEntry(entry: LibraryEntryEntity)

    @Query("DELETE FROM library_entries WHERE workId = :workId")
    suspend fun deleteLibraryEntry(workId: String)
}
