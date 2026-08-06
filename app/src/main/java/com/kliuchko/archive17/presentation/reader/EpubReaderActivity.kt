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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kliuchko.archive17.data.reader.ReadiumService
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.presentation.theme.Archive17Theme
import java.io.File
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class, kotlinx.coroutines.FlowPreview::class)
class EpubReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener {
    private val localBookRepository: LocalBookRepository by inject()
    private val readiumService: ReadiumService by inject()

    private lateinit var readerContainer: FrameLayout
    private lateinit var titleView: ComposeView
    private var navigator: EpubNavigatorFragment? = null
    private var openedPublication: org.readium.r2.shared.publication.Publication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        super.onCreate(null)
        createContent()

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID)
        if (bookId == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            val book = localBookRepository.getLocalBook(bookId)
            if (book == null || !File(book.filePath).exists()) {
                showTitle("Книга недоступна")
                return@launch
            }

            try {
                showTitle(book.title)
                val publication = readiumService.open(File(book.filePath))
                openedPublication = publication
                val initialLocator = book.progressionJson?.let {
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
                observeProgression(bookId)
            } catch (_: Throwable) {
                showTitle("Не удалось открыть книгу")
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

    private fun observeProgression(bookId: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                navigator?.currentLocator
                    ?.debounce(500)
                    ?.collect { locator ->
                        localBookRepository.saveProgression(bookId, locator.toJSON().toString())
                    }
            }
        }
    }

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
        showTitle("Открываем книгу…")
    }

    private fun showTitle(title: String) {
        titleView.setContent {
            Archive17Theme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onBackPressedDispatcher::onBackPressed) {
                            Text("Назад")
                        }
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"
        private const val NAVIGATOR_TAG = "epub_navigator"

        fun createIntent(context: android.content.Context, bookId: String): Intent =
            Intent(context, EpubReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}
