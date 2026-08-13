package com.kliuchko.archive17.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kliuchko.archive17.data.networking.CoverSize
import com.kliuchko.archive17.data.networking.CoverUrlBuilder
import com.kliuchko.archive17.domain.model.Work
import com.kliuchko.archive17.domain.model.LocalBook
import com.kliuchko.archive17.domain.model.FreeBook
import java.io.File

@Composable
fun ArchiveMark(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "17",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ArchiveSeal(
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(size)
            .border(2.dp, color, CircleShape)
            .padding(9.dp)
            .border(1.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "17",
            style = MaterialTheme.typography.displayLarge,
            color = color,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.55f,
        )
    }
}

@Composable
fun ArchiveBrand(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArchiveMark()
        Text(
            text = "ARCHIVE 17",
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f,
        )
    }
}

@Composable
fun ArchiveFloatingHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shadowElevation = 7.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BookCover(
    work: Work,
    modifier: Modifier = Modifier,
    width: Dp = 58.dp,
    height: Dp = 86.dp,
    coverSize: CoverSize = CoverSize.MEDIUM,
) {
    val shape = RoundedCornerShape(topStart = 5.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 5.dp)
    val coverUrl = CoverUrlBuilder.build(work.coverId, coverSize)

    if (coverUrl == null) {
        GeneratedCover(
            title = work.title,
            modifier = modifier,
            width = width,
            height = height,
        )
    } else {
        StableBookCover(
            model = coverUrl,
            title = work.title,
            modifier = modifier,
            width = width,
            height = height,
            shape = shape,
        )
    }
}

@Composable
fun FreeBookCover(
    book: FreeBook,
    modifier: Modifier = Modifier,
    width: Dp = 58.dp,
    height: Dp = 86.dp,
) {
    val shape = RoundedCornerShape(topStart = 5.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 5.dp)
    val coverUrl = book.coverUrl ?: CoverUrlBuilder.build(book.coverId, CoverSize.MEDIUM)
    if (coverUrl == null) {
        GeneratedCover(book.title, modifier, width, height)
    } else {
        StableBookCover(
            model = coverUrl,
            title = book.title,
            modifier = modifier,
            width = width,
            height = height,
            shape = shape,
        )
    }
}

@Composable
fun LocalBookCover(
    book: LocalBook,
    modifier: Modifier = Modifier,
    width: Dp = 58.dp,
    height: Dp = 86.dp,
) {
    val coverPath = book.coverPath
    if (coverPath == null) {
        GeneratedCover(
            title = book.title,
            modifier = modifier,
            width = width,
            height = height,
        )
    } else {
        val shape = RoundedCornerShape(topStart = 5.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 5.dp)
        StableBookCover(
            model = File(coverPath),
            title = book.title,
            modifier = modifier,
            width = width,
            height = height,
            shape = shape,
        )
    }
}

@Composable
private fun StableBookCover(
    model: Any,
    title: String,
    width: Dp,
    height: Dp,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .crossfade(180)
            .build()
    }
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.tertiary),
    ) {
        GeneratedCover(
            title = title,
            width = width,
            height = height,
        )
        AsyncImage(
            model = request,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun GeneratedCover(
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 58.dp,
    height: Dp = 86.dp,
) {
    val shape = RoundedCornerShape(topStart = 5.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 5.dp)
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = 5.dp, height = height)
                .background(MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.16f)),
        )
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiary,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

enum class ArchiveNavIcon {
    HOME,
    SEARCH,
    SHELF,
    PROFILE,
}

@Composable
fun ArchiveNavigationIcon(
    icon: ArchiveNavIcon,
    modifier: Modifier = Modifier,
) {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx())
        when (icon) {
            ArchiveNavIcon.HOME -> {
                val path = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.48f)
                    lineTo(size.width * 0.5f, size.height * 0.18f)
                    lineTo(size.width * 0.85f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.84f)
                    lineTo(size.width * 0.22f, size.height * 0.84f)
                    lineTo(size.width * 0.22f, size.height * 0.48f)
                    close()
                }
                drawPath(path, color = color, style = stroke)
            }

            ArchiveNavIcon.SEARCH -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.43f, size.height * 0.42f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.63f, size.height * 0.63f),
                    end = Offset(size.width * 0.84f, size.height * 0.84f),
                    strokeWidth = stroke.width,
                )
            }

            ArchiveNavIcon.SHELF -> {
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.2f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.2f, size.height * 0.62f),
                    style = stroke,
                )
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * 0.41f, size.height * 0.14f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.2f, size.height * 0.68f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.69f, size.height * 0.22f),
                    end = Offset(size.width * 0.83f, size.height * 0.8f),
                    strokeWidth = stroke.width,
                )
            }

            ArchiveNavIcon.PROFILE -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.17f,
                    center = Offset(size.width * 0.5f, size.height * 0.32f),
                    style = stroke,
                )
                val path = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.82f)
                    quadraticTo(
                        size.width * 0.25f,
                        size.height * 0.57f,
                        size.width * 0.5f,
                        size.height * 0.57f,
                    )
                    quadraticTo(
                        size.width * 0.75f,
                        size.height * 0.57f,
                        size.width * 0.78f,
                        size.height * 0.82f,
                    )
                }
                drawPath(path, color = color, style = stroke)
            }
        }
    }
}

@Composable
fun EmptyMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
