package com.kliuchko.archive17.data.networking.api

import com.kliuchko.archive17.data.networking.dto.InternetArchiveMetadataDto
import com.kliuchko.archive17.data.networking.dto.InternetArchiveSearchDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface InternetArchiveApi {
    @GET("metadata/{identifier}")
    suspend fun getMetadata(
        @Path("identifier") identifier: String,
    ): InternetArchiveMetadataDto

    @GET("advancedsearch.php")
    suspend fun findEpubIdentifiers(
        @Query("q") query: String,
        @Query("fl[]") field: String = "identifier",
        @Query("rows") rows: Int,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json",
    ): InternetArchiveSearchDto

    companion object {
        const val BASE_URL = "https://archive.org/"
    }
}
