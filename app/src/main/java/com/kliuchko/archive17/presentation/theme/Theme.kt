package com.kliuchko.archive17.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ArchiveNightBrass,
    onPrimary = ArchiveNight,
    primaryContainer = ArchiveNightBrassSoft,
    onPrimaryContainer = ArchiveNightInk,
    secondary = ArchiveNightForest,
    onSecondary = ArchiveNightInk,
    tertiary = ArchiveNightWine,
    onTertiary = ArchiveNightInk,
    background = ArchiveNight,
    onBackground = ArchiveNightInk,
    surface = ArchiveNightRaised,
    onSurface = ArchiveNightInk,
    surfaceVariant = ArchiveNightMuted,
    onSurfaceVariant = ArchiveNightInkMuted,
    outline = ArchiveNightOutline,
    outlineVariant = ArchiveNightOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = ArchiveBrass,
    onPrimary = ArchivePaperRaised,
    primaryContainer = ArchiveBrassSoft,
    onPrimaryContainer = ArchiveInk,
    secondary = ArchiveForest,
    onSecondary = ArchivePaperRaised,
    tertiary = ArchiveWine,
    onTertiary = ArchivePaperRaised,
    background = ArchivePaper,
    onBackground = ArchiveInk,
    surface = ArchivePaperRaised,
    onSurface = ArchiveInk,
    surfaceVariant = ArchivePaperMuted,
    onSurfaceVariant = ArchiveInkMuted,
    outline = ArchiveOutline,
    outlineVariant = ArchiveOutline,
)

@Composable
fun Archive17Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
