package com.nexasense.presentation.compass

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexasense.domain.math.AngleMath
import com.nexasense.domain.model.AccuracyLevel
import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.HeadingMode
import com.nexasense.domain.model.HeadingSource
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.QiblaAlignment
import com.nexasense.domain.model.QiblaState
import com.nexasense.domain.model.QiblaStatus
import com.nexasense.domain.model.TrueNorthUnavailableReason
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.components.DataCard
import com.nexasense.presentation.components.EmptyState
import com.nexasense.presentation.components.EngineLifecycleEffect
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.StatusPill
import androidx.compose.ui.res.painterResource
import java.util.Locale

@Composable
fun CompassScreen(
    container: AppContainer,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: CompassViewModel = viewModel(initializer = {
        CompassViewModel(
            compassEngine = container.compassEngine,
            magneticMonitor = container.magneticFieldMonitor,
            qiblaEngine = container.qiblaEngine,
            settingsStore = container.settingsStore,
        )
    })
    val heading by viewModel.heading.collectAsStateWithLifecycle()
    val magneticField by viewModel.magneticField.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val liveCalibration by viewModel.liveCalibration.collectAsStateWithLifecycle()
    val qiblaState by viewModel.qiblaState.collectAsStateWithLifecycle()
    val hapticTick by viewModel.hapticTick.collectAsStateWithLifecycle()
    val sensorBlocked by viewModel.sensorBlocked.collectAsStateWithLifecycle()

    EngineLifecycleEffect(active = true, onStateChanged = viewModel::setActive)

    // Keep the heading in the user's frame of reference across display
    // rotations (auto-rotate): sensors report in the device's natural frame.
    val view = LocalView.current
    val rotationDegrees = (view.display?.rotation ?: 0) * 90
    LaunchedEffect(rotationDegrees) {
        viewModel.setDisplayRotation(rotationDegrees)
    }

    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(hapticTick) {
        if (hapticTick > 0) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.refresh()
    }

    ScreenScaffold(
        title = stringResource(R.string.compass_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.refresh),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (!heading.isAvailable) {
                EmptyState(
                    icon = painterResource(
                        if (sensorBlocked) R.drawable.ic_sensors else R.drawable.ic_explore,
                    ),
                    title = stringResource(
                        if (sensorBlocked) R.string.sensors_blocked_title else R.string.compass_unavailable,
                    ),
                    message = stringResource(
                        if (sensorBlocked) R.string.sensors_blocked_message else R.string.compass_unavailable_message,
                    ),
                )
                return@ScreenScaffold
            }

            // When auto-hide is enabled and the magnetic accuracy is low or
            // unreliable, the numbers and source details are hidden so the
            // screen never shows a misleading confident reading.
            val lowAccuracy = magneticField.accuracy == AccuracyLevel.LOW ||
                magneticField.accuracy == AccuracyLevel.UNRELIABLE
            val hideNumbers = settings.autoHideDetailsOnLowAccuracy && lowAccuracy

            if (settings.showHeadingReadout && !hideNumbers) {
                HeadingReadout(heading = heading)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (settings.showNorthReferenceBadge) {
                NorthReferenceBadge(heading = heading)
                Spacer(modifier = Modifier.height(8.dp))
            }

            CompassDial(
                headingDegrees = heading.degrees,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentDescription = stringResource(
                    R.string.compass_heading_desc,
                    heading.degrees.toInt(),
                ),
                tickColor = MaterialTheme.colorScheme.onSurface,
                labelColor = MaterialTheme.colorScheme.onSurface,
                markerColor = MaterialTheme.colorScheme.error,
                dialColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                style = settings.compassStyle,
                showCardinalLabels = settings.showCardinalLabels,
                showDegreeTicks = settings.showDegreeTicks,
                showDegreeNumbers = settings.showDegreeNumbers && !hideNumbers,
                qiblaBearingDegrees = if (settings.qiblaEnabled && settings.showQiblaOnCompass) {
                    qiblaState.bearingInDeviceReferenceDegrees
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            heading.trueNorthUnavailableReason?.let { reason ->
                TrueNorthBanner(reason = reason, onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                })
            }

            if (magneticField.interference) {
                InterferenceBanner(
                    title = stringResource(R.string.compass_interference_detected),
                    message = stringResource(R.string.compass_interference_message),
                )
            }

            if (settings.qiblaEnabled && settings.showQiblaCard) {
                QiblaCard(
                    state = qiblaState,
                    showDistance = settings.showQiblaDistance,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (settings.showAccuracyPanel) {
                // Bottom details in a closed card; the µT value renders in a
                // fixed-width slot (invisible widest-value placeholder) so its
                // digits changing never shift the accuracy pill to the right.
                DataCard {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // Magnetic field strength.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.compass_field_strength),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Box {
                                    Text(
                                        stringResource(R.string.compass_field_microtesla, "888.8"),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.Transparent,
                                        maxLines = 1,
                                    )
                                    Text(
                                        stringResource(
                                            R.string.compass_field_microtesla,
                                            formatFloat(magneticField.magnitudeMicroTesla),
                                        ),
                                        style = MaterialTheme.typography.headlineMedium,
                                        maxLines = 1,
                                    )
                                }
                            }
                            StatusPill(
                                available = magneticField.accuracy != AccuracyLevel.UNRELIABLE,
                                label = stringResource(accuracyLabel(magneticField.accuracy)),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Accuracy + calibration status.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.accuracy_label, stringResource(accuracyLabel(magneticField.accuracy))),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = viewModel::resetCalibration) {
                                Text(stringResource(R.string.reset))
                            }
                        }
                        Text(
                            text = if (liveCalibration.isCalibrated) {
                                stringResource(R.string.compass_calibration_status, stringResource(R.string.compass_calibration_complete))
                            } else {
                                stringResource(
                                    R.string.compass_calibration_status,
                                    stringResource(
                                        R.string.compass_calibration_in_progress,
                                        (liveCalibration.coverage * 100).toInt(),
                                    ),
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (magneticField.accuracy == AccuracyLevel.LOW || magneticField.accuracy == AccuracyLevel.UNRELIABLE) {
                            Text(
                                text = stringResource(R.string.accuracy_calibration_recommended),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            if (settings.showCompassDetails && !hideNumbers) {
                // Source/declination details in a matching closed card.
                DataCard {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.compass_source, sourceLabel(heading.source)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (heading.mode == HeadingMode.TRUE && heading.declinationDegrees != null) {
                            Text(
                                text = stringResource(R.string.compass_declination, formatFloat(heading.declinationDegrees!!)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * The heading readout inside a closed card. The degree number renders in a
 * fixed-width slot sized by an invisible placeholder (the widest value,
 * "888°"), so the digits changing as the compass moves never re-measure
 * and shift the text — it stays fixed in place no matter the heading.
 */
@Composable
private fun HeadingReadout(heading: Heading) {
    DataCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Invisible placeholder sizes the slot to the widest possible
                // value, keeping the real readout fixed-width and centered.
                Text(
                    text = "888°",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.Transparent,
                    maxLines = 1,
                )
                Text(
                    text = "${heading.degrees.toInt()}°",
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Text(
                text = heading.cardinal.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                // Live region: TalkBack announces the heading only when the cardinal
                // direction changes (its text only changes then), never at sensor rate.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun NorthReferenceBadge(heading: Heading) {
    val label = when (heading.requestedNorthReference) {
        NorthReference.AUTOMATIC -> stringResource(
            R.string.compass_north_reference_automatic_using,
            stringResource(
                if (heading.effectiveNorthReference == NorthReference.TRUE_NORTH) {
                    R.string.compass_true_north
                } else {
                    R.string.compass_magnetic_north
                },
            ),
        )

        NorthReference.TRUE_NORTH -> stringResource(R.string.compass_true_north)
        NorthReference.MAGNETIC_NORTH -> stringResource(R.string.compass_magnetic_north)
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun QiblaCard(
    state: QiblaState,
    showDistance: Boolean,
    onRequestPermission: () -> Unit,
) {
    val message: String
    val showPermissionButton = state.status == QiblaStatus.LOCATION_PERMISSION_REQUIRED
    message = when (state.status) {
        QiblaStatus.QIBLA_DISABLED -> return
        QiblaStatus.LOCATION_PERMISSION_REQUIRED -> stringResource(R.string.qibla_location_permission_required)
        QiblaStatus.LOCATION_UNAVAILABLE -> stringResource(R.string.qibla_location_unavailable)
        QiblaStatus.CALCULATING -> stringResource(R.string.qibla_calculating)
        QiblaStatus.COMPASS_UNAVAILABLE -> stringResource(R.string.qibla_compass_unavailable)
        QiblaStatus.COMPASS_ACCURACY_LOW -> stringResource(R.string.qibla_compass_accuracy_low)
        QiblaStatus.LOCATION_ACCURACY_LOW -> stringResource(R.string.qibla_location_accuracy_low)
        QiblaStatus.CALIBRATION_REQUIRED -> stringResource(R.string.qibla_calibration_required)
        QiblaStatus.READY, QiblaStatus.ALIGNED -> ""
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.qibla_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (state.isReady || state.status == QiblaStatus.COMPASS_ACCURACY_LOW ||
                state.status == QiblaStatus.LOCATION_ACCURACY_LOW || state.status == QiblaStatus.CALIBRATION_REQUIRED
            ) {
                state.bearingDegrees?.let {
                    Text(
                        text = "${it.toInt()}°",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                val relative = state.relativeQiblaDegrees
                Text(
                    text = when {
                        state.alignment == QiblaAlignment.ALIGNED -> stringResource(R.string.qibla_aligned)
                        relative != null && relative > 0f ->
                            stringResource(R.string.qibla_turn_right, formatFloat(relative))

                        relative != null -> stringResource(R.string.qibla_turn_left, formatFloat(-relative))
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (showDistance && state.distanceKm != null) {
                    Text(
                        text = stringResource(
                            R.string.qibla_distance_to_kaaba,
                            String.format(Locale.US, "%,.0f", state.distanceKm),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                // Solar position: when the sun is (nearly) aligned with the
                // Qibla bearing, shadows point exactly away from Qibla — a
                // compass-free verification, immune to magnetic interference.
                state.sunAzimuthDegrees?.let { sunAzimuth ->
                    Text(
                        text = stringResource(
                            R.string.qibla_sun_position,
                            formatFloat(sunAzimuth),
                            formatFloat(state.sunElevationDegrees ?: 0f),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    val sunElevation = state.sunElevationDegrees ?: 0f
                    val qiblaBearing = state.bearingDegrees
                    if (sunElevation > 0f && qiblaBearing != null &&
                        AngleMath.angularDistance(qiblaBearing, sunAzimuth) <= SUN_QIBLA_ALIGNMENT_DEGREES
                    ) {
                        Text(
                            text = stringResource(R.string.qibla_sun_aligned),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                // Location accuracy and compass accuracy are reported separately.
                Text(
                    text = stringResource(
                        R.string.qibla_location_accuracy,
                        stringResource(locationAccuracyLabel(state.locationAccuracy)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(
                        R.string.qibla_compass_accuracy,
                        stringResource(accuracyLabel(state.compassAccuracy ?: AccuracyLevel.UNRELIABLE)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (showPermissionButton) {
                    Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.qibla_grant_permission))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrueNorthBanner(reason: TrueNorthUnavailableReason, onRequestPermission: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.compass_true_north_unavailable),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = when (reason) {
                    TrueNorthUnavailableReason.PERMISSION_DENIED ->
                        stringResource(R.string.compass_location_permission_denied)

                    TrueNorthUnavailableReason.LOCATION_UNKNOWN ->
                        stringResource(R.string.compass_location_unknown)

                    TrueNorthUnavailableReason.LOCATION_REQUIRED ->
                        stringResource(R.string.compass_location_required)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (reason == TrueNorthUnavailableReason.PERMISSION_DENIED) {
                Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.compass_location_required))
                }
            }
        }
    }
}

@Composable
private fun InterferenceBanner(title: String, message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** Sun azimuth tolerance for the "sun aligned with Qibla" hint. */
private const val SUN_QIBLA_ALIGNMENT_DEGREES = 3f

private fun accuracyLabel(accuracy: AccuracyLevel): Int = when (accuracy) {
    AccuracyLevel.HIGH -> R.string.accuracy_high
    AccuracyLevel.MEDIUM -> R.string.accuracy_medium
    AccuracyLevel.LOW -> R.string.accuracy_low
    AccuracyLevel.UNRELIABLE -> R.string.accuracy_unreliable
}

private fun locationAccuracyLabel(accuracy: com.nexasense.domain.model.LocationAccuracyLevel?): Int =
    when (accuracy) {
        com.nexasense.domain.model.LocationAccuracyLevel.HIGH -> R.string.accuracy_high
        com.nexasense.domain.model.LocationAccuracyLevel.MEDIUM -> R.string.accuracy_medium
        com.nexasense.domain.model.LocationAccuracyLevel.LOW -> R.string.accuracy_low
        null -> R.string.accuracy_unreliable
    }

private fun sourceLabel(source: HeadingSource): Int = when (source) {
    HeadingSource.ROTATION_VECTOR -> R.string.compass_source_rotation_vector
    HeadingSource.GEOMAGNETIC_ROTATION_VECTOR -> R.string.compass_source_geomagnetic_rotation_vector
    HeadingSource.ACCELEROMETER_MAGNETOMETER -> R.string.compass_source_accel_mag
    HeadingSource.UNAVAILABLE -> R.string.not_available
}

private fun formatFloat(value: Float): String = String.format(Locale.US, "%.1f", value)
