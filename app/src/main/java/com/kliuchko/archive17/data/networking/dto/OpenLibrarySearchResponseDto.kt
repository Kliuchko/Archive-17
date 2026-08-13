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
    @SerializedName("ebook_access")
    val ebookAccess: String? = null,
    @SerializedName("editions")
    val editions: OpenLibraryEditionsDto? = null,
)

data class OpenLibraryEditionsDto(
    @SerializedName("docs")
    val docs: List<OpenLibraryEditionSearchDto> = emptyList(),
)

data class OpenLibraryEditionSearchDto(
    @SerializedName("key")
    val key: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("language")
    val languages: List<String>? = null,
    @SerializedName("ebook_access")
    val ebookAccess: String? = null,
    @SerializedName("ia")
    val archiveIdentifiers: List<String>? = null,
    @SerializedName("cover_i")
    val coverId: Int? = null,
    @SerializedName("isbn")
    val isbns: List<String>? = null,
)
