package com.kliuchko.archive17.data.networking.mapper

import java.util.Locale

internal fun String.withCatalogQueryAlias(): String {
    val key = lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")
    return CATALOG_QUERY_ALIASES[key] ?: this
}

private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

private val CATALOG_QUERY_ALIASES = mapOf(
    "аллатра" to "AllatRa",
    "стальнаякрыса" to "The Stainless Steel Rat",
    "гарригаррисон" to "Harry Harrison",
)
