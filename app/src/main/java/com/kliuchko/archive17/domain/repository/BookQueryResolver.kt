package com.kliuchko.archive17.domain.repository

interface BookQueryResolver {
    suspend fun resolve(query: String, preferredLanguageCode: String? = null): List<String>

    suspend fun resolveOriginalLanguage(
        query: String,
        preferredLanguageCode: String? = null,
    ): String? = null

    suspend fun resolveTranslationMetadata(
        query: String,
        preferredLanguageCode: String? = null,
        languageCodes: List<String> = emptyList(),
    ): BookTranslationMetadata = BookTranslationMetadata(
        originalLanguageCode = resolveOriginalLanguage(query, preferredLanguageCode),
    )
}

data class BookTranslationMetadata(
    val originalLanguageCode: String? = null,
    val titlesByLanguage: Map<String, String> = emptyMap(),
)
