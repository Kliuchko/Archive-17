package com.kliuchko.archive17.data.networking.dto

data class InternetArchiveMetadataDto(
    val files: List<InternetArchiveFileDto> = emptyList(),
)

data class InternetArchiveFileDto(
    val name: String? = null,
    val format: String? = null,
    val size: String? = null,
)

data class InternetArchiveSearchDto(
    val response: InternetArchiveSearchResponseDto = InternetArchiveSearchResponseDto(),
)

data class InternetArchiveSearchResponseDto(
    val docs: List<InternetArchiveSearchDocumentDto> = emptyList(),
)

data class InternetArchiveSearchDocumentDto(
    val identifier: String? = null,
)
