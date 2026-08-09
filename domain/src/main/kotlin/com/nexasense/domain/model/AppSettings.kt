package com.nexasense.domain.model

/** User-configurable application settings, persisted via DataStore. */
data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val language: LanguagePreference = LanguagePreference.SYSTEM,
    val northReference: NorthReference = NorthReference.AUTOMATIC,
    val qiblaEnabled: Boolean = false,
    val showQiblaOnCompass: Boolean = true,
    val showQiblaCard: Boolean = true,
    val showQiblaDistance: Boolean = false,
    val qiblaHapticFeedback: Boolean = false,
    val smoothing: SmoothingPreference = SmoothingPreference.MEDIUM,
    val sensorRate: SensorRatePreference = SensorRatePreference.GAME,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val developerMode: Boolean = false,
) {
    companion object {
        val DEFAULT: AppSettings = AppSettings()
    }
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Supported UI languages. [localeTag] is the BCP-47 tag passed to the
 * AndroidX per-app-locale API; SYSTEM uses the device language.
 */
enum class LanguagePreference(val localeTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    ITALIAN("it"),
    TURKISH("tr"),
    RUSSIAN("ru"),
    UKRAINIAN("uk"),
    POLISH("pl"),
    DUTCH("nl"),
    INDONESIAN("in"),
    MALAY("ms"),
    HINDI("hi"),
    BENGALI("bn"),
    URDU("ur"),
    PERSIAN("fa"),
    CHINESE_SIMPLIFIED("zh"),
    CHINESE_TRADITIONAL("zh-TW"),
    JAPANESE("ja"),
    KOREAN("ko"),
    VIETNAMESE("vi"),
    THAI("th"),
}

/** Heading smoothing strength. */
enum class SmoothingPreference(val alpha: Float) {
    NONE(1f),
    LIGHT(0.45f),
    MEDIUM(0.25f),
    STRONG(0.12f),
}

/** Requested sensor update rate; the actual rate is always measured. */
enum class SensorRatePreference(val delayMicros: Long, val labelKey: String) {
    NORMAL(200_000L, "settings_rate_normal"),
    UI(60_000L, "settings_rate_ui"),
    GAME(20_000L, "settings_rate_game"),
}
