package com.kliuchko.archive17.data.networking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverUrlBuilderTest {
    @Test
    fun `builds cover url with requested size`() {
        val result = CoverUrlBuilder.build(12645118, CoverSize.LARGE)

        assertEquals("https://covers.openlibrary.org/b/id/12645118-L.jpg", result)
    }

    @Test
    fun `returns null when cover id is missing`() {
        assertNull(CoverUrlBuilder.build(null))
    }
}
