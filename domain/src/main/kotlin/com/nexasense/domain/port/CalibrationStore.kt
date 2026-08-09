package com.nexasense.domain.port

import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.model.MagnetometerCalibration
import kotlinx.coroutines.flow.Flow

/** Persistence for calibration data (implemented with DataStore). */
interface CalibrationStore {
    val magnetometerCalibration: Flow<MagnetometerCalibration>

    val levelCalibration: Flow<LevelCalibration>

    suspend fun saveMagnetometer(calibration: MagnetometerCalibration)

    suspend fun resetMagnetometer()

    suspend fun saveLevel(calibration: LevelCalibration)

    suspend fun resetLevel()
}
