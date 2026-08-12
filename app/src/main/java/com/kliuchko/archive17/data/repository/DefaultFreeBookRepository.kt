package com.kliuchko.archive17.data.repository

import android.content.Context
import com.kliuchko.archive17.data.networking.api.InternetArchiveApi
import com.kliuchko.archive17.data.networking.api.OpenLibraryApi
import com.kliuchko.archive17.data.networking.dto.InternetArchiveFileDto
import com.kliuchko.archive17.data.networking.mapper.toFreeBooks
import com.kliuchko.archive17.data.networking.mapper.toDomain
import com.kliuchko.archive17.domain.model.DownloadedBookMetadata
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookDetails
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.repository.FreeBookRepository
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
import okhttp3.OkHttpClient
import okhttp3.Request

class DefaultFreeBookRepository(
    context: Context,
    private val openLibraryApi: OpenLibraryApi,
    private val internetArchiveApi: InternetArchiveApi,
    private val client: OkHttpClient,
    private val localBookRepository: LocalBookRepository,
) : FreeBookRepository {
    private val downloadDirectory = File(context.applicationContext.cacheDir, "free-book-downloads")
    private val booksByEditionId = ConcurrentHashMap<String, FreeBook>()

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

        return try {
            val catalogQuery = normalizedQuery.ifBlank { STARTER_CATALOG_QUERY }
            val searchQuery = "$catalogQuery ebook_access:public language:$languageCode"
            val books = openLibraryApi.searchFreeBooks(
                query = searchQuery,
                language = languageCode.toIso6391(),
                page = page.coerceAtLeast(1),
            ).toFreeBooks(expectedLanguageCode = languageCode)
            books.forEach { book -> booksByEditionId[book.editionId] = book }
            RepositoryResult.Success(books)
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            RepositoryResult.Error("Не удалось найти бесплатные книги.")
        }
    }

    override suspend fun getBookDetails(
        editionId: String,
    ): RepositoryResult<FreeBookDetails> {
        val book = booksByEditionId[editionId]
            ?: return RepositoryResult.Error("Откройте книгу ещё раз из Каталога.")

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

        val identifiers = books
            .map(FreeBook::archiveIdentifier)
            .filter { it.matches(ARCHIVE_IDENTIFIER_PATTERN) }
            .distinct()
        if (identifiers.isEmpty()) return RepositoryResult.Success(emptyList())

        return try {
            val identifierQuery = identifiers.joinToString(separator = " OR ")
            val languageQuery = languageCode.toInternetArchiveLanguageQuery()
            val response = internetArchiveApi.findEpubIdentifiers(
                query = "identifier:($identifierQuery) AND format:(EPUB OR EPUB3) " +
                    "AND language:($languageQuery)",
                rows = identifiers.size,
            )
            val downloadableIdentifiers = response.response.docs
                .mapNotNull { it.identifier?.trim()?.takeIf(String::isNotEmpty) }
                .toSet()
            val verifiedBooks = coroutineScope {
                books
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
            RepositoryResult.Success(verifiedBooks)
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            RepositoryResult.Error("Не удалось проверить доступность EPUB-файлов.")
        }
    }

    override suspend fun downloadToShelf(book: FreeBook): RepositoryResult<LocalBook> =
        withContext(Dispatchers.IO) {
            downloadDirectory.mkdirs()
            val temporaryFile = File(downloadDirectory, "${UUID.randomUUID()}.epub")
            try {
                val fileName = book.epubFileName
                    ?: return@withContext RepositoryResult.Error(
                        "EPUB этого издания не прошёл проверку доступности.",
                    )
                val downloadUrl = "https://archive.org".toHttpUrl().newBuilder()
                    .addPathSegment("download")
                    .addPathSegment(book.archiveIdentifier)
                    .addPathSegment(fileName)
                    .build()
                val request = Request.Builder().url(downloadUrl).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext RepositoryResult.Error(
                            "Источник временно не отдаёт файл книги.",
                        )
                    }
                    val body = response.body
                        ?: return@withContext RepositoryResult.Error(
                            "Источник вернул пустой файл книги.",
                        )
                    if (body.contentLength() > MAX_EPUB_BYTES) {
                        return@withContext RepositoryResult.Error("EPUB-файл слишком большой для загрузки.")
                    }
                    temporaryFile.outputStream().buffered().use { output ->
                        body.byteStream().buffered().copyToWithLimit(output, MAX_EPUB_BYTES)
                    }
                }

                localBookRepository.importDownloadedBook(
                    sourceFilePath = temporaryFile.absolutePath,
                    metadata = DownloadedBookMetadata(
                        title = book.title,
                        author = book.authors.firstOrNull(),
                        identifier = book.editionId,
                        languageCode = book.languageCode,
                        sourceName = SOURCE_NAME,
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

    private fun String.toInternetArchiveLanguageQuery(): String = when (this) {
        "rus" -> "rus OR Russian"
        "eng" -> "eng OR English"
        "ukr" -> "ukr OR Ukrainian"
        "ita" -> "ita OR Italian"
        "ger" -> "ger OR German"
        "fre" -> "fre OR French"
        "spa" -> "spa OR Spanish"
        else -> this
    }

    private fun com.kliuchko.archive17.data.networking.dto.InternetArchiveFilesDto
        .selectDownloadableEpub(): InternetArchiveFileDto? = result
        .asSequence()
        .filter { it.format?.contains("EPUB", ignoreCase = true) == true }
        .filter { it.name?.endsWith(".epub", ignoreCase = true) == true }
        .filter { file -> file.size?.toLongOrNull()?.let { it <= MAX_EPUB_BYTES } == true }
        .minByOrNull { it.size?.toLongOrNull() ?: Long.MAX_VALUE }

    private companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 2
        const val MAX_EPUB_BYTES = 50L * 1024L * 1024L
        const val SOURCE_NAME = "Open Library · Internet Archive"
        const val STARTER_CATALOG_QUERY = "subject:fiction"
        const val MAX_DETAIL_SUBJECTS = 6
        val ISO_639_2_PATTERN = Regex("[a-z]{3}")
        val ARCHIVE_IDENTIFIER_PATTERN = Regex("[A-Za-z0-9._-]+")
    }
}
