package com.kliuchko.archive17.data.networking.dto

data class WikisourceSearchResponseDto(
    val query: WikisourceQueryDto? = null,
)

data class WikisourceQueryDto(
    val search: List<WikisourceSearchResultDto> = emptyList(),
)

data class WikisourceSearchResultDto(
    val pageid: Int? = null,
    val ns: Int? = null,
    val title: String? = null,
    val snippet: String? = null,
)
