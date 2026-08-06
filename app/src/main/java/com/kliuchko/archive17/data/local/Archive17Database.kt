package com.kliuchko.archive17.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kliuchko.archive17.data.local.dao.EditionDao
import com.kliuchko.archive17.data.local.dao.LibraryEntryDao
import com.kliuchko.archive17.data.local.dao.LocalBookDao
import com.kliuchko.archive17.data.local.dao.WorkDao
import com.kliuchko.archive17.data.local.entity.EditionEntity
import com.kliuchko.archive17.data.local.entity.LibraryEntryEntity
import com.kliuchko.archive17.data.local.entity.LocalBookEntity
import com.kliuchko.archive17.data.local.entity.WorkEntity

@Database(
    entities = [
        WorkEntity::class,
        EditionEntity::class,
        LibraryEntryEntity::class,
        LocalBookEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Archive17TypeConverters::class)
abstract class Archive17Database : RoomDatabase() {
    abstract fun workDao(): WorkDao
    abstract fun editionDao(): EditionDao
    abstract fun libraryEntryDao(): LibraryEntryDao
    abstract fun localBookDao(): LocalBookDao

    companion object {
        const val DATABASE_NAME = "archive17.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_books (
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT,
                        identifier TEXT,
                        filePath TEXT NOT NULL,
                        coverPath TEXT,
                        progressionJson TEXT,
                        readingStatus TEXT NOT NULL,
                        addedAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_books_readingStatus ON local_books(readingStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_books_updatedAt ON local_books(updatedAt)")
            }
        }
    }
}
