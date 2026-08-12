package com.kliuchko.archive17.presentation.reader

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.positions
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
                    ReaderBook(
                        title = book.title,
                        file = File(book.filePath),
                        progressionJson = book.progressionJson,
                    )
                }
            } else {
                resolveTemporaryBook(temporaryEditionId.orEmpty())
            }
            if (readerBook == null || !readerBook.file.exists()) {
                showTitle(getString(R.string.book_unavailable))
                return@launch
            }

            try {
                showTitle(readerBook.title)
                val publication = readiumService.open(readerBook.file)
                openedPublication = publication
                totalPositions = runCatching { publication.positions().size }.getOrDefault(0)
                val initialLocator = readerBook.progressionJson?.let {
                    runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull()
                }
                val factory = EpubNavigatorFactory(publication).createFragmentFactory(
                    initialLocator = initialLocator,
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
        )
    }

    private fun temporaryProgressionPreferences() = getSharedPreferences(
        TEMPORARY_PROGRESSION_PREFERENCES,
        MODE_PRIVATE,
    )

    private fun createContent() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.rgb(247, 243, 235))
        }
        titleView = ComposeView(this)
        root.addView(
            titleView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
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
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        setContentView(root)
        titleView.setContent {
            Archive17Theme {
                Surface(color = MaterialTheme.colorScheme.surface) {
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
            }
        }
        showTitle(getString(R.string.opening_book))
    }

    private fun showTitle(title: String) {
        readerTitle = title
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
        private const val EXTRA_BOOK_ID = "book_id"
        private const val EXTRA_TEMPORARY_EDITION_ID = "temporary_edition_id"
        private const val EXTRA_TEMPORARY_TITLE = "temporary_title"
        private const val EXTRA_TEMPORARY_FILE_PATH = "temporary_file_path"
        private const val NAVIGATOR_TAG = "epub_navigator"
        private const val TEMPORARY_READING_DIRECTORY = "temporary-reading"
        private const val TEMPORARY_PROGRESSION_PREFERENCES = "temporary_reader_progress"

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
    )
}
