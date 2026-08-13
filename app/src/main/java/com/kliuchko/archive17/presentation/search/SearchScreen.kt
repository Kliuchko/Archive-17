package com.kliuchko.archive17.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.R
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.TextEditionType
import com.kliuchko.archive17.domain.model.TemporaryBook
import com.kliuchko.archive17.presentation.components.ArchiveBrand
import com.kliuchko.archive17.presentation.components.ArchiveFloatingHeader
import com.kliuchko.archive17.presentation.components.BookCover
import com.kliuchko.archive17.presentation.components.bookLanguageName
import com.kliuchko.archive17.presentation.components.EmptyMessage
import com.kliuchko.archive17.presentation.components.FreeBookCover
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    onBookClick: (String) -> Unit,
    onFreeBookClick: (String) -> Unit,
    onFreeBookReady: (String) -> Unit,
    onTemporaryBookReady: (TemporaryBook) -> Unit,
    externalQuery: String? = null,
    onExternalQueryHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(externalQuery) {
        externalQuery?.takeIf(String::isNotBlank)?.let { requestedQuery ->
            viewModel.searchByAuthor(requestedQuery)
            onExternalQueryHandled()
        }
    }

    LaunchedEffect(uiState.downloadedBookId) {
        uiState.downloadedBookId?.let { bookId ->
            onFreeBookReady(bookId)
            viewModel.onDownloadedBookHandled()
        }
    }

    LaunchedEffect(uiState.temporaryBook) {
        uiState.temporaryBook?.let { book ->
            onTemporaryBookReady(book)
            viewModel.onTemporaryBookHandled()
        }
    }

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerPadding = with(LocalDensity.current) { headerHeightPx.toDp() } + 10.dp

    Box(modifier = modifier.fillMaxSize()) {
        SearchResults(
            uiState = uiState,
            onBookClick = onBookClick,
            onFreeBookClick = onFreeBookClick,
            onDownloadBook = viewModel::downloadBook,
            onReadNow = viewModel::readNow,
            onAddWorkToShelf = viewModel::addWorkToShelf,
            onLoadNextPage = viewModel::loadNextPage,
            onRetryNextPage = viewModel::retryNextPage,
            contentTopPadding = headerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        )
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.catalog_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.catalog_question),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ArchiveBrand()
            }
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text(text = stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Text(
                        text = "⌕",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = if (uiState.query.isNotEmpty()) {
                    {
                        val clearSearchDescription = stringResource(R.string.search_clear)
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Text(
                                text = "×",
                                modifier = Modifier.semantics {
                                    contentDescription = clearSearchDescription
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    null
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = uiState.selectedMode == CatalogMode.FREE,
                    onClick = { viewModel.onModeSelected(CatalogMode.FREE) },
                    label = { Text(stringResource(R.string.catalog_free_filter)) },
                )
                FilterChip(
                    selected = uiState.selectedMode == CatalogMode.ALL,
                    onClick = { viewModel.onModeSelected(CatalogMode.ALL) },
                    label = { Text(stringResource(R.string.catalog_all_filter)) },
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onBookClick: (String) -> Unit,
    onFreeBookClick: (String) -> Unit,
    onDownloadBook: (FreeBook) -> Unit,
    onReadNow: (FreeBook) -> Unit,
    onAddWorkToShelf: (Work) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryNextPage: () -> Unit,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.showMinimumQueryState -> {
            EmptyMessage(
                title = stringResource(R.string.minimum_query_title),
                body = stringResource(R.string.minimum_query_body),
                modifier = modifier.padding(top = contentTopPadding),
            )
        }

        uiState.errorMessage != null &&
            uiState.books.isEmpty() &&
            uiState.freeBooks.isEmpty() &&
            uiState.alternativeEditionBooks.isEmpty() &&
            uiState.otherLanguageBooks.isEmpty() &&
            uiState.otherFreeBooks.isEmpty() -> {
            EmptyMessage(
                title = stringResource(R.string.catalog_unavailable),
                body = uiState.errorMessage,
                modifier = modifier.padding(top = contentTopPadding),
            )
        }

        uiState.isLoading &&
            uiState.books.isEmpty() &&
            uiState.freeBooks.isEmpty() &&
            uiState.alternativeEditionBooks.isEmpty() &&
            uiState.otherLanguageBooks.isEmpty() &&
            uiState.otherFreeBooks.isEmpty() -> {
            CatalogLoadingPlaceholder(
                contentTopPadding = contentTopPadding,
                modifier = modifier,
            )
        }

        uiState.showEmptyState -> {
            EmptyMessage(
                title = stringResource(
                    if (uiState.selectedMode == CatalogMode.FREE) {
                        R.string.free_catalog_empty
                    } else {
                        R.string.nothing_found
                    },
                ),
                body = stringResource(
                    if (uiState.selectedMode == CatalogMode.FREE) {
                        R.string.free_catalog_empty_body
                    } else {
                        R.string.nothing_found_body
                    },
                ),
                modifier = modifier.padding(top = contentTopPadding),
            )
        }

        uiState.freeBooks.isNotEmpty() ||
            uiState.alternativeEditionBooks.isNotEmpty() ||
            uiState.otherLanguageBooks.isNotEmpty() ||
            uiState.otherFreeBooks.isNotEmpty() -> {
            val listState = key(uiState.catalogListVersion, uiState.selectedMode) {
                rememberLazyListState()
            }
            LoadNextPageEffect(
                listState = listState,
                enabled = uiState.canLoadMore && !uiState.isLoadingNextPage,
                onLoadNextPage = onLoadNextPage,
            )
            Box(modifier = modifier) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = contentTopPadding + 6.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "free-catalog-context") {
                        FreeCatalogContextCard(languageCode = uiState.bookLanguageCode)
                    }
                    uiState.actionMessage?.let { message ->
                        item(key = "catalog-message") {
                            CatalogMessage(message = message)
                        }
                    }
                    items(
                        items = uiState.freeBooks,
                        key = { it.editionId },
                    ) { book ->
                        FreeBookResultCard(
                            book = book,
                            editionCount = uiState.editionCount(book),
                            canDownload = true,
                            isDownloading = uiState.downloadingBookId == book.editionId,
                            isPreparingToRead = uiState.readingBookId == book.editionId,
                            actionsEnabled = uiState.downloadingBookId == null &&
                                uiState.readingBookId == null,
                            onOpen = { onFreeBookClick(book.workId) },
                            onRead = { onReadNow(book) },
                            onDownload = { onDownloadBook(book) },
                        )
                    }
                    if (uiState.otherFreeBooks.isNotEmpty()) {
                        item(key = "other-open-editions-heading") {
                            Column(
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.other_open_editions),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.other_open_editions_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(
                            items = uiState.otherFreeBooks,
                            key = { "other-${it.editionId}" },
                        ) { book ->
                            FreeBookResultCard(
                                book = book,
                                editionCount = uiState.editionCount(book),
                                canDownload = false,
                                isDownloading = false,
                                isPreparingToRead = false,
                                actionsEnabled = false,
                                onOpen = { onFreeBookClick(book.workId) },
                                onRead = { onFreeBookClick(book.workId) },
                                onDownload = { onFreeBookClick(book.workId) },
                            )
                        }
                    }
                    if (uiState.isLoadingNextPage || uiState.nextPageError != null) {
                        item(key = "free-catalog-pagination") {
                            CatalogPaginationFooter(
                                isLoading = uiState.isLoadingNextPage,
                                hasError = uiState.nextPageError != null,
                                onRetry = onRetryNextPage,
                            )
                        }
                    }
                }
                if (uiState.isLoading || uiState.isCheckingFreeBooks) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        uiState.books.isNotEmpty() -> {
            val listState = key(uiState.catalogListVersion, uiState.selectedMode) {
                rememberLazyListState()
            }
            LoadNextPageEffect(
                listState = listState,
                enabled = uiState.canLoadMore && !uiState.isLoadingNextPage,
                onLoadNextPage = onLoadNextPage,
            )
            Box(modifier = modifier) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = contentTopPadding + 6.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (uiState.selectedMode == CatalogMode.FREE) {
                        item(key = "free-catalog-context") {
                            FreeCatalogContextCard(languageCode = uiState.bookLanguageCode)
                        }
                    }
                    if (uiState.isFreeCatalogFallback) {
                        item(key = "free-catalog-fallback") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.free_catalog_fallback_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.free_catalog_fallback_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    uiState.actionMessage?.let { message ->
                        item(key = "catalog-message") {
                            CatalogMessage(message = message)
                        }
                    }
                    items(
                        items = uiState.books,
                        key = { it.id },
                    ) { work ->
                        SearchResultCard(
                            work = work,
                            isSaving = uiState.savingWorkId == work.id,
                            onClick = { onBookClick(work.id) },
                            onAddToShelf = { onAddWorkToShelf(work) },
                        )
                    }
                    if (uiState.isLoadingNextPage || uiState.nextPageError != null) {
                        item(key = "all-catalog-pagination") {
                            CatalogPaginationFooter(
                                isLoading = uiState.isLoadingNextPage,
                                hasError = uiState.nextPageError != null,
                                onRetry = onRetryNextPage,
                            )
                        }
                    }
                }
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        else -> {
            CatalogWelcome(
                mode = uiState.selectedMode,
                modifier = modifier.padding(top = contentTopPadding),
            )
        }
    }
}

@Composable
private fun LoadNextPageEffect(
    listState: LazyListState,
    enabled: Boolean,
    onLoadNextPage: () -> Unit,
) {
    LaunchedEffect(listState, enabled) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            enabled &&
                layoutInfo.totalItemsCount > 0 &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad) onLoadNextPage()
            }
    }
}

@Composable
private fun CatalogPaginationFooter(
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.width(22.dp))
                Text(
                    text = stringResource(R.string.catalog_loading_more),
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            hasError -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.catalog_next_page_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeBookResultCard(
    book: FreeBook,
    editionCount: Int,
    canDownload: Boolean,
    isDownloading: Boolean,
    isPreparingToRead: Boolean,
    actionsEnabled: Boolean,
    onOpen: () -> Unit,
    onRead: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FreeBookCover(book = book)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = book.authors.joinToString().ifBlank { stringResource(R.string.author_unknown) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (editionCount > 1) {
                    Text(
                        text = stringResource(R.string.edition_variants_available),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "${stringResource(
                        if (canDownload) R.string.public_access else R.string.open_edition,
                    )} · ${localizedLanguageName(book.languageCode)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (book.textEditionType == TextEditionType.HISTORICAL_ORTHOGRAPHY) {
                    Text(
                        text = stringResource(R.string.historical_orthography_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (book.textEditionType == TextEditionType.MODERN_ORTHOGRAPHY) {
                    Text(
                        text = stringResource(R.string.modern_orthography),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                book.editionLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (canDownload) {
                        book.sourceName
                    } else {
                        stringResource(R.string.epub_not_confirmed_short)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canDownload) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRead,
                            enabled = actionsEnabled,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isPreparingToRead) {
                                        R.string.opening_for_reading
                                    } else {
                                        R.string.read_now
                                    },
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                        Button(
                            onClick = onDownload,
                            enabled = actionsEnabled,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isDownloading) {
                                        R.string.downloading_book
                                    } else {
                                        R.string.download_to_shelf
                                    },
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Button(onClick = onOpen) {
                        Text(stringResource(R.string.more_details))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FreeCatalogContextCard(
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.catalog_book_language,
                    localizedLanguageName(languageCode),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.catalog_language_scope),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.regional_rights_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CatalogLoadingPlaceholder(
    contentTopPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = contentTopPadding + 6.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp))
                Text(
                    text = stringResource(R.string.catalog_loading_books),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(count = 4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {}
        }
    }
}

@Composable
private fun SearchResultCard(
    work: Work,
    isSaving: Boolean,
    onClick: () -> Unit,
    onAddToShelf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(work = work)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = work.authors.joinToString().ifBlank { stringResource(R.string.author_unknown) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = work.metadataLine(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.more_details),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Button(
                    onClick = onAddToShelf,
                    enabled = !isSaving,
                ) {
                    Text(
                        text = stringResource(
                            if (isSaving) R.string.adding else R.string.download_to_shelf,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogWelcome(
    mode: CatalogMode,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(
                        if (mode == CatalogMode.FREE) R.string.free_catalog_welcome else R.string.find_new_book,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(
                        if (mode == CatalogMode.FREE) {
                            R.string.free_catalog_welcome_body
                        } else {
                            R.string.catalog_welcome_body
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (mode == CatalogMode.ALL) {
            Text(
                text = stringResource(R.string.free_books_soon),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun localizedLanguageName(code: String): String = bookLanguageName(code)

@Composable
private fun Work.metadataLine(): String {
    val published = firstPublishYear?.let { "$it" }
    return listOfNotNull(published)
        .joinToString(" · ")
        .ifBlank { stringResource(R.string.edition_details_pending) }
}

private const val LOAD_MORE_THRESHOLD = 4
