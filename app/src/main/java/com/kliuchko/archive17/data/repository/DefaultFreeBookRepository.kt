package com.kliuchko.archive17.data.repository

import android.content.Context
import com.kliuchko.archive17.data.networking.CoverMatchEnricher
import com.kliuchko.archive17.data.networking.FreeBookMetadataMerger
import com.kliuchko.archive17.data.networking.api.InternetArchiveApi
import com.kliuchko.archive17.data.networking.api.OpenLibraryApi
import com.kliuchko.archive17.data.networking.api.WikisourceApi
import com.kliuchko.archive17.data.networking.dto.InternetArchiveFileDto
import com.kliuchko.archive17.data.networking.mapper.curatedEnglishStandardEbooks
import com.kliuchko.archive17.data.networking.mapper.curatedAuthorizedRussianBooks
import com.kliuchko.archive17.data.networking.mapper.authorizedPublisherDetails
import com.kliuchko.archive17.data.networking.mapper.isTrustedAuthorizedPublisherEpub
import com.kliuchko.archive17.data.networking.mapper.curatedRussianWikisourceBooks
import com.kliuchko.archive17.data.networking.mapper.standardEbookDetails
import com.kliuchko.archive17.data.networking.mapper.toFreeBooks
import com.kliuchko.archive17.data.networking.mapper.toDomain
import com.kliuchko.archive17.domain.model.DownloadedBookMetadata
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookDetails
import com.kliuchko.archive17.domain.model.FreeBookSource
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.TemporaryBook
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.repository.FreeBookRepository
import com.kliuchko.archive17.domain.repository.BookQueryResolver
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class DefaultFreeBookRepository(
    context: Context,
    private val openLibraryApi: OpenLibraryApi,
    private val internetArchiveApi: InternetArchiveApi,
    private val wikisourceApi: WikisourceApi,
    private val client: OkHttpClient,
    private val localBookRepository: LocalBookRepository,
    private val queryResolver: BookQueryResolver,
) : FreeBookRepository {
    private val downloadDirectory = File(context.applicationContext.cacheDir, "free-book-downloads")
    private val temporaryReadingDirectory = File(
        context.applicationContext.cacheDir,
        TEMPORARY_READING_DIRECTORY,
    )
    private val booksByEditionId = ConcurrentHashMap<String, FreeBook>()
    private val catalogCache = FreeBookCatalogCache(context)
    private val coverMatchEnricher = CoverMatchEnricher()
    private val metadataMerger = FreeBookMetadataMerger(queryResolver, coverMatchEnricher)

    override suspend fun searchBooks(
        query: String,
        languageCode: String,
        page: Int,
    ): RepositoryResult<List<FreeBook>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isNotEmpty() && normalizedQuery.length < MIN_SEARCH_QUERY_LENGTH) {
            return RepositoryResult.Success(emptyList())
        }
        if (!languageCode.matches(ISO_639_2_PATTERN)) {
            return RepositoryResult.Error("Не удалось определить язык бесплатных книг.")
        }

        if (normalizedQuery.isEmpty()) {
            val primarySourceBooks = runCatching {
                searchPrimarySourceBooks(normalizedQuery, languageCode, page)
            }.getOrDefault(emptyList())
            val cachedBooks = catalogCache.read(languageCode, page)
            if (primarySourceBooks.isEmpty() && cachedBooks.isEmpty() && page > 1) {
                val (remoteBooks, remoteError) = fetchBooksSafely {
                    fetchOpenLibraryBooks("", languageCode, page)
                }
                if (remoteError != null) {
                    return RepositoryResult.Error("Не удалось загрузить следующую страницу книг.")
                }
                val books = coverMatchEnricher.enrichFreeBooks(remoteBooks)
                catalogCache.write(languageCode, page, books, System.currentTimeMillis())
                books.forEach { book -> booksByEditionId[book.editionId] = book }
                return RepositoryResult.Success(books)
            }
            val books = combineCatalogBooks(primarySourceBooks, cachedBooks)
            books.forEach { book -> booksByEditionId[book.editionId] = book }
            return RepositoryResult.Success(books)
        }

        val resolvedQueries = queryResolver.resolve(normalizedQuery, languageCode)
            .ifEmpty { listOf(normalizedQuery) }
            .take(MAX_SOURCE_QUERIES)
        val englishAlternativeQuery = if (languageCode != ENGLISH_LANGUAGE && page == 1) {
            resolvedQueries.firstOrNull { query -> query.containsLatinLetters() }
        } else {
            null
        }
        val (openLibraryResult, primarySourceResult, alternativeLanguageResult) = coroutineScope {
            val openLibrary = async {
                fetchBooksSafely {
                    fetchOpenLibraryBooks(resolvedQueries, languageCode, page)
                }
            }
            val primarySource = async {
                fetchBooksSafely {
                    searchPrimarySourceBooks(resolvedQueries, languageCode, page)
                }
            }
            val alternativeLanguage = async {
                englishAlternativeQuery?.let { query ->
                    fetchBooksSafely {
                        fetchOpenLibraryBooks(query, ENGLISH_LANGUAGE, page)
                    }
                } ?: (emptyList<FreeBook>() to null)
            }
            Triple(openLibrary.await(), primarySource.await(), alternativeLanguage.await())
        }
        val (openLibraryBooks, openLibraryError) = openLibraryResult
        val (primarySourceBooks, primarySourceError) = primarySourceResult

        return if (openLibraryError != null && primarySourceError != null) {
            RepositoryResult.Error("Не удалось найти бесплатные книги.")
        } else {
            val alternativeLanguageBooks = alternativeLanguageResult.first
            val books = combineCatalogBooks(
                primarySourceBooks,
                openLibraryBooks + alternativeLanguageBooks,
            )
            books.forEach { book -> booksByEditionId[book.editionId] = book }
            RepositoryResult.Success(books)
        }
    }

    override suspend fun refreshStarterCatalog(
        languageCode: String,
        page: Int,
    ): RepositoryResult<List<FreeBook>> {
        if (!languageCode.matches(ISO_639_2_PATTERN)) {
            return RepositoryResult.Error("Не удалось определить язык бесплатных книг.")
        }
        val safePage = page.coerceAtLeast(1)
        val primary = runCatching {
            searchPrimarySourceBooks("", languageCode, safePage)
        }.getOrDefault(emptyList())
        val cached = catalogCache.read(languageCode, safePage)
        if (
            cached.isNotEmpty() &&
            !catalogCache.shouldRefresh(languageCode, safePage, System.currentTimeMillis())
        ) {
            val books = combineCatalogBooks(primary, cached)
            books.forEach { book -> booksByEditionId[book.editionId] = book }
            return RepositoryResult.Success(books)
        }
        return try {
            val remote = fetchOpenLibraryBooks("", languageCode, safePage)
            catalogCache.write(languageCode, safePage, remote, System.currentTimeMillis())
            val books = combineCatalogBooks(primary, remote)
            books.forEach { book -> booksByEditionId[book.editionId] = book }
            RepositoryResult.Success(books)
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            if (primary.isNotEmpty()) {
                RepositoryResult.Cached(primary, "Сетевой каталог пока не обновился.")
            } else {
                RepositoryResult.Error("Не удалось обновить бесплатный каталог.")
            }
        }
    }

    override suspend fun getBookDetails(
        editionId: String,
    ): RepositoryResult<FreeBookDetails> {
        val book = booksByEditionId[editionId]
            ?: return RepositoryResult.Error("Откройте книгу ещё раз из Каталога.")

        if (book.source == FreeBookSource.WIKISOURCE) {
            return RepositoryResult.Success(
                FreeBookDetails(
                    book = book,
                    description = null,
                    subjects = emptyList(),
                ),
            )
        }
        if (book.source == FreeBookSource.STANDARD_EBOOKS) {
            return standardEbookDetails(book)
                ?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.Success(
                    FreeBookDetails(book = book, description = null, subjects = emptyList()),
                )
        }
        if (book.source == FreeBookSource.AUTHORIZED_PUBLISHER) {
            return authorizedPublisherDetails(book)
                ?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.Success(
                    FreeBookDetails(book = book, description = null, subjects = emptyList()),
                )
        }

        return try {
            val fallback = Work(
                id = book.workId,
                title = book.title,
                authors = book.authors,
                coverId = book.coverId,
                firstPublishYear = book.firstPublishYear,
                editionCount = null,
                editionLanguages = listOf(book.languageCode),
                description = null,
                subjects = emptyList(),
                lastUpdatedAt = null,
            )
            val work = openLibraryApi.getWork(book.workId).toDomain(fallback = fallback)
                ?: fallback
            RepositoryResult.Success(
                FreeBookDetails(
                    book = book,
                    description = work.description,
                    subjects = work.subjects.take(MAX_DETAIL_SUBJECTS),
                ),
            )
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            RepositoryResult.Cached(
                data = FreeBookDetails(
                    book = book,
                    description = null,
                    subjects = emptyList(),
                ),
                message = "Подробное описание пока не загрузилось.",
            )
        }
    }

    override suspend fun keepDownloadableBooks(
        books: List<FreeBook>,
        languageCode: String,
    ): RepositoryResult<List<FreeBook>> {
        if (books.isEmpty()) return RepositoryResult.Success(emptyList())
        if (!languageCode.matches(ISO_639_2_PATTERN)) {
            return RepositoryResult.Error("Не удалось проверить язык EPUB-файлов.")
        }

        val directlyDownloadable = books
            .filter { it.source != FreeBookSource.OPEN_LIBRARY && it.epubDownloadUrl != null }
            .onEach { book -> booksByEditionId[book.editionId] = book }
        val archiveBooks = books.filter { it.source == FreeBookSource.OPEN_LIBRARY }
        val identifiers = archiveBooks
            .map(FreeBook::archiveIdentifier)
            .filter { it.matches(ARCHIVE_IDENTIFIER_PATTERN) }
            .distinct()
        if (identifiers.isEmpty()) return RepositoryResult.Success(directlyDownloadable)

        return try {
            val identifierQuery = identifiers.joinToString(separator = " OR ")
            val response = internetArchiveApi.findEpubIdentifiers(
                query = "identifier:($identifierQuery) AND format:(EPUB OR EPUB3)",
                rows = identifiers.size,
            )
            val downloadableIdentifiers = response.response.docs
                .mapNotNull { it.identifier?.trim()?.takeIf(String::isNotEmpty) }
                .toSet()
            val verifiedBooks = coroutineScope {
                archiveBooks
                    .filter { it.archiveIdentifier in downloadableIdentifiers }
                    .map { book ->
                        async {
                            runCatching {
                                val epub = internetArchiveApi
                                    .getFiles(book.archiveIdentifier)
                                    .selectDownloadableEpub()
                                    ?: return@runCatching null
                                book.copy(
                                    epubFileName = epub.name,
                                    epubSizeBytes = epub.size?.toLongOrNull(),
                                ).also { verified ->
                                    booksByEditionId[verified.editionId] = verified
                                }
                            }.getOrNull()
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
            }
            RepositoryResult.Success(directlyDownloadable + verifiedBooks)
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            if (directlyDownloadable.isNotEmpty()) {
                RepositoryResult.Cached(
                    data = directlyDownloadable,
                    message = "Часть EPUB из Internet Archive пока не удалось проверить.",
                )
            } else {
                RepositoryResult.Error("Не удалось проверить доступность EPUB-файлов.")
            }
        }
    }

    override suspend fun downloadToShelf(book: FreeBook): RepositoryResult<LocalBook> =
        withContext(Dispatchers.IO) {
            downloadDirectory.mkdirs()
            val temporaryFile = File(downloadDirectory, "${UUID.randomUUID()}.epub")
            try {
                when (val download = downloadBookFile(book, temporaryFile)) {
                    is RepositoryResult.Error -> return@withContext download
                    is RepositoryResult.Cached -> return@withContext RepositoryResult.Error(download.message)
                    is RepositoryResult.Success -> Unit
                }

                localBookRepository.importDownloadedBook(
                    sourceFilePath = temporaryFile.absolutePath,
                    metadata = DownloadedBookMetadata(
                        title = book.title,
                        author = book.authors.firstOrNull(),
                        identifier = book.editionId,
                        languageCode = book.languageCode,
                        sourceName = book.sourceName,
                        sourceUrl = book.sourceUrl,
                        isPublicAccess = true,
                    ),
                )
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                RepositoryResult.Error("Не удалось загрузить EPUB. Попробуйте ещё раз позже.")
            } finally {
                temporaryFile.delete()
            }
        }

    override suspend fun downloadForReading(
        book: FreeBook,
    ): RepositoryResult<TemporaryBook> = withContext(Dispatchers.IO) {
        temporaryReadingDirectory.mkdirs()
        val stableName = UUID.nameUUIDFromBytes(book.editionId.toByteArray()).toString()
        val cachedFile = File(temporaryReadingDirectory, "$stableName.epub")
        if (cachedFile.hasEpubSignature()) {
            cachedFile.setLastModified(System.currentTimeMillis())
            return@withContext RepositoryResult.Success(book.toTemporaryBook(cachedFile))
        }

        val partialFile = File(temporaryReadingDirectory, "$stableName.part")
        try {
            partialFile.delete()
            when (val download = downloadBookFile(book, partialFile)) {
                is RepositoryResult.Error -> return@withContext download
                is RepositoryResult.Cached -> return@withContext RepositoryResult.Error(download.message)
                is RepositoryResult.Success -> Unit
            }
            if (!partialFile.hasEpubSignature()) {
                return@withContext RepositoryResult.Error(
                    "Источник вернул файл, который не удалось распознать как EPUB.",
                )
            }
            cachedFile.delete()
            if (!partialFile.renameTo(cachedFile)) {
                partialFile.copyTo(cachedFile, overwrite = true)
            }
            cleanTemporaryReadingCache(except = cachedFile)
            RepositoryResult.Success(book.toTemporaryBook(cachedFile))
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            RepositoryResult.Error("Не удалось открыть книгу. Попробуйте ещё раз позже.")
        } finally {
            partialFile.delete()
        }
    }

    private fun downloadBookFile(
        book: FreeBook,
        destination: File,
    ): RepositoryResult<File> {
        val downloadUrl = book.epubDownloadUrl
            ?.toHttpUrlOrNull()
            ?.takeIf { url -> book.isTrustedDirectDownload(url) }
            ?: book.epubFileName?.let { fileName ->
                "https://archive.org".toHttpUrl().newBuilder()
                    .addPathSegment("download")
                    .addPathSegment(book.archiveIdentifier)
                    .addPathSegment(fileName)
                    .build()
            }
            ?: return RepositoryResult.Error(
                "EPUB этого издания не прошёл проверку доступности.",
            )
        val request = Request.Builder().url(downloadUrl).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return RepositoryResult.Error("Источник временно не отдаёт файл книги.")
            }
            val body = response.body
                ?: return RepositoryResult.Error("Источник вернул пустой файл книги.")
            if (body.contentLength() > MAX_EPUB_BYTES) {
                return RepositoryResult.Error("EPUB-файл слишком большой для загрузки.")
            }
            destination.outputStream().buffered().use { output ->
                body.byteStream().buffered().copyToWithLimit(output, MAX_EPUB_BYTES)
            }
        }
        return RepositoryResult.Success(destination)
    }

    private fun FreeBook.toTemporaryBook(file: File): TemporaryBook = TemporaryBook(
        editionId = editionId,
        title = title,
        filePath = file.absolutePath,
    )

    private fun cleanTemporaryReadingCache(except: File) {
        temporaryReadingDirectory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.extension.equals("epub", ignoreCase = true) && it != except }
            .sortedByDescending(File::lastModified)
            .drop(MAX_TEMPORARY_BOOKS - 1)
            .forEach(File::delete)
    }

    private fun InputStream.copyToWithLimit(
        output: java.io.OutputStream,
        maxBytes: Long,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) return
            copied += count
            check(copied <= maxBytes) { "EPUB exceeds download limit" }
            output.write(buffer, 0, count)
        }
    }

    private suspend fun searchPrimarySourceBooks(
        query: String,
        languageCode: String,
        page: Int,
    ): List<FreeBook> = when (languageCode) {
        RUSSIAN_LANGUAGE -> interleaveBooks(
            curatedAuthorizedRussianBooks(query, page),
            searchWikisourceBooks(query, page),
        )
        ENGLISH_LANGUAGE -> curatedEnglishStandardEbooks(query, page)
        else -> emptyList()
    }

    private suspend fun searchPrimarySourceBooks(
        queries: List<String>,
        languageCode: String,
        page: Int,
    ): List<FreeBook> = coroutineScope {
        val results = queries.map { query ->
            async { fetchBooksSafely { searchPrimarySourceBooks(query, languageCode, page) } }
        }.awaitAll()
        if (results.all { (_, error) -> error != null }) {
            throw results.firstNotNullOf { (_, error) -> error }
        }
        results.flatMap { (books, _) -> books }.distinctBy(FreeBook::editionId)
    }

    private suspend fun fetchOpenLibraryBooks(
        query: String,
        languageCode: String,
        page: Int,
    ): List<FreeBook> {
        val catalogQuery = query.ifBlank { STARTER_CATALOG_QUERY }
        return openLibraryApi.searchFreeBooks(
            query = catalogQuery,
            language = languageCode,
            responseLanguage = languageCode.toIso6391(),
            page = page.coerceAtLeast(1),
        ).toFreeBooks(expectedLanguageCode = languageCode)
    }

    private suspend fun fetchOpenLibraryBooks(
        queries: List<String>,
        languageCode: String,
        page: Int,
    ): List<FreeBook> = coroutineScope {
        val results = queries.map { query ->
            async { fetchBooksSafely { fetchOpenLibraryBooks(query, languageCode, page) } }
        }.awaitAll()
        if (results.all { (_, error) -> error != null }) {
            throw results.firstNotNullOf { (_, error) -> error }
        }
        coverMatchEnricher
            .enrichFreeBooks(results.flatMap { (books, _) -> books })
            .distinctBy(FreeBook::editionId)
    }

    private suspend fun searchWikisourceBooks(
        query: String,
        page: Int,
    ): List<FreeBook> {
        if (query.isBlank()) return curatedRussianWikisourceBooks(page)

        val safeQuery = query.replace('"', ' ').trim()
        if (safeQuery.isEmpty()) return emptyList()
        return wikisourceApi.search(
            query = "intitle:\"$safeQuery\"",
            offset = (page.coerceAtLeast(1) - 1) * WikisourceApi.SEARCH_LIMIT,
        ).toFreeBooks(RUSSIAN_LANGUAGE)
    }

    private fun FreeBook.isTrustedDirectDownload(url: okhttp3.HttpUrl): Boolean =
        url.isHttps &&
        when (source) {
            FreeBookSource.WIKISOURCE -> url.host == WS_EXPORT_HOST
            FreeBookSource.STANDARD_EBOOKS ->
                url.host == STANDARD_EBOOKS_HOST &&
                    url.encodedPath.startsWith("/ebooks/") &&
                    url.encodedPath.endsWith(".epub")
            FreeBookSource.AUTHORIZED_PUBLISHER ->
                isTrustedAuthorizedPublisherEpub(url)
            FreeBookSource.OPEN_LIBRARY -> false
        }

    private suspend fun fetchBooksSafely(
        block: suspend () -> List<FreeBook>,
    ): Pair<List<FreeBook>, Throwable?> = try {
        block() to null
    } catch (exception: Throwable) {
        if (exception is CancellationException) throw exception
        emptyList<FreeBook>() to exception
    }

    private fun interleaveBooks(
        primary: List<FreeBook>,
        secondary: List<FreeBook>,
    ): List<FreeBook> = buildList(primary.size + secondary.size) {
        val maxSize = maxOf(primary.size, secondary.size)
        repeat(maxSize) { index ->
            primary.getOrNull(index)?.let(::add)
            secondary.getOrNull(index)?.let(::add)
        }
    }

    private suspend fun combineCatalogBooks(
        primary: List<FreeBook>,
        secondary: List<FreeBook>,
    ): List<FreeBook> = metadataMerger.merge(interleaveBooks(primary, secondary))

    private fun String.toIso6391(): String = when (this) {
        "rus" -> "ru"
        "eng" -> "en"
        "ukr" -> "uk"
        "ita" -> "it"
        "ger" -> "de"
        "fre" -> "fr"
        "spa" -> "es"
        else -> take(2)
    }

    private fun String.containsLatinLetters(): Boolean = any { it in 'A'..'Z' || it in 'a'..'z' }

    private fun com.kliuchko.archive17.data.networking.dto.InternetArchiveFilesDto
        .selectDownloadableEpub(): InternetArchiveFileDto? = result
        .asSequence()
        .filter { it.format?.contains("EPUB", ignoreCase = true) == true }
        .filter { it.name?.endsWith(".epub", ignoreCase = true) == true }
        .filter { file -> file.size?.toLongOrNull()?.let { it <= MAX_EPUB_BYTES } == true }
        .minByOrNull { it.size?.toLongOrNull() ?: Long.MAX_VALUE }

    private companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 2
        const val MAX_SOURCE_QUERIES = 3
        const val MAX_EPUB_BYTES = 50L * 1024L * 1024L
        const val MAX_TEMPORARY_BOOKS = 4
        const val TEMPORARY_READING_DIRECTORY = "temporary-reading"
        const val STARTER_CATALOG_QUERY = "subject:fiction"
        const val MAX_DETAIL_SUBJECTS = 6
        const val RUSSIAN_LANGUAGE = "rus"
        const val ENGLISH_LANGUAGE = "eng"
        const val WS_EXPORT_HOST = "ws-export.wmcloud.org"
        const val STANDARD_EBOOKS_HOST = "standardebooks.org"
        val ISO_639_2_PATTERN = Regex("[a-z]{3}")
        val ARCHIVE_IDENTIFIER_PATTERN = Regex("[A-Za-z0-9._-]+")
    }
}

internal fun File.hasEpubSignature(): Boolean {
    if (!isFile || length() < MIN_EPUB_BYTES) return false
    return inputStream().buffered().use { input ->
        input.read() == ZIP_MAGIC_FIRST_BYTE && input.read() == ZIP_MAGIC_SECOND_BYTE
    }
}

private const val MIN_EPUB_BYTES = 4L
private const val ZIP_MAGIC_FIRST_BYTE = 0x50
private const val ZIP_MAGIC_SECOND_BYTE = 0x4B
