package com.kliuchko.archive17.data.networking.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogQueryAliasesTest {
    @Test
    fun `Cyrillic AllatRa variants use catalog spelling`() {
        assertEquals("AllatRa", "Аллатра".withCatalogQueryAlias())
        assertEquals("AllatRa", "Аллат Ра".withCatalogQueryAlias())
        assertEquals("AllatRa", "аЛлАтРа".withCatalogQueryAlias())
    }

    @Test
    fun `unrelated query is unchanged`() {
        assertEquals("Стальная Крыса", "Стальная Крыса".withCatalogQueryAlias())
    }
}
