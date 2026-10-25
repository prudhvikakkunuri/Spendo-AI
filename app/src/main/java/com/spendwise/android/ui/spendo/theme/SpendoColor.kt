package com.spendwise.android.ui.spendo.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Three appearance options for Spendo AI.
 */
enum class ThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * Dynamic theme-aware color tokens for Nothing OS-inspired design system.
 * Minimalist, high contrast, monochrome-first with a restrained signature red accent.
 */
data class SpendoColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceDark: Color,
    val surfaceVariant: Color,
    val surfaceMuted: Color,
    val cardDark: Color,
    val cardDarkText: Color,
    val cardDarkTextSecondary: Color,
    val cardDarkBorder: Color,
    val darkCardContainer: Color,
    val darkTrack: Color,
    val dotPattern: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color,
    val textInverseMuted: Color,
    val accentRed: Color = Color(0xFFD71921),             // Signature Nothing signal red
    val accentRedForeground: Color,                     // Contrast-adapted for text/icons
    val accentRedSubtle: Color = Color(0x1AD71921),       // 10% red for alert badges
    val successGreen: Color,                            // Data-state green: accessible on surface
    val successGreenSubtle: Color,
    val warningAmber: Color = Color(0xFFFFB703),
    val divider: Color,
    val borderSubtle: Color,
    val borderDark: Color = Color(0xFF2C2C2C)
)

val LightSpendoColors = SpendoColors(
    isDark = false,
    background = Color(0xFFF2F2F2),        // Warm grey paper canvas
    surface = Color(0xFFFFFFFF),           // Crisp pure white card
    surfaceDark = Color(0xFF0E0E0E),       // Stark pitch-black for inverted hero cards
    surfaceVariant = Color(0xFFE5E7EB),    // Secondary surface for chips, pills
    surfaceMuted = Color(0xFFE0E0DC),      // Muted structural containers
    cardDark = Color(0xFF0E0E0E),          // Inverted card background
    cardDarkText = Color(0xFFFFFFFF),      // Crisp white text on dark card
    cardDarkTextSecondary = Color(0xFFBBBBBB), // Readable light grey on dark card
    cardDarkBorder = Color(0xFF222222),    // Dark hairline border
    darkCardContainer = Color(0xFF1A1A1A),
    darkTrack = Color(0xFFCCCCCC),
    dotPattern = Color(0xFFCCCCCC),
    textPrimary = Color(0xFF111111),       // Stark jet black
    textSecondary = Color(0xFF585A5A),     // Readable high-contrast slate grey (WCAG AA > 4.5:1)
    textMuted = Color(0xFF888888),         // Subtle tertiary text
    textInverse = Color(0xFFFFFFFF),       // White text on dark elements
    textInverseMuted = Color(0xFFB0B0B0),
    accentRed = Color(0xFFD71921),
    accentRedForeground = Color(0xFFC2141C), // Slightly darkened red for text readability on light
    accentRedSubtle = Color(0x1AD71921),
    successGreen = Color(0xFF2E7D32),      // High-contrast accessible green on white
    successGreenSubtle = Color(0x1A2E7D32),
    warningAmber = Color(0xFFB26A00),
    divider = Color(0xFFE5E7EB),           // 1px hairline divider
    borderSubtle = Color(0xFFD1D5DB)       // Crisp 1px border
)

val DarkSpendoColors = SpendoColors(
    isDark = true,
    background = Color(0xFF000000),        // Pitch black OLED background
    surface = Color(0xFF0E0E0E),           // Deep charcoal card surface
    surfaceDark = Color(0xFF141414),       // Elevated dark container
    surfaceVariant = Color(0xFF1A1A1A),    // Chips, inputs, button surfaces
    surfaceMuted = Color(0xFF222222),      // Subtle divider containers
    cardDark = Color(0xFF121212),          // Hero card background
    cardDarkText = Color(0xFFFFFFFF),      // Pure white heading on hero card
    cardDarkTextSecondary = Color(0xFF9E9E9E), // Readable slate grey
    cardDarkBorder = Color(0xFF262626),    // 1px dark border
    darkCardContainer = Color(0xFF161616),
    darkTrack = Color(0xFF2A2A2A),
    dotPattern = Color(0xFF333333),
    textPrimary = Color(0xFFEDEDED),       // High contrast white
    textSecondary = Color(0xFF8C8C8C),     // Slate grey (WCAG AA compliant)
    textMuted = Color(0xFF5A5A5A),         // Muted dark tertiary text
    textInverse = Color(0xFF000000),       // Dark text on inverted light buttons
    textInverseMuted = Color(0xFF444444),
    accentRed = Color(0xFFD71921),
    accentRedForeground = Color(0xFFFF4438), // Brightened red for dark mode contrast
    accentRedSubtle = Color(0x26D71921),
    successGreen = Color(0xFF7BE38A),      // Vibrant Nothing green
    successGreenSubtle = Color(0x267BE38A),
    warningAmber = Color(0xFFF2C94C),
    divider = Color(0xFF222222),           // Hairline dark separator
    borderSubtle = Color(0xFF262626)       // Ultra-clean 1px dark border
)

val LocalSpendoColors = staticCompositionLocalOf { LightSpendoColors }

/**
 * Backward-compatible object delegating to the current theme's colors.
 */
object SpendoColor {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.surface
    val SurfaceDark: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.surfaceDark
    val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.surfaceVariant
    val SurfaceMuted: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.surfaceMuted
    val CardDark: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.cardDark
    val CardDarkText: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.cardDarkText
    val CardDarkTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.cardDarkTextSecondary
    val CardDarkBorder: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.cardDarkBorder
    val DarkCardContainer: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.darkCardContainer
    val DarkTrack: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.darkTrack
    val DotPatternDark: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.dotPattern
    val DotPatternLight: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.dotPattern

    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.textSecondary
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.textMuted
    val TextInverse: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.textInverse
    val TextInverseMuted: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.textInverseMuted

    val AccentRed: Color = Color(0xFFD71921)
    val AccentRedForeground: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.accentRedForeground
    val AccentRedSubtle: Color = Color(0x1AD71921)
    val SuccessGreen: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.successGreen
    val SuccessGreenSubtle: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.successGreenSubtle
    val WarningAmber: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.warningAmber

    val Divider: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.divider
    val BorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.borderSubtle
    val BorderDark: Color @Composable @ReadOnlyComposable get() = LocalSpendoColors.current.borderDark
}
