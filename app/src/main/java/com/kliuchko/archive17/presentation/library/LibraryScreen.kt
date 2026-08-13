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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kliuchko.archive17.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.presentation.components.ArchiveBrand
import com.kliuchko.archive17.presentation.components.ArchiveFloatingHeader
import com.kliuchko.archive17.presentation.components.ArchiveLoadingState
import com.kliuchko.archive17.presentation.components.ArchiveStateIllustration
import com.kliuchko.archive17.presentation.components.BookCover
import com.kliuchko.archive17.presentation.components.LocalBookCover
import com.kliuchko.archive17.presentation.components.localizedDisplayName
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onLocalBookClick: (String) -> Unit,
    onLocalBookDetailsClick: (String) -> Unit,
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

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerPadding = with(LocalDensity.current) { headerHeightPx.toDp() } + 12.dp

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            when {
                uiState.isLoading -> {
                    ArchiveLoadingState(
                        label = stringResource(R.string.shelf_loading),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = headerPadding),
                    )
                }

                uiState.isEmpty -> {
                    EmptyShelf(
                        selectedFilter = uiState.selectedFilter,
                        onCatalogClick = onCatalogClick,
                        onImportClick = openFilePicker,
                        isImporting = uiState.isImporting,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = headerPadding),
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(104.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = headerPadding,
                            bottom = 24.dp,
                        ),
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
                                onDetailsClick = { onLocalBookDetailsClick(book.id) },
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
        ArchiveFloatingHeader(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.shelf_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = shelfSubtitle(uiState.bookCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    ArchiveBrand()
                    TextButton(onClick = openFilePicker, enabled = !uiState.isImporting) {
                        Text(
                            stringResource(
                                if (uiState.isImporting) R.string.adding else R.string.add_epub,
                            ),
                        )
                    }
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
                        label = { Text(text = filter.localizedDisplayName()) },
                    )
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
            text = book.entry.readingStatus.localizedDisplayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ShelfLocalBook(
    book: LocalBook,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
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
            text = book.readingStatus.localizedDisplayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.book_information),
            modifier = Modifier.clickable(onClick = onDetailsClick),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ArchiveStateIllustration(height = 126.dp)
                Text(
                    text = stringResource(
                        if (selectedFilter == LibraryFilter.ALL) R.string.shelf_empty else R.string.shelf_section_empty,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    text = if (selectedFilter == LibraryFilter.ALL) {
                        stringResource(R.string.shelf_empty_body)
                    } else {
                        stringResource(R.string.shelf_section_empty_body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                if (selectedFilter == LibraryFilter.ALL) {
                    Text(
                        text = stringResource(R.string.open_catalog),
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .clickable(onClick = onCatalogClick),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(
                            if (isImporting) R.string.adding_uppercase else R.string.add_epub_from_file,
                        ),
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

@Composable
private fun shelfSubtitle(count: Int): String =
    when {
        count == 0 -> stringResource(R.string.shelf_personal_space)
        count % 10 == 1 && count % 100 != 11 -> stringResource(R.string.shelf_count_one, count)
        count % 10 in 2..4 && count % 100 !in 12..14 -> stringResource(R.string.shelf_count_few, count)
        else -> stringResource(R.string.shelf_count_many, count)
    }
