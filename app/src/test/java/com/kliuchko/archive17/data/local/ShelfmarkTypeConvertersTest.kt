package com.kliuchko.archive17.data.local

import com.kliuchko.archive17.domain.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfmarkTypeConvertersTest {
    private val converters = Archive17TypeConverters()

    @Test
    fun `round trips string lists`() {
        val encoded = converters.fromStringList(listOf("Jane Austen", "Open Library"))

        assertEquals(listOf("Jane Austen", "Open Library"), converters.toStringList(encoded))
    }

    @Test
    fun `returns empty list for blank string list value`() {
        assertEquals(emptyList<String>(), converters.toStringList(""))
    }

    @Test
    fun `round trips reading status`() {
        val encoded = converters.fromReadingStatus(ReadingStatus.READING)

        assertEquals(ReadingStatus.READING, converters.toReadingStatus(encoded))
    }
}
