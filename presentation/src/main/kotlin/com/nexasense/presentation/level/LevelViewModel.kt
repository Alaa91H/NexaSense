package com.nexasense.presentation.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.model.OrientationAngles
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.LevelEngine
import com.nexasense.domain.port.SettingsStore
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LevelViewModel(
    private val levelEngine: LevelEngine,
    private val calibrationStore: CalibrationStore,
    settingsStore: SettingsStore,
) : ViewModel() {

    val orientation = levelEngine.orientation
    val isAvailable = levelEngine.isAvailable

    val calibration: StateFlow<LevelCalibration> = calibrationStore.levelCalibration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LevelCalibration.NONE,
    )

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings.DEFAULT,
    )

    /**
     * Increments when the bubble enters the centered zone, so the UI can
     * fire one short haptic pulse — the user can level a surface without
     * watching the screen (standard bubble-level UX).
     */
    private val _hapticTick = MutableStateFlow(0)
    val hapticTick: StateFlow<Int> = _hapticTick.asStateFlow()

    private var lastCentered = false
    private var lastHapticAtMillis = 0L

    init {
        viewModelScope.launch {
            combine(orientation, settings) { current, currentSettings ->
                current to currentSettings
            }.collect { (current, currentSettings) ->
                maybeFireHaptic(current, currentSettings)
            }
        }
    }

    fun setActive(active: Boolean) = levelEngine.setActive(active)

    fun setDisplayRotation(rotationDegrees: Int) =
        levelEngine.setDisplayRotation(rotationDegrees)

    private fun maybeFireHaptic(orientation: OrientationAngles, currentSettings: AppSettings) {
        val centered = abs(orientation.pitch) < CENTERED_THRESHOLD_DEGREES &&
            abs(orientation.roll) < CENTERED_THRESHOLD_DEGREES
        val entering = centered && !lastCentered
        lastCentered = centered
        if (!entering) return
        if (!currentSettings.hapticsEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastHapticAtMillis < HAPTIC_COOLDOWN_MILLIS) return
        lastHapticAtMillis = now
        _hapticTick.value += 1
    }

    private companion object {
        /** Matches the bubble's visual "level" zone on the canvas. */
        const val CENTERED_THRESHOLD_DEGREES = 1.5f
        const val HAPTIC_COOLDOWN_MILLIS = 2_000L
    }

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
