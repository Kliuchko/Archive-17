package com.kliuchko.archive17.domain.model

data class Work(
    val id: String,
    val title: String,
    val authors: List<String>,
    val coverId: Int?,
    val firstPublishYear: Int?,
    val editionCount: Int?,
    val editionLanguages: List<String>,
    val description: String?,
    val subjects: List<String>,
    val lastUpdatedAt: Long?,
)
