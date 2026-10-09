package com.vaultspec.authenticator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vaultspec.authenticator.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val VaultSpecTypography = Typography(
    // TOTP code
    displayMedium = style(30, 36, FontWeight.SemiBold, 1.0),
    // Large title
    displaySmall = style(34, 41, FontWeight.Bold, -0.6),
    // Title 1
    headlineLarge = style(28, 34, FontWeight.Bold, -0.5),
    // Title 2
    headlineMedium = style(22, 28, FontWeight.Bold, -0.4),
    // Title 3
    headlineSmall = style(20, 25, FontWeight.SemiBold, -0.3),
    titleLarge = style(20, 25, FontWeight.SemiBold, -0.3),
    // Headline
    titleMedium = style(17, 22, FontWeight.SemiBold, -0.3),
    titleSmall = style(15, 20, FontWeight.SemiBold, -0.2),
    // Body
    bodyLarge = style(17, 22, FontWeight.Normal, -0.3),
    // Subheadline
    bodyMedium = style(15, 20, FontWeight.Normal, -0.2),
    // Footnote
    bodySmall = style(13, 18, FontWeight.Normal, -0.1),
    labelLarge = style(15, 20, FontWeight.Medium, -0.2),
    labelMedium = style(13, 18, FontWeight.Medium, -0.1),
    // Caption
    labelSmall = style(12, 16, FontWeight.Normal),
)
