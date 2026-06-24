package com.kliuchko.archive17.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val authors: List<String>,
    val coverId: Int?,
    val firstPublishYear: Int?,
    val editionCount: Int?,
    val editionLanguages: List<String>,
    val description: String?,
    val subjects: List<String>,
    val lastUpdatedAt: Long,
)
