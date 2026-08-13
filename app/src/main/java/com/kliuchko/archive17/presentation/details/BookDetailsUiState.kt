package com.kliuchko.archive17.presentation.details

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.PublicationEdition
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.TemporaryBook
import com.kliuchko.archive17.domain.model.Work

data class BookDetailsUiState(
    val workId: String,
    val work: Work? = null,
    val selectedStatus: ReadingStatus? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isCached: Boolean = false,
    val isStale: Boolean = false,
    val editions: List<PublicationEdition> = emptyList(),
    val originalLanguageCode: String? = null,
    val freeBooksByEditionId: Map<String, FreeBook> = emptyMap(),
    val isLoadingEditions: Boolean = false,
    val showAllEditionVariants: Boolean = false,
    val downloadingEditionId: String? = null,
    val readingEditionId: String? = null,
    val downloadedBookId: String? = null,
    val temporaryBook: TemporaryBook? = null,
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = work != null && !isRefreshing

    val preferredFreeBook: FreeBook?
        get() = editions.firstNotNullOfOrNull { edition ->
            freeBooksByEditionId[edition.id]
        }

    val visibleEditions: List<PublicationEdition>
        get() = if (showAllEditionVariants) editions else editions.take(PREVIEW_EDITION_COUNT)

    val hiddenEditionCount: Int
        get() = (editions.size - PREVIEW_EDITION_COUNT).coerceAtLeast(0)

    val languageLabel: String
        get() = work?.editionLanguages
            .orEmpty()
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString()
            ?: "Не указан"

    companion object {
        const val PREVIEW_EDITION_COUNT = 3
    }
}

fun ReadingStatus.displayName(): String =
    when (this) {
        ReadingStatus.WANT_TO_READ -> "Отложено"
        ReadingStatus.READING -> "Читаю"
        ReadingStatus.FINISHED -> "Прочитано"
    }
