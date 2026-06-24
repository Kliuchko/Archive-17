package com.kliuchko.archive17.data.local.mapper

import com.kliuchko.archive17.data.local.entity.EditionEntity
import com.kliuchko.archive17.data.local.entity.LibraryEntryEntity
import com.kliuchko.archive17.data.local.entity.WorkEntity
import com.kliuchko.archive17.domain.model.Edition
import com.kliuchko.archive17.domain.model.LibraryEntry
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMapperTest {
    @Test
    fun `maps work domain to entity and uses current time when cache timestamp is missing`() {
        val work = Work(
            id = "OL45883W",
            title = "Pride and Prejudice",
            authors = listOf("Jane Austen"),
            coverId = 12645118,
            firstPublishYear = 1813,
            editionCount = 428,
            editionLanguages = listOf("eng"),
            description = "A classic novel.",
            subjects = listOf("Love stories"),
            lastUpdatedAt = null,
        )

        val entity = work.toEntity(now = 123L)

        assertEquals("OL45883W", entity.id)
        assertEquals("Pride and Prejudice", entity.title)
        assertEquals(123L, entity.lastUpdatedAt)
    }

    @Test
    fun `maps work entity to domain`() {
        val entity = WorkEntity(
            id = "OL45883W",
            title = "Pride and Prejudice",
            authors = listOf("Jane Austen"),
            coverId = 12645118,
            firstPublishYear = 1813,
            editionCount = 428,
            editionLanguages = listOf("eng"),
            description = "A classic novel.",
            subjects = listOf("Love stories"),
            lastUpdatedAt = 123L,
        )

        val work = entity.toDomain()

        assertEquals("OL45883W", work.id)
        assertEquals(listOf("Jane Austen"), work.authors)
        assertEquals(123L, work.lastUpdatedAt)
    }

    @Test
    fun `maps edition and builds stable local id when remote id is missing`() {
        val entity = Edition(
            id = null,
            workId = "OL45883W",
            title = "Pride and Prejudice",
            languageCode = "eng",
        ).toEntity()

        assertEquals("OL45883W:eng:Pride and Prejudice", entity.id)
        assertEquals("OL45883W", entity.workId)
    }

    @Test
    fun `maps edition entity to domain`() {
        val edition = EditionEntity(
            id = "edition-1",
            workId = "OL45883W",
            title = "Pride and Prejudice",
            languageCode = "eng",
        ).toDomain()

        assertEquals("edition-1", edition.id)
        assertEquals("eng", edition.languageCode)
    }

    @Test
    fun `maps library entry both ways`() {
        val entry = LibraryEntry(
            workId = "OL45883W",
            readingStatus = ReadingStatus.WANT_TO_READ,
            savedAt = 100L,
            updatedAt = 200L,
        )

        val entity = entry.toEntity()
        val mappedBack = LibraryEntryEntity(
            workId = entity.workId,
            readingStatus = entity.readingStatus,
            savedAt = entity.savedAt,
            updatedAt = entity.updatedAt,
        ).toDomain()

        assertEquals(entry, mappedBack)
    }
}
