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
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
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

    override suspend fun getLocalBook(id: String): LocalBook? =
        localBookDao.getLocalBook(id)?.toDomain()

    override suspend fun importBook(sourceUri: String): RepositoryResult<LocalBook> =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            booksDirectory.mkdirs()
            val destination = File(booksDirectory, "$id.epub")

            try {
                val uri = Uri.parse(sourceUri)
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use(input::copyTo)
                } ?: return@withContext RepositoryResult.Error("Не удалось прочитать выбранный файл.")

                val publication = readiumService.open(destination)
                try {
                    if (!publication.conformsTo(Publication.Profile.EPUB)) {
                        destination.delete()
                        return@withContext RepositoryResult.Error("Пока можно добавлять только книги в формате EPUB.")
                    }

                    val now = timeProvider.currentTimeMillis()
                    val book = LocalBook(
                        id = id,
                        title = publication.metadata.title?.takeIf(String::isNotBlank)
                            ?: destination.nameWithoutExtension,
                        author = publication.metadata.authors.firstOrNull()?.name
                            ?.takeIf(String::isNotBlank),
                        identifier = publication.metadata.identifier,
                        filePath = destination.absolutePath,
                        coverPath = storeCover(id, publication),
                        progressionJson = null,
                        readingStatus = ReadingStatus.WANT_TO_READ,
                        addedAt = now,
                        updatedAt = now,
                    )
                    localBookDao.upsertLocalBook(book.toEntity())
                    RepositoryResult.Success(book)
                } finally {
                    publication.close()
                }
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                destination.delete()
                RepositoryResult.Error("Не удалось открыть EPUB. Проверьте, что файл не повреждён.")
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
}
