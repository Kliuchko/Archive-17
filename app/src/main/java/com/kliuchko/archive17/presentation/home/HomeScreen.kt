package com.kliuchko.archive17.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kliuchko.archive17.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.domain.model.LibraryBook
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.presentation.components.ArchiveBrand
import com.kliuchko.archive17.presentation.components.ArchiveNavIcon
import com.kliuchko.archive17.presentation.components.ArchiveNavigationIcon
import com.kliuchko.archive17.presentation.components.ArchiveStateIllustration
import com.kliuchko.archive17.presentation.components.BookCover
import com.kliuchko.archive17.presentation.components.LocalBookCover
import com.kliuchko.archive17.presentation.components.SectionHeading
import com.kliuchko.archive17.presentation.library.LibraryViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime

@Composable
fun HomeScreen(
    onBookClick: (String) -> Unit,
    onLocalBookClick: (String) -> Unit,
    onCatalogClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBooks = buildList {
        uiState.books
            .filter { it.entry.readingStatus == ReadingStatus.READING }
            .forEach { add(CurrentBook.Catalog(it)) }
        uiState.localBooks
            .filter { it.readingStatus == ReadingStatus.READING }
            .forEach { add(CurrentBook.Local(it)) }
    }.sortedByDescending(CurrentBook::updatedAt)
    val greeting = greetingForHour(LocalTime.now().hour)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArchiveBrand()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    ArchiveNavigationIcon(
                        icon = ArchiveNavIcon.PROFILE,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.archive_open),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = readingRoomMessage(currentBooks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (currentBooks.isNotEmpty()) {
            items(
                items = currentBooks.take(2),
                key = CurrentBook::key,
            ) { currentBook ->
                when (currentBook) {
                    is CurrentBook.Catalog -> ContinueReadingCard(
                        book = currentBook.book,
                        onClick = { onBookClick(currentBook.book.work.id) },
                    )
                    is CurrentBook.Local -> ContinueReadingLocalBookCard(
                        book = currentBook.book,
                        onClick = { onLocalBookClick(currentBook.book.id) },
                    )
                }
            }
        } else {
            item {
                EmptyReadingRoom(onCatalogClick = onCatalogClick)
            }
        }

        item {
            SectionHeading(
                title = stringResource(R.string.archive_halls),
                trailing = stringResource(R.string.all_directions),
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 20.dp),
            ) {
                item {
                    HallCard(
                        title = stringResource(R.string.free_books),
                        eyebrow = stringResource(R.string.open_collection),
                        subtitle = stringResource(R.string.search_catalog),
                        onClick = onCatalogClick,
                    )
                }
                item {
                    HallCard(
                        title = stringResource(R.string.by_subscription),
                        eyebrow = stringResource(R.string.reading_room),
                        subtitle = stringResource(R.string.soon),
                    )
                }
                item {
                    HallCard(
                        title = stringResource(R.string.author_books),
                        eyebrow = stringResource(R.string.new_names),
                        subtitle = stringResource(R.string.soon),
                    )
                }
                item {
                    HallCard(
                        title = "Archive 17",
                        eyebrow = stringResource(R.string.archive_original),
                        subtitle = stringResource(R.string.soon),
                        emphasized = true,
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.document_01),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.document_message),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private sealed interface CurrentBook {
    val key: String
    val updatedAt: Long

    data class Catalog(val book: LibraryBook) : CurrentBook {
        override val key: String = "catalog-${book.work.id}"
        override val updatedAt: Long = book.entry.updatedAt
    }

    data class Local(val book: LocalBook) : CurrentBook {
        override val key: String = "local-${book.id}"
        override val updatedAt: Long = book.updatedAt
    }
}

@Composable
private fun ContinueReadingCard(
    book: LibraryBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                work = book.work,
                width = 68.dp,
                height = 100.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(R.string.stopped_here),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = book.work.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = book.work.authors.joinToString().ifBlank { stringResource(R.string.author_unknown) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.continue_book),
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ContinueReadingLocalBookCard(
    book: LocalBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocalBookCover(
                book = book,
                width = 68.dp,
                height = 100.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(R.string.stopped_here),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = book.author ?: stringResource(R.string.author_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.continue_book),
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyReadingRoom(
    onCatalogClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCatalogClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArchiveStateIllustration(height = 92.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = stringResource(R.string.reading_room_quiet),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.reading_room_invitation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.open_catalog),
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun HallCard(
    title: String,
    eyebrow: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = modifier
            .width(184.dp)
            .height(124.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun readingRoomMessage(count: Int): String =
    when (count) {
        0 -> stringResource(R.string.reading_room_empty_message)
        1 -> stringResource(R.string.reading_room_one)
        else -> stringResource(R.string.reading_room_many, count)
    }

@Composable
private fun greetingForHour(hour: Int): String =
    when (hour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..17 -> stringResource(R.string.greeting_day)
        in 18..23 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }
