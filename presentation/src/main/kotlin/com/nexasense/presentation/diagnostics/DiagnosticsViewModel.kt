package com.nexasense.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.data.diagnostics.DiagnosticReportFactory
import com.nexasense.domain.model.FeatureAvailability
import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.presentation.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val sensors: List<SensorDescriptor> = emptyList(),
    val capabilities: List<Pair<String, FeatureAvailability>> = emptyList(),
    val developerMode: Boolean = false,
    val magnetometerCalibrated: Boolean = false,
    val reportText: String? = null,
    val reportReady: Boolean = false,
)

class DiagnosticsViewModel(container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    private val reportFactory: DiagnosticReportFactory = container.diagnosticReportFactory

    init {
        viewModelScope.launch {
            val sensors = container.sensorDiscovery.getSensors()
            val report = reportFactory.build()
            val developerMode = container.settingsStore.settings.first().developerMode
            _state.value = DiagnosticsUiState(
                sensors = sensors,
                capabilities = report.capabilities,
                developerMode = developerMode,
                magnetometerCalibrated = report.magnetometerCalibrated,
            )
        }
    }

    fun generateReport() {
        viewModelScope.launch {
            val report = reportFactory.build()
            _state.value = _state.value.copy(
                reportText = report.buildText(),
                reportReady = true,
                magnetometerCalibrated = report.magnetometerCalibrated,
            )
        }
    }
}
