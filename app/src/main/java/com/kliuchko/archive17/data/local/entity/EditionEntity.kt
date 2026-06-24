package com.kliuchko.archive17.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "editions",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["workId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workId")],
)
data class EditionEntity(
    @PrimaryKey
    val id: String,
    val workId: String,
    val title: String?,
    val languageCode: String?,
)
