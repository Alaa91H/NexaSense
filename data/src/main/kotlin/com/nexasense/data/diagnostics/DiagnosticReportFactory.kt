package com.nexasense.data.diagnostics

import com.nexasense.domain.engine.SourceSelector
import com.nexasense.domain.math.AngleMath
import com.nexasense.domain.model.AvailabilityReason
import com.nexasense.domain.model.DiagnosticReport
import com.nexasense.domain.model.FeatureAvailability
import com.nexasense.domain.model.HeadingMode
import com.nexasense.domain.model.NorthReferenceDiagnostics
import com.nexasense.domain.model.QiblaStatus
import com.nexasense.domain.model.SensorKind
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.CompassEngine
import com.nexasense.domain.port.DeviceInfoProvider
import com.nexasense.domain.port.QiblaEngine
import com.nexasense.domain.port.SensorDiscovery
import kotlinx.coroutines.flow.first

/**
 * Builds the diagnostic report from runtime discovery, calibration state and a
 * north-reference/Qibla snapshot. The report contains hardware, capability and
 * configuration information only — never the user's coordinates or personal
 * data.
 */
class DiagnosticReportFactory(
    private val discovery: SensorDiscovery,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val calibrationStore: CalibrationStore,
    private val compassEngine: CompassEngine,
    private val qiblaEngine: QiblaEngine,
) {

    suspend fun build(): DiagnosticReport {
        val sensors = discovery.getSensors()
        val kinds = sensors.map { it.kind }.toSet()
        val magnetometerCalibration = calibrationStore.magnetometerCalibration.first()
        val levelCalibration = calibrationStore.levelCalibration.first()
        return DiagnosticReport(
            generatedAtMillis = System.currentTimeMillis(),
            device = deviceInfoProvider.deviceInfo(),
            sensors = sensors,
            capabilities = buildCapabilities(kinds),
            magnetometerCalibrated = magnetometerCalibration.isCalibrated,
            levelCalibrated = levelCalibration.isSet,
            northReference = buildNorthReferenceDiagnostics(),
        )
    }

    private fun buildNorthReferenceDiagnostics(): NorthReferenceDiagnostics {
        val heading = compassEngine.heading.value
        val qibla = qiblaEngine.state.value

        val declination = heading.declinationDegrees
        val magnetic = when (heading.mode) {
            HeadingMode.TRUE -> declination?.let { AngleMath.normalizeDegrees(heading.degrees - it) }
            HeadingMode.MAGNETIC -> heading.degrees.takeIf { heading.isAvailable }
        }
        val trueHeading = when (heading.mode) {
            HeadingMode.TRUE -> heading.degrees.takeIf { heading.isAvailable }
            HeadingMode.MAGNETIC -> declination?.let { AngleMath.normalizeDegrees(heading.degrees + it) }
        }

        return NorthReferenceDiagnostics(
            requestedNorthReference = heading.requestedNorthReference,
            effectiveNorthReference = heading.effectiveNorthReference,
            magneticHeadingDegrees = magnetic,
            trueHeadingDegrees = trueHeading,
            declinationDegrees = declination,
            qiblaEnabled = qibla.status != QiblaStatus.QIBLA_DISABLED,
            qiblaBearingDegrees = qibla.bearingDegrees,
            relativeQiblaDegrees = qibla.relativeQiblaDegrees,
            locationAccuracy = qibla.locationAccuracy,
        )
    }

    private fun buildCapabilities(kinds: Set<SensorKind>): List<Pair<String, FeatureAvailability>> = listOf(
        "Compass" to capability(SourceSelector.bestSource(kinds) != com.nexasense.domain.model.HeadingSource.UNAVAILABLE),
        "Level" to capability(SensorKind.ACCELEROMETER in kinds),
        "Gyroscope" to capability(SensorKind.GYROSCOPE in kinds),
        "Barometer" to capability(SensorKind.PRESSURE in kinds),
        "Thermometer" to capability(SensorKind.AMBIENT_TEMPERATURE in kinds),
        "Humidity" to capability(SensorKind.RELATIVE_HUMIDITY in kinds),
        "Step Counter" to capability(SensorKind.STEP_COUNTER in kinds),
        "Light" to capability(SensorKind.LIGHT in kinds),
        "Proximity" to capability(SensorKind.PROXIMITY in kinds),
    )

    private fun capability(available: Boolean): FeatureAvailability =
        if (available) {
            FeatureAvailability.available()
        } else {
            FeatureAvailability.unavailable(AvailabilityReason.MISSING_SENSOR)
        }
}
