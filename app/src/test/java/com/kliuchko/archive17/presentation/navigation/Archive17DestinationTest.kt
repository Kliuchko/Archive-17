package com.kliuchko.archive17.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class Archive17DestinationTest {
    @Test
    fun `creates details route with work id`() {
        assertEquals(
            "details/OL45883W",
            Archive17Destination.Details.createRoute("OL45883W"),
        )
    }
}
