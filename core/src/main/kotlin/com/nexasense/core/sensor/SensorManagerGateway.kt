package com.nexasense.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.HandlerThread
import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.model.AccuracyLevel
import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.domain.model.SensorKind
import com.nexasense.domain.model.SensorReading
import com.nexasense.domain.port.SensorDiscovery
import com.nexasense.domain.port.SensorEventStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext

/**
 * Bridges the Android Sensor Framework to the domain ports.
 *
 * Discovery never guesses: the sensor type id is authoritative, the name and
 * vendor strings are reported to the UI as-is. Registration failures and
 * missing sensors complete the flow silently instead of throwing.
 */
class SensorManagerGateway(context: Context) : SensorDiscovery, SensorEventStream {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    override suspend fun getSensors(): List<SensorDescriptor> = withContext(Dispatchers.Default) {
        val manager = sensorManager ?: return@withContext emptyList()
        // Some partial or misconfigured sensor HALs (custom ROMs) throw when
        // the sensor list is inconsistent. Discovery must never crash the app:
        // report an empty list and log, exactly as if no sensors existed.
        runCatching {
            manager.getSensorList(Sensor.TYPE_ALL).map { it.toDescriptor() }
        }.getOrElse { error ->
            NexaLogger.e("Sensor list unavailable: ${error.message}")
            emptyList()
        }
    }

    override suspend fun hasSensor(kind: SensorKind): Boolean {
        val manager = sensorManager ?: return false
        return runCatching { manager.getDefaultSensor(kind.type) != null }
            .getOrElse { error ->
                NexaLogger.w("Sensor lookup failed for ${kind.name}: ${error.message}")
                false
            }
    }

    override suspend fun sensorsOf(kind: SensorKind): List<SensorDescriptor> =
        getSensors().filter { it.kind == kind }

    override fun stream(
        kind: SensorKind,
        delayMicros: Long,
        sensorId: Int?,
    ): Flow<SensorReading> = channelFlow {
        val manager = sensorManager
        val sensor = resolveSensor(manager, kind, sensorId)
        if (manager == null || sensor == null) {
            NexaLogger.w("No sensor available for ${kind.name}; stream is empty.")
            close()
            return@channelFlow
        }

        val thread = HandlerThread("NexaSense-${kind.name}").apply { start() }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // The framework hands us a fresh array per event, but copy to
                // be safe; the work here is kept minimal (no allocation-heavy
                // processing inside the callback).
                trySend(
                    SensorReading(
                        sensorId = event.sensor.id,
                        kind = kind,
                        values = event.values.copyOf(),
                        accuracy = AccuracyLevel.fromStatus(event.accuracy),
                        timestampNanos = event.timestamp,
                    ),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = try {
            manager.registerListener(listener, sensor, delayMicros.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(), 0)
        } catch (t: Throwable) {
            NexaLogger.e("Sensor registration failed for ${kind.name}", t)
            false
        }

        if (!registered) {
            NexaLogger.w("SensorManager refused registration for ${kind.name}.")
            thread.quitSafely()
            close()
            return@channelFlow
        }

        awaitClose {
            try {
                manager.unregisterListener(listener)
            } catch (t: Throwable) {
                NexaLogger.e("Failed to unregister sensor listener", t)
            }
            thread.quitSafely()
        }
    }

    private fun resolveSensor(
        manager: SensorManager?,
        kind: SensorKind,
        sensorId: Int?,
    ): Sensor? {
        if (manager == null) return null
        return try {
            val sensors = manager.getSensorList(kind.type)
            if (sensors.isEmpty()) {
                null
            } else {
                sensorId?.let { id -> sensors.firstOrNull { it.id == id } }
                    ?: sensors.firstOrNull { !it.isWakeUpSensor }
                    ?: sensors.first()
            }
        } catch (t: Throwable) {
            // A throwing HAL must not take down the screen that opened the
            // stream; treat it as "no sensor" and log instead.
            NexaLogger.e("Sensor resolution failed for ${kind.name}: ${t.message}")
            null
        }
    }

    private fun Sensor.toDescriptor(): SensorDescriptor = SensorDescriptor(
        id = id,
        kind = SensorKind.fromType(type),
        name = name,
        vendor = vendor,
        version = version,
        stringType = stringType,
        resolution = resolution,
        maxRange = maximumRange,
        powerMilliAmps = power,
        minDelayMicros = minDelay,
        maxDelayMicros = maxDelay,
        isWakeUp = isWakeUpSensor,
        isDynamic = isDynamicSensor,
        reportingMode = reportingModeName(reportingMode),
        maxFifoCount = fifoMaxEventCount,
    )

    private fun reportingModeName(mode: Int): String = when (mode) {
        Sensor.REPORTING_MODE_CONTINUOUS -> "continuous"
        Sensor.REPORTING_MODE_ON_CHANGE -> "on-change"
        Sensor.REPORTING_MODE_ONE_SHOT -> "one-shot"
        Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "special"
        else -> "unknown($mode)"
    }
}
