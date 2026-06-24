package com.kliuchko.archive17.data.networking.api

import com.kliuchko.archive17.data.networking.dto.OpenLibrarySearchResponseDto
import com.kliuchko.archive17.data.networking.dto.OpenLibraryWorkDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenLibraryApi {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fields") fields: String = SEARCH_FIELDS,
        @Query("limit") limit: Int = SEARCH_LIMIT,
    ): OpenLibrarySearchResponseDto

    @GET("works/{workId}.json")
    suspend fun getWork(
        @Path("workId") workId: String,
    ): OpenLibraryWorkDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
        const val SEARCH_LIMIT = 20
        const val SEARCH_FIELDS = "key,title,author_name,cover_i,first_publish_year,edition_count,language"
    }
}
