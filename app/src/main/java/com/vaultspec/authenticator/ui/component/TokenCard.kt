package com.vaultspec.authenticator.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultspec.authenticator.ui.theme.LocalVaultSpecColors
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens

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
    val extra = LocalVaultSpecColors.current

    // The featured card is filled with the accent color and uses white content
    val containerColor = if (isFeatured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val titleColor = if (isFeatured) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isFeatured) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val codeColor = when {
        !isRevealed -> if (isFeatured) Color.White.copy(alpha = 0.7f) else extra.tertiaryLabel
        isFeatured -> Color.White
        else -> extra.codeText
    }

    // Urgency-aware ring color
    val urgencyColor = when {
        isFeatured -> Color.White
        secondsRemaining <= 5 -> extra.red
        secondsRemaining <= 10 -> extra.orange
        else -> MaterialTheme.colorScheme.primary
    }

    val highlightBorder by animateColorAsState(
        targetValue = when {
            !isHighlighted -> Color.Transparent
            isFeatured -> Color.White
            else -> MaterialTheme.colorScheme.primary
        },
        label = "highlight",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(VaultSpecDimens.CardShape)
            .background(containerColor)
            .border(2.dp, highlightBorder, VaultSpecDimens.CardShape)
            .clickable {
                if (onTap != null) {
                    onTap()
                } else {
                    copyToClipboard(context, code)
                }
            }
            .padding(16.dp)
    ) {
        // Top row: service icon + name + account + copy
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ServiceIcon(issuer = issuer, size = 44.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = issuer,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (accountName.isNotBlank()) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            CircleIconButton(
                icon = AppIcons.Copy,
                contentDescription = "Copy code",
                onClick = { copyToClipboard(context, code) },
                tint = if (isFeatured) Color.White else MaterialTheme.colorScheme.primary,
                containerColor = if (isFeatured) Color.White.copy(alpha = 0.2f) else extra.fillOnSurface,
                size = 36.dp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom row: TOTP code + countdown
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRevealed) formatCode(code) else "••• •••",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = codeColor,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            CountdownIndicator(
                secondsRemaining = secondsRemaining,
                totalSeconds = period,
                size = 44.dp,
                strokeWidth = 3.5.dp,
                progressColor = urgencyColor,
                backgroundColor = if (isFeatured) Color.White.copy(alpha = 0.25f) else extra.fillOnSurface,
            )
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
