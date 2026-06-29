@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kliuchko.archive17.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kliuchko.archive17.data.networking.CoverSize
import com.kliuchko.archive17.data.networking.CoverUrlBuilder
import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.presentation.details.displayName
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onBookClick = onBookClick,
        onSearchClick = onSearchClick,
        modifier = modifier,
    )
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onBookClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "My library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(onClick = onSearchClick) {
                Text(text = "Search")
            }
        }

        FilterRow(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = onFilterSelected,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LibraryState(
                uiState = uiState,
                onBookClick = onBookClick,
                onSearchClick = onSearchClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.displayName()) },
            )
        }
    }
}

@Composable
private fun LibraryState(
    uiState: LibraryUiState,
    onBookClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.isEmpty -> {
            EmptyLibraryState(
                selectedFilter = uiState.selectedFilter,
                onSearchClick = onSearchClick,
                modifier = modifier,
            )
        }

        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = uiState.books,
                    key = { it.work.id },
                ) { libraryBook ->
                    LibraryBookItem(
                        libraryBook = libraryBook,
                        onClick = { onBookClick(libraryBook.work.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    selectedFilter: LibraryFilter,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = if (selectedFilter == LibraryFilter.ALL) {
        "Your library is empty."
    } else {
        "No saved books match this filter."
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onSearchClick,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(text = "Find books")
        }
    }
}

@Composable
private fun LibraryBookItem(
    libraryBook: LibraryBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverImage(work = libraryBook.work)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = libraryBook.work.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = libraryBook.work.authors.joinToString().ifBlank { "Unknown author" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = libraryBook.entry.readingStatus.displayName(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = libraryBook.work.metadataLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CoverImage(
    work: Work,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val coverUrl = CoverUrlBuilder.build(work.coverId, CoverSize.MEDIUM)

    if (coverUrl == null) {
        Box(
            modifier = modifier
                .size(width = 56.dp, height = 84.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No cover",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = coverUrl,
            contentDescription = work.title,
            modifier = modifier
                .size(width = 56.dp, height = 84.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun Work.metadataLine(): String {
    val published = firstPublishYear?.let { "First published $it" }
    val editions = editionCount?.let { "$it editions" }

    return listOfNotNull(published, editions)
        .joinToString(" | ")
        .ifBlank { "Publication metadata unavailable" }
}
