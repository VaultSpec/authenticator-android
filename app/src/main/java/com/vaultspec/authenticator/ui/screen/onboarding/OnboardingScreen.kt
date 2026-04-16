package com.vaultspec.authenticator.ui.screen.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultspec.authenticator.ui.theme.Inter
import com.vaultspec.authenticator.ui.theme.LocalVaultSpecColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

// ── Bar geometry (108×108 viewport) ──

private data class BarSpec(val cx: Float, val topY: Float, val h: Float)

private val BARS = listOf(
    BarSpec(21f, 37f, 34f),
    BarSpec(27f, 33f, 42f),
    BarSpec(33f, 28f, 52f),
    BarSpec(39f, 23f, 62f),
    BarSpec(45f, 18f, 72f),
    BarSpec(51f, 15f, 78f),
    BarSpec(57f, 15f, 78f),
    BarSpec(63f, 18f, 72f),
    BarSpec(69f, 23f, 62f),
    BarSpec(75f, 28f, 52f),
    BarSpec(81f, 33f, 42f),
    BarSpec(87f, 37f, 34f),
)

private const val BAR_W = 4f
private const val CORNER_R = 2f

// ── Onboarding page model ──

private data class OnboardingPage(
    val headline: String,
    val subtext: String,
)

private val PAGES = listOf(
    OnboardingPage(
        headline = "Secure Your Digital Identity",
        subtext = "VaultSpec protects your accounts with\nstrong, private authentication.",
    ),
    OnboardingPage(
        headline = "Fast. Private. Reliable.",
        subtext = "Generate codes instantly\nwith zero tracking.",
    ),
    OnboardingPage(
        headline = "All Your Codes. One Vault.",
        subtext = "Everything secured,\nsimple to access.",
    ),
)

// ── Main composable ──

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val extra = LocalVaultSpecColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingPageContent(
                    page = page,
                    data = PAGES[page],
                )
            }

            // Page indicators + controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
            ) {
                // Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(PAGES.size) { index ->
                        val isActive = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(
                                    width = if (isActive) 24.dp else 8.dp,
                                    height = 8.dp,
                                )
                                .clip(CircleShape)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (pagerState.currentPage == PAGES.size - 1) {
                    // Last page: CTA button
                    val buttonShape = RoundedCornerShape(12.dp)
                    Button(
                        onClick = onComplete,
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                        ),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            extra.unlockGradientTop,
                                            extra.unlockGradientBottom,
                                        ),
                                    ),
                                    shape = buttonShape,
                                ),
                        ) {
                            Text(
                                "Get Started",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White,
                                letterSpacing = 0.3.sp,
                            )
                        }
                    }
                } else {
                    // Not last page: Next + Skip
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(
                            "Continue",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            letterSpacing = 0.2.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onComplete) {
                        Text(
                            "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

// ── Individual page ──

@Composable
private fun OnboardingPageContent(
    page: Int,
    data: OnboardingPage,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
    ) {
        // Animated logo area
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp),
        ) {
            when (page) {
                0 -> ShieldAnimation()
                1 -> WaveAnimation()
                2 -> VaultAnimation()
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = data.headline,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = data.subtext,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Page 1: Shield — bars lock into place from scattered positions ──

@Composable
private fun ShieldAnimation() {
    val barColor = MaterialTheme.colorScheme.onBackground
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(1000, easing = FastOutSlowInEasing),
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = size.width / 108f
        val p = progress.value

        BARS.forEachIndexed { i, bar ->
            // Bars slide in from far above/below and converge to their position
            val distFromCenter = abs(i - 5.5f)
            val scatter = (1f - p) * distFromCenter * 6f
            val barH = bar.h * scale * p
            val barW = BAR_W * scale
            val x = bar.cx * scale - barW / 2f
            val y = (bar.topY + bar.h / 2f) * scale - barH / 2f - scatter * scale
            val cr = CORNER_R * scale

            drawRoundRect(
                color = barColor.copy(alpha = p),
                topLeft = Offset(x, y),
                size = Size(barW, barH.coerceAtLeast(0f)),
                cornerRadius = CornerRadius(cr, cr),
            )
        }
    }
}

// ── Page 2: Wave — bars oscillate horizontally like a signal ──

@Composable
private fun WaveAnimation() {
    val barColor = MaterialTheme.colorScheme.onBackground
    val transition = rememberInfiniteTransition(label = "wave")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = size.width / 108f

        BARS.forEachIndexed { i, bar ->
            val wave = sin(phase.value + i * 0.5f).toFloat()
            val xShift = wave * 3f * scale
            val barH = bar.h * scale
            val barW = BAR_W * scale
            val x = bar.cx * scale - barW / 2f + xShift
            val y = bar.topY * scale
            val cr = CORNER_R * scale

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(cr, cr),
            )
        }
    }
}

// ── Page 3: Vault — bars compress vertically into a vault shape ──

@Composable
private fun VaultAnimation() {
    val barColor = MaterialTheme.colorScheme.onBackground
    val transition = rememberInfiniteTransition(label = "vault")
    val breathe = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = size.width / 108f
        val b = breathe.value

        BARS.forEachIndexed { i, bar ->
            // Equalize heights toward the max (vault = uniform container)
            val maxH = 78f // tallest bar
            val targetH = bar.h + (maxH - bar.h) * b * 0.6f
            val barH = targetH * scale
            val barW = BAR_W * scale
            val centerY = 54f * scale
            val x = bar.cx * scale - barW / 2f
            val y = centerY - barH / 2f
            val cr = CORNER_R * scale

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(cr, cr),
            )
        }
    }
}
