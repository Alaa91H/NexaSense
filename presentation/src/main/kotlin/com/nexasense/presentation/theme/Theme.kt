package com.nexasense.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive theme (Google 2026 design language).
 *
 * - Uses dynamic color (Material You) on Android 12+ when the launcher/app
 *   theme allows it; falls back to the centralized [NexaSenseColors] schemes.
 * - Runs on Compose BOM 2026.06.01 (material3 1.4.0), the first stable
 *   release of Material 3 Expressive: components render with the rounder,
 *   softer expressive defaults out of the box, matching Google's 2026 apps.
 * - The expressive shape scale ([NexaSenseShapes]) is applied centrally so
 *   cards, dialogs, settings rows and the compass/level surfaces all share
 *   the same large, soft corner radii.
 *   (The explicit `MaterialExpressiveTheme`/`MotionScheme` APIs are internal
 *   in material3 1.4.0 and only become public in 1.5.0-alpha; the stable
 *   path is `MaterialTheme` + the expressive shape/typography tokens, which
 *   is what this theme does.)
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
