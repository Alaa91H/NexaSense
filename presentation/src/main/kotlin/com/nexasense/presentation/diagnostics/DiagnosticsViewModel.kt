package com.nexasense.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.data.diagnostics.DiagnosticReportFactory
import com.nexasense.domain.model.FeatureAvailability
import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.domain.port.CrashRecord
import com.nexasense.presentation.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticsUiState(
    val sensors: List<SensorDescriptor> = emptyList(),
    val capabilities: List<Pair<String, FeatureAvailability>> = emptyList(),
    val developerMode: Boolean = false,
    val magnetometerCalibrated: Boolean = false,
    val reportText: String? = null,
    val reportReady: Boolean = false,
    val crashHistory: List<CrashRecord> = emptyList(),
)

class DiagnosticsViewModel(container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    private val reportFactory: DiagnosticReportFactory = container.diagnosticReportFactory
    private val crashLogStore = container.crashLogStore

    init {
        viewModelScope.launch {
            val sensors = container.sensorDiscovery.getSensors()
            val report = reportFactory.build()
            val developerMode = container.settingsStore.settings.first().developerMode
            val crashHistory = withContext(Dispatchers.IO) { crashLogStore.crashes }
            _state.value = DiagnosticsUiState(
                sensors = sensors,
                capabilities = report.capabilities,
                developerMode = developerMode,
                magnetometerCalibrated = report.magnetometerCalibrated,
                crashHistory = crashHistory,
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

    fun clearCrashHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { crashLogStore.clear() }
            _state.value = _state.value.copy(crashHistory = emptyList())
        }
    }
}
