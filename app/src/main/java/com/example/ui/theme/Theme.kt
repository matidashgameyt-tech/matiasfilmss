package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandAccent,
    onPrimary = BrandBg,
    secondary = BrandAccentDark,
    background = BrandBg,
    surface = BrandSurface,
    onBackground = BrandTextPrimary,
    onSurface = BrandTextPrimary,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandTextSecondary,
    outline = BrandBorder
)

// In mobile app context, we force a stunning dark cinematic theme just like the video production website!
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for cinematic video management branding!
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BrandBg.toArgb()
            window.navigationBarColor = BrandBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
