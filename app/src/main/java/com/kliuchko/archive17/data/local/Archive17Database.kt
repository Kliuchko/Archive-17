package com.kliuchko.archive17.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kliuchko.archive17.data.local.dao.EditionDao
import com.kliuchko.archive17.data.local.dao.LibraryEntryDao
import com.kliuchko.archive17.data.local.dao.WorkDao
import com.kliuchko.archive17.data.local.entity.EditionEntity
import com.kliuchko.archive17.data.local.entity.LibraryEntryEntity
import com.kliuchko.archive17.data.local.entity.WorkEntity

@Database(
    entities = [
        WorkEntity::class,
        EditionEntity::class,
        LibraryEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Archive17TypeConverters::class)
abstract class Archive17Database : RoomDatabase() {
    abstract fun workDao(): WorkDao
    abstract fun editionDao(): EditionDao
    abstract fun libraryEntryDao(): LibraryEntryDao
}
