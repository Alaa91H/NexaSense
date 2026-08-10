package com.nexasense.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Centralized Material 3 color schemes.
 *
 * Used when dynamic color (Material You) is not available on the device. The
 * palettes follow the Material 3 tonal system so surfaces, cards, dialogs and
 * text keep correct contrast in both light and dark themes. Functional colors
 * (interference, alignment, accuracy) come from the scheme's error/success
 * roles and are never the only signal — see the accessibility notes in the
 * README.
 *
 * The dark scheme uses a deep-navy "night sky" palette so the app reads as a
 * blue-gradient night instrument, matching [NexaSenseGradients.NightSky].
 */
object NexaSenseColors {

    // Light scheme (fallback, non-dynamic).
    val Light = lightColorScheme(
        primary = Color(0xFF00639B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCEE5FF),
        onPrimaryContainer = Color(0xFF001D33),
        secondary = Color(0xFF526070),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD6E4F7),
        onSecondaryContainer = Color(0xFF0F1D2A),
        tertiary = Color(0xFF69587A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF0DBFF),
        onTertiaryContainer = Color(0xFF231533),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFCFCFF),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFCFCFF),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFDEE3EB),
        onSurfaceVariant = Color(0xFF42474E),
        outline = Color(0xFF72777F),
        outlineVariant = Color(0xFFC2C7CF),
    )

    // Dark scheme (fallback, non-dynamic) — deep-navy "night sky" tones.
    val Dark = darkColorScheme(
        primary = Color(0xFF8FC7FF),
        onPrimary = Color(0xFF003355),
        primaryContainer = Color(0xFF124872),
        onPrimaryContainer = Color(0xFFCFE7FF),
        secondary = Color(0xFFB7C8DA),
        onSecondary = Color(0xFF1F2F40),
        secondaryContainer = Color(0xFF24354E),
        onSecondaryContainer = Color(0xFFD8E6FA),
        tertiary = Color(0xFFD4BFE6),
        onTertiary = Color(0xFF392A49),
        tertiaryContainer = Color(0xFF504060),
        onTertiaryContainer = Color(0xFFF0DBFF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0A1220),
        onBackground = Color(0xFFDCE4F0),
        surface = Color(0xFF0D1626),
        onSurface = Color(0xFFDCE4F0),
        surfaceVariant = Color(0xFF1C2740),
        onSurfaceVariant = Color(0xFFB0BDD2),
        outline = Color(0xFF7F8CA3),
        outlineVariant = Color(0xFF2A3852),
    )

    /** Functional accent used for the Qibla marker on the compass dial. */
    val QiblaMarker = Color(0xFF2E7D32)
}

/**
 * Background gradients for the app.
 *
 * The root composable paints these behind every screen, so the whole app gets
 * a calm sky gradient — deep navy at night, a pale blue-washed sky by day.
 */
object NexaSenseGradients {

    /** Deep-navy night sky used when the app resolves to dark mode. */
    val NightSky = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF070E20),
            Color(0xFF0C1B38),
            Color(0xFF123052),
        ),
    )

    /** Soft pale-blue daylight used when the app resolves to light mode. */
    val DaySky = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FBFF),
            Color(0xFFE7F1FC),
        ),
    )
}
