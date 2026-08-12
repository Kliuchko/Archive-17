package com.kliuchko.archive17.presentation.localdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kliuchko.archive17.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.presentation.components.LocalBookCover
import com.kliuchko.archive17.presentation.components.localizedDisplayName
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LocalBookDetailsScreen(
    bookId: String,
    onBackClick: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocalBookDetailsViewModel = koinViewModel(parameters = { parametersOf(bookId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBackClick()
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
            OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.back)) }
            Text(
                text = stringResource(
                    if (uiState.book?.sourceName == null) {
                        R.string.my_file
                    } else {
                        R.string.open_collection_badge
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 96.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.book == null -> Text(
                text = uiState.errorMessage ?: stringResource(R.string.book_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )

            else -> {
                val book = checkNotNull(uiState.book)
                LocalBookCover(
                    book = book,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    width = 148.dp,
                    height = 220.dp,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = book.author ?: stringResource(R.string.author_unknown),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.epub_offline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (book.sourceName != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BookFact(
                            label = stringResource(R.string.source_label),
                            value = book.sourceName,
                        )
                        if (book.isPublicAccess) {
                            BookFact(
                                label = stringResource(R.string.access_label),
                                value = stringResource(R.string.public_access),
                            )
                        }
                        book.languageCode?.let { code ->
                            BookFact(
                                label = stringResource(R.string.book_language_label),
                                value = localizedBookLanguage(code),
                            )
                        }
                        if (book.isPublicAccess) {
                            Text(
                                text = stringResource(R.string.public_access_details),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        book.sourceUrl?.let { sourceUrl ->
                            TextButton(
                                onClick = { uriHandler.openUri(sourceUrl) },
                                modifier = Modifier.align(Alignment.Start),
                            ) {
                                Text(stringResource(R.string.open_source_page))
                            }
                        }
                    }
                }

                Button(
                    onClick = onReadClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (book.progressionJson == null) R.string.read else R.string.continue_book_plain,
                        ),
                    )
                }

                Text(
                    text = stringResource(R.string.shelf_status),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadingStatus.entries.forEach { status ->
                        FilterChip(
                            selected = book.readingStatus == status,
                            onClick = { viewModel.updateStatus(status) },
                            enabled = !uiState.isSaving,
                            label = { Text(status.localizedDisplayName()) },
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showEditDialog = true },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.edit_book_metadata)) }

                TextButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.remove_from_shelf), color = MaterialTheme.colorScheme.error)
                }

                uiState.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                uiState.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    uiState.book?.let { book ->
        if (showEditDialog) {
            EditMetadataDialog(
                initialTitle = book.title,
                initialAuthor = book.author.orEmpty(),
                onDismiss = { showEditDialog = false },
                onSave = { title, author ->
                    viewModel.updateMetadata(title, author)
                    showEditDialog = false
                },
            )
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.remove_book_title)) },
                text = {
                    Text(stringResource(R.string.remove_book_message))
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteBook()
                    }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
private fun BookFact(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun localizedBookLanguage(code: String): String = stringResource(
    when (code) {
        "rus" -> R.string.language_russian
        "eng" -> R.string.language_english
        "ita" -> R.string.language_italian
        else -> R.string.language_other
    },
)

@Composable
private fun EditMetadataDialog(
    initialTitle: String,
    initialAuthor: String,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var author by remember(initialAuthor) { mutableStateOf(initialAuthor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.book_information)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.book_title)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.book_author)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, author.takeIf(String::isNotBlank)) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
