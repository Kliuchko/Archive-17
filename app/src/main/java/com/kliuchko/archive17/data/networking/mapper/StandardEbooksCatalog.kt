package com.kliuchko.archive17.data.networking.mapper

import com.kliuchko.archive17.domain.model.FreeBook
import com.kliuchko.archive17.domain.model.FreeBookDetails
import com.kliuchko.archive17.domain.model.FreeBookSource
import com.kliuchko.archive17.domain.model.FreeBookRights
import com.kliuchko.archive17.domain.model.FreeAccessBasis

fun curatedEnglishStandardEbooks(
    query: String,
    page: Int,
    pageSize: Int = 4,
): List<FreeBook> {
    val normalizedQuery = query.trim().lowercase()
    return STANDARD_EBOOKS
        .asSequence()
        .filter { book ->
            normalizedQuery.isEmpty() ||
                book.title.lowercase().contains(normalizedQuery) ||
                book.author.lowercase().contains(normalizedQuery)
        }
        .drop((page.coerceAtLeast(1) - 1) * pageSize)
        .take(pageSize)
        .map(StandardEbook::toFreeBook)
        .toList()
}

fun standardEbookDetails(book: FreeBook): FreeBookDetails? {
    val record = STANDARD_EBOOKS.firstOrNull { it.editionId == book.editionId } ?: return null
    return FreeBookDetails(
        book = book,
        description = record.description,
        subjects = record.subjects,
    )
}

private fun StandardEbook.toFreeBook(): FreeBook {
    val pageUrl = "$STANDARD_EBOOKS_BASE_URL/ebooks/$pagePath"
    return FreeBook(
        workId = editionId,
        editionId = editionId,
        title = title,
        authors = listOf(author),
        coverId = null,
        coverUrl = "$pageUrl/downloads/cover.jpg",
        firstPublishYear = year,
        languageCode = ENGLISH_LANGUAGE,
        source = FreeBookSource.STANDARD_EBOOKS,
        sourcePageUrl = pageUrl,
        epubDownloadUrl = "$pageUrl/downloads/$fileSlug.epub?source=download",
        rights = FreeBookRights(
            basis = FreeAccessBasis.PUBLIC_DOMAIN_US,
            licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/",
            territoryCodes = listOf("US"),
        ),
    )
}

private data class StandardEbook(
    val editionId: String,
    val title: String,
    val author: String,
    val year: Int,
    val pagePath: String,
    val fileSlug: String,
    val description: String,
    val subjects: List<String>,
)

private const val STANDARD_EBOOKS_BASE_URL = "https://standardebooks.org"
private const val ENGLISH_LANGUAGE = "eng"

