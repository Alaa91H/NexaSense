package com.nexasense.data.sensor

import com.nexasense.domain.engine.LevelCalculator
import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.model.OrientationAngles
import com.nexasense.domain.model.SensorKind
import com.nexasense.domain.model.Vec3
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.LevelEngine
import com.nexasense.domain.port.SensorEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Spirit level engine. Uses only the accelerometer, so the level works on any
 * device with an accelerometer — a gyroscope is never required. Readings are
 * mapped into the user's display frame and corrected with the saved offsets.
 */
class LevelEngineImpl(
    private val streams: SensorEventStream,
    private val calibrationStore: CalibrationStore,
    private val scope: CoroutineScope,
) : LevelEngine {

    private val _orientation = MutableStateFlow(OrientationAngles.ZERO)
    override val orientation: StateFlow<OrientationAngles> = _orientation.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    @Volatile
    private var displayRotationDegrees = 0

    @Volatile
    private var calibration = LevelCalibration.NONE

    @Volatile
    private var lastRaw: Vec3? = null

    private var job: Job? = null

    override fun setDisplayRotation(rotationDegrees: Int) {
        displayRotationDegrees = rotationDegrees
        lastRaw?.let { publish(it) }
    }

    override fun setActive(active: Boolean) {
        job?.cancel()
        job = null
        lastRaw = null
        if (!active) {
            _isAvailable.value = false
            return
        }
        job = scope.launch {
            launch {
                calibrationStore.levelCalibration.collect { newCalibration ->
                    calibration = newCalibration
                    lastRaw?.let { publish(it) }
                }
            }
            streams.stream(SensorKind.ACCELEROMETER, LEVEL_DELAY_MICROS).collect { reading ->
                val v = reading.vector3()
                if (v.isInvalid) return@collect
                lastRaw = v
                publish(v)
            }
        }
    }

    private fun publish(raw: Vec3) {
        val rawAngles = LevelCalculator.fromAccelerometer(raw)
        val display = LevelCalculator.mapToDisplay(rawAngles, displayRotationDegrees)
        val corrected = LevelCalculator.applyOffsets(display, calibration)
        _orientation.value = corrected
        if (!_isAvailable.value) {
            _isAvailable.value = true
        }
    }

    companion object {
        /** ~60 ms — responsive enough for a bubble level, gentle on battery. */
        const val LEVEL_DELAY_MICROS = 60_000L
    }
}
