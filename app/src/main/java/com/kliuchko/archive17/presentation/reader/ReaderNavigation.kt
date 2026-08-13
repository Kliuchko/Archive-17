package com.kliuchko.archive17.presentation.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kliuchko.archive17.R
import org.readium.r2.shared.publication.Locator

internal enum class ReaderNavigationSection(@StringRes val titleRes: Int) {
    CONTENTS(R.string.reader_contents),
    SEARCH(R.string.reader_search),
    BOOKMARKS(R.string.reader_bookmarks),
}

internal data class ReaderTocEntry(
    val title: String,
    val locator: Locator,
    val depth: Int,
)

internal data class ReaderSearchResult(
    val locator: Locator,
    val excerpt: String,
)

@Composable
internal fun ReaderNavigationPanel(
    section: ReaderNavigationSection,
    tocEntries: List<ReaderTocEntry>,
    bookmarks: List<Locator>,
    searchQuery: String,
    searchResults: List<ReaderSearchResult>,
    searchInProgress: Boolean,
    searchSubmitted: Boolean,
    searchUnavailable: Boolean,
    onSectionChange: (ReaderNavigationSection) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAddBookmark: () -> Unit,
    onRemoveBookmark: (Locator) -> Unit,
    onGo: (Locator) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReaderNavigationSection.entries.forEach { item ->
                FilterChip(
                    selected = section == item,
                    onClick = { onSectionChange(item) },
                    label = { Text(androidx.compose.ui.res.stringResource(item.titleRes)) },
                )
            }
        }

        when (section) {
            ReaderNavigationSection.CONTENTS -> TocContent(tocEntries, onGo)
            ReaderNavigationSection.SEARCH -> SearchContent(
                query = searchQuery,
                results = searchResults,
                inProgress = searchInProgress,
                submitted = searchSubmitted,
                unavailable = searchUnavailable,
                onQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                onGo = onGo,
            )
            ReaderNavigationSection.BOOKMARKS -> BookmarksContent(
                bookmarks = bookmarks,
                onAdd = onAddBookmark,
                onRemove = onRemoveBookmark,
                onGo = onGo,
            )
        }
    }
}

@Composable
private fun TocContent(entries: List<ReaderTocEntry>, onGo: (Locator) -> Unit) {
    if (entries.isEmpty()) {
        ReaderEmptyMessage(R.string.reader_contents_empty)
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
        items(entries, key = { "${it.locator.href}-${it.depth}-${it.title}" }) { entry ->
            TextButton(
                onClick = { onGo(entry.locator) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (entry.depth * 14).dp),
            ) {
                Text(
                    text = entry.title,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchContent(
    query: String,
    results: List<ReaderSearchResult>,
    inProgress: Boolean,
    submitted: Boolean,
    unavailable: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onGo: (Locator) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(androidx.compose.ui.res.stringResource(R.string.reader_search_hint)) },
            enabled = !unavailable,
        )
        TextButton(
            onClick = onSearch,
            enabled = query.trim().length >= 2 && !inProgress && !unavailable,
        ) {
            Text(androidx.compose.ui.res.stringResource(R.string.reader_find))
        }
    }
    when {
        unavailable -> ReaderEmptyMessage(R.string.reader_search_unavailable)
        inProgress -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        submitted && results.isEmpty() ->
            ReaderEmptyMessage(R.string.reader_search_empty)
        else -> LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
            items(results, key = { it.locator.toJSON().toString() }) { result ->
                TextButton(
                    onClick = { onGo(result.locator) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        result.locator.title?.takeIf(String::isNotBlank)?.let { title ->
                            Text(text = title, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = result.excerpt,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksContent(
    bookmarks: List<Locator>,
    onAdd: () -> Unit,
    onRemove: (Locator) -> Unit,
    onGo: (Locator) -> Unit,
) {
    TextButton(onClick = onAdd) {
        Text(androidx.compose.ui.res.stringResource(R.string.reader_add_bookmark))
    }
    if (bookmarks.isEmpty()) {
        ReaderEmptyMessage(R.string.reader_bookmarks_empty)
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
        items(bookmarks, key = { it.toJSON().toString() }) { locator ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onGo(locator) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = bookmarkTitle(locator),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { onRemove(locator) }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun ReaderEmptyMessage(@StringRes messageRes: Int) {
    Text(
        text = androidx.compose.ui.res.stringResource(messageRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun bookmarkTitle(locator: Locator): String = locator.title?.takeIf(String::isNotBlank)
    ?: locator.locations.position?.let { "${it}" }
    ?: "${(locator.locations.totalProgression.orZero() * 100).toInt()}%"

private fun Double?.orZero(): Double = this ?: 0.0
