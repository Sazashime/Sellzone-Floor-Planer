package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WorkshopColorScheme = darkColorScheme(
    primary = EmeraldGreenAccent,
    onPrimary = OnEmeraldGreen,
    primaryContainer = EmeraldGreenContainer,
    onPrimaryContainer = EmeraldGreenAccent,
    secondary = MetallicTitanium,
    onSecondary = GraphiteBackground,
    secondaryContainer = GraphiteSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = WorkshopAmber,
    onTertiary = GraphiteBackground,
    background = GraphiteBackground,
    onBackground = TextPrimary,
    surface = GraphiteSurface,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GraphiteBorder,
    error = WarningRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = WorkshopColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = GraphiteBackground.toArgb()
                window.navigationBarColor = GraphiteBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
