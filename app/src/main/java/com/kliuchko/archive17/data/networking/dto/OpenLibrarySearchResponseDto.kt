package com.kliuchko.archive17.data.networking.dto

import com.google.gson.annotations.SerializedName

data class OpenLibrarySearchResponseDto(
    @SerializedName("docs")
    val docs: List<OpenLibrarySearchDocDto> = emptyList(),
)

data class OpenLibrarySearchDocDto(
    @SerializedName("key")
    val key: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("author_name")
    val authorNames: List<String>?,
    @SerializedName("cover_i")
    val coverId: Int?,
    @SerializedName("first_publish_year")
    val firstPublishYear: Int?,
    @SerializedName("edition_count")
    val editionCount: Int?,
    @SerializedName("language")
    val languages: List<String>?,
)
