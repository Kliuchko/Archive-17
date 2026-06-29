package com.kliuchko.archive17.domain.model

data class WorkDetails(
    val work: Work?,
    val libraryEntry: LibraryEntry?,
    val isCached: Boolean,
    val isStale: Boolean,
)
