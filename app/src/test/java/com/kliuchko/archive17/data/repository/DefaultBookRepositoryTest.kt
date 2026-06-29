package com.kliuchko.archive17.data.repository

import com.google.gson.JsonParser
import com.kliuchko.archive17.core.time.TimeProvider
import com.kliuchko.archive17.data.local.dao.EditionDao
import com.kliuchko.archive17.data.local.dao.LibraryEntryDao
import com.kliuchko.archive17.data.local.dao.WorkDao
import com.kliuchko.archive17.data.local.entity.EditionEntity
import com.kliuchko.archive17.data.local.entity.LibraryEntryEntity
import com.kliuchko.archive17.data.local.entity.WorkEntity
import com.kliuchko.archive17.data.local.mapper.toEntity
import com.kliuchko.archive17.data.local.relation.LibraryEntryWithWorkEntity
import com.kliuchko.archive17.data.networking.api.OpenLibraryApi
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchDocDto
import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchResponseDto
import com.kliuchko.archive17.data.networking.dto.OpenLibraryWorkDto
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.repository.RepositoryResult
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultBookRepositoryTest {
    private val api = FakeOpenLibraryApi()
    private val workDao = FakeWorkDao()
    private val libraryEntryDao = FakeLibraryEntryDao(workDao)
    private val timeProvider = MutableTimeProvider(currentTimeMillis = 1_000L)
    private val repository = DefaultBookRepository(
        api = api,
        workDao = workDao,
        libraryEntryDao = libraryEntryDao,
        timeProvider = timeProvider,
    )

    @Test
    fun `search returns empty result before minimum query length`() = runBlocking {
        val result = repository.searchBooks("a")

        assertEquals(RepositoryResult.Success(emptyList<Work>()), result)
        assertEquals(0, api.searchCallCount)
    }

    @Test
    fun `search maps remote works and caches them`() = runBlocking {
        api.searchResponse = OpenLibrarySearchResponseDto(
            docs = listOf(
                OpenLibrarySearchDocDto(
                    key = "/works/OL1W",
                    title = "Book",
                    authorNames = listOf("Author"),
                    coverId = 10,
                    firstPublishYear = 2001,
                    editionCount = 2,
                    languages = listOf("eng"),
                ),
            ),
        )

        val result = repository.searchBooks("bo")

        require(result is RepositoryResult.Success)
        assertEquals("OL1W", result.data.first().id)
        assertEquals(1_000L, result.data.first().lastUpdatedAt)
        assertEquals(1_000L, workDao.getStored("OL1W")?.lastUpdatedAt)
    }

    @Test
    fun `refresh details updates cached work`() = runBlocking {
        workDao.upsertWork(sampleWork(lastUpdatedAt = 10L).toEntity(now = 10L))
        api.workResponse = OpenLibraryWorkDto(
            key = "/works/OL1W",
            title = "Updated Book",
            description = JsonParser.parseString("""{"value":"Updated details"}"""),
            subjects = listOf("Subject"),
        )

        val result = repository.refreshWorkDetails("OL1W")

        require(result is RepositoryResult.Success)
        assertEquals("Updated Book", result.data.title)
        assertEquals("Updated details", result.data.description)
        assertEquals(1_000L, workDao.getStored("OL1W")?.lastUpdatedAt)
        assertEquals("Updated details", workDao.getStored("OL1W")?.description)
    }

    @Test
    fun `refresh details returns cached work when network fails`() = runBlocking {
        workDao.upsertWork(sampleWork(lastUpdatedAt = 10L).toEntity(now = 10L))
        api.workError = IOException("Offline")

        val result = repository.refreshWorkDetails("OL1W")

        require(result is RepositoryResult.Cached)
        assertEquals("OL1W", result.data.id)
    }

    @Test
    fun `refresh details returns error when network fails without cache`() = runBlocking {
        api.workError = IOException("Offline")

        val result = repository.refreshWorkDetails("OL1W")

        require(result is RepositoryResult.Error)
        assertEquals("Unable to load book details.", result.message)
    }

    @Test
    fun `observe details marks stale cached work and exposes local library status`() = runBlocking {
        val oldTimestamp = 1_000L - DefaultBookRepository.DEFAULT_CACHE_FRESHNESS_MILLIS
        workDao.upsertWork(sampleWork(lastUpdatedAt = oldTimestamp).toEntity(now = oldTimestamp))
        libraryEntryDao.upsertLibraryEntry(
            LibraryEntryEntity(
                workId = "OL1W",
                readingStatus = ReadingStatus.READING,
                savedAt = 100L,
                updatedAt = 200L,
            ),
        )

        val details = repository.observeWorkDetails("OL1W").first()

        assertTrue(details.isCached)
        assertTrue(details.isStale)
        assertEquals(ReadingStatus.READING, details.libraryEntry?.readingStatus)
    }

    @Test
    fun `save work to library preserves original saved timestamp when status changes`() = runBlocking {
        val work = sampleWork(lastUpdatedAt = null)

        timeProvider.currentTimeMillis = 100L
        repository.saveWorkToLibrary(work, ReadingStatus.WANT_TO_READ)

        timeProvider.currentTimeMillis = 200L
        val result = repository.saveWorkToLibrary(work, ReadingStatus.FINISHED)

        require(result is RepositoryResult.Success)
        assertEquals(100L, result.data.entry.savedAt)
        assertEquals(200L, result.data.entry.updatedAt)
        assertEquals(ReadingStatus.FINISHED, result.data.entry.readingStatus)
    }

    @Test
    fun `observe library emits saved books with work metadata`() = runBlocking {
        repository.saveWorkToLibrary(sampleWork(lastUpdatedAt = null), ReadingStatus.WANT_TO_READ)

        val libraryBooks = repository.observeLibrary().first()

        assertEquals(1, libraryBooks.size)
        assertEquals("OL1W", libraryBooks.first().work.id)
        assertEquals(ReadingStatus.WANT_TO_READ, libraryBooks.first().entry.readingStatus)
    }

    @Test
    fun `remove from library deletes entry but keeps cached work`() = runBlocking {
        repository.saveWorkToLibrary(sampleWork(lastUpdatedAt = null), ReadingStatus.WANT_TO_READ)

        val result = repository.removeFromLibrary("OL1W")

        assertEquals(RepositoryResult.Success(Unit), result)
        assertEquals(emptyList<Any>(), repository.observeLibrary().first())
        assertEquals("OL1W", workDao.getStored("OL1W")?.id)
    }

    private fun sampleWork(lastUpdatedAt: Long?): Work =
        Work(
            id = "OL1W",
            title = "Book",
            authors = listOf("Author"),
            coverId = 10,
            firstPublishYear = 2001,
            editionCount = 2,
            editionLanguages = listOf("eng"),
            description = "Cached details",
            subjects = listOf("Subject"),
            lastUpdatedAt = lastUpdatedAt,
        )
}

