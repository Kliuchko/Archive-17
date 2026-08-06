package com.kliuchko.archive17.presentation.details

import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work

data class BookDetailsUiState(
    val workId: String,
    val work: Work? = null,
    val selectedStatus: ReadingStatus? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isCached: Boolean = false,
    val isStale: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = work != null && !isRefreshing

    val languageLabel: String
        get() = work?.editionLanguages
            .orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.joinToString()
            ?: "Не указан"
}

fun ReadingStatus.displayName(): String =
    when (this) {
        ReadingStatus.WANT_TO_READ -> "Отложено"
        ReadingStatus.READING -> "Читаю"
        ReadingStatus.FINISHED -> "Прочитано"
    }
