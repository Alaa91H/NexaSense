package com.nexasense.android

import android.content.Context
import com.nexasense.core.diagnostics.DeviceInfoProviderImpl
import com.nexasense.core.location.LocationProviderImpl
import com.nexasense.core.location.Wmm2025DeclinationProvider
import com.nexasense.core.permissions.PermissionChecker
import com.nexasense.core.sensor.SensorManagerGateway
import com.nexasense.data.diagnostics.DiagnosticReportFactory
import com.nexasense.data.preferences.DataStoreCalibrationStore
import com.nexasense.data.preferences.DataStoreSettingsStore
import com.nexasense.data.sensor.CompassEngineImpl
import com.nexasense.data.sensor.LevelEngineImpl
import com.nexasense.data.sensor.QiblaEngineImpl
import com.nexasense.domain.engine.DeclinationCache
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.CompassEngine
import com.nexasense.domain.port.DeviceInfoProvider
import com.nexasense.domain.port.LevelEngine
import com.nexasense.domain.port.MagneticFieldMonitor
import com.nexasense.domain.port.QiblaEngine
import com.nexasense.domain.port.SensorDiscovery
import com.nexasense.domain.port.SensorEventStream
import com.nexasense.domain.port.SettingsStore
import com.nexasense.presentation.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph. All engines share one application scope; sensor
 * registration itself is still gated by each screen's lifecycle.
 */
class AppContainerImpl(context: Context) : AppContainer {

    private val appContext: Context = context.applicationContext

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val gateway = SensorManagerGateway(appContext)
    private val settingsStoreImpl = DataStoreSettingsStore(appContext)
    private val calibrationStoreImpl = DataStoreCalibrationStore(appContext)
    private val deviceInfoProviderImpl = DeviceInfoProviderImpl()

    private val declinationProvider = Wmm2025DeclinationProvider()

    private val compassEngineImpl = CompassEngineImpl(
        discovery = gateway,
        streams = gateway,
        declinationProvider = declinationProvider,
        locationProvider = LocationProviderImpl(appContext),
        calibrationStore = calibrationStoreImpl,
        settingsStore = settingsStoreImpl,
        scope = applicationScope,
        locationPermissionGranted = { PermissionChecker.hasLocationPermission(appContext) },
    )

    private val levelEngineImpl = LevelEngineImpl(
        streams = gateway,
        calibrationStore = calibrationStoreImpl,
        scope = applicationScope,
    )

    private val qiblaEngineImpl = QiblaEngineImpl(
        compassEngine = compassEngineImpl,
        magneticMonitor = compassEngineImpl,
        locationProvider = LocationProviderImpl(appContext),
        declinationProvider = declinationProvider,
        declinationCache = DeclinationCache(),
        settingsStore = settingsStoreImpl,
        scope = applicationScope,
        locationPermissionGranted = { PermissionChecker.hasLocationPermission(appContext) },
    )

    override val settingsStore: SettingsStore get() = settingsStoreImpl
    override val calibrationStore: CalibrationStore get() = calibrationStoreImpl
    override val sensorDiscovery: SensorDiscovery get() = gateway
    override val sensorEventStream: SensorEventStream get() = gateway
    override val compassEngine: CompassEngine get() = compassEngineImpl
    override val magneticFieldMonitor: MagneticFieldMonitor get() = compassEngineImpl
    override val deviceInfoProvider: DeviceInfoProvider get() = deviceInfoProviderImpl
    override val levelEngine: LevelEngine get() = levelEngineImpl
    override val qiblaEngine: QiblaEngine get() = qiblaEngineImpl
    override val diagnosticReportFactory: DiagnosticReportFactory =
        DiagnosticReportFactory(gateway, deviceInfoProviderImpl, calibrationStoreImpl, compassEngineImpl, qiblaEngineImpl)

    override fun hasLocationPermission(): Boolean = PermissionChecker.hasLocationPermission(appContext)

    override val appVersionName: String = BuildConfig.VERSION_NAME
    override val appVersionCode: Int = BuildConfig.VERSION_CODE
    override val buildType: String = BuildConfig.BUILD_TYPE
}
