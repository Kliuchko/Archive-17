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
        @Query("page") page: Int = 1,
    ): OpenLibrarySearchResponseDto

    @GET("search.json")
    suspend fun searchFreeBooks(
        @Query("q") query: String,
        @Query("fields") fields: String = FREE_SEARCH_FIELDS,
        @Query("limit") limit: Int = FREE_SEARCH_LIMIT,
        @Query("page") page: Int = 1,
        @Query("language") language: String,
        @Query("ebook_access") ebookAccess: String = "public",
        @Query("lang") responseLanguage: String,
    ): OpenLibrarySearchResponseDto

    @GET("search.json")
    suspend fun searchEditionMetadata(
        @Query("q") query: String,
        @Query("fields") fields: String = EDITION_METADATA_FIELDS,
        @Query("limit") limit: Int = EDITION_METADATA_LIMIT,
        @Query("page") page: Int = 1,
        @Query("language") language: String,
        @Query("lang") responseLanguage: String,
    ): OpenLibrarySearchResponseDto

    @GET("works/{workId}.json")
    suspend fun getWork(
        @Path("workId") workId: String,
    ): OpenLibraryWorkDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
        const val SEARCH_LIMIT = 20
        const val FREE_SEARCH_LIMIT = 12
        const val EDITION_METADATA_LIMIT = 5
        const val SEARCH_FIELDS = "key,title,author_name,cover_i,first_publish_year,edition_count,language"
        const val FREE_SEARCH_FIELDS =
            "key,title,author_name,cover_i,first_publish_year,language,ebook_access," +
                "editions,editions.key,editions.title,editions.language,editions.ebook_access," +
                "editions.ia,editions.cover_i,editions.isbn," +
                "editions.publish_date,editions.publisher,editions.contributor"
        const val EDITION_METADATA_FIELDS =
            "key,title,author_name,cover_i,first_publish_year,language,editions," +
                "editions.key,editions.title,editions.language,editions.cover_i," +
                "editions.publish_date,editions.publisher,editions.contributor"
    }
}
