package com.nexasense.presentation.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.engine.LevelCalculator
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.model.OrientationAngles
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.LevelEngine
import com.nexasense.domain.port.SettingsStore
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LevelViewModel(
    private val levelEngine: LevelEngine,
    private val calibrationStore: CalibrationStore,
    settingsStore: SettingsStore,
) : ViewModel() {

    val orientation = levelEngine.orientation
    val isAvailable = levelEngine.isAvailable
    val sensorBlocked = levelEngine.sensorBlocked

    /**
     * The level mode is derived automatically from how the device is held:
     * - held upright (|pitch| above 45°) → vertical (plumb) mode, a one-axis
     *   water/mercury tube level for checking walls, edges and posts;
     * - held flat (|pitch| below 45°) → horizontal mode, the two-axis bubble.
     */
    val verticalMode: StateFlow<Boolean> = orientation
        .map { abs(it.pitch) > VERTICAL_MODE_THRESHOLD_DEGREES }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

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
     * A discrete haptic pulse request for the UI. [strength] is normalized
     * 0..1 (amplitude), [id] strictly increases so every pulse retriggers the
     * UI effect even at equal strength, and [centered] marks the final strong
     * pulse fired on entering the perfectly-level zone.
     */
    data class HapticPulse(val id: Long, val strength: Float, val centered: Boolean)

    /**
     * Emits a pulse when the level needs attention: the strength ramps up in
     * bands as the device approaches level/plumb in both modes (a graded
     * "getting closer" feel), and both modes fire a final strong pulse when
     * the centered zone is entered — the user can level a surface without
     * watching the screen (standard bubble-level UX).
     */
    private val _hapticPulse = MutableStateFlow<HapticPulse?>(null)
    val hapticPulse: StateFlow<HapticPulse?> = _hapticPulse.asStateFlow()

    private var lastCentered = false
    private var lastCenteredHapticAtMillis = 0L
    private var lastProximityBand = -1
    private var lastBandPulseAtMillis = 0L
    private var pulseId = 0L

    init {
        viewModelScope.launch {
            combine(orientation, settings, verticalMode) { current, currentSettings, vertical ->
                Triple(current, currentSettings, vertical)
            }.collect { (current, currentSettings, vertical) ->
                maybeFireHaptic(current, currentSettings, vertical)
            }
        }
    }

    fun setActive(active: Boolean) = levelEngine.setActive(active)

    fun setDisplayRotation(rotationDegrees: Int) =
        levelEngine.setDisplayRotation(rotationDegrees)

    private fun maybeFireHaptic(
        orientation: OrientationAngles,
        currentSettings: AppSettings,
        vertical: Boolean,
    ) {
        val pitchDeviation = LevelCalculator.verticalDeviation(orientation.pitch)

        // Grade the pulse strength by how close the device is to level/plumb
        // — each proximity band crossed inward fires a stronger pulse. Works
        // in both modes: vertical uses the deviation-from-upright + roll,
        // flat uses pitch + roll (both 0 exactly at level).
        if (currentSettings.hapticsEnabled) {
            maybeFireProximityBandPulse(
                deviation = max(
                    abs(if (vertical) pitchDeviation else orientation.pitch),
                    abs(orientation.roll),
                ),
            )
        }

        val centered = if (vertical) {
            abs(pitchDeviation) < CENTERED_THRESHOLD_DEGREES &&
                abs(orientation.roll) < CENTERED_THRESHOLD_DEGREES
        } else {
            abs(orientation.pitch) < CENTERED_THRESHOLD_DEGREES &&
                abs(orientation.roll) < CENTERED_THRESHOLD_DEGREES
        }
        val entering = centered && !lastCentered
        lastCentered = centered
        if (!entering) return
        val now = System.currentTimeMillis()
        if (now - lastCenteredHapticAtMillis < HAPTIC_COOLDOWN_MILLIS) return
        lastCenteredHapticAtMillis = now
        // The centered pulse fires regardless of the haptics toggle; the UI
        // decides which feedback to render (vibration, sound, both, or
        // neither) from the settings, keeping them independent.
        firePulse(strength = 1f, centered = true)
    }

    /**
     * Fires a pulse when the device crosses inward through a proximity band
     * (8° → 5° → 3° → 2° → 1.5° → 1° → 0.5°), with the strength scaling with
     * how close the band is to level/plumb. Crossing outward never refires
     * until the device leaves the outer band, so hovering near level doesn't
     * spam.
     */
    private fun maybeFireProximityBandPulse(deviation: Float) {
        var band = -1
        for (i in PROXIMITY_HAPTIC_BANDS.indices) {
            if (deviation < PROXIMITY_HAPTIC_BANDS[i]) band = i
        }
        if (band < 0) {
            lastProximityBand = -1
            return
        }
        if (band <= lastProximityBand) return
        val now = System.currentTimeMillis()
        if (now - lastBandPulseAtMillis < BAND_PULSE_MIN_GAP_MILLIS) return
        lastBandPulseAtMillis = now
        lastProximityBand = band
        firePulse(
            strength = (band + 1) / PROXIMITY_HAPTIC_BANDS.size.toFloat(),
            centered = false,
        )
    }

    private fun firePulse(strength: Float, centered: Boolean) {
        _hapticPulse.value = HapticPulse(
            id = ++pulseId,
            strength = strength,
            centered = centered,
        )
    }

    private companion object {
        /** Matches the bubble's visual "level" zone on the canvas. */
        const val CENTERED_THRESHOLD_DEGREES = 1.5f
        const val HAPTIC_COOLDOWN_MILLIS = 2_000L

        /** Above this |pitch| the device counts as held upright (plumb mode). */
        const val VERTICAL_MODE_THRESHOLD_DEGREES = 45f

        /**
         * Proximity bands for the graded haptic (both level modes), outermost
         * first. Crossing inward through a band fires a pulse whose strength
         * scales with the band index (closest band = strongest pulse).
         */
        val PROXIMITY_HAPTIC_BANDS = floatArrayOf(8f, 5f, 3f, 2f, 1.5f, 1f, 0.5f)

        /** Minimum gap between band pulses, to avoid boundary flutter. */
        const val BAND_PULSE_MIN_GAP_MILLIS = 250L
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
