package com.nexasense.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.model.SensorKind
import com.nexasense.presentation.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val compass: Boolean = false,
    val level: Boolean = false,
    val gyroscope: Boolean = false,
    val barometer: Boolean = false,
    val thermometer: Boolean = false,
    val humidity: Boolean = false,
    val loaded: Boolean = false,
)

/** Detects feature capabilities at runtime for the home screen. */
class HomeViewModel(container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val kinds = container.sensorDiscovery.getSensors().map { it.kind }.toSet()
            _state.value = HomeUiState(
                compass = com.nexasense.domain.engine.SourceSelector.bestSource(kinds) !=
                    com.nexasense.domain.model.HeadingSource.UNAVAILABLE,
                level = SensorKind.ACCELEROMETER in kinds,
                gyroscope = SensorKind.GYROSCOPE in kinds,
                barometer = SensorKind.PRESSURE in kinds,
                thermometer = SensorKind.AMBIENT_TEMPERATURE in kinds,
                humidity = SensorKind.RELATIVE_HUMIDITY in kinds,
                loaded = true,
            )
        }
    }
}
