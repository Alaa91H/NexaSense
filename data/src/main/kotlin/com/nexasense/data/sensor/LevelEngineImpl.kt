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
import kotlinx.coroutines.delay
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

    private val _sensorBlocked = MutableStateFlow(false)
    override val sensorBlocked: StateFlow<Boolean> = _sensorBlocked.asStateFlow()

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
        _sensorBlocked.value = false
        if (!active) {
            _isAvailable.value = false
            return
        }
        job = scope.launch {
            // If the accelerometer exists but no event arrives within the
            // timeout, the stream is open but silent — the system "Sensors
            // Off" toggle or a per-app sensor permission (AOSP ROMs) is
            // blocking it. A closed stream (no sensor) is NOT blocked.
            val received = MutableStateFlow(false)
            val watchdog = scope.launch {
                delay(SENSOR_BLOCKED_TIMEOUT_MILLIS)
                if (!received.value) _sensorBlocked.value = true
            }
            launch {
                calibrationStore.levelCalibration.collect { newCalibration ->
                    calibration = newCalibration
                    lastRaw?.let { publish(it) }
                }
            }
            try {
                streams.stream(SensorKind.ACCELEROMETER, LEVEL_DELAY_MICROS).collect { reading ->
                    received.value = true
                    _sensorBlocked.value = false
                    val v = reading.vector3()
                    if (v.isInvalid) return@collect
                    lastRaw = v
                    publish(v)
                }
            } finally {
                watchdog.cancel()
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

        /** Silence threshold before declaring the sensor blocked (ms). */
        const val SENSOR_BLOCKED_TIMEOUT_MILLIS = 3_000L
    }
}
