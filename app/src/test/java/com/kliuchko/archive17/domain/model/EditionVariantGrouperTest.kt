package com.kliuchko.archive17.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EditionVariantGrouperTest {
    @Test
    fun `groups printings but keeps translations and orthography variants separate`() {
        val editions = listOf(
            edition("hardcover", "rus", translator = "Иван Петров", publisher = "A"),
            edition("paperback", "rus", translator = "Иван Петров", publisher = "B"),
            edition("other-translation", "rus", translator = "Анна Смирнова"),
            edition(
                id = "historical",
                language = "rus",
                type = TextEditionType.HISTORICAL_ORTHOGRAPHY,
            ),
            edition("original", "eng"),
        )

        val variants = editions.groupMeaningfulVariants("rus", originalLanguageCode = "eng")

        assertEquals(4, variants.size)
        assertEquals(3, variants.count { it.languageCode == "rus" })
        assertEquals(1, variants.count { it.translator == "Иван Петров" })
        assertEquals("eng", variants.first().languageCode)
    }

    private fun edition(
        id: String,
        language: String,
        translator: String? = null,
        publisher: String? = null,
        type: TextEditionType = TextEditionType.UNSPECIFIED,
    ) = PublicationEdition(
        id = id,
        workId = "work",
        title = "Book",
        authors = listOf("Author"),
        languageCode = language,
        translator = translator,
        publisher = publisher,
        textEditionType = type,
    )
}
