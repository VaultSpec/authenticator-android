package com.vaultspec.authenticator.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultspec.authenticator.ui.theme.LocalVaultSpecColors

@Composable
fun CountdownIndicator(
    secondsRemaining: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 3.dp,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = LocalVaultSpecColors.current.fillOnSurface,
    textColor: Color = progressColor,
) {
    val progress by animateFloatAsState(
        targetValue = secondsRemaining.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "countdown_progress"
    )

    val animatedProgressColor by animateColorAsState(
        targetValue = progressColor,
        animationSpec = tween(durationMillis = 400),
        label = "countdown_color"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = textColor,
        animationSpec = tween(durationMillis = 400),
        label = "countdown_text_color"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            val inset = strokePx / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - strokePx,
                this.size.height - strokePx,
            )
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            // Background ring
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            // Progress ring
            drawArc(
                color = animatedProgressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Text(
            text = "$secondsRemaining",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = if (size >= 40.dp) 13.sp else 11.sp,
                fontFeatureSettings = "tnum",
            ),
            color = animatedTextColor,
        )
    }
}
