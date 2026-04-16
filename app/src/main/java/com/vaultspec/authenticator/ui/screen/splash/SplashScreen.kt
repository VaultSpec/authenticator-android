package com.vaultspec.authenticator.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bar data: centerX, centerY, width, height in a 108×108 viewport.
 * Derived from the VaultSpec shield logo vector.
 */
private data class BarSpec(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
)

private val BARS = listOf(
    BarSpec(21f, 54f, 4f, 34f),
    BarSpec(27f, 54f, 4f, 42f),
    BarSpec(33f, 54f, 4f, 52f),
    BarSpec(39f, 54f, 4f, 62f),
    BarSpec(45f, 54f, 4f, 72f),
    BarSpec(51f, 54f, 4f, 78f),
    BarSpec(57f, 54f, 4f, 78f),
    BarSpec(63f, 54f, 4f, 72f),
    BarSpec(69f, 54f, 4f, 62f),
    BarSpec(75f, 54f, 4f, 52f),
    BarSpec(81f, 54f, 4f, 42f),
    BarSpec(87f, 54f, 4f, 34f),
)

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    // Phase 1: bars grow from center (0 → full height), staggered
    // Phase 2: subtle pulse (scale 1.0 → 0.92 → 1.0)
    // Phase 3: fade out

    val barAnimations = remember { BARS.map { Animatable(0f) } }
    val pulseAnim = remember { Animatable(1f) }
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: staggered bar growth from center outward
        val center = BARS.size / 2
        BARS.indices.forEach { i ->
            val distFromCenter = kotlin.math.abs(i - center + 0.5f)
            val delayMs = (distFromCenter * 40).toInt()
            launch {
                delay(delayMs.toLong())
                barAnimations[i].animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }
        delay(600)

        // Phase 2: gentle pulse
        pulseAnim.animateTo(
            0.93f,
            tween(400, easing = FastOutSlowInEasing),
        )
        pulseAnim.animateTo(
            1f,
            tween(350, easing = FastOutSlowInEasing),
        )

        delay(200)

        // Phase 3: fade out
        alphaAnim.animateTo(
            0f,
            tween(300, easing = FastOutSlowInEasing),
        )

        onFinished()
    }

    val barColor = MaterialTheme.colorScheme.onBackground

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(
            modifier = Modifier.size(140.dp),
        ) {
            val scale = size.width / 108f
            val pulse = pulseAnim.value
            val alpha = alphaAnim.value

            BARS.forEachIndexed { i, bar ->
                val progress = barAnimations[i].value
                val barH = bar.h * scale * progress * pulse
                val barW = bar.w * scale
                val x = bar.cx * scale - barW / 2f
                val y = bar.cy * scale - barH / 2f
                val cornerR = 2f * scale

                drawRoundRect(
                    color = barColor.copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
            }
        }
    }
}
