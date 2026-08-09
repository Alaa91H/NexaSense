package com.nexasense.presentation.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.LevelEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LevelViewModel(
    private val levelEngine: LevelEngine,
    private val calibrationStore: CalibrationStore,
) : ViewModel() {

    val orientation = levelEngine.orientation
    val isAvailable = levelEngine.isAvailable

    val calibration: StateFlow<LevelCalibration> = calibrationStore.levelCalibration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LevelCalibration.NONE,
    )

    fun setActive(active: Boolean) = levelEngine.setActive(active)

    fun setDisplayRotation(rotationDegrees: Int) =
        levelEngine.setDisplayRotation(rotationDegrees)

    /** Captures the current reading as the new zero point. */
    fun setZero() {
        val current = orientation.value
        viewModelScope.launch {
            calibrationStore.saveLevel(
                LevelCalibration(
                    pitchOffsetDegrees = current.pitch,
                    rollOffsetDegrees = current.roll,
                    isSet = true,
                ),
            )
        }
    }

    fun resetCalibration() {
        viewModelScope.launch { calibrationStore.resetLevel() }
    }
}
