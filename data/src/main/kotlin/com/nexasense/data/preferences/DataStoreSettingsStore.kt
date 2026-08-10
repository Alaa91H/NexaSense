package com.nexasense.data.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.CompassStyle
import com.nexasense.domain.model.LanguagePreference
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.SensorRatePreference
import com.nexasense.domain.model.SmoothingPreference
import com.nexasense.domain.model.ThemePreference
import com.nexasense.domain.port.SettingsStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(
    name = "nexasense_settings",
    // If the store file is ever corrupt (crash mid-write, storage error),
    // replace it with an empty store instead of throwing on every read/write.
    corruptionHandler = ReplaceFileCorruptionHandler { error ->
        NexaLogger.e("Settings DataStore corrupt; resetting to defaults.", error)
        emptyPreferences()
    },
)

/**
 * Settings persisted with DataStore Preferences (not SharedPreferences).
 *
 * The previous `compass_mode` key (MAGNETIC/TRUE) is migrated to
 * `north_reference` on read; a missing value defaults to AUTOMATIC.
 */
class DataStoreSettingsStore(private val context: Context) : SettingsStore {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val NORTH_REFERENCE = stringPreferencesKey("north_reference")

        /** Legacy key from the pre-Qibla build; read for migration only. */
        val LEGACY_COMPASS_MODE = stringPreferencesKey("compass_mode")

        val QIBLA_ENABLED = booleanPreferencesKey("qibla_enabled")
        val SHOW_QIBLA_ON_COMPASS = booleanPreferencesKey("show_qibla_on_compass")
        val SHOW_QIBLA_CARD = booleanPreferencesKey("show_qibla_card")
        val SHOW_QIBLA_DISTANCE = booleanPreferencesKey("show_qibla_distance")
        val QIBLA_HAPTIC_FEEDBACK = booleanPreferencesKey("qibla_haptic_feedback")

        val SMOOTHING = stringPreferencesKey("smoothing")
        val SENSOR_RATE = stringPreferencesKey("sensor_rate")
        val HAPTICS = booleanPreferencesKey("haptics")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")

        // Compass appearance.
        val COMPASS_STYLE = stringPreferencesKey("compass_style")
        val SHOW_CARDINAL_LABELS = booleanPreferencesKey("show_cardinal_labels")
        val SHOW_DEGREE_TICKS = booleanPreferencesKey("show_degree_ticks")
        val SHOW_DEGREE_NUMBERS = booleanPreferencesKey("show_degree_numbers")
        val SHOW_HEADING_READOUT = booleanPreferencesKey("show_heading_readout")
        val SHOW_NORTH_REFERENCE_BADGE = booleanPreferencesKey("show_north_reference_badge")
        val SHOW_COMPASS_DETAILS = booleanPreferencesKey("show_compass_details")
        val SHOW_ACCURACY_PANEL = booleanPreferencesKey("show_accuracy_panel")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.THEME] = next.theme.name
            prefs[Keys.LANGUAGE] = next.language.name
            prefs[Keys.NORTH_REFERENCE] = next.northReference.name
            prefs[Keys.QIBLA_ENABLED] = next.qiblaEnabled
            prefs[Keys.SHOW_QIBLA_ON_COMPASS] = next.showQiblaOnCompass
            prefs[Keys.SHOW_QIBLA_CARD] = next.showQiblaCard
            prefs[Keys.SHOW_QIBLA_DISTANCE] = next.showQiblaDistance
            prefs[Keys.QIBLA_HAPTIC_FEEDBACK] = next.qiblaHapticFeedback
            prefs[Keys.SMOOTHING] = next.smoothing.name
            prefs[Keys.SENSOR_RATE] = next.sensorRate.name
            prefs[Keys.HAPTICS] = next.hapticsEnabled
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[Keys.DEVELOPER_MODE] = next.developerMode
            prefs[Keys.COMPASS_STYLE] = next.compassStyle.name
            prefs[Keys.SHOW_CARDINAL_LABELS] = next.showCardinalLabels
            prefs[Keys.SHOW_DEGREE_TICKS] = next.showDegreeTicks
            prefs[Keys.SHOW_DEGREE_NUMBERS] = next.showDegreeNumbers
            prefs[Keys.SHOW_HEADING_READOUT] = next.showHeadingReadout
            prefs[Keys.SHOW_NORTH_REFERENCE_BADGE] = next.showNorthReferenceBadge
            prefs[Keys.SHOW_COMPASS_DETAILS] = next.showCompassDetails
            prefs[Keys.SHOW_ACCURACY_PANEL] = next.showAccuracyPanel
        }
    }

    override suspend fun reset() {
        context.settingsDataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        theme = enumValueOr<ThemePreference>(this[Keys.THEME], ThemePreference.SYSTEM),
        language = enumValueOr<LanguagePreference>(this[Keys.LANGUAGE], LanguagePreference.SYSTEM),
        northReference = northReferenceOrLegacy(),
        qiblaEnabled = this[Keys.QIBLA_ENABLED] ?: false,
        showQiblaOnCompass = this[Keys.SHOW_QIBLA_ON_COMPASS] ?: true,
        showQiblaCard = this[Keys.SHOW_QIBLA_CARD] ?: true,
        showQiblaDistance = this[Keys.SHOW_QIBLA_DISTANCE] ?: false,
        qiblaHapticFeedback = this[Keys.QIBLA_HAPTIC_FEEDBACK] ?: false,
        smoothing = enumValueOr<SmoothingPreference>(this[Keys.SMOOTHING], SmoothingPreference.MEDIUM),
        sensorRate = enumValueOr<SensorRatePreference>(this[Keys.SENSOR_RATE], SensorRatePreference.GAME),
        hapticsEnabled = this[Keys.HAPTICS] ?: true,
        keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: false,
        developerMode = this[Keys.DEVELOPER_MODE] ?: false,
        compassStyle = enumValueOr<CompassStyle>(this[Keys.COMPASS_STYLE], CompassStyle.CLASSIC),
        showCardinalLabels = this[Keys.SHOW_CARDINAL_LABELS] ?: true,
        showDegreeTicks = this[Keys.SHOW_DEGREE_TICKS] ?: true,
        showDegreeNumbers = this[Keys.SHOW_DEGREE_NUMBERS] ?: false,
        showHeadingReadout = this[Keys.SHOW_HEADING_READOUT] ?: true,
        showNorthReferenceBadge = this[Keys.SHOW_NORTH_REFERENCE_BADGE] ?: true,
        showCompassDetails = this[Keys.SHOW_COMPASS_DETAILS] ?: true,
        showAccuracyPanel = this[Keys.SHOW_ACCURACY_PANEL] ?: true,
    )

    private fun Preferences.northReferenceOrLegacy(): NorthReference {
        this[Keys.NORTH_REFERENCE]?.let { name ->
            enumValueOr<NorthReference>(name, NorthReference.AUTOMATIC)?.let { return it }
        }
        // Migration from the pre-Qibla compass_mode key.
        return when (this[Keys.LEGACY_COMPASS_MODE]) {
            "MAGNETIC" -> NorthReference.MAGNETIC_NORTH
            "TRUE" -> NorthReference.TRUE_NORTH
            else -> NorthReference.AUTOMATIC
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOr(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
