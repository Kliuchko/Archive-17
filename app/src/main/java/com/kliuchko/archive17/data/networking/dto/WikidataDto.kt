package com.kliuchko.archive17.data.networking.dto

data class WikidataSearchResponseDto(
    val search: List<WikidataSearchItemDto> = emptyList(),
)

data class WikidataSearchItemDto(
    val id: String? = null,
    val label: String? = null,
    val description: String? = null,
    val match: WikidataMatchDto? = null,
)

data class WikidataMatchDto(
    val text: String? = null,
    val language: String? = null,
)

data class WikidataEntitiesResponseDto(
    val entities: Map<String, WikidataEntityDto> = emptyMap(),
)

data class WikidataEntityDto(
    val id: String? = null,
    val labels: Map<String, WikidataTermDto> = emptyMap(),
    val aliases: Map<String, List<WikidataTermDto>> = emptyMap(),
    val descriptions: Map<String, WikidataTermDto> = emptyMap(),
)

data class WikidataTermDto(
    val language: String? = null,
    val value: String? = null,
)
