package com.kliuchko.archive17.data.networking.dto

import com.google.gson.annotations.SerializedName

data class OpenLibraryWorkEditionsDto(
    @SerializedName("size")
    val size: Int? = null,
    @SerializedName("entries")
    val entries: List<OpenLibraryWorkEditionDto> = emptyList(),
)

data class OpenLibraryWorkEditionDto(
    @SerializedName("key")
    val key: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("languages")
    val languages: List<OpenLibraryKeyDto>? = null,
    @SerializedName("publish_date")
    val publishDate: String? = null,
    @SerializedName("publishers")
    val publishers: List<String>? = null,
    @SerializedName("contributions")
    val contributions: List<String>? = null,
    @SerializedName("covers")
    val coverIds: List<Int>? = null,
)

data class OpenLibraryKeyDto(
    @SerializedName("key")
    val key: String? = null,
)
