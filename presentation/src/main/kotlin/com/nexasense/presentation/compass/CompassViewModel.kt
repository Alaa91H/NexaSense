package com.nexasense.presentation.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.QiblaAlignment
import com.nexasense.domain.model.QiblaState
import com.nexasense.domain.port.CompassEngine
import com.nexasense.domain.port.MagneticFieldMonitor
import com.nexasense.domain.port.QiblaEngine
import com.nexasense.domain.port.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompassViewModel(
    private val compassEngine: CompassEngine,
    private val magneticMonitor: MagneticFieldMonitor,
    private val qiblaEngine: QiblaEngine,
    settingsStore: SettingsStore,
) : ViewModel() {

    val heading = compassEngine.heading
    val magneticField = compassEngine.magneticField
    val liveCalibration = magneticMonitor.liveCalibration
    val qiblaState: StateFlow<QiblaState> = qiblaEngine.state
    val sensorBlocked = compassEngine.sensorBlocked

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings.DEFAULT,
    )

    /**
     * Increments each time the user turns from left/right into the aligned
     * zone, so the UI can fire one short haptic pulse. Cooldown prevents
     * rapid repeats while staying inside the alignment zone.
     */
    private val _hapticTick = MutableStateFlow(0)
    val hapticTick: StateFlow<Int> = _hapticTick.asStateFlow()

    private var lastAlignment = QiblaAlignment.UNAVAILABLE
    private var lastHapticAtMillis = 0L

    init {
        viewModelScope.launch {
            combine(qiblaState, settings) { qibla, currentSettings ->
                qibla to currentSettings
            }.collect { (qibla, currentSettings) ->
                maybeFireHaptic(qibla, currentSettings)
            }
        }
    }

    fun setActive(active: Boolean) {
        compassEngine.setActive(active)
        qiblaEngine.setActive(active)
    }

    /** Keeps the heading in the user's frame across display rotations. */
    fun setDisplayRotation(rotationDegrees: Int) {
        compassEngine.setDisplayRotation(rotationDegrees)
    }

    fun resetCalibration() = magneticMonitor.resetCalibration()

    fun refresh() {
        compassEngine.setActive(false)
        compassEngine.setActive(true)
        qiblaEngine.refreshLocation()
    }

    private fun maybeFireHaptic(qibla: QiblaState, currentSettings: AppSettings) {
        val aligned = qibla.alignment == QiblaAlignment.ALIGNED
        val turning = lastAlignment == QiblaAlignment.TURN_LEFT || lastAlignment == QiblaAlignment.TURN_RIGHT
        lastAlignment = qibla.alignment
        if (!aligned || !turning) return
        if (!currentSettings.qiblaHapticFeedback || !currentSettings.hapticsEnabled) return

        val now = System.currentTimeMillis()
        if (now - lastHapticAtMillis < HAPTIC_COOLDOWN_MILLIS) return
        lastHapticAtMillis = now
        _hapticTick.value += 1
    }

    private companion object {
        const val HAPTIC_COOLDOWN_MILLIS = 3_000L
    }
}
