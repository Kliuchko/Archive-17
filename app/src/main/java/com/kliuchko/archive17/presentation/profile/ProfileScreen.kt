package com.kliuchko.archive17.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kliuchko.archive17.presentation.components.ArchiveBrand

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ArchiveBrand() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Профиль",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Настройки и синхронизация появятся здесь позднее.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { ProfileRow(title = "Читательский билет", value = "Локальный профиль") }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { ProfileRow(title = "Подписка", value = "Скоро") }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { ProfileRow(title = "Синхронизация", value = "Скоро") }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { ProfileRow(title = "Документ о проекте", value = "Archive 17") }
    }
}

@Composable
private fun ProfileRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
