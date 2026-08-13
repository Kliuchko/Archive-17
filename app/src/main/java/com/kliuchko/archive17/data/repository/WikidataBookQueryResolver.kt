package com.kliuchko.archive17.data.repository

import android.content.Context
import android.util.Base64
import com.kliuchko.archive17.data.networking.api.WikidataApi
import com.kliuchko.archive17.data.networking.dto.WikidataEntityDto
import com.kliuchko.archive17.data.networking.dto.WikidataSearchItemDto
import com.kliuchko.archive17.domain.repository.BookQueryResolver
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class WikidataBookQueryResolver(
    context: Context,
    private val api: WikidataApi,
) : BookQueryResolver {
    private val preferences = context.applicationContext.getSharedPreferences(
        QUERY_CACHE_PREFERENCES,
        Context.MODE_PRIVATE,
    )
    private val mutex = Mutex()

    override suspend fun resolve(query: String, preferredLanguageCode: String?): List<String> {
        val original = query.trim()
        if (original.length < MIN_QUERY_LENGTH) return listOf(original).filter(String::isNotEmpty)
        val language = original.detectSearchLanguage(preferredLanguageCode)
        val cacheKey = cacheKey(original, language)
        readCache(cacheKey)?.let { return it.withOriginalFirst(original) }

        return mutex.withLock {
            readCache(cacheKey)?.let { return@withLock it.withOriginalFirst(original) }
            val resolved = try {
                val search = api.searchEntities(original, language)
                val candidateIds = selectWikidataCandidateIds(original, search.search)
                if (candidateIds.isEmpty()) {
                    listOf(original)
                } else {
                    val entities = api.getEntities(
                        ids = candidateIds.joinToString("|"),
                        languages = requestedLanguages(language, preferredLanguageCode),
                    )
                    buildResolvedBookQueries(
                        original = original,
                        candidateIds = candidateIds,
                        entities = entities.entities,
                        preferredLanguageCode = preferredLanguageCode,
                    )
                }
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                listOf(original)
            }
            writeCache(cacheKey, resolved)
            resolved
        }
    }

    override suspend fun resolveOriginalLanguage(
        query: String,
        preferredLanguageCode: String?,
    ): String? {
        val original = query.trim()
        if (original.length < MIN_QUERY_LENGTH) return null
        val language = original.detectSearchLanguage(preferredLanguageCode)
        val key = cacheKey(original, language)
        readOriginalLanguageCache(key)?.let { return it }
        return mutex.withLock {
            readOriginalLanguageCache(key)?.let { return@withLock it }
            val resolved = try {
                val search = api.searchEntities(original, language)
                val candidateIds = selectWikidataCandidateIds(original, search.search)
                if (candidateIds.isEmpty()) {
                    null
                } else {
                    val entities = api.getEntities(
                        ids = candidateIds.joinToString("|"),
                        languages = requestedLanguages(language, preferredLanguageCode),
                    )
                    candidateIds.firstNotNullOfOrNull { id ->
                        entities.entities[id]?.originalLanguageCode()
                    }
                }
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                null
            }
            writeOriginalLanguageCache(key, resolved)
            resolved
        }
    }

    private fun readCache(key: String): List<String>? = runCatching {
        val raw = preferences.getString(key, null) ?: return null
        val json = JSONObject(raw)
        val savedAt = json.optLong(CACHE_TIMESTAMP, 0L)
        if (System.currentTimeMillis() - savedAt > CACHE_TTL_MILLIS) return null
        val values = json.getJSONArray(CACHE_QUERIES)
        buildList {
            for (index in 0 until values.length()) {
                values.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }.takeIf(List<String>::isNotEmpty)
    }.getOrNull()

    private fun writeCache(key: String, queries: List<String>) {
        val json = JSONObject()
            .put(CACHE_TIMESTAMP, System.currentTimeMillis())
            .put(CACHE_QUERIES, JSONArray(queries))
        preferences.edit().putString(key, json.toString()).apply()
    }

    private fun readOriginalLanguageCache(key: String): String? = runCatching {
        val raw = preferences.getString("original-$key", null) ?: return null
        val json = JSONObject(raw)
        val savedAt = json.optLong(CACHE_TIMESTAMP, 0L)
        if (System.currentTimeMillis() - savedAt > CACHE_TTL_MILLIS) return null
        json.optString(CACHE_ORIGINAL_LANGUAGE).takeIf(String::isNotBlank)
    }.getOrNull()

    private fun writeOriginalLanguageCache(key: String, languageCode: String?) {
        if (languageCode == null) return
        val json = JSONObject()
            .put(CACHE_TIMESTAMP, System.currentTimeMillis())
            .put(CACHE_ORIGINAL_LANGUAGE, languageCode)
        preferences.edit().putString("original-$key", json.toString()).apply()
    }

    private fun cacheKey(query: String, language: String): String {
        val value = "$language|${query.toQueryKey()}"
        return Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val QUERY_CACHE_PREFERENCES = "book_query_resolver_cache"
        const val CACHE_TIMESTAMP = "saved_at"
        const val CACHE_QUERIES = "queries"
        const val CACHE_ORIGINAL_LANGUAGE = "original_language"
        const val CACHE_TTL_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }
}

private fun WikidataEntityDto.originalLanguageCode(): String? = claims["P364"]
    .orEmpty()
    .firstNotNullOfOrNull { claim ->
        claim.mainsnak?.datavalue?.value
            ?.takeIf { value -> value.isJsonObject }
            ?.asJsonObject
            ?.get("id")
            ?.takeIf { id -> id.isJsonPrimitive }
            ?.asString
            ?.let(ORIGINAL_LANGUAGE_CODES::get)
    }

internal fun selectWikidataCandidateIds(
    query: String,
    results: List<WikidataSearchItemDto>,
): List<String> {
    val queryKey = query.toQueryKey()
    return results
        .mapNotNull { item ->
            val id = item.id?.takeIf { it.matches(WIKIDATA_ID_PATTERN) } ?: return@mapNotNull null
            val labelKey = item.label.toQueryKey()
            val matchKey = item.match?.text.toQueryKey()
            val description = item.description.orEmpty().lowercase(Locale.ROOT)
            val exact = labelKey == queryKey || matchKey == queryKey
            val bookRelated = BOOK_DESCRIPTION_MARKERS.any(description::contains)
            val excluded = NON_BOOK_DESCRIPTION_MARKERS.any(description::contains)
            if (excluded || (!exact && !bookRelated)) return@mapNotNull null
            val score = (if (exact) 100 else 0) +
                (if (bookRelated) 30 else 0) -
                (if (excluded) 60 else 0)
            id to score
        }
        .sortedByDescending(Pair<String, Int>::second)
        .take(MAX_WIKIDATA_CANDIDATES)
        .map(Pair<String, Int>::first)
}

internal fun buildResolvedBookQueries(
    original: String,
    candidateIds: List<String>,
    entities: Map<String, WikidataEntityDto>,
    preferredLanguageCode: String? = null,
): List<String> = buildList {
    add(original)
    val candidates = candidateIds.mapNotNull(entities::get)
    val preferredLanguage = preferredLanguageCode?.toIso6391()
    val priorityLanguages = listOfNotNull(preferredLanguage, "en").distinct()
    priorityLanguages.forEach { language ->
        candidates.forEach { entity ->
            entity.labels[language]?.value?.takeIf(String::isNotBlank)?.let(::add)
        }
    }
    priorityLanguages.forEach { language ->
        candidates.forEach { entity ->
            entity.aliases[language].orEmpty().forEach { alias ->
                alias.value?.takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }
    PREFERRED_LABEL_LANGUAGES.filterNot(priorityLanguages::contains).forEach { language ->
        candidates.forEach { entity ->
            entity.labels[language]?.value?.takeIf(String::isNotBlank)?.let(::add)
        }
        candidates.forEach { entity ->
            entity.aliases[language].orEmpty().forEach { alias ->
                alias.value?.takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }
    candidates.forEach { entity ->
        entity.labels.values.forEach { it.value?.takeIf(String::isNotBlank)?.let(::add) }
    }
    candidates.forEach { entity ->
        entity.aliases.values.flatten().forEach {
            it.value?.takeIf(String::isNotBlank)?.let(::add)
        }
    }
}.distinctBy(String::toQueryKey).take(MAX_RESOLVED_QUERIES)

private fun requestedLanguages(inputLanguage: String, preferredLanguageCode: String?): String =
    buildList {
        add(inputLanguage)
        preferredLanguageCode?.toIso6391()?.let(::add)
        addAll(PREFERRED_LABEL_LANGUAGES)
    }.distinct().joinToString("|")

private fun String.detectSearchLanguage(preferredLanguageCode: String?): String = when {
    any { it in UKRAINIAN_LETTERS } -> "uk"
    any { it in '\u0400'..'\u04FF' } -> preferredLanguageCode
        ?.toIso6391()
        ?.takeIf { it == "ru" || it == "uk" }
        ?: "ru"
    else -> preferredLanguageCode?.toIso6391() ?: "en"
}

private fun String.toIso6391(): String = when (lowercase(Locale.ROOT)) {
    "rus" -> "ru"
    "eng" -> "en"
    "ukr" -> "uk"
    "ita" -> "it"
    "deu", "ger" -> "de"
    "fra", "fre" -> "fr"
    "spa" -> "es"
    else -> take(2)
}

private fun List<String>.withOriginalFirst(original: String): List<String> =
    (listOf(original) + this).distinctBy(String::toQueryKey).take(MAX_RESOLVED_QUERIES)

private fun String?.toQueryKey(): String = Normalizer
    .normalize(orEmpty(), Normalizer.Form.NFKD)
    .lowercase(Locale.ROOT)
    .replace(COMBINING_MARKS_PATTERN, "")
    .replace('ё', 'е')
    .replace(NON_ALPHANUMERIC_PATTERN, "")

private val WIKIDATA_ID_PATTERN = Regex("Q[1-9][0-9]*")
private val COMBINING_MARKS_PATTERN = Regex("\\p{M}+")
private val NON_ALPHANUMERIC_PATTERN = Regex("[^\\p{L}\\p{N}]+")
private const val MAX_WIKIDATA_CANDIDATES = 3
private const val MAX_RESOLVED_QUERIES = 8
private val PREFERRED_LABEL_LANGUAGES = listOf("en", "ru", "uk", "it", "de", "fr", "es")
private val BOOK_DESCRIPTION_MARKERS = listOf(
    "book", "novel", "novella", "story", "stories", "poem", "literary", "writer", "author",
    "книга", "роман", "повесть", "рассказ", "поэма", "литератур", "писатель", "автор",
    "libro", "romanzo", "scrittore", "autore", "livre", "écrivain", "buch", "schriftsteller",
)
private val NON_BOOK_DESCRIPTION_MARKERS = listOf(
    "film", "television", "video game", "fictional character", "фильм", "телесериал", "персонаж",
)
private val ORIGINAL_LANGUAGE_CODES = mapOf(
    "Q1860" to "eng",
    "Q7737" to "rus",
    "Q8798" to "ukr",
    "Q150" to "fre",
    "Q188" to "ger",
    "Q652" to "ita",
    "Q1321" to "spa",
    "Q809" to "pol",
    "Q5287" to "jpn",
    "Q7850" to "chi",
    "Q13955" to "ara",
    "Q5146" to "por",
    "Q9176" to "kor",
    "Q256" to "tur",
    "Q7411" to "dut",
    "Q9027" to "swe",
    "Q1412" to "fin",
    "Q9056" to "ces",
)
private const val UKRAINIAN_LETTERS = "іїєґІЇЄҐ"
