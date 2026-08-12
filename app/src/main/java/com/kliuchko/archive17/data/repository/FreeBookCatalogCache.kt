package com.kliuchko.archive17.data.repository

import android.content.Context
import com.google.gson.Gson
import com.kliuchko.archive17.domain.model.FreeBook

internal class FreeBookCatalogCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val gson = Gson()

    fun read(languageCode: String, page: Int): List<FreeBook> = runCatching {
        preferences.getString(cacheKey(languageCode, page), null)
            ?.let { gson.fromJson(it, CachedCatalogPage::class.java) }
            ?.books
            .orEmpty()
    }.getOrDefault(emptyList())

    fun shouldRefresh(languageCode: String, page: Int, nowMillis: Long): Boolean {
        val updatedAt = preferences.getLong(updatedAtKey(languageCode, page), 0L)
        return nowMillis - updatedAt >= CACHE_LIFETIME_MILLIS
    }

    fun write(languageCode: String, page: Int, books: List<FreeBook>, nowMillis: Long) {
        if (books.isEmpty()) return
        preferences.edit()
            .putString(cacheKey(languageCode, page), gson.toJson(CachedCatalogPage(books)))
            .putLong(updatedAtKey(languageCode, page), nowMillis)
            .apply()
    }

    private fun cacheKey(languageCode: String, page: Int): String =
        "catalog_${languageCode}_${page.coerceAtLeast(1)}"

    private fun updatedAtKey(languageCode: String, page: Int): String =
        "${cacheKey(languageCode, page)}_updated_at"

    private data class CachedCatalogPage(val books: List<FreeBook>)

    private companion object {
        const val PREFERENCES_NAME = "free_book_catalog_cache"
        const val CACHE_LIFETIME_MILLIS = 12L * 60L * 60L * 1000L
    }
}
