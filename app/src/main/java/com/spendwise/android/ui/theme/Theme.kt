package com.spendwise.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// SpendWise Dark Scheme: Deep charcoal surfaces with restrained lime-green accent
private val DarkColorScheme = darkColorScheme(
    primary = LimeAccent,
    onPrimary = CharcoalDark,
    primaryContainer = LimeAccentVariant,
    onPrimaryContainer = LimeAccent,
    secondary = SlateTextPrimary,
    onSecondary = CharcoalDark,
    secondaryContainer = CharcoalSurfaceVariant,
    onSecondaryContainer = SlateTextPrimary,
    tertiary = CreditGreen,
    onTertiary = Color.White,
    background = CharcoalDark,
    onBackground = SlateTextPrimary,
    surface = CharcoalSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = CharcoalBorder
)

// SpendWise Light Scheme: Crisp high-contrast surfaces
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = LightPrimary,
    secondary = LightTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = CreditGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve SpendWise brand charcoal/lime identity by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
