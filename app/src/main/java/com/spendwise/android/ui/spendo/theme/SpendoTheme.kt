package com.spendwise.android.ui.spendo.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SpendoLightColorScheme = lightColorScheme(
    primary = LightSpendoColors.textPrimary,
    onPrimary = LightSpendoColors.surface,
    primaryContainer = LightSpendoColors.surfaceDark,
    onPrimaryContainer = LightSpendoColors.textInverse,
    secondary = LightSpendoColors.textSecondary,
    onSecondary = LightSpendoColors.textPrimary,
    background = LightSpendoColors.background,
    onBackground = LightSpendoColors.textPrimary,
    surface = LightSpendoColors.surface,
    onSurface = LightSpendoColors.textPrimary,
    surfaceVariant = LightSpendoColors.surfaceVariant,
    onSurfaceVariant = LightSpendoColors.textSecondary,
    outline = LightSpendoColors.divider,
    error = LightSpendoColors.accentRed,
    onError = LightSpendoColors.surface
)

private val SpendoDarkColorScheme = darkColorScheme(
    primary = DarkSpendoColors.textPrimary,
    onPrimary = DarkSpendoColors.surface,
    primaryContainer = DarkSpendoColors.surfaceDark,
    onPrimaryContainer = DarkSpendoColors.textPrimary,
    secondary = DarkSpendoColors.textSecondary,
    onSecondary = DarkSpendoColors.textPrimary,
    background = DarkSpendoColors.background,
    onBackground = DarkSpendoColors.textPrimary,
    surface = DarkSpendoColors.surface,
    onSurface = DarkSpendoColors.textPrimary,
    surfaceVariant = DarkSpendoColors.surfaceVariant,
    onSurfaceVariant = DarkSpendoColors.textSecondary,
    outline = DarkSpendoColors.divider,
    error = DarkSpendoColors.accentRed,
    onError = DarkSpendoColors.surface
)

val SpendoShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val SpendoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    )
)

object SpendoTheme {
    val colors: SpendoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSpendoColors.current
}

@Composable
fun SpendoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val spendoColors = if (darkTheme) DarkSpendoColors else LightSpendoColors
    val colorScheme = if (darkTheme) SpendoDarkColorScheme else SpendoLightColorScheme

    CompositionLocalProvider(LocalSpendoColors provides spendoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpendoTypography,
            shapes = SpendoShapes,
            content = content
        )
    }
}
