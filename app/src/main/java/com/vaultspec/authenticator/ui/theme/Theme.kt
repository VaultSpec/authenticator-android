package com.vaultspec.authenticator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Custom extras that M3 colorScheme doesn't cover
data class VaultSpecExtraColors(
    val cardBlue: Color,
    val cardWhite: Color,
    val cardBorder: Color,
    val textOnBlue: Color,
    val ringBackground: Color,
    val ringProgress: Color,
    val urgencyWarning: Color,
    val urgencyDanger: Color,
    val categorySelected: Color,
    val categoryUnselected: Color,
    val unlockGradientTop: Color,
    val unlockGradientBottom: Color,
)

val LocalVaultSpecColors = staticCompositionLocalOf {
    VaultSpecExtraColors(
        cardBlue = CardBlue,
        cardWhite = CardWhite,
        cardBorder = CardBorder,
        textOnBlue = TextOnBlue,
        ringBackground = RingBackground,
        ringProgress = RingProgress,
        urgencyWarning = UrgencyWarning,
        urgencyDanger = UrgencyDanger,
        categorySelected = CategorySelected,
        categoryUnselected = CategoryUnselected,
        unlockGradientTop = UnlockGradientTopLight,
        unlockGradientBottom = UnlockGradientBottomLight,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue600,
    secondary = CategorySelected,
    onSecondary = White,
    secondaryContainer = GraySurface,
    onSecondaryContainer = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = GraySurface,
    onSurfaceVariant = TextSecondary,
    background = GrayBackground,
    onBackground = TextPrimary,
    outline = GrayLight,
    outlineVariant = GrayLight,
)

private val LightExtraColors = VaultSpecExtraColors(
    cardBlue = CardBlue,
    cardWhite = CardWhite,
    cardBorder = CardBorder,
    textOnBlue = TextOnBlue,
    ringBackground = RingBackground,
    ringProgress = RingProgress,
    urgencyWarning = UrgencyWarning,
    urgencyDanger = UrgencyDanger,
    categorySelected = CategorySelected,
    categoryUnselected = CategoryUnselected,
    unlockGradientTop = UnlockGradientTopLight,
    unlockGradientBottom = UnlockGradientBottomLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Blue600,
    onPrimaryContainer = Blue100,
    secondary = DarkCategorySelected,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    outline = DarkGrayLight,
    outlineVariant = DarkCardBorder,
)

private val DarkExtraColors = VaultSpecExtraColors(
    cardBlue = Blue500,
    cardWhite = DarkCardWhite,
    cardBorder = DarkCardBorder,
    textOnBlue = TextOnBlue,
    ringBackground = DarkRingBackground,
    ringProgress = Blue400,
    urgencyWarning = UrgencyWarning,
    urgencyDanger = UrgencyDanger,
    categorySelected = DarkCategorySelected,
    categoryUnselected = DarkCategoryUnselected,
    unlockGradientTop = UnlockGradientTopDark,
    unlockGradientBottom = UnlockGradientBottomDark,
)

private val PitchBlackColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Blue600,
    onPrimaryContainer = Blue100,
    secondary = DarkCategorySelected,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = PitchBlackSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    surface = PitchBlackSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = PitchBlackSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    background = PitchBlackBackground,
    onBackground = DarkTextPrimary,
    outline = PitchBlackGrayLight,
    outlineVariant = PitchBlackCardBorder,
)

private val PitchBlackExtraColors = VaultSpecExtraColors(
    cardBlue = Blue500,
    cardWhite = PitchBlackCardWhite,
    cardBorder = PitchBlackCardBorder,
    textOnBlue = TextOnBlue,
    ringBackground = PitchBlackRingBackground,
    ringProgress = Blue400,
    urgencyWarning = UrgencyWarning,
    urgencyDanger = UrgencyDanger,
    categorySelected = DarkCategorySelected,
    categoryUnselected = PitchBlackCategoryUnselected,
    unlockGradientTop = UnlockGradientTopDark,
    unlockGradientBottom = UnlockGradientBottomDark,
)

@Composable
fun VaultSpecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pitchBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme && pitchBlack -> PitchBlackColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extraColors = when {
        darkTheme && pitchBlack -> PitchBlackExtraColors
        darkTheme -> DarkExtraColors
        else -> LightExtraColors
    }

    CompositionLocalProvider(LocalVaultSpecColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VaultSpecTypography,
            content = content,
        )
    }
}
