package com.kliuchko.archive17.presentation.freedetails

import com.kliuchko.archive17.domain.model.FreeBookDetails
import com.kliuchko.archive17.domain.model.TemporaryBook

data class FreeBookDetailsUiState(
    val details: FreeBookDetails? = null,
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val isPreparingToRead: Boolean = false,
    val downloadedBookId: String? = null,
    val temporaryBook: TemporaryBook? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)
