package com.kliuchko.archive17.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class OpenLibraryWorkDto(
    @SerializedName("key")
    val key: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: JsonElement?,
    @SerializedName("subjects")
    val subjects: List<String>?,
)
