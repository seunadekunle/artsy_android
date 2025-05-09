package com.example.asssignment_4.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


// Define light colors
private val LightColors = lightColorScheme(
    primary = lightArtsyBlue,
    secondary = lightArtsyDarkBlue,
    background = Color.White,
    surface = lightArtsyGray,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF000000),
)

// Define dark colors
private val DarkColors = darkColorScheme(
    primary = artsyDarkBlue,
    secondary = artsyBlue,
    background = Color.Black,
    surface = Color(0xFF0D0D10),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFE3E3E3),
)

@Composable
fun YourAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalTonalElevationEnabled provides false) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }

}
