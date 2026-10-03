package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VfxDarkColorScheme = darkColorScheme(
    primary = VfxAmber,
    onPrimary = VfxBlack,
    primaryContainer = VfxAmberDim,
    onPrimaryContainer = VfxTextPrimary,
    secondary = VfxTextSecondary,
    onSecondary = VfxBlack,
    secondaryContainer = VfxSurfaceVariant,
    onSecondaryContainer = VfxTextPrimary,
    tertiary = VfxCyan,
    onTertiary = VfxBlack,
    background = VfxBlack,
    onBackground = VfxTextPrimary,
    surface = VfxSurface,
    onSurface = VfxTextPrimary,
    surfaceVariant = VfxSurfaceVariant,
    onSurfaceVariant = VfxTextSecondary,
    outline = VfxBorder,
    outlineVariant = VfxBorderSubtle,
    error = VfxRed,
    onError = VfxTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Preserve cinema dark styling
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VfxBlack.toArgb()
                window.navigationBarColor = VfxBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = VfxDarkColorScheme,
        typography = Typography,
        content = content
    )
}
