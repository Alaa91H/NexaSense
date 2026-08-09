package com.nexasense.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 theme.
 *
 * - Uses dynamic color (Material You) on Android 12+ when the launcher/app
 *   theme allows it; falls back to the centralized [NexaSenseColors] schemes.
 * - Shapes and typography come from [NexaSenseShapes] and
 *   [NexaSenseTypography] so every screen shares one design system.
 *
 * @param darkTheme resolved from the app setting (System/Light/Dark) by the
 *   root composable.
 */
@Composable
fun NexaSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NexaSenseColors.Dark
        else -> NexaSenseColors.Light
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexaSenseTypography,
        shapes = NexaSenseShapes,
        content = content,
    )
}
