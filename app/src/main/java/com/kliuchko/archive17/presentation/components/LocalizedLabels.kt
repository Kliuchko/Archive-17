package com.kliuchko.archive17.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kliuchko.archive17.R
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.presentation.library.LibraryFilter

@Composable
fun ReadingStatus.localizedDisplayName(): String = stringResource(
    when (this) {
        ReadingStatus.WANT_TO_READ -> R.string.status_deferred
        ReadingStatus.READING -> R.string.status_reading
        ReadingStatus.FINISHED -> R.string.status_finished
    },
)

@Composable
fun LibraryFilter.localizedDisplayName(): String =
    readingStatus?.localizedDisplayName() ?: stringResource(R.string.filter_all)
