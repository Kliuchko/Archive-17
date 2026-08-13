package com.kliuchko.archive17.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kliuchko.archive17.presentation.theme.ArchiveBrass
import com.kliuchko.archive17.presentation.theme.ArchiveInk
import com.kliuchko.archive17.presentation.theme.ArchiveWine
import kotlinx.coroutines.delay

@Composable
fun ArchiveSplashScreen(modifier: Modifier = Modifier) {
    val entrance = remember { Animatable(0f) }
    val progress by entrance.asState()
    val opening = ((progress - 0.08f) / 0.5f).coerceIn(0f, 1f)
    val zoom = ((progress - 0.52f) / 0.48f).coerceIn(0f, 1f)
    val entranceScale = 0.9f + progress * 0.1f + zoom * 5.8f

    LaunchedEffect(Unit) {
        delay(90)
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 880,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer {
                    scaleX = entranceScale
                    scaleY = entranceScale
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(122.dp)
                    .background(ArchiveInk, CircleShape),
            )
            ArchiveDoorFrame(modifier = Modifier.fillMaxSize())
            ArchiveDoorLeaf(
                modifier = Modifier.graphicsLayer {
                    rotationY = -82f * opening
                    transformOrigin = TransformOrigin(0.06f, 0.5f)
                    cameraDistance = 14f * density
                    translationX = -6.dp.toPx() * opening
                    alpha = 1f - opening * 0.12f
                },
            )
        }
        Text(
            text = "ARCHIVE 17",
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = 112.dp.toPx() }
                .alpha((1f - zoom * 2.2f).coerceIn(0f, 1f)),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ArchiveDoorFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = center
        drawCircle(color = ArchiveBrass, radius = size.minDimension * 0.5f, center = center)
        drawCircle(color = ArchiveInk, radius = size.minDimension * 0.42f, center = center)
        drawCircle(color = ArchiveBrass, radius = size.minDimension * 0.38f, center = center)
        drawCircle(color = ArchiveInk, radius = size.minDimension * 0.345f, center = center)
    }
}

@Composable
private fun ArchiveDoorLeaf(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(102.dp)
            .background(ArchiveWine, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.47f
            repeat(8) { index ->
                val angle = Math.toRadians((index * 45.0) - 90.0)
                drawLine(
                    color = ArchiveBrass.copy(alpha = 0.78f),
                    start = center,
                    end = androidx.compose.ui.geometry.Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * radius,
                        y = center.y + kotlin.math.sin(angle).toFloat() * radius,
                    ),
                    strokeWidth = 1.4.dp.toPx(),
                )
            }
            drawCircle(
                color = ArchiveBrass,
                radius = size.minDimension * 0.2f,
                center = center,
            )
            drawCircle(
                color = Color(0xFFF4ECDF),
                radius = size.minDimension * 0.165f,
                center = center,
            )
        }
        Text(
            text = "17",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ArchiveInk,
        )
    }
}
