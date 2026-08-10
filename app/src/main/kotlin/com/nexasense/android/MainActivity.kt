package com.nexasense.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.model.LanguagePreference
import com.nexasense.presentation.NexaSenseRoot

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as NexaSenseApplication).container
        setContent {
            NexaSenseRoot(
                container = container,
                applyLanguage = ::applyLanguage,
            )
        }
    }

    /**
     * Applies the user's language preference through the AndroidX per-app
     * locale mechanism (which recreates the activity with the new locale).
     * Only called when the selection actually differs from the current one.
     * SYSTEM uses the device language via an empty locale list.
     */
    private fun applyLanguage(language: LanguagePreference) {
        val requested = language.localeTag?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        val current = AppCompatDelegate.getApplicationLocales()
        if (!current.equals(requested)) {
            // Per-app locales can fail on some OEM builds; a failure here must
            // never prevent the app from launching (the device locale is kept).
            runCatching { AppCompatDelegate.setApplicationLocales(requested) }
                .onFailure { NexaLogger.w("setApplicationLocales failed: ${it.message}") }
        }
    }
}
