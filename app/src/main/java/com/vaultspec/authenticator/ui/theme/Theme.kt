package com.vaultspec.authenticator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Semantic colors that M3 colorScheme doesn't cover
data class VaultSpecExtraColors(
    val fill: Color,           // controls on the page background
    val fillOnSurface: Color,  // controls on a card / dialog / sheet
    val separator: Color,
    val tertiaryLabel: Color,
    val codeText: Color,       // TOTP digits
    val switchOff: Color,
    val segmentThumb: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val indigo: Color,
    val teal: Color,
    val pink: Color,
    val gray: Color,
)

// Corner radii and metrics shared by every screen
object VaultSpecDimens {
    val ScreenPadding = 20.dp
    val ControlHeight = 52.dp
    val CardShape = RoundedCornerShape(20.dp)
    val GroupShape = RoundedCornerShape(16.dp)
    val ControlShape = RoundedCornerShape(14.dp)
    val DialogShape = RoundedCornerShape(24.dp)
}

private val LightExtraColors = VaultSpecExtraColors(
    fill = LightFill,
    fillOnSurface = LightFillOnSurface,
    separator = LightSeparator,
    tertiaryLabel = LightTertiaryLabel,
    codeText = LightLabel,
    switchOff = Color(0xFFE3E3E8),
    segmentThumb = Color.White,
    green = GreenLight,
    orange = OrangeLight,
    red = RedLight,
    indigo = IndigoLight,
    teal = TealLight,
    pink = PinkLight,
    gray = GrayLight,
)

private val DarkExtraColors = VaultSpecExtraColors(
    fill = DarkFill,
    fillOnSurface = DarkFillOnSurface,
    separator = DarkSeparator,
    tertiaryLabel = DarkTertiaryLabel,
    codeText = CodeAmberDark,
    switchOff = Color(0xFF48484A),
    segmentThumb = Color(0xFF636366),
    green = GreenDark,
    orange = OrangeDark,
    red = RedDark,
    indigo = IndigoDark,
    teal = TealDark,
    pink = PinkDark,
    gray = GrayDark,
)

private val PitchBlackExtraColors = DarkExtraColors.copy(
    fill = PitchBlackFill,
    fillOnSurface = PitchBlackFillOnSurface,
    separator = PitchBlackSeparator,
)

val LocalVaultSpecColors = staticCompositionLocalOf { LightExtraColors }

private val LightColorScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    primaryContainer = AccentLight.copy(alpha = 0.12f),
    onPrimaryContainer = AccentLight,
    secondary = AccentLight,
    onSecondary = Color.White,
    secondaryContainer = LightFill,
    onSecondaryContainer = LightLabel,
    surface = LightSurface,
    onSurface = LightLabel,
    surfaceVariant = LightFillOnSurface,
    onSurfaceVariant = LightSecondaryLabel,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,
    surfaceContainerHighest = LightFillOnSurface,
    background = LightBackground,
    onBackground = LightLabel,
    outline = LightSeparator,
    outlineVariant = LightSeparator,
    error = RedLight,
    onError = Color.White,
    scrim = Color.Black,
)

private fun darkScheme(
    background: Color,
    surface: Color,
    fillOnSurface: Color,
    separator: Color,
) = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    primaryContainer = AccentDark.copy(alpha = 0.18f),
    onPrimaryContainer = AccentDark,
    secondary = AccentDark,
    onSecondary = Color.White,
    secondaryContainer = fillOnSurface,
    onSecondaryContainer = DarkLabel,
    surface = surface,
    onSurface = DarkLabel,
    surfaceVariant = fillOnSurface,
    onSurfaceVariant = DarkSecondaryLabel,
    surfaceContainerLowest = surface,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = fillOnSurface,
    background = background,
    onBackground = DarkLabel,
    outline = separator,
    outlineVariant = separator,
    error = RedDark,
    onError = Color.White,
    scrim = Color.Black,
)

private val DarkColorScheme = darkScheme(
    background = DarkBackground,
    surface = DarkSurface,
    fillOnSurface = DarkFillOnSurface,
    separator = DarkSeparator,
)

private val PitchBlackColorScheme = darkScheme(
    background = PitchBlackBackground,
    surface = PitchBlackSurface,
    fillOnSurface = PitchBlackFillOnSurface,
    separator = PitchBlackSeparator,
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
