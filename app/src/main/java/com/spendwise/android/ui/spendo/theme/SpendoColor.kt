package com.spendwise.android.ui.spendo.theme

import androidx.compose.ui.graphics.Color

/**
 * Spendo Design System Palette
 * Inspired by Nothing OS: Monochrome-first, generous whitespace, thin 1px borders, restrained red accent.
 */
object SpendoColor {
    // Primary Surfaces
    val Background = Color(0xFFF5F5F2)       // Light warm gray / off-white
    val Surface = Color(0xFFFFFFFF)          // Crisp white card surface
    val SurfaceDark = Color(0xFF141414)      // Deep black-gray for inverted hero sections
    val SurfaceVariant = Color(0xFFEBEBE7)   // Secondary surface for chips, pills, inputs
    val SurfaceMuted = Color(0xFFE5E5E0)     // Tertiary subtle container surface

    // Dark Card Elements
    val DarkCardContainer = Color(0xFF1E1E1E) // Inner dark card container
    val DarkTrack = Color(0xFF2A2A2A)        // Progress bar background in dark cards
    val DotPatternDark = Color(0xFF383838)   // Decorative dot matrix in dark cards
    val DotPatternLight = Color(0xFFD5D5CF)  // Decorative dot matrix on light cards

    // Typography
    val TextPrimary = Color(0xFF111111)      // Jet black for high-contrast readability
    val TextSecondary = Color(0xFF707070)    // Muted slate gray for secondary labels
    val TextMuted = Color(0xFFA0A0A0)        // Subtle tertiary text
    val TextInverse = Color(0xFFF5F5F2)      // Light text on dark surfaces
    val TextInverseMuted = Color(0xFF888888) // Secondary text on dark surfaces

    // Accents & Signals
    val AccentRed = Color(0xFFD71921)        // Signature Nothing red indicator
    val AccentRedSubtle = Color(0x1AD71921)  // 10% alpha red for badges & highlights
    val SuccessGreen = Color(0xFFB7F34A)     // Chartreuse neon green for streak/income
    val SuccessGreenSubtle = Color(0x1AB7F34A)
    val WarningAmber = Color(0xFFFFB703)     // Soft amber for budget near-limit warning

    // Structural Elements
    val Divider = Color(0xFFE5E5E1)          // Thin 1px structural separator
    val BorderSubtle = Color(0xFFDFDFDA)     // Ultra-crisp card border
    val BorderDark = Color(0xFF2C2C2C)       // Dark card outline
}
