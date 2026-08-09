package com.nexasense.presentation

import com.nexasense.data.diagnostics.DiagnosticReportFactory
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.CompassEngine
import com.nexasense.domain.port.DeviceInfoProvider
import com.nexasense.domain.port.LevelEngine
import com.nexasense.domain.port.MagneticFieldMonitor
import com.nexasense.domain.port.QiblaEngine
import com.nexasense.domain.port.SensorDiscovery
import com.nexasense.domain.port.SensorEventStream
import com.nexasense.domain.port.SettingsStore

/**
 * Manual dependency container. Implemented by the app module and passed down
 * through composition; keeps the presentation layer free of Android wiring.
 */
interface AppContainer {
    val settingsStore: SettingsStore
    val calibrationStore: CalibrationStore
    val sensorDiscovery: SensorDiscovery
    val sensorEventStream: SensorEventStream
    val compassEngine: CompassEngine
    val magneticFieldMonitor: MagneticFieldMonitor
    val levelEngine: LevelEngine
    val qiblaEngine: QiblaEngine
    val deviceInfoProvider: DeviceInfoProvider
    val diagnosticReportFactory: DiagnosticReportFactory

    /** Whether the location permission is currently granted. */
    fun hasLocationPermission(): Boolean

    val appVersionName: String

    /** Version code from the single build-config source. */
    val appVersionCode: Int

    /** Build type (debug/release) from the single build-config source. */
    val buildType: String
}
