package com.kliuchko.archive17.domain.repository

interface BookQueryResolver {
    suspend fun resolve(query: String, preferredLanguageCode: String? = null): List<String>

    suspend fun resolveOriginalLanguage(
        query: String,
        preferredLanguageCode: String? = null,
    ): String? = null
}
