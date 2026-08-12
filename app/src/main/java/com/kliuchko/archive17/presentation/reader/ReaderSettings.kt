@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kliuchko.archive17.presentation.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kliuchko.archive17.R
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ReaderSettingsPanel(
    preferences: ReaderPreferences,
    onChange: (ReaderPreferences) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReaderSettingRow(label = stringResource(R.string.reader_text_size)) {
            TextButton(
                onClick = { onChange(preferences.decreaseFontSize()) },
                enabled = preferences.fontSize > MIN_FONT_SIZE,
            ) { Text("A−") }
            Text(
                text = "${(preferences.fontSize * 100).toInt()}%",
                modifier = Modifier.width(52.dp),
                style = MaterialTheme.typography.labelMedium,
            )
            TextButton(
                onClick = { onChange(preferences.increaseFontSize()) },
                enabled = preferences.fontSize < MAX_FONT_SIZE,
            ) { Text("A+") }
        }
        ReaderSettingRow(label = stringResource(R.string.reader_font)) {
            ReaderFont.entries.forEach { font ->
                FilterChip(
                    selected = preferences.font == font,
                    onClick = { onChange(preferences.copy(font = font)) },
                    label = { Text(stringResource(font.labelRes)) },
                )
            }
        }
        ReaderSettingRow(label = stringResource(R.string.reader_line_height)) {
            LINE_HEIGHTS.forEach { lineHeight ->
                FilterChip(
                    selected = preferences.lineHeight == lineHeight.value,
                    onClick = { onChange(preferences.copy(lineHeight = lineHeight.value)) },
                    label = { Text(stringResource(lineHeight.labelRes)) },
                )
            }
        }
        ReaderSettingRow(label = stringResource(R.string.reader_theme)) {
            ReaderTheme.entries.forEach { theme ->
                FilterChip(
                    selected = preferences.theme == theme,
                    onClick = { onChange(preferences.copy(theme = theme)) },
                    label = { Text(stringResource(theme.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun ReaderSettingRow(
    label: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

internal data class ReaderPreferences(
    val fontSize: Double,
    val lineHeight: Double,
    val font: ReaderFont,
    val theme: ReaderTheme,
) {
    fun increaseFontSize(): ReaderPreferences = copy(
        fontSize = (fontSize + FONT_SIZE_STEP).coerceAtMost(MAX_FONT_SIZE),
    )

    fun decreaseFontSize(): ReaderPreferences = copy(
        fontSize = (fontSize - FONT_SIZE_STEP).coerceAtLeast(MIN_FONT_SIZE),
    )

    fun toEpubPreferences(): EpubPreferences = EpubPreferences(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = when (font) {
            ReaderFont.PUBLISHER -> null
            ReaderFont.SERIF -> FontFamily.SERIF
            ReaderFont.SANS_SERIF -> FontFamily.SANS_SERIF
        },
        theme = when (theme) {
            ReaderTheme.LIGHT -> Theme.LIGHT
            ReaderTheme.SEPIA -> Theme.SEPIA
            ReaderTheme.DARK -> Theme.DARK
        },
    )

    companion object {
        fun defaults() = ReaderPreferences(
            fontSize = 1.0,
            lineHeight = 1.5,
            font = ReaderFont.PUBLISHER,
            theme = ReaderTheme.LIGHT,
        )

        fun restored(
            fontSize: Double,
            lineHeight: Double,
            font: ReaderFont,
            theme: ReaderTheme,
        ) = ReaderPreferences(
            fontSize = ((fontSize * 10).roundToInt() / 10.0).coerceIn(
                MIN_FONT_SIZE,
                MAX_FONT_SIZE,
            ),
            lineHeight = SUPPORTED_LINE_HEIGHTS.minBy { abs(it - lineHeight) },
            font = font,
            theme = theme,
        )
    }

    @Composable
    fun toolbarColor(): Color = when (theme) {
        ReaderTheme.LIGHT -> MaterialTheme.colorScheme.surface
        ReaderTheme.SEPIA -> Color(0xFFF3E8D2)
        ReaderTheme.DARK -> MaterialTheme.colorScheme.surface
    }
}

internal enum class ReaderFont(
    @StringRes val labelRes: Int,
) {
    PUBLISHER(R.string.reader_font_publisher),
    SERIF(R.string.reader_font_serif),
    SANS_SERIF(R.string.reader_font_sans),
}

internal enum class ReaderTheme(
    @StringRes val labelRes: Int,
) {
    LIGHT(R.string.reader_theme_light),
    SEPIA(R.string.reader_theme_sepia),
    DARK(R.string.reader_theme_dark),
}

private data class LineHeightOption(val value: Double, @StringRes val labelRes: Int)

private val LINE_HEIGHTS = listOf(
    LineHeightOption(1.2, R.string.reader_line_compact),
    LineHeightOption(1.5, R.string.reader_line_normal),
    LineHeightOption(1.8, R.string.reader_line_relaxed),
)

private val SUPPORTED_LINE_HEIGHTS = LINE_HEIGHTS.map(LineHeightOption::value)

internal const val MIN_FONT_SIZE = 0.8
internal const val MAX_FONT_SIZE = 1.6
private const val FONT_SIZE_STEP = 0.1
