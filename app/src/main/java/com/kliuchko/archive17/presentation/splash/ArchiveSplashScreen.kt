package com.kliuchko.archive17.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kliuchko.archive17.presentation.components.ArchiveSeal

@Composable
fun ArchiveSplashScreen(modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    val sealScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.88f,
        animationSpec = tween(durationMillis = 560),
        label = "splash-seal-scale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 480, delayMillis = 80),
        label = "splash-content-alpha",
    )

    LaunchedEffect(Unit) { started = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ArchiveSeal(
                modifier = Modifier
                    .scale(sealScale)
                    .alpha(contentAlpha),
                size = 132.dp,
            )
            Text(
                text = "ARCHIVE 17",
                modifier = Modifier.alpha(contentAlpha),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 1.8f,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