private class MutableTimeProvider(
    var currentTimeMillis: Long,
) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTimeMillis
}

private class FakeOpenLibraryApi : OpenLibraryApi {
    var searchResponse = OpenLibrarySearchResponseDto()
    var workResponse = OpenLibraryWorkDto(
        key = "/works/OL1W",
        title = "Book",
        description = null,
        subjects = emptyList(),
    )
    var searchError: Throwable? = null
    var workError: Throwable? = null
    var searchCallCount = 0

    override suspend fun searchBooks(
        query: String,
        fields: String,
        limit: Int,
    ): OpenLibrarySearchResponseDto {
        searchCallCount += 1
        searchError?.let { throw it }
        return searchResponse
    }

    override suspend fun getWork(workId: String): OpenLibraryWorkDto {
        workError?.let { throw it }
        return workResponse
    }
}

private class FakeWorkDao : WorkDao {
    private val works = linkedMapOf<String, WorkEntity>()
    private val flows = mutableMapOf<String, MutableStateFlow<WorkEntity?>>()

    override fun observeWork(workId: String): Flow<WorkEntity?> = flowFor(workId)

    override suspend fun getWork(workId: String): WorkEntity? = getStored(workId)

    override suspend fun upsertWork(work: WorkEntity) {
        works[work.id] = work
        flowFor(work.id).value = work
    }

    override suspend fun upsertWorks(works: List<WorkEntity>) {
        works.forEach { upsertWork(it) }
    }

    override suspend fun deleteWork(workId: String) {
        works.remove(workId)
        flowFor(workId).value = null
    }

    fun getStored(workId: String): WorkEntity? = works[workId]

    private fun flowFor(workId: String): MutableStateFlow<WorkEntity?> =
        flows.getOrPut(workId) { MutableStateFlow(works[workId]) }
}

private class FakeLibraryEntryDao(
    private val workDao: FakeWorkDao,
) : LibraryEntryDao {
    private val entries = linkedMapOf<String, LibraryEntryEntity>()
    private val entryFlows = mutableMapOf<String, MutableStateFlow<LibraryEntryEntity?>>()
    private val libraryFlow = MutableStateFlow<List<LibraryEntryEntity>>(emptyList())

    override fun observeLibraryEntries(status: ReadingStatus?): Flow<List<LibraryEntryEntity>> =
        libraryFlow.map { entries ->
            entries.filterByStatus(status)
        }

    override fun observeLibraryEntriesWithWorks(
        status: ReadingStatus?,
    ): Flow<List<LibraryEntryWithWorkEntity>> =
        libraryFlow.map { entries ->
            entries
                .filterByStatus(status)
                .mapNotNull { entry ->
                    workDao.getStored(entry.workId)?.let { work ->
                        LibraryEntryWithWorkEntity(
                            entry = entry,
                            work = work,
                        )
                    }
                }
        }

    override fun observeLibraryEntry(workId: String): Flow<LibraryEntryEntity?> = flowFor(workId)

    override suspend fun getLibraryEntry(workId: String): LibraryEntryEntity? = entries[workId]

    override suspend fun upsertLibraryEntry(entry: LibraryEntryEntity) {
        entries[entry.workId] = entry
        flowFor(entry.workId).value = entry
        publishLibrary()
    }

    override suspend fun deleteLibraryEntry(workId: String) {
        entries.remove(workId)
        flowFor(workId).value = null
        publishLibrary()
    }

    private fun flowFor(workId: String): MutableStateFlow<LibraryEntryEntity?> =
        entryFlows.getOrPut(workId) { MutableStateFlow(entries[workId]) }

    private fun publishLibrary() {
        libraryFlow.value = entries.values.sortedByDescending { it.updatedAt }
    }

    private fun List<LibraryEntryEntity>.filterByStatus(
        status: ReadingStatus?,
    ): List<LibraryEntryEntity> =
        if (status == null) {
            this
        } else {
            filter { it.readingStatus == status }
        }
}

private class FakeEditionDao : EditionDao {
    override fun observeEditionsForWork(workId: String): Flow<List<EditionEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun getEditionsForWork(workId: String): List<EditionEntity> = emptyList()

    override suspend fun upsertEditions(editions: List<EditionEntity>) = Unit

    override suspend fun deleteEditionsForWork(workId: String) = Unit
}
