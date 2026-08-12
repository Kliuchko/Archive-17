package com.kliuchko.archive17.presentation.localdetails

import com.kliuchko.archive17.domain.model.LocalBook

data class LocalBookDetailsUiState(
    val book: LocalBook? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)
