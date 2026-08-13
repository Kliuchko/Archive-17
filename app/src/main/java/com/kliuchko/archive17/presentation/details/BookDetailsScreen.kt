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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kliuchko.archive17.domain.model.EditionAccessMode
import com.kliuchko.archive17.domain.model.EditionAvailability
import com.kliuchko.archive17.domain.model.PublicationEdition
import com.kliuchko.archive17.domain.model.TemporaryBook
import com.kliuchko.archive17.presentation.components.BookCover
import com.kliuchko.archive17.presentation.components.localizedDisplayName
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BookDetailsScreen(
    workId: String,
    onBackClick: () -> Unit,
    onBookReady: (String) -> Unit,
    onTemporaryBookReady: (TemporaryBook) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailsViewModel = koinViewModel(parameters = { parametersOf(workId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.downloadedBookId) {
        uiState.downloadedBookId?.let { bookId ->
            onBookReady(bookId)
            viewModel.onDownloadedBookHandled()
        }
    }
    LaunchedEffect(uiState.temporaryBook) {
        uiState.temporaryBook?.let { book ->
            onTemporaryBookReady(book)
            viewModel.onTemporaryBookHandled()
        }
    }

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

                val preferredFreeBook = uiState.preferredFreeBook
                if (preferredFreeBook != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.readNow(preferredFreeBook) },
                            enabled = uiState.readingEditionId == null &&
                                uiState.downloadingEditionId == null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.read_now))
                        }
                        Button(
                            onClick = { viewModel.addEditionToShelf(preferredFreeBook) },
                            enabled = uiState.readingEditionId == null &&
                                uiState.downloadingEditionId == null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.download_to_shelf))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.purchase_options_soon))
                    }
                }

                if (preferredFreeBook == null && uiState.selectedStatus == null) {
                    Button(
                        onClick = { viewModel.saveStatus(ReadingStatus.WANT_TO_READ) },
                        enabled = uiState.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.place_in_archive))
                    }
                } else if (uiState.selectedStatus != null) {
                    Text(
                        text = stringResource(R.string.in_archive),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }

                DetailsSection(title = stringResource(R.string.available_editions)) {
                    Text(
                        text = stringResource(R.string.available_editions_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    work.editionCount?.let { count ->
                        Text(
                            text = stringResource(R.string.catalog_editions_found, count),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    when {
                        uiState.isLoadingEditions -> CircularProgressIndicator()
                        uiState.editions.isEmpty() -> Text(
                            text = stringResource(R.string.edition_details_pending),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> uiState.editions.forEach { edition ->
                            WorkEditionCard(
                                edition = edition,
                                isFree = edition.id in uiState.freeBooksByEditionId,
                                isBusy = uiState.readingEditionId != null ||
                                    uiState.downloadingEditionId != null,
                                onRead = uiState.freeBooksByEditionId[edition.id]?.let { book ->
                                    { viewModel.readNow(book) }
                                },
                                onAddToShelf = uiState.freeBooksByEditionId[edition.id]?.let { book ->
                                    { viewModel.addEditionToShelf(book) }
                                },
                            )
                        }
                    }
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
private fun WorkEditionCard(
    edition: PublicationEdition,
    isFree: Boolean,
    isBusy: Boolean,
    onRead: (() -> Unit)?,
    onAddToShelf: (() -> Unit)?,
) {
    val access = edition.accessOptions.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(edition.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = listOfNotNull(
                    editionLanguageName(edition.languageCode),
                    edition.publishedYear?.toString(),
                    edition.translator?.let { stringResource(R.string.edition_translator_value, it) },
                    edition.publisher,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isFree) {
                    stringResource(R.string.edition_access_free)
                } else {
                    accessLabel(access?.mode, access?.availability)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            access?.providerName?.let { provider ->
                Text(
                    text = provider,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onRead != null && onAddToShelf != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onRead,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.read_now))
                    }
                    Button(
                        onClick = onAddToShelf,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.download_to_shelf))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.purchase_options_soon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun editionLanguageName(code: String): String = stringResource(
    when (code) {
        "rus" -> R.string.language_russian
        "eng" -> R.string.language_english
        "ita" -> R.string.language_italian
        else -> R.string.language_other
    },
)

@Composable
private fun accessLabel(
    mode: EditionAccessMode?,
    availability: EditionAvailability?,
): String = when (availability) {
    EditionAvailability.AVAILABLE -> when (mode) {
        EditionAccessMode.FREE -> stringResource(R.string.edition_access_free)
        EditionAccessMode.SUBSCRIPTION -> stringResource(R.string.edition_access_subscription)
        EditionAccessMode.PURCHASE -> stringResource(R.string.edition_access_purchase)
        EditionAccessMode.PARTNER_PURCHASE -> stringResource(R.string.edition_access_partner)
        EditionAccessMode.OWNED_FILE -> stringResource(R.string.edition_access_owned_file)
        else -> stringResource(R.string.edition_access_unavailable)
    }
    EditionAvailability.EXTERNAL_ONLY -> stringResource(R.string.edition_access_external)
    EditionAvailability.REGION_RESTRICTED ->
        stringResource(R.string.edition_access_region_restricted)
    EditionAvailability.COMING_SOON -> stringResource(R.string.edition_access_coming_soon)
    EditionAvailability.UNAVAILABLE, null -> stringResource(R.string.edition_access_unavailable)
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
