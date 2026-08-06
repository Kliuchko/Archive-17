package com.kliuchko.archive17.presentation.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.presentation.components.ArchiveBrand
import com.kliuchko.archive17.presentation.components.BookCover
import com.kliuchko.archive17.presentation.components.LocalBookCover
import com.kliuchko.archive17.presentation.details.displayName
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onLocalBookClick: (String) -> Unit,
    onCatalogClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onFileSelected(it.toString()) }
    }
    val openFilePicker = {
        filePicker.launch(arrayOf("application/epub+zip", "application/octet-stream"))
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        ArchiveBrand()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = "Полка", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = shelfSubtitle(uiState.bookCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = openFilePicker, enabled = !uiState.isImporting) {
                Text(if (uiState.isImporting) "Добавляем…" else "+ EPUB")
            }
        }

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryFilter.entries.size) { index ->
                val filter = LibraryFilter.entries[index]
                FilterChip(
                    selected = uiState.selectedFilter == filter,
                    onClick = { viewModel.onFilterSelected(filter) },
                    label = { Text(text = filter.displayName()) },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.isEmpty -> {
                    EmptyShelf(
                        selectedFilter = uiState.selectedFilter,
                        onCatalogClick = onCatalogClick,
                        onImportClick = openFilePicker,
                        isImporting = uiState.isImporting,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(104.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(
                            items = uiState.localBooks,
                            key = { "local-${it.id}" },
                        ) { book ->
                            ShelfLocalBook(
                                book = book,
                                onClick = { onLocalBookClick(book.id) },
                            )
                        }
                        items(
                            items = uiState.books,
                            key = { it.work.id },
                        ) { book ->
                            ShelfBook(
                                book = book,
                                onClick = { onBookClick(book.work.id) },
                            )
                        }
                    }
                }
            }
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun ShelfBook(
    book: LibraryBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BookCover(
            work = book.work,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            width = 94.dp,
            height = 138.dp,
        )
        Text(
            text = book.work.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = book.entry.readingStatus.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ShelfLocalBook(
    book: LocalBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LocalBookCover(
            book = book,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            width = 94.dp,
            height = 138.dp,
        )
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = book.readingStatus.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyShelf(
    selectedFilter: LibraryFilter,
    onCatalogClick: () -> Unit,
    onImportClick: () -> Unit,
    isImporting: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = if (selectedFilter == LibraryFilter.ALL) "На Полке пока пусто" else "В этом разделе пока пусто",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = if (selectedFilter == LibraryFilter.ALL) {
                        "Найдите книгу в каталоге или добавьте свой EPUB с устройства."
                    } else {
                        "Измените статус книги или выберите другой раздел."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedFilter == LibraryFilter.ALL) {
                    Text(
                        text = "Открыть каталог →",
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .clickable(onClick = onCatalogClick),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (isImporting) "ДОБАВЛЯЕМ…" else "ДОБАВИТЬ EPUB ИЗ ФАЙЛА →",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(enabled = !isImporting, onClick = onImportClick),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun shelfSubtitle(count: Int): String =
    when {
        count == 0 -> "Ваше личное книжное пространство"
        count % 10 == 1 && count % 100 != 11 -> "$count книга в архиве"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "$count книги в архиве"
        else -> "$count книг в архиве"
    }
