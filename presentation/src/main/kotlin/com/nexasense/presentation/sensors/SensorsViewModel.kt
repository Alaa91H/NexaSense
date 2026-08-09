package com.nexasense.presentation.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.presentation.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorsViewModel(container: AppContainer) : ViewModel() {

    private val _sensors = MutableStateFlow<List<SensorDescriptor>>(emptyList())
    val sensors: StateFlow<List<SensorDescriptor>> = _sensors.asStateFlow()

    init {
        viewModelScope.launch {
            _sensors.value = container.sensorDiscovery.getSensors()
        }
    }
}
