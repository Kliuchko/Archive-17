package com.kliuchko.archive17.presentation.freedetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.R
import com.kliuchko.archive17.domain.model.FreeAccessBasis
import com.kliuchko.archive17.domain.model.EditionAvailability
import com.kliuchko.archive17.domain.model.EditionAccessMode
import com.kliuchko.archive17.domain.model.PublicationEdition
import com.kliuchko.archive17.domain.model.TextEditionType
import com.kliuchko.archive17.domain.model.TemporaryBook
import com.kliuchko.archive17.presentation.components.FreeBookCover
import com.kliuchko.archive17.presentation.components.bookLanguageName
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FreeBookDetailsScreen(
    editionId: String,
    onBackClick: () -> Unit,
    onBookReady: (String) -> Unit,
    onTemporaryBookReady: (TemporaryBook) -> Unit,
    onEditionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreeBookDetailsViewModel = koinViewModel(parameters = { parametersOf(editionId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

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
                Text(stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.open_collection_badge),
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
            ) {
                CircularProgressIndicator()
            }

            uiState.details == null -> Text(
                text = uiState.errorMessage ?: stringResource(R.string.book_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )

            else -> {
                val details = checkNotNull(uiState.details)
                val book = details.book
                FreeBookCover(
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
                        text = book.authors.joinToString().ifBlank {
                            stringResource(R.string.author_unknown)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = listOfNotNull(
                            book.firstPublishYear?.toString(),
                            stringResource(R.string.epub_format).takeIf { book.isDownloadable },
                            book.epubSizeBytes?.let(::formatFileSize),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DetailFact(
                    label = stringResource(R.string.edition_language_label),
                    value = localizedBookLanguage(book.languageCode),
                )

                val otherEditions = uiState.relatedEditions
                    .ifEmpty { details.relatedEditions }
                    .filterNot { edition ->
                    edition.id == book.editionId
                }
                if (otherEditions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.available_editions),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.available_editions_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    otherEditions.forEach { edition ->
                        RelatedEditionCard(
                            edition = edition,
                            onClick = if (
                                edition.accessOptions.firstOrNull()?.availability !=
                                EditionAvailability.UNAVAILABLE
                            ) {
                                { onEditionClick(edition.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
                if (book.textEditionType != TextEditionType.UNSPECIFIED) {
                    DetailFact(
                        label = stringResource(R.string.edition_type_label),
                        value = stringResource(
                            if (book.textEditionType == TextEditionType.HISTORICAL_ORTHOGRAPHY) {
                                R.string.historical_orthography_warning
                            } else {
                                R.string.modern_orthography
                            },
                        ),
                    )
                }
                book.editionLabel?.let { label ->
                    DetailFact(
                        label = stringResource(R.string.edition_details_label),
                        value = label,
                    )
                }
                book.translator?.let { translator ->
                    DetailFact(
                        label = stringResource(R.string.edition_translator_label),
                        value = translator,
                    )
                }
                book.publisher?.let { publisher ->
                    DetailFact(
                        label = stringResource(R.string.edition_publisher_label),
                        value = publisher,
                    )
                }
                book.editionYear?.let { year ->
                    DetailFact(
                        label = stringResource(R.string.edition_year_label),
                        value = year.toString(),
                    )
                }
                Text(
                    text = stringResource(R.string.catalog_language_scope),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DetailFact(
                    label = stringResource(R.string.access_label),
                    value = stringResource(
                        if (book.isDownloadable) {
                            R.string.public_access
                        } else {
                            R.string.open_edition
                        },
                    ),
                )
                DetailFact(
                    label = stringResource(R.string.rights_basis_label),
                    value = stringResource(book.rights.basis.labelResource()),
                )

                if (book.isDownloadable) {
                    Button(
                        onClick = viewModel::readNow,
                        enabled = !uiState.isDownloading && !uiState.isPreparingToRead,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isPreparingToRead) {
                                    R.string.opening_for_reading
                                } else {
                                    R.string.read_now
                                },
                            ),
                        )
                    }
                    OutlinedButton(
                        onClick = viewModel::downloadBook,
                        enabled = !uiState.isDownloading && !uiState.isPreparingToRead,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isDownloading) {
                                    R.string.downloading_book
                                } else {
                                    R.string.download_to_shelf
                                },
                            ),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.epub_not_confirmed_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                uiState.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                details.description?.let { description ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.about_book),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (details.subjects.isNotEmpty()) {
                    DetailFact(
                        label = stringResource(R.string.catalog_topics),
                        value = details.subjects.joinToString(" · "),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.source_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = book.sourceName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.public_access_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { uriHandler.openUri(book.sourceUrl) }) {
                        Text(stringResource(R.string.open_source_page))
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedEditionCard(
    edition: PublicationEdition,
    onClick: (() -> Unit)?,
) {
    val access = edition.accessOptions.firstOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = edition.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = listOfNotNull(
                    localizedBookLanguage(edition.languageCode),
                    edition.textEditionType.takeIf {
                        it == TextEditionType.HISTORICAL_ORTHOGRAPHY
                    }?.let { stringResource(R.string.historical_orthography_warning) },
                    edition.publishedYear?.toString(),
                    edition.label,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            edition.translator?.let { translator ->
                Text(
                    text = stringResource(R.string.edition_translator_value, translator),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            edition.publisher?.let { publisher ->
                Text(
                    text = publisher,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = editionAccessLabel(access?.mode, access?.availability),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            access?.providerName?.let { provider ->
                Text(
                    text = provider,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onClick != null) {
                TextButton(onClick = onClick) {
                    Text(stringResource(R.string.choose_edition))
                }
            }
        }
    }
}

@Composable
private fun editionAccessLabel(
    mode: EditionAccessMode?,
    availability: EditionAvailability?,
): String = when (availability) {
    EditionAvailability.COMING_SOON -> stringResource(R.string.edition_access_coming_soon)
    EditionAvailability.REGION_RESTRICTED ->
        stringResource(R.string.edition_access_region_restricted)
    EditionAvailability.UNAVAILABLE -> stringResource(R.string.edition_access_unavailable)
    EditionAvailability.EXTERNAL_ONLY -> stringResource(R.string.edition_access_external)
    EditionAvailability.AVAILABLE -> when (mode) {
        EditionAccessMode.FREE -> stringResource(R.string.edition_access_free)
        EditionAccessMode.REFERENCE -> stringResource(R.string.edition_access_unavailable)
        EditionAccessMode.SUBSCRIPTION -> stringResource(R.string.edition_access_subscription)
        EditionAccessMode.PURCHASE -> stringResource(R.string.edition_access_purchase)
        EditionAccessMode.PARTNER_PURCHASE -> stringResource(R.string.edition_access_partner)
        EditionAccessMode.OWNED_FILE -> stringResource(R.string.edition_access_owned_file)
        null -> stringResource(R.string.open_edition)
    }
    null -> stringResource(R.string.open_edition)
}

@Composable
private fun DetailFact(label: String, value: String) {
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
private fun localizedBookLanguage(code: String): String = bookLanguageName(code)

private fun formatFileSize(bytes: Long): String {
    val megabytes = bytes.toDouble() / (1024.0 * 1024.0)
    return if (megabytes >= 10) {
        "%.0f MB".format(megabytes)
    } else {
        "%.1f MB".format(megabytes)
    }
}

private fun FreeAccessBasis.labelResource(): Int = when (this) {
    FreeAccessBasis.OPEN_LICENSE -> R.string.rights_open_license
    FreeAccessBasis.RIGHTS_HOLDER_PERMISSION -> R.string.rights_holder_permission
    FreeAccessBasis.PUBLIC_DOMAIN_US -> R.string.rights_public_domain_us
    FreeAccessBasis.SOURCE_REPORTED_PUBLIC -> R.string.rights_source_reported_public
}
