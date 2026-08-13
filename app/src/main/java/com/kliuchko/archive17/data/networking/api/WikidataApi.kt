package com.kliuchko.archive17.data.networking.api

import com.kliuchko.archive17.data.networking.dto.WikidataEntitiesResponseDto
import com.kliuchko.archive17.data.networking.dto.WikidataSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WikidataApi {
    @GET("w/api.php")
    suspend fun searchEntities(
        @Query("search") search: String,
        @Query("language") language: String,
        @Query("uselang") userLanguage: String = language,
        @Query("action") action: String = "wbsearchentities",
        @Query("type") type: String = "item",
        @Query("limit") limit: Int = SEARCH_LIMIT,
        @Query("format") format: String = "json",
        @Query("origin") origin: String = "*",
    ): WikidataSearchResponseDto

    @GET("w/api.php")
    suspend fun getEntities(
        @Query("ids") ids: String,
        @Query("languages") languages: String,
        @Query("action") action: String = "wbgetentities",
        @Query("props") properties: String = "labels|aliases|descriptions",
        @Query("languagefallback") languageFallback: Int = 1,
        @Query("format") format: String = "json",
        @Query("origin") origin: String = "*",
    ): WikidataEntitiesResponseDto

    companion object {
        const val BASE_URL = "https://www.wikidata.org/"
        const val SEARCH_LIMIT = 8
    }
}
