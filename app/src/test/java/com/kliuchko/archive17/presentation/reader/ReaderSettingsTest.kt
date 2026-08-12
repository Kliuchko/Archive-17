package com.kliuchko.archive17.presentation.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderSettingsTest {
    @Test
    fun `font size stays inside supported reader range`() {
        val maximum = generateSequence(ReaderPreferences.defaults()) { it.increaseFontSize() }
            .drop(20)
            .first()
        val minimum = generateSequence(ReaderPreferences.defaults()) { it.decreaseFontSize() }
            .drop(20)
            .first()

        assertEquals(MAX_FONT_SIZE, maximum.fontSize, 0.001)
        assertEquals(MIN_FONT_SIZE, minimum.fontSize, 0.001)
    }

    @Test
    fun `default reading style is calm and publisher friendly`() {
        val preferences = ReaderPreferences.defaults()

        assertEquals(1.0, preferences.fontSize, 0.001)
        assertEquals(1.5, preferences.lineHeight, 0.001)
        assertEquals(ReaderFont.PUBLISHER, preferences.font)
        assertEquals(ReaderTheme.LIGHT, preferences.theme)
    }

    @Test
    fun `stored float values return to exact supported steps`() {
        val preferences = ReaderPreferences.restored(
            fontSize = 1.100000023841858,
            lineHeight = 1.7999999523162842,
            font = ReaderFont.SANS_SERIF,
            theme = ReaderTheme.SEPIA,
        )

        assertEquals(1.1, preferences.fontSize, 0.0)
        assertEquals(1.8, preferences.lineHeight, 0.0)
    }
}
