package com.kliuchko.archive17.data.repository

import com.kliuchko.archive17.core.time.TimeProvider
import com.kliuchko.archive17.data.local.dao.LibraryEntryDao
import com.kliuchko.archive17.data.local.dao.WorkDao
import com.kliuchko.archive17.data.local.entity.WorkEntity
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class DefaultBookRepository(
    private val api: OpenLibraryApi,
    private val workDao: WorkDao,
    private val libraryEntryDao: LibraryEntryDao,
    private val timeProvider: TimeProvider,
    private val queryResolver: BookQueryResolver,
    private val cacheFreshnessMillis: Long = DEFAULT_CACHE_FRESHNESS_MILLIS,
) : BookRepository {
    private val coverMatchEnricher = CoverMatchEnricher()
    private val knownEditionCounts = ConcurrentHashMap<String, Int>()

    override suspend fun searchBooks(
        query: String,
        page: Int,
        resolveAliases: Boolean,
    ): RepositoryResult<List<Work>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_SEARCH_QUERY_LENGTH) {
            return RepositoryResult.Success(emptyList())
        }

        return runRepositoryCatching(
            errorMessage = "Unable to search books.",
        ) {
            val now = timeProvider.currentTimeMillis()
            val resolvedQueries = if (resolveAliases) {
                queryResolver.resolve(normalizedQuery)
                    .ifEmpty { listOf(normalizedQuery) }
                    .take(MAX_SOURCE_QUERIES)
            } else {
                listOf(normalizedQuery)
            }
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
                .sortedWith(WORK_POPULARITY_ORDER)

            workDao.upsertWorks(works.map { it.toEntity(now) })
            workDao.trimCatalogCache(MAX_CACHED_WORKS)
            RepositoryResult.Success(works)
        }
    }

    override suspend fun searchCachedBooks(query: String, limit: Int): List<Work> {
        val key = query.toCatalogSearchKey()
        if (key.length < MIN_SEARCH_QUERY_LENGTH) return emptyList()
        return workDao.getRecentWorks(CACHED_SEARCH_SCAN_LIMIT)
            .asSequence()
            .map(WorkEntity::toDomain)
            .filter { work ->
                val updatedAt = work.lastUpdatedAt ?: return@filter false
                timeProvider.currentTimeMillis() - updatedAt <= SEARCH_CACHE_LIFETIME_MILLIS
            }
            .filter { work ->
                work.title.toCatalogSearchKey().contains(key) ||
                    work.authors.any { author -> author.toCatalogSearchKey().contains(key) }
            }
            .sortedWith(WORK_POPULARITY_ORDER)
            .take(limit.coerceIn(1, CACHED_SEARCH_RESULT_LIMIT))
            .toList()
    }

    override suspend fun cacheCatalogWorks(works: List<Work>): RepositoryResult<Unit> =
        runRepositoryCatching(errorMessage = "Не удалось сохранить сведения каталога.") {
            val now = timeProvider.currentTimeMillis()
            workDao.upsertWorks(works.map { work -> work.toEntity(now) })
            workDao.trimCatalogCache(MAX_CACHED_WORKS)
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
        originalLanguageCode: String?,
        localizedTitles: Map<String, String>,
    ): RepositoryResult<List<PublicationEdition>> = runRepositoryCatching(
        errorMessage = "Не удалось загрузить издания книги.",
    ) {
        val availableLanguages = work.editionLanguages.toSet()
        val languageCodes = buildList {
            originalLanguageCode?.let(::add)
            add(preferredLanguageCode)
            addAll(PRIORITY_EDITION_LANGUAGES)
            addAll(work.editionLanguages)
        }
            .filter { it.matches(EDITION_LANGUAGE_PATTERN) }
            .filter { language ->
                availableLanguages.isEmpty() || language in availableLanguages
            }
            .distinct()
        val targetedLanguageCodes = listOfNotNull(
            originalLanguageCode,
            preferredLanguageCode,
        )
            .filter(languageCodes::contains)
            .distinct()
        val (workEditionsResponse, searchResponses) = coroutineScope {
            val editions = async {
                runCatching {
                    api.getWorkEditions(work.id)
                }.getOrNull()
            }
            targetedLanguageCodes.map { languageCode ->
                async {
                    runCatching {
                        languageCode to api.searchEditionMetadata(
                            query = work.title,
                            language = languageCode,
                            responseLanguage = languageCode.toIso6391(),
                        )
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull().let { responses -> editions.await() to responses }
        }
        workEditionsResponse?.size?.let { count -> knownEditionCounts[work.id] = count }
        val workEditions = workEditionsResponse?.toPublicationEditions(work).orEmpty()
        val metadataEditions = searchResponses
            .flatMap { (languageCode, response) ->
                response.toPublicationEditions(languageCode)
            }
            .filter { edition -> edition.workId == work.id }
        val resolvedEditions = (workEditions + metadataEditions)
            .withLocalizedTitles(localizedTitles)
        val languagePlaceholders = languageCodes
            .filterNot { languageCode ->
                resolvedEditions.any { edition -> edition.languageCode == languageCode }
            }
            .map { languageCode ->
                PublicationEdition(
                    id = "language:${work.id}:$languageCode",
                    workId = work.id,
                    title = localizedTitles[languageCode] ?: work.title,
                    authors = work.authors,
                    languageCode = languageCode,
                )
            }
        val editions = (resolvedEditions + languagePlaceholders)
            .distinctBy(PublicationEdition::id)
            .sortedWith(
                compareByDescending<PublicationEdition> {
                    it.languageCode == preferredLanguageCode
                }.thenByDescending { it.publishedYear ?: Int.MIN_VALUE },
            )
        RepositoryResult.Success(editions)
    }

    override fun loadPublicationEditionUpdates(
        work: Work,
        languageCodes: List<String>,
        localizedTitles: Map<String, String>,
    ): Flow<List<PublicationEdition>> = flow {
        val editionCount = knownEditionCounts[work.id].orEmptyCount()
        val lastOffset = minOf(editionCount, MAX_WORK_EDITION_RECORDS)
        for (offset in OpenLibraryApi.WORK_EDITIONS_LIMIT until lastOffset step OpenLibraryApi.WORK_EDITIONS_LIMIT) {
            val page = runCatching {
                api.getWorkEditions(
                    workId = work.id,
                    limit = OpenLibraryApi.WORK_EDITIONS_LIMIT,
                    offset = offset,
                ).toPublicationEditions(work)
            }.getOrDefault(emptyList())
                .withLocalizedTitles(localizedTitles)
            if (page.isNotEmpty()) emit(page)
        }

        languageCodes
            .filter { code -> code.matches(EDITION_LANGUAGE_PATTERN) }
            .distinct()
            .chunked(EDITION_LANGUAGE_REQUEST_CONCURRENCY)
            .forEach { batch ->
                val editions = coroutineScope {
                    batch.map { languageCode ->
                        async {
                            runCatching {
                                api.searchEditionMetadata(
                                    query = work.title,
                                    language = languageCode,
                                    responseLanguage = languageCode.toIso6391(),
                                ).toPublicationEditions(languageCode)
                                    .filter { edition -> edition.workId == work.id }
                            }.getOrDefault(emptyList())
                        }
                    }.awaitAll().flatten()
                }.withLocalizedTitles(localizedTitles)
                if (editions.isNotEmpty()) emit(editions)
            }
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
        private const val CACHED_SEARCH_SCAN_LIMIT = 200
        private const val CACHED_SEARCH_RESULT_LIMIT = 50
        private const val MAX_CACHED_WORKS = 300
        private const val SEARCH_CACHE_LIFETIME_MILLIS = 14L * 24L * 60L * 60L * 1000L
        private const val MAX_WORK_EDITION_RECORDS = 500
        private const val EDITION_LANGUAGE_REQUEST_CONCURRENCY = 2
        private val PRIORITY_EDITION_LANGUAGES = listOf(
            "eng", "rus", "ukr", "spa", "fre", "ger", "ita", "pol",
            "por", "chi", "jpn", "ara", "kor", "tur", "dut", "swe",
        )
        private val EDITION_LANGUAGE_PATTERN = Regex("[a-z]{3}")
        private val WORK_POPULARITY_ORDER =
            compareByDescending<Work> { it.editionCount ?: 0 }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    }
}

private fun List<PublicationEdition>.withLocalizedTitles(
    titlesByLanguage: Map<String, String>,
): List<PublicationEdition> = map { edition ->
    titlesByLanguage[edition.languageCode]
        ?.takeIf(String::isNotBlank)
        ?.let { title -> edition.copy(title = title) }
        ?: edition
}

private fun Int?.orEmptyCount(): Int = this ?: 0

private fun String.toCatalogSearchKey(): String = Normalizer
    .normalize(this, Normalizer.Form.NFKD)
    .lowercase(Locale.ROOT)
    .replace(Regex("\\p{M}+"), "")
    .replace('ё', 'е')
    .replace(Regex("[^\\p{L}\\p{N}]+"), "")

private fun String.toIso6391(): String = when (this) {
    "rus" -> "ru"
    "eng" -> "en"
    "ita" -> "it"
    else -> take(2)
}
