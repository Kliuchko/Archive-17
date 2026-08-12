package com.kliuchko.archive17.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kliuchko.archive17.domain.model.ReadingStatus

@Entity(
    tableName = "local_books",
    indices = [
        Index("readingStatus"),
        Index("updatedAt"),
        Index(value = ["contentHash"], unique = true),
    ],
)
data class LocalBookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String?,
    val identifier: String?,
    val contentHash: String?,
    val filePath: String,
    val coverPath: String?,
    val progressionJson: String?,
    val readingStatus: ReadingStatus,
    val addedAt: Long,
    val updatedAt: Long,
)
