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
    val textOnBlue: Color,
    val ringBackground: Color,
    val ringProgress: Color,
    val categorySelected: Color,
    val categoryUnselected: Color,
)

val LocalVaultSpecColors = staticCompositionLocalOf {
    VaultSpecExtraColors(
        cardBlue = CardBlue,
        cardWhite = CardWhite,
        textOnBlue = TextOnBlue,
        ringBackground = RingBackground,
        ringProgress = RingProgress,
        categorySelected = CategorySelected,
        categoryUnselected = CategoryUnselected,
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
    textOnBlue = TextOnBlue,
    ringBackground = RingBackground,
    ringProgress = RingProgress,
    categorySelected = CategorySelected,
    categoryUnselected = CategoryUnselected,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color(0xFF003258),
    primaryContainer = Blue600,
    onPrimaryContainer = Blue100,
    secondary = DarkCategorySelected,
    onSecondary = Color(0xFF003258),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    outline = DarkGrayLight,
    outlineVariant = DarkGrayLight,
)

private val DarkExtraColors = VaultSpecExtraColors(
    cardBlue = Blue500,
    cardWhite = DarkCardWhite,
    textOnBlue = TextOnBlue,
    ringBackground = DarkRingBackground,
    ringProgress = Blue400,
    categorySelected = DarkCategorySelected,
    categoryUnselected = DarkCategoryUnselected,
)

@Composable
fun VaultSpecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalVaultSpecColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VaultSpecTypography,
            content = content,
        )
    }
}
