package com.kliuchko.archive17.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.kliuchko.archive17.core.time.TimeProvider
import com.kliuchko.archive17.data.local.dao.LocalBookDao
import com.kliuchko.archive17.data.local.mapper.toDomain
import com.kliuchko.archive17.data.local.mapper.toEntity
import com.kliuchko.archive17.data.reader.ReadiumService
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.DownloadedBookMetadata
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover

class DefaultLocalBookRepository(
    context: Context,
    private val localBookDao: LocalBookDao,
    private val readiumService: ReadiumService,
    private val timeProvider: TimeProvider,
) : LocalBookRepository {
    private val appContext = context.applicationContext
    private val booksDirectory = File(appContext.filesDir, "books")
    private val coversDirectory = File(appContext.filesDir, "covers")

    override fun observeLocalBooks(status: ReadingStatus?): Flow<List<LocalBook>> =
        localBookDao.observeLocalBooks(status).map { books -> books.map { it.toDomain() } }

    override fun observeLocalBook(id: String): Flow<LocalBook?> =
        localBookDao.observeLocalBook(id).map { it?.toDomain() }

    override suspend fun getLocalBook(id: String): LocalBook? =
        localBookDao.getLocalBook(id)?.toDomain()

    override suspend fun getLocalBookByIdentifier(identifier: String): LocalBook? =
        localBookDao.getLocalBookByIdentifier(identifier)?.toDomain()

    override suspend fun importBook(sourceUri: String): RepositoryResult<LocalBook> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(sourceUri)
            val input = appContext.contentResolver.openInputStream(uri)
                ?: return@withContext RepositoryResult.Error("Не удалось прочитать выбранный файл.")
            input.use { importFromStream(it, metadata = null) }
        }

    override suspend fun importDownloadedBook(
        sourceFilePath: String,
        metadata: DownloadedBookMetadata,
    ): RepositoryResult<LocalBook> = withContext(Dispatchers.IO) {
        val source = File(sourceFilePath)
        if (!source.isFile) {
            return@withContext RepositoryResult.Error("Загруженный файл книги больше не доступен.")
        }
        source.inputStream().buffered().use { importFromStream(it, metadata) }
    }

    override suspend fun updateMetadata(
        id: String,
        title: String,
        author: String?,
    ): RepositoryResult<LocalBook> = withContext(Dispatchers.IO) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) {
            return@withContext RepositoryResult.Error("Название книги не может быть пустым.")
        }

        runCatching {
            localBookDao.updateMetadata(
                id = id,
                title = normalizedTitle,
                author = author?.trim()?.takeIf(String::isNotEmpty),
                updatedAt = timeProvider.currentTimeMillis(),
            )
            localBookDao.getLocalBook(id)?.toDomain()
                ?: return@withContext RepositoryResult.Error("Книга больше не доступна.")
        }.fold(
            onSuccess = { RepositoryResult.Success(it) },
            onFailure = { RepositoryResult.Error("Не удалось сохранить сведения о книге.") },
        )
    }

    override suspend fun updateReadingStatus(
        id: String,
        status: ReadingStatus,
    ): RepositoryResult<LocalBook> = withContext(Dispatchers.IO) {
        runCatching {
            localBookDao.updateReadingStatus(id, status, timeProvider.currentTimeMillis())
            localBookDao.getLocalBook(id)?.toDomain()
                ?: return@withContext RepositoryResult.Error("Книга больше не доступна.")
        }.fold(
            onSuccess = { RepositoryResult.Success(it) },
            onFailure = { RepositoryResult.Error("Не удалось изменить статус книги.") },
        )
    }

    override suspend fun deleteLocalBook(id: String): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val book = localBookDao.getLocalBook(id)?.toDomain()
                ?: return@withContext RepositoryResult.Success(Unit)

            try {
                localBookDao.deleteLocalBook(id)
                File(book.filePath).delete()
                book.coverPath?.let(::File)?.delete()
                RepositoryResult.Success(Unit)
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                RepositoryResult.Error("Не удалось удалить книгу с Полки.")
            }
        }

    override suspend fun saveProgression(id: String, progressionJson: String) {
        localBookDao.updateProgression(
            id = id,
            progressionJson = progressionJson,
            readingStatus = ReadingStatus.READING,
            updatedAt = timeProvider.currentTimeMillis(),
        )
    }

    private suspend fun storeCover(id: String, publication: Publication): String? {
        val cover = publication.cover() ?: return null
        coversDirectory.mkdirs()
        val destination = File(coversDirectory, "$id.png")
        FileOutputStream(destination).use { output ->
            val resized = Bitmap.createScaledBitmap(cover, 360, 540, true)
            resized.compress(Bitmap.CompressFormat.PNG, 90, output)
            if (resized !== cover) resized.recycle()
        }
        return destination.absolutePath
    }

    private suspend fun importFromStream(
        input: InputStream,
        metadata: DownloadedBookMetadata?,
    ): RepositoryResult<LocalBook> {
        val id = UUID.randomUUID().toString()
        booksDirectory.mkdirs()
        val destination = File(booksDirectory, "$id.epub")

        try {
            destination.outputStream().buffered().use(input::copyTo)

            val contentHash = destination.sha256()
            backfillContentHashes()
            val duplicate = localBookDao.getLocalBookByContentHash(contentHash)
            if (duplicate != null) {
                destination.delete()
                return RepositoryResult.Error(
                    "Эта книга уже находится на Полке: «${duplicate.title}».",
                )
            }

            val publication = readiumService.open(destination)
            try {
                if (!publication.conformsTo(Publication.Profile.EPUB)) {
                    destination.delete()
                    return RepositoryResult.Error("Пока можно добавлять только книги в формате EPUB.")
                }

                val now = timeProvider.currentTimeMillis()
                val book = LocalBook(
                    id = id,
                    title = metadata?.title?.takeIf(String::isNotBlank)
                        ?: publication.metadata.title?.takeIf(String::isNotBlank)
                        ?: destination.nameWithoutExtension,
                    author = metadata?.author?.takeIf(String::isNotBlank)
                        ?: publication.metadata.authors.firstOrNull()?.name?.takeIf(String::isNotBlank),
                    identifier = publication.metadata.identifier ?: metadata?.identifier,
                    contentHash = contentHash,
                    filePath = destination.absolutePath,
                    coverPath = storeCover(id, publication),
                    progressionJson = null,
                    readingStatus = ReadingStatus.WANT_TO_READ,
                    addedAt = now,
                    updatedAt = now,
                    languageCode = metadata?.languageCode,
                    sourceName = metadata?.sourceName,
                    sourceUrl = metadata?.sourceUrl,
                    isPublicAccess = metadata?.isPublicAccess == true,
                )
                localBookDao.upsertLocalBook(book.toEntity())
                return RepositoryResult.Success(book)
            } finally {
                publication.close()
            }
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            destination.delete()
            return RepositoryResult.Error("Не удалось открыть EPUB. Проверьте, что файл не повреждён.")
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private suspend fun backfillContentHashes() {
        localBookDao.getLocalBooksWithoutContentHash().forEach { book ->
            val file = File(book.filePath)
            if (!file.exists()) return@forEach
            runCatching {
                localBookDao.updateContentHash(book.id, file.sha256())
            }
        }
    }
}
