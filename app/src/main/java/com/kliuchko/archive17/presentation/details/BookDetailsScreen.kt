@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kliuchko.archive17.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kliuchko.archive17.data.networking.CoverSize
import com.kliuchko.archive17.data.networking.CoverUrlBuilder
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.model.Work
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BookDetailsScreen(
    workId: String,
    onBackClick: () -> Unit,
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailsViewModel = koinViewModel(
        parameters = { parametersOf(workId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BookDetailsContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onLibraryClick = onLibraryClick,
        onRefreshClick = viewModel::refresh,
        onStatusClick = viewModel::saveStatus,
        modifier = modifier,
    )
}

@Composable
private fun BookDetailsContent(
    uiState: BookDetailsUiState,
    onBackClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onStatusClick: (ReadingStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailsToolbar(
            onBackClick = onBackClick,
            onLibraryClick = onLibraryClick,
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.work == null -> MissingDetailsState(
                workId = uiState.workId,
                errorMessage = uiState.errorMessage,
                onRefreshClick = onRefreshClick,
            )
            else -> WorkDetails(
                uiState = uiState,
                onStatusClick = onStatusClick,
                onRefreshClick = onRefreshClick,
            )
        }
    }
}

@Composable
private fun DetailsToolbar(
    onBackClick: () -> Unit,
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onBackClick) {
            Text(text = "Back")
        }
        OutlinedButton(onClick = onLibraryClick) {
            Text(text = "Library")
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MissingDetailsState(
    workId: String,
    errorMessage: String?,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Book details unavailable",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = errorMessage ?: "No cached data exists for $workId.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRefreshClick) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun WorkDetails(
    uiState: BookDetailsUiState,
    onStatusClick: (ReadingStatus) -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val work = uiState.work ?: return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CoverImage(work = work)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = work.authors.joinToString().ifBlank { "Unknown author" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = work.publicationLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        StatusIndicators(uiState = uiState)

        if (uiState.message != null) {
            InfoText(text = uiState.message)
        }
        if (uiState.errorMessage != null) {
            InfoText(text = uiState.errorMessage)
        }

        Section(title = "Reading status") {
            ReadingStatusSelector(
                selectedStatus = uiState.selectedStatus,
                enabled = uiState.canSave,
                onStatusClick = onStatusClick,
            )
        }

        Section(title = "Description") {
            Text(
                text = work.description?.takeIf { it.isNotBlank() } ?: "No description available.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Section(title = "Edition language") {
            Text(
                text = uiState.languageLabel,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Section(title = "Subjects") {
            SubjectList(subjects = work.subjects)
        }

        OutlinedButton(
            onClick = onRefreshClick,
            enabled = !uiState.isRefreshing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = if (uiState.isRefreshing) "Refreshing" else "Refresh details")
        }
    }
}

@Composable
private fun CoverImage(
    work: Work,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val coverUrl = CoverUrlBuilder.build(work.coverId, CoverSize.LARGE)

    if (coverUrl == null) {
        Box(
            modifier = modifier
                .size(width = 112.dp, height = 168.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No cover",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = coverUrl,
            contentDescription = work.title,
            modifier = modifier
                .size(width = 112.dp, height = 168.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun StatusIndicators(
    uiState: BookDetailsUiState,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (uiState.isCached) {
            AssistChip(
                onClick = {},
                label = { Text(text = "Cached") },
            )
        }
        if (uiState.isStale) {
            AssistChip(
                onClick = {},
                label = { Text(text = "May be outdated") },
            )
        }
        if (uiState.isRefreshing) {
            AssistChip(
                onClick = {},
                label = { Text(text = "Refreshing") },
            )
        }
    }
}

@Composable
private fun ReadingStatusSelector(
    selectedStatus: ReadingStatus?,
    enabled: Boolean,
    onStatusClick: (ReadingStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReadingStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusClick(status) },
                enabled = enabled,
                label = { Text(text = status.displayName()) },
            )
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
            text = "No subjects listed.",
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
        subjects.take(MAX_VISIBLE_SUBJECTS).forEach { subject ->
            AssistChip(
                onClick = {},
                label = { Text(text = subject) },
            )
        }
    }
}

@Composable
private fun Section(
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
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun InfoText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Work.publicationLabel(): String =
    firstPublishYear
        ?.let { "First published $it" }
        ?: "First publication year unavailable"

private const val MAX_VISIBLE_SUBJECTS = 8
