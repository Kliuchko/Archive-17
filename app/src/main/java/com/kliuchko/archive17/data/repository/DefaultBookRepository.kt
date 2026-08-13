package com.kliuchko.archive17.data.repository

import com.kliuchko.archive17.core.time.TimeProvider
import com.kliuchko.archive17.data.local.dao.LibraryEntryDao
import com.kliuchko.archive17.data.local.dao.WorkDao
import com.kliuchko.archive17.data.local.mapper.toDomain
import com.kliuchko.archive17.data.local.mapper.toEntity
import com.kliuchko.archive17.data.networking.api.OpenLibraryApi
import com.kliuchko.archive17.data.networking.CoverMatchEnricher
import com.kliuchko.archive17.data.networking.mapper.toDomain
import com.kliuchko.archive17.data.networking.mapper.toPublicationEditions
import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LibraryEntry
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.PublicationEdition
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.WorkDetails
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.domain.repository.BookQueryResolver
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DefaultBookRepository(
    private val api: OpenLibraryApi,
    private val workDao: WorkDao,
    private val libraryEntryDao: LibraryEntryDao,
    private val timeProvider: TimeProvider,
    private val queryResolver: BookQueryResolver,
    private val cacheFreshnessMillis: Long = DEFAULT_CACHE_FRESHNESS_MILLIS,
) : BookRepository {
    private val coverMatchEnricher = CoverMatchEnricher()

    override suspend fun searchBooks(query: String, page: Int): RepositoryResult<List<Work>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_SEARCH_QUERY_LENGTH) {
            return RepositoryResult.Success(emptyList())
        }

        return runRepositoryCatching(
            errorMessage = "Unable to search books.",
        ) {
            val now = timeProvider.currentTimeMillis()
            val resolvedQueries = queryResolver.resolve(normalizedQuery)
                .ifEmpty { listOf(normalizedQuery) }
                .take(MAX_SOURCE_QUERIES)
            val responses = coroutineScope {
                resolvedQueries.map { resolvedQuery ->
                    async {
                        try {
                            api.searchBooks(
                                query = resolvedQuery,
                                page = page.coerceAtLeast(1),
                            )
                        } catch (exception: Throwable) {
                            if (exception is CancellationException) throw exception
                            null
                        }
                    }
                }.awaitAll()
            }
            if (responses.all { it == null }) {
                error("All catalog searches failed")
            }
            val works = coverMatchEnricher.enrichWorks(
                responses.filterNotNull()
                    .flatMap { it.toDomain() },
            )
                .distinctBy(Work::id)
                .map { it.copy(lastUpdatedAt = now) }

            workDao.upsertWorks(works.map { it.toEntity(now) })
            RepositoryResult.Success(works)
        }
    }

    override suspend fun cacheCatalogWorks(works: List<Work>): RepositoryResult<Unit> =
        runRepositoryCatching(errorMessage = "Не удалось сохранить сведения каталога.") {
            val now = timeProvider.currentTimeMillis()
            workDao.upsertWorks(works.map { work -> work.toEntity(now) })
            RepositoryResult.Success(Unit)
        }

    override fun observeWorkDetails(workId: String): Flow<WorkDetails> {
        val workFlow = workDao.observeWork(workId).map { it?.toDomain() }
        val entryFlow = libraryEntryDao.observeLibraryEntry(workId).map { it?.toDomain() }

        return combine(workFlow, entryFlow) { work, entry ->
            WorkDetails(
                work = work,
                libraryEntry = entry,
                isCached = work != null,
                isStale = work?.isStale() ?: false,
            )
        }
    }

    override suspend fun refreshWorkDetails(workId: String): RepositoryResult<Work> {
        val cachedWork = workDao.getWork(workId)?.toDomain()

        return try {
            val now = timeProvider.currentTimeMillis()
            val refreshedWork = api.getWork(workId)
                .toDomain(
                    fallback = cachedWork,
                    lastUpdatedAt = now,
                )

            if (refreshedWork == null) {
                cachedWork?.let {
                    RepositoryResult.Cached(
                        data = it,
                        message = "Showing cached details because remote details were incomplete.",
                    )
                } ?: RepositoryResult.Error("Remote details were incomplete.")
            } else {
                workDao.upsertWork(refreshedWork.toEntity(now))
                RepositoryResult.Success(refreshedWork)
            }
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception

            cachedWork?.let {
                RepositoryResult.Cached(
                    data = it,
                    message = "Showing cached details because refresh failed.",
                )
            } ?: RepositoryResult.Error("Unable to load book details.")
        }
    }

    override suspend fun getPublicationEditions(
        work: Work,
        preferredLanguageCode: String,
    ): RepositoryResult<List<PublicationEdition>> = runRepositoryCatching(
        errorMessage = "Не удалось загрузить издания книги.",
    ) {
        val fallbackLanguageCode = if (preferredLanguageCode == ENGLISH_EDITION_LANGUAGE) {
            RUSSIAN_EDITION_LANGUAGE
        } else {
            ENGLISH_EDITION_LANGUAGE
        }
        val languageCodes = listOf(preferredLanguageCode, fallbackLanguageCode)
            .filter { it.matches(EDITION_LANGUAGE_PATTERN) }
            .distinct()
        val responses = coroutineScope {
            languageCodes.map { languageCode ->
                async {
                    languageCode to api.searchEditionMetadata(
                        query = work.title,
                        language = languageCode,
                        responseLanguage = languageCode.toIso6391(),
                    )
                }
            }.awaitAll()
        }
        val editions = responses
            .flatMap { (languageCode, response) ->
                response.toPublicationEditions(languageCode)
            }
            .filter { edition -> edition.workId == work.id }
            .distinctBy(PublicationEdition::id)
            .sortedWith(
                compareByDescending<PublicationEdition> {
                    it.languageCode == preferredLanguageCode
                }.thenByDescending { it.publishedYear ?: Int.MIN_VALUE },
            )
        RepositoryResult.Success(editions)
    }

    override fun observeLibrary(status: ReadingStatus?): Flow<List<LibraryBook>> =
        libraryEntryDao.observeLibraryEntriesWithWorks(status)
            .map { entries -> entries.map { it.toDomain() } }

    override suspend fun saveWorkToLibrary(
        work: Work,
        readingStatus: ReadingStatus,
    ): RepositoryResult<LibraryBook> =
        runRepositoryCatching(
            errorMessage = "Unable to save book to library.",
        ) {
            val now = timeProvider.currentTimeMillis()
            val existingEntry = libraryEntryDao.getLibraryEntry(work.id)
            val entry = LibraryEntry(
                workId = work.id,
                readingStatus = readingStatus,
                savedAt = existingEntry?.savedAt ?: now,
                updatedAt = now,
            )

            workDao.upsertWork(work.toEntity(now))
            libraryEntryDao.upsertLibraryEntry(entry.toEntity())

            RepositoryResult.Success(
                LibraryBook(
                    work = work.copy(lastUpdatedAt = work.lastUpdatedAt ?: now),
                    entry = entry,
                ),
            )
        }

    override suspend fun removeFromLibrary(workId: String): RepositoryResult<Unit> =
        runRepositoryCatching(
            errorMessage = "Unable to remove book from library.",
        ) {
            libraryEntryDao.deleteLibraryEntry(workId)
            RepositoryResult.Success(Unit)
        }

    private fun Work.isStale(): Boolean {
        val updatedAt = lastUpdatedAt ?: return true
        return timeProvider.currentTimeMillis() - updatedAt >= cacheFreshnessMillis
    }

    private inline fun <T> runRepositoryCatching(
        errorMessage: String,
        block: () -> RepositoryResult<T>,
    ): RepositoryResult<T> =
        try {
            block()
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            RepositoryResult.Error(errorMessage)
        }

    companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 2
        const val MAX_SOURCE_QUERIES = 3
        const val DEFAULT_CACHE_FRESHNESS_MILLIS = 24L * 60L * 60L * 1000L
        private const val ENGLISH_EDITION_LANGUAGE = "eng"
        private const val RUSSIAN_EDITION_LANGUAGE = "rus"
        private val EDITION_LANGUAGE_PATTERN = Regex("[a-z]{3}")
    }
}

private fun String.toIso6391(): String = when (this) {
    "rus" -> "ru"
    "eng" -> "en"
    "ita" -> "it"
    else -> take(2)
}
