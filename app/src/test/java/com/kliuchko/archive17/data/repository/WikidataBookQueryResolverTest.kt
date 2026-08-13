package com.kliuchko.archive17.data.repository

import com.kliuchko.archive17.data.networking.dto.WikidataEntityDto
import com.kliuchko.archive17.data.networking.dto.WikidataMatchDto
import com.kliuchko.archive17.data.networking.dto.WikidataSearchItemDto
import com.kliuchko.archive17.data.networking.dto.WikidataTermDto
import com.google.gson.JsonParser
import com.kliuchko.archive17.data.networking.dto.WikidataClaimDto
import com.kliuchko.archive17.data.networking.dto.WikidataDataValueDto
import com.kliuchko.archive17.data.networking.dto.WikidataSnakDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WikidataBookQueryResolverTest {
    @Test
    fun `candidate selection keeps books and authors but excludes adaptations and characters`() {
        val candidates = selectWikidataCandidateIds(
            query = "Стальная Крыса",
            results = listOf(
                searchItem("Q1", "Стальная Крыса", "серия научно-фантастических романов"),
                searchItem("Q2", "Стальная Крыса", "роман Гарри Гаррисона"),
                searchItem("Q3", "Стальная Крыса", "вымышленный персонаж романов"),
                searchItem("Q4", "Стальная Крыса", "фильм"),
            ),
        )

        assertEquals(listOf("Q1", "Q2"), candidates)
    }

    @Test
    fun `resolved queries include translated labels and aliases without duplicates`() {
        val queries = buildResolvedBookQueries(
            original = "Стальная Крыса",
            candidateIds = listOf("Q2"),
            entities = mapOf(
                "Q2" to WikidataEntityDto(
                    id = "Q2",
                    labels = mapOf(
                        "ru" to term("ru", "Стальная Крыса"),
                        "en" to term("en", "The Stainless Steel Rat"),
                        "uk" to term("uk", "Сталевий пацюк"),
                    ),
                    aliases = mapOf(
                        "en" to listOf(term("en", "Stainless Steel Rat")),
                        "ru" to listOf(term("ru", "Стальная крыса")),
                    ),
                ),
            ),
        )

        assertEquals("Стальная Крыса", queries.first())
        assertTrue("The Stainless Steel Rat" in queries)
        assertTrue("Сталевий пацюк" in queries)
        assertTrue("Stainless Steel Rat" in queries)
        assertEquals(4, queries.size)
    }

    @Test
    fun `candidate selection is generic for another translated book title`() {
        val candidates = selectWikidataCandidateIds(
            query = "Маленький принц",
            results = listOf(
                searchItem("Q25338", "Маленький принц", "повесть-сказка Антуана де Сент-Экзюпери"),
                searchItem("Q999", "Маленький принц", "телесериал"),
            ),
        )

        assertEquals(listOf("Q25338"), candidates)
        assertFalse(candidates.contains("Q999"))
    }

    @Test
    fun `English labels from several candidates are preferred before other translations`() {
        val queries = buildResolvedBookQueries(
            original = "Стальная Крыса",
            candidateIds = listOf("Q1", "Q2"),
            entities = mapOf(
                "Q1" to WikidataEntityDto(
                    labels = mapOf(
                        "en" to term("en", "Stainless Steel Rat"),
                        "uk" to term("uk", "Сталевий пацюк"),
                    ),
                ),
                "Q2" to WikidataEntityDto(
                    labels = mapOf(
                        "en" to term("en", "The Stainless Steel Rat"),
                        "uk" to term("uk", "Нержавіючий сталевий пацюк"),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("Стальная Крыса", "Stainless Steel Rat", "The Stainless Steel Rat"),
            queries.take(3),
        )
    }

    @Test
    fun `selected catalog language keeps its label and alias in source query budget`() {
        val queries = buildResolvedBookQueries(
            original = "Oliver Twist",
            candidateIds = listOf("Q164974"),
            entities = mapOf(
                "Q164974" to WikidataEntityDto(
                    labels = mapOf(
                        "en" to term("en", "Oliver Twist"),
                        "ru" to term("ru", "Приключения Оливера Твиста"),
                    ),
                    aliases = mapOf(
                        "ru" to listOf(term("ru", "Оливер Твист")),
                    ),
                ),
            ),
            preferredLanguageCode = "rus",
        )

        assertEquals(
            listOf("Oliver Twist", "Приключения Оливера Твиста", "Оливер Твист"),
            queries.take(3),
        )
    }

    @Test
    fun `translation metadata uses localized work titles for every requested language`() {
        val metadata = buildTranslationMetadata(
            candidates = listOf(
                WikidataEntityDto(
                    labels = mapOf(
                        "en" to term("en", "Fahrenheit 451"),
                        "ru" to term("ru", "451 градус по Фаренгейту"),
                        "uk" to term("uk", "451 градус за Фаренгейтом"),
                    ),
                    claims = mapOf(
                        "P364" to listOf(
                            WikidataClaimDto(
                                WikidataSnakDto(
                                    WikidataDataValueDto(JsonParser.parseString("""{"id":"Q1860"}""")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            requestedCatalogLanguages = listOf("eng", "rus", "ukr"),
        )

        assertEquals("eng", metadata.originalLanguageCode)
        assertEquals("451 градус по Фаренгейту", metadata.titlesByLanguage["rus"])
        assertEquals("451 градус за Фаренгейтом", metadata.titlesByLanguage["ukr"])
    }

    private fun searchItem(id: String, label: String, description: String) =
        WikidataSearchItemDto(
            id = id,
            label = label,
            description = description,
            match = WikidataMatchDto(text = label, language = "ru"),
        )

    private fun term(language: String, value: String) =
        WikidataTermDto(language = language, value = value)
}
