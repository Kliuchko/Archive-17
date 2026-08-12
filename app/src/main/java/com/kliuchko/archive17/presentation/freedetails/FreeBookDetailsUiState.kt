package com.kliuchko.archive17.presentation.freedetails

import com.kliuchko.archive17.domain.model.FreeBookDetails

data class FreeBookDetailsUiState(
    val details: FreeBookDetails? = null,
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val downloadedBookId: String? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)
