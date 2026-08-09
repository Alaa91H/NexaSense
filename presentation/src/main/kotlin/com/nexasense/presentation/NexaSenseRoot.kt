package com.nexasense.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.LanguagePreference
import com.nexasense.domain.model.ThemePreference
import com.nexasense.presentation.navigation.NexaNavHost
import com.nexasense.presentation.theme.NexaSenseTheme

/**
 * Application root: applies the theme, propagates the language preference and
 * hosts the navigation graph.
 *
 * @param applyLanguage called whenever the user changes the language so the
 *   activity can recreate with the new locale.
 */
@Composable
fun NexaSenseRoot(
    container: AppContainer,
    applyLanguage: (LanguagePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = AppSettings.DEFAULT)

    LaunchedEffect(settings.language) {
        applyLanguage(settings.language)
    }

    // Keep-screen-on uses the window flag directly (no permission needed).
    val view = LocalView.current
    LaunchedEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
    }

    val systemDark = isSystemInDarkTheme()
    val darkTheme = remember(settings.theme, systemDark) {
        when (settings.theme) {
            ThemePreference.SYSTEM -> systemDark
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }
    }

    NexaSenseTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier) {
            CompositionLocalProvider(
                LocalAppContainer provides container,
            ) {
                NexaNavHost(container)
            }
        }
    }
}

private val LocalAppContainer = androidx.compose.runtime.staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

/** Access to the application container from any screen. */
@Composable
fun rememberAppContainer(): AppContainer = LocalAppContainer.current
