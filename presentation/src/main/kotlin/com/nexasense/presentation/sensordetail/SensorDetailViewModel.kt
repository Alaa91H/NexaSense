package com.nexasense.presentation.sensordetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.engine.SamplingRateEstimator
import com.nexasense.domain.model.SamplingStats
import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.domain.model.SensorReading
import com.nexasense.domain.port.SensorDiscovery
import com.nexasense.domain.port.SensorEventStream
import com.nexasense.domain.port.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shows one sensor's metadata plus a live stream with the *measured* sampling
 * rate — the requested delay is displayed but never assumed to be the actual
 * rate.
 */
class SensorDetailViewModel(
    sensorId: Int,
    sensorDiscovery: SensorDiscovery,
    private val streams: SensorEventStream,
    settingsStore: SettingsStore,
) : ViewModel() {

    private val _descriptor = MutableStateFlow<SensorDescriptor?>(null)
    val descriptor: StateFlow<SensorDescriptor?> = _descriptor.asStateFlow()

    private val _reading = MutableStateFlow<SensorReading?>(null)
    val reading: StateFlow<SensorReading?> = _reading.asStateFlow()

    private val _samplingStats = MutableStateFlow(SamplingStats.EMPTY)
    val samplingStats: StateFlow<SamplingStats> = _samplingStats.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val estimator = SamplingRateEstimator()
    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            val sensors = sensorDiscovery.getSensors()
            _descriptor.value = sensors.firstOrNull { it.id == sensorId }
            _isStreaming.value = _descriptor.value?.isContinuous == true
        }
        // Keep the requested delay in sync with the user's rate setting.
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _samplingStats.value = estimator.current(requestedDelayMicros = settings.sensorRate.delayMicros)
            }
        }
    }

    fun setActive(active: Boolean) {
        val sensor = _descriptor.value ?: return
        streamJob?.cancel()
        streamJob = null
        estimator.reset()
        _reading.value = null
        if (!active || !sensor.isContinuous) return
        streamJob = viewModelScope.launch {
            // Fastest rate: this screen explicitly measures the real maximum.
            streams.stream(sensor.kind, delayMicros = 0L, sensorId = sensor.id).collect { event ->
                _reading.value = event
                _samplingStats.value = estimator.update(event.timestampNanos)
            }
        }
    }
}
