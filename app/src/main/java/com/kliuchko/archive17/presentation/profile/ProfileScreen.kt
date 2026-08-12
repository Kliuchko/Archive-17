package com.kliuchko.archive17.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kliuchko.archive17.R
import com.kliuchko.archive17.domain.model.AppLanguage
import com.kliuchko.archive17.domain.model.BookLanguage
import com.kliuchko.archive17.presentation.components.ArchiveBrand
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val preferredBookLanguage by viewModel.preferredBookLanguage.collectAsStateWithLifecycle()
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showBookLanguageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ArchiveBrand() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.profile_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            ProfileRow(
                title = stringResource(R.string.interface_language),
                value = appLanguageLabel(viewModel.currentAppLanguage()),
                onClick = { showAppLanguageDialog = true },
            )
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item {
            ProfileRow(
                title = stringResource(R.string.preferred_book_language),
                value = bookLanguageLabel(preferredBookLanguage),
                onClick = { showBookLanguageDialog = true },
            )
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { ProfileRow(stringResource(R.string.reader_card), stringResource(R.string.local_profile)) }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { ProfileRow(stringResource(R.string.subscription), stringResource(R.string.soon)) }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { ProfileRow(stringResource(R.string.sync), stringResource(R.string.soon)) }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { ProfileRow(stringResource(R.string.project_document), "Archive 17") }
    }

    if (showAppLanguageDialog) {
        LanguageDialog(
            title = stringResource(R.string.interface_language),
            options = AppLanguage.entries,
            selected = viewModel.currentAppLanguage(),
            label = { appLanguageLabel(it) },
            onSelect = {
                showAppLanguageDialog = false
                viewModel.setAppLanguage(it)
            },
            onDismiss = { showAppLanguageDialog = false },
        )
    }
    if (showBookLanguageDialog) {
        LanguageDialog(
            title = stringResource(R.string.preferred_book_language),
            options = BookLanguage.entries,
            selected = preferredBookLanguage,
            label = { bookLanguageLabel(it) },
            onSelect = {
                viewModel.setPreferredBookLanguage(it)
                showBookLanguageDialog = false
            },
            onDismiss = { showBookLanguageDialog = false },
        )
    }
}

@Composable
private fun ProfileRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T> LanguageDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = selected == option, onClick = { onSelect(option) })
                        Text(label(option), modifier = Modifier.padding(start = 8.dp, top = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun appLanguageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.RUSSIAN -> stringResource(R.string.language_russian)
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
}

@Composable
private fun bookLanguageLabel(language: BookLanguage): String = when (language) {
    BookLanguage.DEVICE -> stringResource(R.string.language_device)
    BookLanguage.RUSSIAN -> stringResource(R.string.language_russian)
    BookLanguage.ENGLISH -> stringResource(R.string.language_english)
}
