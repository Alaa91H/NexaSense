package com.nexasense.domain.model

import com.nexasense.domain.model.LocationAccuracyLevel
import com.nexasense.domain.model.NorthReference
import java.time.Instant

/**
 * North-reference / Qibla snapshot included in the report. Exact user
 * coordinates are never included.
 */
data class NorthReferenceDiagnostics(
    val requestedNorthReference: NorthReference? = null,
    val effectiveNorthReference: NorthReference? = null,
    val magneticHeadingDegrees: Float? = null,
    val trueHeadingDegrees: Float? = null,
    val declinationDegrees: Float? = null,
    val qiblaEnabled: Boolean? = null,
    val qiblaBearingDegrees: Float? = null,
    val relativeQiblaDegrees: Float? = null,
    val locationAccuracy: LocationAccuracyLevel? = null,
)

/**
 * A text diagnostic report that can be shared. Built from hardware discovery
 * and capability data only — it contains no personal data, no location and no
 * sensor value streams.
 */
data class DiagnosticReport(
    val generatedAtMillis: Long,
    val device: DeviceInfo,
    val sensors: List<SensorDescriptor>,
    val capabilities: List<Pair<String, FeatureAvailability>>,
    val magnetometerCalibrated: Boolean,
    val levelCalibrated: Boolean,
    val northReference: NorthReferenceDiagnostics = NorthReferenceDiagnostics(),
) {

    fun buildText(): String = buildString {
        appendLine("NexaSense Diagnostic Report")
        appendLine("==========================")
        appendLine("Generated: ${Instant.ofEpochMilli(generatedAtMillis)}")
        appendLine()
        appendLine("Device")
        appendLine("------")
        appendLine("Manufacturer: ${device.manufacturer}")
        appendLine("Model: ${device.model}")
        appendLine("Device: ${device.device}")
        appendLine("Product: ${device.product}")
        appendLine("Android: ${device.androidVersion} (API ${device.sdkInt})")
        appendLine("Build fingerprint: ${device.buildFingerprint}")
        appendLine("Kernel: ${device.kernelVersion}")
        appendLine("Board: ${device.board}")
        appendLine("Hardware: ${device.hardware}")
        appendLine("Build tags: ${device.buildTags}")
        appendLine()
        appendLine("Sensors (${sensors.size})")
        appendLine("---------------------")
        sensors.forEach { s ->
            appendLine(
                "- ${s.kind} (type ${s.kind.type}) \"${s.name}\" by ${s.vendor}, " +
                    "v${s.version}, resolution=${s.resolution}, range=${s.maxRange}, " +
                    "power=${s.powerMilliAmps} mA, minDelay=${s.minDelayMicros} µs, " +
                    "maxDelay=${s.maxDelayMicros} µs, wake=${s.isWakeUp}, " +
                    "dynamic=${s.isDynamic}, mode=${s.reportingMode}",
            )
        }
        appendLine()
        appendLine("Capabilities")
        appendLine("------------")
        capabilities.forEach { (name, availability) ->
            appendLine("- $name: ${availability.status}")
        }
        appendLine()
        appendLine("Calibration")
        appendLine("-----------")
        appendLine("Magnetometer calibrated: $magnetometerCalibrated")
        appendLine("Level calibrated: $levelCalibrated")
        appendLine()
        appendLine("North Reference & Qibla")
        appendLine("------------------------")
        appendLine("North reference requested: ${northReference.requestedNorthReference ?: "n/a"}")
        appendLine("North reference effective: ${northReference.effectiveNorthReference ?: "n/a"}")
        appendLine("Magnetic heading: ${formatDegrees(northReference.magneticHeadingDegrees)}")
        appendLine("True heading: ${formatDegrees(northReference.trueHeadingDegrees)}")
        appendLine("Declination: ${formatDegrees(northReference.declinationDegrees)}")
        appendLine("Qibla enabled: ${northReference.qiblaEnabled ?: "n/a"}")
        appendLine("Qibla bearing: ${formatDegrees(northReference.qiblaBearingDegrees)}")
        appendLine("Relative Qibla: ${formatDegrees(northReference.relativeQiblaDegrees)}")
        appendLine("Location accuracy: ${northReference.locationAccuracy ?: "n/a"}")
        appendLine()
        appendLine("This report contains hardware and configuration information only.")
        appendLine("User coordinates are never included.")
    }

    private fun formatDegrees(degrees: Float?): String =
        degrees?.let { String.format(java.util.Locale.US, "%.1f°", it) } ?: "n/a"
}
