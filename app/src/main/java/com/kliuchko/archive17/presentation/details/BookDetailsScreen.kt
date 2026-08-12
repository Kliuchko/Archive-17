@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kliuchko.archive17.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.R
import com.kliuchko.archive17.data.networking.CoverSize
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.presentation.components.BookCover
import com.kliuchko.archive17.presentation.components.localizedDisplayName
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BookDetailsScreen(
    workId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailsViewModel = koinViewModel(parameters = { parametersOf(workId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBackClick) {
                Text(text = stringResource(R.string.back))
            }
            Text(
                text = stringResource(
                    if (uiState.selectedStatus == null) R.string.catalog_uppercase else R.string.in_archive_uppercase,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.work == null -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.book_unavailable),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.book_unavailable_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = viewModel::refresh) {
                        Text(text = stringResource(R.string.retry))
                    }
                }
            }

            else -> {
                val work = checkNotNull(uiState.work)
                val message = uiState.message
                val errorMessage = uiState.errorMessage
                BookCover(
                    work = work,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    width = 148.dp,
                    height = 220.dp,
                    coverSize = CoverSize.LARGE,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = work.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = work.authors.joinToString().ifBlank { stringResource(R.string.author_unknown) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = work.firstPublishYear?.toString() ?: stringResource(R.string.year_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                StatusIndicators(uiState = uiState)

                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.read_soon))
                }

                if (uiState.selectedStatus == null) {
                    Button(
                        onClick = { viewModel.saveStatus(ReadingStatus.WANT_TO_READ) },
                        enabled = uiState.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.place_in_archive))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.in_archive),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }

                DetailsSection(title = stringResource(R.string.shelf_status)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReadingStatus.entries.forEach { status ->
                            FilterChip(
                                selected = uiState.selectedStatus == status,
                                onClick = { viewModel.saveStatus(status) },
                                enabled = uiState.canSave,
                                label = { Text(text = status.localizedDisplayName()) },
                            )
                        }
                    }
                }

                DetailsSection(title = stringResource(R.string.about_book)) {
                    Text(
                        text = work.description?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.description_missing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                DetailsSection(title = stringResource(R.string.edition_language)) {
                    Text(
                        text = uiState.languageLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                DetailsSection(title = stringResource(R.string.subjects)) {
                    SubjectList(subjects = work.subjects)
                }

                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicators(
    uiState: BookDetailsUiState,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (uiState.isCached) {
            AssistChip(onClick = {}, label = { Text(text = stringResource(R.string.saved_locally)) })
        }
        if (uiState.isStale) {
            AssistChip(onClick = {}, label = { Text(text = stringResource(R.string.details_may_be_stale)) })
        }
        if (uiState.isRefreshing) {
            AssistChip(onClick = {}, label = { Text(text = stringResource(R.string.updating)) })
        }
    }
}

@Composable
private fun SubjectList(
    subjects: List<String>,
    modifier: Modifier = Modifier,
) {
    if (subjects.isEmpty()) {
        Text(
            text = stringResource(R.string.subjects_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        subjects.take(8).forEach { subject ->
            AssistChip(onClick = {}, label = { Text(text = subject) })
        }
    }
}

@Composable
private fun DetailsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        content()
    }
}
