package com.vaultspec.authenticator.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultspec.authenticator.ui.theme.*

@Composable
fun TokenCard(
    issuer: String,
    accountName: String,
    code: String,
    secondsRemaining: Int,
    period: Int,
    isFeatured: Boolean,
    modifier: Modifier = Modifier,
    isRevealed: Boolean = true,
    isHighlighted: Boolean = false,
    onTap: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val displayCode = if (isRevealed) formatCode(code) else "• • • • • •"

    val extra = LocalVaultSpecColors.current
    val backgroundColor = extra.cardWhite
    val contentColor = MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Urgency-aware ring colors
    val urgencyColor = when {
        secondsRemaining <= 5 -> extra.urgencyDanger
        secondsRemaining <= 10 -> extra.urgencyWarning
        else -> extra.ringProgress
    }
    val ringBg = extra.ringBackground
    val ringText = urgencyColor
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    val highlightBorder by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "highlight",
    )

    // Subtle border — featured gets blue accent, others get neutral
    val cardBorder = when {
        isHighlighted -> BorderStroke(1.5.dp, highlightBorder)
        isFeatured -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        else -> BorderStroke(1.dp, extra.cardBorder)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (onTap != null) {
                    onTap()
                } else {
                    copyToClipboard(context, code)
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top row: Service icon + name + email + copy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ServiceIcon(
                    issuer = issuer,
                    size = 40.dp,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = issuer,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (accountName.isNotBlank()) {
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                            maxLines = 1,
                        )
                    }
                }

                IconButton(onClick = { copyToClipboard(context, code) }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy code",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: TOTP code + countdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayCode,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        letterSpacing = 5.sp,
                    ),
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                )

                CountdownIndicator(
                    secondsRemaining = secondsRemaining,
                    totalSeconds = period,
                    size = 42.dp,
                    strokeWidth = 3.5.dp,
                    progressColor = urgencyColor,
                    backgroundColor = ringBg,
                    textColor = ringText,
                )
            }
        }
    }
}

private fun formatCode(code: String): String {
    return if (code.length == 6) {
        "${code.substring(0, 3)} ${code.substring(3)}"
    } else if (code.length == 8) {
        "${code.substring(0, 4)} ${code.substring(4)}"
    } else {
        code
    }
}

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("TOTP Code", code)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()

    // Auto-clear clipboard after 30 seconds
    Handler(Looper.getMainLooper()).postDelayed({
        try {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        } catch (_: Exception) { }
    }, 30_000)
}
