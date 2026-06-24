package com.kliuchko.archive17.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kliuchko.archive17.domain.model.ReadingStatus

@Entity(
    tableName = "library_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["workId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("workId"),
        Index("readingStatus"),
    ],
)
data class LibraryEntryEntity(
    @PrimaryKey
    val workId: String,
    val readingStatus: ReadingStatus,
    val savedAt: Long,
    val updatedAt: Long,
)
