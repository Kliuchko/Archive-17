package com.kliuchko.archive17.data.networking.api

import com.kliuchko.archive17.data.networking.dto.WikisourceSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WikisourceApi {
    @GET("w/api.php")
    suspend fun search(
        @Query("srsearch") query: String,
        @Query("sroffset") offset: Int,
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("formatversion") formatVersion: Int = 2,
        @Query("list") list: String = "search",
        @Query("srnamespace") namespace: Int = 0,
        @Query("srlimit") limit: Int = SEARCH_LIMIT,
    ): WikisourceSearchResponseDto

    companion object {
        const val BASE_URL = "https://ru.wikisource.org/"
        const val SEARCH_LIMIT = 20
    }
}
