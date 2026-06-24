package com.kliuchko.archive17.data.remote

object CoverUrlBuilder {
    private const val BASE_URL = "https://covers.openlibrary.org/b/id"

    fun build(
        coverId: Int?,
        size: CoverSize = CoverSize.MEDIUM,
    ): String? = coverId?.let { "$BASE_URL/$it-${size.apiValue}.jpg" }
}

enum class CoverSize(val apiValue: String) {
    SMALL("S"),
    MEDIUM("M"),
    LARGE("L"),
}