private val STANDARD_EBOOKS = listOf(
    StandardEbook(
        editionId = "standard-ebooks-jane-austen-pride-and-prejudice",
        title = "Pride and Prejudice",
        author = "Jane Austen",
        year = 1813,
        pagePath = "jane-austen/pride-and-prejudice",
        fileSlug = "jane-austen_pride-and-prejudice",
        description = "Five sisters navigate family expectations, first impressions, and love " +
            "after two wealthy gentlemen arrive in their neighborhood.",
        subjects = listOf("Fiction", "Romance", "Social life"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-charlotte-bronte-jane-eyre",
        title = "Jane Eyre",
        author = "Charlotte Brontë",
        year = 1847,
        pagePath = "charlotte-bronte/jane-eyre",
        fileSlug = "charlotte-bronte_jane-eyre",
        description = "An orphaned young woman becomes a governess at Thornfield Hall and is " +
            "drawn to its mysterious master.",
        subjects = listOf("Fiction", "Gothic", "Coming of age"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-mary-shelley-frankenstein",
        title = "Frankenstein",
        author = "Mary Shelley",
        year = 1818,
        pagePath = "mary-shelley/frankenstein",
        fileSlug = "mary-shelley_frankenstein",
        description = "A scientist creates life and recoils from the being he has made, with " +
            "tragic consequences for creator and creation alike.",
        subjects = listOf("Horror", "Science fiction", "Gothic"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-oscar-wilde-the-picture-of-dorian-gray",
        title = "The Picture of Dorian Gray",
        author = "Oscar Wilde",
        year = 1890,
        pagePath = "oscar-wilde/the-picture-of-dorian-gray",
        fileSlug = "oscar-wilde_the-picture-of-dorian-gray",
        description = "A beautiful young man remains outwardly unchanged while his portrait " +
            "records the cost of a life devoted to pleasure.",
        subjects = listOf("Fiction", "Gothic", "Philosophical fiction"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-bram-stoker-dracula",
        title = "Dracula",
        author = "Bram Stoker",
        year = 1897,
        pagePath = "bram-stoker/dracula",
        fileSlug = "bram-stoker_dracula",
        description = "Letters, diaries, and reports recount Count Dracula’s journey to England " +
            "and the group determined to stop him.",
        subjects = listOf("Horror", "Gothic", "Epistolary fiction"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-herman-melville-moby-dick",
        title = "Moby-Dick",
        author = "Herman Melville",
        year = 1851,
        pagePath = "herman-melville/moby-dick",
        fileSlug = "herman-melville_moby-dick",
        description = "A sailor joins the Pequod and witnesses Captain Ahab’s consuming pursuit " +
            "of the white whale that maimed him.",
        subjects = listOf("Fiction", "Adventure", "Sea stories"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-charles-dickens-great-expectations",
        title = "Great Expectations",
        author = "Charles Dickens",
        year = 1861,
        pagePath = "charles-dickens/great-expectations",
        fileSlug = "charles-dickens_great-expectations",
        description = "An orphan’s unexpected fortune carries him from a country childhood into " +
            "London society and forces him to reconsider what makes a gentleman.",
        subjects = listOf("Fiction", "Coming of age", "Social life"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-lewis-carroll-alices-adventures-in-wonderland",
        title = "Alice’s Adventures in Wonderland",
        author = "Lewis Carroll",
        year = 1865,
        pagePath = "lewis-carroll/alices-adventures-in-wonderland/john-tenniel",
        fileSlug = "lewis-carroll_alices-adventures-in-wonderland_john-tenniel",
        description = "Alice follows a hurried White Rabbit into a world governed by curious " +
            "creatures, wordplay, and dreamlike logic.",
        subjects = listOf("Fantasy", "Children’s fiction", "Adventure"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-emily-bronte-wuthering-heights",
        title = "Wuthering Heights",
        author = "Emily Brontë",
        year = 1847,
        pagePath = "emily-bronte/wuthering-heights",
        fileSlug = "emily-bronte_wuthering-heights",
        description = "The fierce bond between Catherine Earnshaw and Heathcliff shapes two " +
            "families across generations on the Yorkshire moors.",
        subjects = listOf("Fiction", "Gothic", "Tragedy"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-mark-twain-the-adventures-of-huckleberry-finn",
        title = "The Adventures of Huckleberry Finn",
        author = "Mark Twain",
        year = 1884,
        pagePath = "mark-twain/the-adventures-of-huckleberry-finn",
        fileSlug = "mark-twain_the-adventures-of-huckleberry-finn",
        description = "Huck Finn and Jim travel down the Mississippi River, encountering danger, " +
            "hypocrisy, and difficult questions of freedom and conscience.",
        subjects = listOf("Fiction", "Adventure", "Satire"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-louisa-may-alcott-little-women",
        title = "Little Women",
        author = "Louisa May Alcott",
        year = 1868,
        pagePath = "louisa-may-alcott/little-women",
        fileSlug = "louisa-may-alcott_little-women",
        description = "The four March sisters grow toward adulthood while their family faces " +
            "financial hardship, separation, ambition, and loss.",
        subjects = listOf("Fiction", "Family", "Coming of age"),
    ),
    StandardEbook(
        editionId = "standard-ebooks-james-joyce-dubliners",
        title = "Dubliners",
        author = "James Joyce",
        year = 1914,
        pagePath = "james-joyce/dubliners",
        fileSlug = "james-joyce_dubliners",
        description = "Fifteen stories portray childhood, work, family, and public life in " +
            "early-twentieth-century Dublin.",
        subjects = listOf("Fiction", "Short stories", "Irish literature"),
    ),
)
