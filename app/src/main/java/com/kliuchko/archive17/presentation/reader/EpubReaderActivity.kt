@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kliuchko.archive17.presentation.reader

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kliuchko.archive17.R
import com.kliuchko.archive17.data.reader.ReadiumService
import com.kliuchko.archive17.domain.model.TemporaryBook
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.presentation.theme.Archive17Theme
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.isSearchable
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class, kotlinx.coroutines.FlowPreview::class)
class EpubReaderActivity : AppCompatActivity(), EpubNavigatorFragment.Listener {
    private val localBookRepository: LocalBookRepository by inject()
    private val readiumService: ReadiumService by inject()

    private lateinit var readerContainer: FrameLayout
    private lateinit var titleView: ComposeView
    private var navigator: EpubNavigatorFragment? = null
    private var openedPublication: org.readium.r2.shared.publication.Publication? = null
    private var readerTitle by mutableStateOf("")
    private var pageLabel by mutableStateOf("")
    private var settingsVisible by mutableStateOf(false)
    private var navigationVisible by mutableStateOf(false)
    private var navigationSection by mutableStateOf(ReaderNavigationSection.CONTENTS)
    private var readerPreferences by mutableStateOf(ReaderPreferences.defaults())
    private var tocEntries by mutableStateOf(emptyList<ReaderTocEntry>())
    private var bookmarks by mutableStateOf(emptyList<Locator>())
    private var searchQuery by mutableStateOf("")
    private var searchResults by mutableStateOf(emptyList<ReaderSearchResult>())
    private var searchInProgress by mutableStateOf(false)
    private var searchSubmitted by mutableStateOf(false)
    private var searchUnavailable by mutableStateOf(false)
    private var readerStorageKey: String? = null
    private var legacyReaderStorageKeys: List<String> = emptyList()
    private var searchJob: Job? = null
    private var totalPositions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        super.onCreate(null)
        createContent()

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID)
        val temporaryEditionId = intent.getStringExtra(EXTRA_TEMPORARY_EDITION_ID)
        if (bookId == null && temporaryEditionId == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val readerBook = if (bookId != null) {
                localBookRepository.getLocalBook(bookId)?.let { book ->
                    val migratedProgression = book.progressionJson
                        ?: book.identifier?.let { editionId ->
                            temporaryProgressionPreferences().getString(editionId, null)
                        }
                    ReaderBook(
                        title = book.title,
                        file = File(book.filePath),
                        progressionJson = migratedProgression,
                        storageKey = book.identifier
                            ?.takeIf(String::isNotBlank)
                            ?.let { editionId -> "edition:$editionId" }
                            ?: "local:${book.id}",
                        legacyStorageKeys = listOf("local:${book.id}"),
                    )
                }
            } else {
                resolveTemporaryBook(temporaryEditionId.orEmpty())
            }
            if (readerBook == null || !readerBook.file.exists()) {
                showTitle(getString(R.string.book_unavailable))
                return@launch
            }

            readerStorageKey = readerBook.storageKey
            legacyReaderStorageKeys = readerBook.legacyStorageKeys
            readerPreferences = loadReaderPreferences()
            bookmarks = loadBookmarks()

            try {
                showTitle(readerBook.title)
                val publication = readiumService.open(readerBook.file)
                openedPublication = publication
                tocEntries = flattenTableOfContents(publication.tableOfContents, publication)
                searchUnavailable = !publication.isSearchable
                totalPositions = runCatching { publication.positions().size }.getOrDefault(0)
                val initialLocator = readerBook.progressionJson?.let {
                    runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull()
                }
                val factory = EpubNavigatorFactory(publication).createFragmentFactory(
                    initialLocator = initialLocator,
                    initialPreferences = readerPreferences.toEpubPreferences(),
                    listener = this@EpubReaderActivity,
                )
                supportFragmentManager.fragmentFactory = factory
                readerContainer.removeAllViews()
                supportFragmentManager.commitNow {
                    replace(
                        readerContainer.id,
                        EpubNavigatorFragment::class.java,
                        Bundle(),
                        NAVIGATOR_TAG,
                    )
                }
                navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG)
                    as EpubNavigatorFragment
                navigator?.currentLocator?.value?.let(::updatePageLabel)
                observeProgression(bookId, temporaryEditionId)
            } catch (_: Throwable) {
                showTitle(getString(R.string.open_book_failed))
            }
        }
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        if (!url.isHttp) return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url.toString())))
        }
    }

    override fun onDestroy() {
        searchJob?.cancel()
        navigator = null
        super.onDestroy()
        openedPublication?.close()
        openedPublication = null
    }

    private fun observeProgression(bookId: String?, temporaryEditionId: String?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                navigator?.currentLocator
                    ?.onEach(::updatePageLabel)
                    ?.debounce(500)
                    ?.collect { locator ->
                        val progression = locator.toJSON().toString()
                        when {
                            bookId != null -> localBookRepository.saveProgression(bookId, progression)
                            temporaryEditionId != null -> temporaryProgressionPreferences()
                                .edit()
                                .putString(temporaryEditionId, progression)
                                .apply()
                        }
                    }
            }
        }
    }

    private fun resolveTemporaryBook(editionId: String): ReaderBook? {
        if (editionId.isBlank()) return null
        val title = intent.getStringExtra(EXTRA_TEMPORARY_TITLE)?.takeIf(String::isNotBlank)
            ?: return null
        val path = intent.getStringExtra(EXTRA_TEMPORARY_FILE_PATH) ?: return null
        val cacheRoot = File(cacheDir, TEMPORARY_READING_DIRECTORY)
        val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return null
        val trustedRoot = runCatching { cacheRoot.canonicalFile }.getOrNull() ?: return null
        if (file.parentFile != trustedRoot || !file.isFile) return null
        return ReaderBook(
            title = title,
            file = file,
            progressionJson = temporaryProgressionPreferences().getString(editionId, null),
            storageKey = "edition:$editionId",
            legacyStorageKeys = listOf("temporary:$editionId"),
        )
    }

    private fun temporaryProgressionPreferences() = getSharedPreferences(
        TEMPORARY_PROGRESSION_PREFERENCES,
        MODE_PRIVATE,
    )

    private fun createContent() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(247, 243, 235))
        }
        readerContainer = FrameLayout(this).apply {
            id = View.generateViewId()
            addView(
                ProgressBar(this@EpubReaderActivity),
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }
        root.addView(
            readerContainer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = (READER_TOOLBAR_HEIGHT_DP * resources.displayMetrics.density).toInt()
            },
        )
        titleView = ComposeView(this)
        root.addView(
            titleView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP,
            ),
        )
        setContentView(root)
        titleView.setContent {
            Archive17Theme(darkTheme = readerPreferences.theme == ReaderTheme.DARK) {
                Column {
                    Surface(
                        color = readerPreferences.toolbarColor().copy(alpha = 0.94f),
                        shadowElevation = 5.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onBackPressedDispatcher::onBackPressed) {
                                Text(getString(R.string.back))
                            }
                            Text(
                                text = readerTitle,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (pageLabel.isNotEmpty()) {
                                Text(
                                    text = pageLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            IconButton(
                                onClick = {
                                    navigationVisible = !navigationVisible
                                    settingsVisible = false
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = getString(R.string.reader_navigation)
                                },
                            ) {
                                Text(text = "☰", style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(
                                onClick = {
                                    settingsVisible = !settingsVisible
                                    navigationVisible = false
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = getString(R.string.reader_settings)
                                },
                            ) {
                                Text(text = "Aa", style = MaterialTheme.typography.labelLarge)
                            }
                            IconButton(
                                onClick = { navigator?.goBackward(animated = true) },
                                modifier = Modifier.semantics {
                                    contentDescription = getString(R.string.reader_previous_page)
                                },
                            ) {
                                Text(text = "‹", fontSize = 28.sp)
                            }
                            IconButton(
                                onClick = { navigator?.goForward(animated = true) },
                                modifier = Modifier.semantics {
                                    contentDescription = getString(R.string.reader_next_page)
                                },
                            ) {
                                Text(text = "›", fontSize = 28.sp)
                            }
                        }
                    }
                    if (settingsVisible) {
                        ReaderFloatingPanel(onClose = { settingsVisible = false }) {
                            ReaderSettingsPanel(
                                preferences = readerPreferences,
                                onChange = ::applyReaderPreferences,
                            )
                        }
                    }
                    if (navigationVisible) {
                        ReaderFloatingPanel(onClose = { navigationVisible = false }) {
                            ReaderNavigationPanel(
                                section = navigationSection,
                                tocEntries = tocEntries,
                                bookmarks = bookmarks,
                                searchQuery = searchQuery,
                                searchResults = searchResults,
                                searchInProgress = searchInProgress,
                                searchSubmitted = searchSubmitted,
                                searchUnavailable = searchUnavailable,
                                onSectionChange = { navigationSection = it },
                                onSearchQueryChange = {
                                    searchQuery = it
                                    searchSubmitted = false
                                },
                                onSearch = ::searchPublication,
                                onAddBookmark = ::addCurrentBookmark,
                                onRemoveBookmark = ::removeBookmark,
                                onGo = ::goToLocator,
                            )
                        }
                    }
                }
            }
        }
        showTitle(getString(R.string.opening_book))
    }

    private fun showTitle(title: String) {
        readerTitle = title
    }

    private fun applyReaderPreferences(preferences: ReaderPreferences) {
        readerPreferences = preferences
        saveReaderPreferences(preferences)
        navigator?.submitPreferences(preferences.toEpubPreferences())
    }

    private fun flattenTableOfContents(
        links: List<Link>,
        publication: org.readium.r2.shared.publication.Publication,
        depth: Int = 0,
    ): List<ReaderTocEntry> = links.flatMap { link ->
        val current = link.title?.takeIf(String::isNotBlank)?.let { title ->
            publication.locatorFromLink(link)?.let { locator ->
                listOf(ReaderTocEntry(title = title, locator = locator, depth = depth))
            }
        }.orEmpty()
        current + flattenTableOfContents(link.children, publication, depth + 1)
    }

    private fun searchPublication() {
        val query = searchQuery.trim()
        val publication = openedPublication ?: return
        if (query.length < 2 || searchInProgress || searchUnavailable) return
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            searchInProgress = true
            searchSubmitted = true
            searchResults = emptyList()
            try {
                val iterator = publication.search(query)
                if (iterator == null) {
                    searchUnavailable = true
                    return@launch
                }
                try {
                    val found = mutableListOf<ReaderSearchResult>()
                    while (found.size < MAX_SEARCH_RESULTS) {
                        val collection = iterator.next().getOrNull() ?: break
                        if (collection.locators.isEmpty()) break
                        found += collection.locators.map { locator ->
                            ReaderSearchResult(locator, searchExcerpt(locator))
                        }
                        searchResults = found.take(MAX_SEARCH_RESULTS)
                    }
                } finally {
                    iterator.close()
                }
            } catch (_: Throwable) {
                searchUnavailable = true
            } finally {
                searchInProgress = false
            }
        }
    }

    private fun searchExcerpt(locator: Locator): String {
        val text = locator.text
        return buildString {
            text.before?.takeLast(60)?.takeIf(String::isNotBlank)?.let {
                append("…")
                append(it.trim())
                append(' ')
            }
            text.highlight?.takeIf(String::isNotBlank)?.let { append(it.trim()) }
            text.after?.take(90)?.takeIf(String::isNotBlank)?.let {
                append(' ')
                append(it.trim())
                append("…")
            }
        }.ifBlank { locator.title.orEmpty() }
    }

    private fun goToLocator(locator: Locator) {
        navigator?.go(locator, animated = true)
        navigationVisible = false
    }

    private fun addCurrentBookmark() {
        val locator = navigator?.currentLocator?.value ?: return
        if (bookmarks.any { bookmarkIdentity(it) == bookmarkIdentity(locator) }) return
        bookmarks = (bookmarks + locator).sortedBy {
            it.locations.totalProgression ?: Double.MAX_VALUE
        }
        saveBookmarks()
    }

    private fun removeBookmark(locator: Locator) {
        val identity = bookmarkIdentity(locator)
        bookmarks = bookmarks.filterNot { bookmarkIdentity(it) == identity }
        saveBookmarks()
    }

    private fun bookmarkIdentity(locator: Locator): String = buildString {
        append(locator.href.removeFragment())
        append(':')
        append(locator.locations.position ?: "")
        append(':')
        append(locator.locations.progression?.let { (it * 10_000).toInt() } ?: "")
    }

    private fun loadBookmarks(): List<Locator> {
        val storageKey = readerStorageKey ?: return emptyList()
        val preferences = getSharedPreferences(READER_BOOKMARKS, MODE_PRIVATE)
        val raw = (listOf(storageKey) + legacyReaderStorageKeys)
            .firstNotNullOfOrNull { key -> preferences.getString(key, null) }
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    Locator.fromJSON(array.getJSONObject(index))?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveBookmarks() {
        val storageKey = readerStorageKey ?: return
        val array = JSONArray().apply {
            bookmarks.forEach { put(it.toJSON()) }
        }
        getSharedPreferences(READER_BOOKMARKS, MODE_PRIVATE)
            .edit()
            .putString(storageKey, array.toString())
            .apply()
    }

    private fun loadReaderPreferences(): ReaderPreferences {
        val preferences = getSharedPreferences(READER_PREFERENCES, MODE_PRIVATE)
        val storageKey = readerStorageKey
        fun keyed(key: String): String? = storageKey?.let { "$it:$key" }
        return ReaderPreferences.restored(
            fontSize = preferences.getFloat(
                keyed(KEY_FONT_SIZE) ?: KEY_FONT_SIZE,
                preferences.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE.toFloat()),
            ).toDouble(),
            lineHeight = preferences.getFloat(
                keyed(KEY_LINE_HEIGHT) ?: KEY_LINE_HEIGHT,
                preferences.getFloat(KEY_LINE_HEIGHT, DEFAULT_LINE_HEIGHT.toFloat()),
            ).toDouble(),
            font = preferences.getString(keyed(KEY_FONT) ?: KEY_FONT, null)
                ?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
                ?: preferences.getString(KEY_FONT, null)
                ?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
                ?: ReaderFont.PUBLISHER,
            theme = preferences.getString(keyed(KEY_THEME) ?: KEY_THEME, null)
                ?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() }
                ?: preferences.getString(KEY_THEME, null)
                ?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() }
                ?: ReaderTheme.LIGHT,
        )
    }

    private fun saveReaderPreferences(preferences: ReaderPreferences) {
        val storageKey = readerStorageKey ?: return
        getSharedPreferences(READER_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putFloat("$storageKey:$KEY_FONT_SIZE", preferences.fontSize.toFloat())
            .putFloat("$storageKey:$KEY_LINE_HEIGHT", preferences.lineHeight.toFloat())
            .putString("$storageKey:$KEY_FONT", preferences.font.name)
            .putString("$storageKey:$KEY_THEME", preferences.theme.name)
            .apply()
    }

    private fun updatePageLabel(locator: Locator) {
        val position = locator.locations.position
        pageLabel = when {
            position != null && totalPositions > 0 -> getString(
                R.string.reader_page_of,
                position,
                totalPositions,
            )
            position != null -> getString(R.string.reader_page, position)
            locator.locations.totalProgression != null -> getString(
                R.string.reader_progress,
                (locator.locations.totalProgression!! * 100).toInt().coerceIn(0, 100),
            )
            else -> ""
        }
    }

    companion object {
        private const val READER_TOOLBAR_HEIGHT_DP = 56
        private const val EXTRA_BOOK_ID = "book_id"
        private const val EXTRA_TEMPORARY_EDITION_ID = "temporary_edition_id"
        private const val EXTRA_TEMPORARY_TITLE = "temporary_title"
        private const val EXTRA_TEMPORARY_FILE_PATH = "temporary_file_path"
        private const val NAVIGATOR_TAG = "epub_navigator"
        private const val TEMPORARY_READING_DIRECTORY = "temporary-reading"
        private const val TEMPORARY_PROGRESSION_PREFERENCES = "temporary_reader_progress"
        private const val READER_PREFERENCES = "reader_preferences"
        private const val READER_BOOKMARKS = "reader_bookmarks"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_LINE_HEIGHT = "line_height"
        private const val KEY_FONT = "font"
        private const val KEY_THEME = "theme"
        private const val DEFAULT_FONT_SIZE = 1.0
        private const val DEFAULT_LINE_HEIGHT = 1.5
        private const val MAX_SEARCH_RESULTS = 100

        fun createIntent(context: android.content.Context, bookId: String): Intent =
            Intent(context, EpubReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)

        fun createTemporaryIntent(
            context: android.content.Context,
            book: TemporaryBook,
        ): Intent = Intent(context, EpubReaderActivity::class.java)
            .putExtra(EXTRA_TEMPORARY_EDITION_ID, book.editionId)
            .putExtra(EXTRA_TEMPORARY_TITLE, book.title)
            .putExtra(EXTRA_TEMPORARY_FILE_PATH, book.filePath)
    }

    private data class ReaderBook(
        val title: String,
        val file: File,
        val progressionJson: String?,
        val storageKey: String,
        val legacyStorageKeys: List<String> = emptyList(),
    )
}

@Composable
private fun ReaderFloatingPanel(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val closeDescription = stringResource(R.string.reader_close_panel)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Box {
            Box(modifier = Modifier.padding(top = 34.dp)) {
                content()
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .semantics {
                        contentDescription = closeDescription
                    },
            ) {
                Text(text = "×", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
