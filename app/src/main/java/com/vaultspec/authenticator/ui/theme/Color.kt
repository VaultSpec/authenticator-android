package com.vaultspec.authenticator.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary accent — soft desaturated blue with slight cyan influence ──
val Blue500 = Color(0xFF5B7FFF) // primary — calm, muted, not neon
val Blue600 = Color(0xFF4968D9) // pressed / gradient bottom
val Blue400 = Color(0xFF7B9AFF) // lighter accent for dark theme
val Blue300 = Color(0xFF9FB6FF) // subtle highlights
val Blue100 = Color(0xFFDAE3FF) // containers (light)
val Blue50 = Color(0xFFF0F3FF)  // tinted backgrounds

// Action button gradient (very subtle depth)
val UnlockGradientTopLight = Color(0xFF5B7FFF)
val UnlockGradientBottomLight = Color(0xFF4968D9)
val UnlockGradientTopDark = Color(0xFF7B9AFF)
val UnlockGradientBottomDark = Color(0xFF5B7FFF)

// Urgency colors for countdown
val UrgencyNormal = Blue500
val UrgencyWarning = Color(0xFFCC6D00) // muted amber
val UrgencyDanger = Color(0xFFC93131) // muted red — not neon, not pink

// ── Light palette ──
val White = Color(0xFFFFFFFF)
val GrayBackground = Color(0xFFF7F7F8) // near-white, no blue tint
val GraySurface = Color(0xFFEFEFF1)    // cards / elevated
val GrayLight = Color(0xFFE2E2E5)      // borders, dividers

val TextPrimary = Color(0xFF111118)     // near-black
val TextSecondary = Color(0xFF6E6E7A)   // muted body
val TextOnBlue = Color(0xFFFFFFFF)

val CardBlue = Color(0xFF5B7FFF)
val CardWhite = Color(0xFFFFFFFF)       // card surface
val CardBorder = Color(0xFFE2E2E5)      // matches GrayLight

val RingBackground = Color(0xFFE2E2E5)
val RingProgress = Color(0xFF5B7FFF)

val CategorySelected = Color(0xFF111118) // near-black pill
val CategoryUnselected = Color(0xFFEFEFF1)

// ── Dark palette ──
val DarkBackground = Color(0xFF0A0A0A)  // true dark
val DarkSurface = Color(0xFF121212)     // card level 1
val DarkSurfaceVariant = Color(0xFF1A1A1A) // card level 2 / inputs
val DarkGrayLight = Color(0xFF2A2A2A)   // borders, dividers

val DarkTextPrimary = Color(0xFFEAEAEA) // high contrast
val DarkTextSecondary = Color(0xFF8E8E96)

val DarkCardWhite = Color(0xFF141414)   // card surface
val DarkCardBorder = Color(0xFF232323)  // subtle

val DarkRingBackground = Color(0xFF2A2A2A)

val DarkCategorySelected = Color(0xFF7B9AFF) // matches Blue400
val DarkCategoryUnselected = Color(0xFF1A1A1A)

// ── Pitch Black (AMOLED) palette ──
val PitchBlackBackground = Color(0xFF000000)
val PitchBlackSurface = Color(0xFF0A0A0A)
val PitchBlackSurfaceVariant = Color(0xFF111111)
val PitchBlackGrayLight = Color(0xFF1E1E1E)

val PitchBlackCardWhite = Color(0xFF0C0C0C)
val PitchBlackCardBorder = Color(0xFF1A1A1A)

val PitchBlackRingBackground = Color(0xFF1E1E1E)

val PitchBlackCategoryUnselected = Color(0xFF111111)
