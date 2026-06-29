package com.kliuchko.archive17.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.kliuchko.archive17.data.local.entity.LibraryEntryEntity
import com.kliuchko.archive17.data.local.entity.WorkEntity

data class LibraryEntryWithWorkEntity(
    @Embedded
    val entry: LibraryEntryEntity,
    @Relation(
        parentColumn = "workId",
        entityColumn = "id",
    )
    val work: WorkEntity,
)
