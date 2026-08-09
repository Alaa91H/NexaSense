package com.nexasense.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.model.MagnetometerCalibration
import com.nexasense.domain.port.CalibrationStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.calibrationDataStore by preferencesDataStore(name = "nexasense_calibration")

/** Calibration data persisted with DataStore Preferences. */
class DataStoreCalibrationStore(private val context: Context) : CalibrationStore {

    private object Keys {
        val MAG_OFFSET_X = floatPreferencesKey("mag_offset_x")
        val MAG_OFFSET_Y = floatPreferencesKey("mag_offset_y")
        val MAG_OFFSET_Z = floatPreferencesKey("mag_offset_z")
        val MAG_SCALE_X = floatPreferencesKey("mag_scale_x")
        val MAG_SCALE_Y = floatPreferencesKey("mag_scale_y")
        val MAG_SCALE_Z = floatPreferencesKey("mag_scale_z")
        val MAG_SAMPLES = intPreferencesKey("mag_samples")
        val MAG_COVERAGE = floatPreferencesKey("mag_coverage")
        val MAG_CALIBRATED = booleanPreferencesKey("mag_calibrated")

        val LEVEL_PITCH = floatPreferencesKey("level_pitch_offset")
        val LEVEL_ROLL = floatPreferencesKey("level_roll_offset")
        val LEVEL_SET = booleanPreferencesKey("level_set")
    }

    override val magnetometerCalibration: Flow<MagnetometerCalibration> =
        context.calibrationDataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { it.toMagnetometerCalibration() }

    override val levelCalibration: Flow<LevelCalibration> =
        context.calibrationDataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { it.toLevelCalibration() }

    override suspend fun saveMagnetometer(calibration: MagnetometerCalibration) {
        context.calibrationDataStore.edit { prefs ->
            prefs[Keys.MAG_OFFSET_X] = calibration.offsetX
            prefs[Keys.MAG_OFFSET_Y] = calibration.offsetY
            prefs[Keys.MAG_OFFSET_Z] = calibration.offsetZ
            prefs[Keys.MAG_SCALE_X] = calibration.scaleX
            prefs[Keys.MAG_SCALE_Y] = calibration.scaleY
            prefs[Keys.MAG_SCALE_Z] = calibration.scaleZ
            prefs[Keys.MAG_SAMPLES] = calibration.sampleCount
            prefs[Keys.MAG_COVERAGE] = calibration.coverage
            prefs[Keys.MAG_CALIBRATED] = calibration.isCalibrated
        }
    }

    override suspend fun resetMagnetometer() {
        context.calibrationDataStore.edit { prefs ->
            listOf(
                Keys.MAG_OFFSET_X, Keys.MAG_OFFSET_Y, Keys.MAG_OFFSET_Z,
                Keys.MAG_SCALE_X, Keys.MAG_SCALE_Y, Keys.MAG_SCALE_Z,
                Keys.MAG_SAMPLES, Keys.MAG_COVERAGE, Keys.MAG_CALIBRATED,
            ).forEach { prefs.remove(it) }
        }
    }

    override suspend fun saveLevel(calibration: LevelCalibration) {
        context.calibrationDataStore.edit { prefs ->
            prefs[Keys.LEVEL_PITCH] = calibration.pitchOffsetDegrees
            prefs[Keys.LEVEL_ROLL] = calibration.rollOffsetDegrees
            prefs[Keys.LEVEL_SET] = calibration.isSet
        }
    }

    override suspend fun resetLevel() {
        context.calibrationDataStore.edit { prefs ->
            listOf(Keys.LEVEL_PITCH, Keys.LEVEL_ROLL, Keys.LEVEL_SET).forEach { prefs.remove(it) }
        }
    }

    private fun Preferences.toMagnetometerCalibration(): MagnetometerCalibration =
        MagnetometerCalibration(
            offsetX = this[Keys.MAG_OFFSET_X] ?: 0f,
            offsetY = this[Keys.MAG_OFFSET_Y] ?: 0f,
            offsetZ = this[Keys.MAG_OFFSET_Z] ?: 0f,
            scaleX = this[Keys.MAG_SCALE_X] ?: 1f,
            scaleY = this[Keys.MAG_SCALE_Y] ?: 1f,
            scaleZ = this[Keys.MAG_SCALE_Z] ?: 1f,
            sampleCount = this[Keys.MAG_SAMPLES] ?: 0,
            coverage = this[Keys.MAG_COVERAGE] ?: 0f,
            isCalibrated = this[Keys.MAG_CALIBRATED] ?: false,
        )

    private fun Preferences.toLevelCalibration(): LevelCalibration = LevelCalibration(
        pitchOffsetDegrees = this[Keys.LEVEL_PITCH] ?: 0f,
        rollOffsetDegrees = this[Keys.LEVEL_ROLL] ?: 0f,
        isSet = this[Keys.LEVEL_SET] ?: false,
    )
}
