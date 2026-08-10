package com.nexasense.presentation.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.components.EngineLifecycleEffect
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.StatusPill
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

@Composable
fun LevelScreen(
    container: AppContainer,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: LevelViewModel = viewModel(initializer = {
        LevelViewModel(
            levelEngine = container.levelEngine,
            calibrationStore = container.calibrationStore,
            settingsStore = container.settingsStore,
        )
    })
    val orientation by viewModel.orientation.collectAsStateWithLifecycle()
    val isAvailable by viewModel.isAvailable.collectAsStateWithLifecycle()
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val hapticTick by viewModel.hapticTick.collectAsStateWithLifecycle()
    val sensorBlocked by viewModel.sensorBlocked.collectAsStateWithLifecycle()
    val verticalMode by viewModel.verticalMode.collectAsStateWithLifecycle()

    EngineLifecycleEffect(active = true, onStateChanged = viewModel::setActive)

    // One short pulse when the bubble enters the centered zone.
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(hapticTick) {
        if (hapticTick > 0) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Keep pitch/roll in the user's frame of reference across display rotations.
    val view = LocalView.current
    val rotationDegrees = (view.display?.rotation ?: 0) * 90
    LaunchedEffect(rotationDegrees) {
        viewModel.setDisplayRotation(rotationDegrees)
    }

    var showCalibrationDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.level_title),
        onBack = onBack,
    ) {
        if (!isAvailable) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(
                        if (sensorBlocked) R.string.sensors_blocked_title else R.string.level_unavailable,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(
                        if (sensorBlocked) R.string.sensors_blocked_message else R.string.level_unavailable_message,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@ScreenScaffold
        }

        // In vertical (plumb) mode the pitch is re-based to the deviation from
        // upright: the tube bubble centers when the device is exactly vertical.
        val pitchDeviation = if (verticalMode) {
            if (orientation.pitch >= 0f) orientation.pitch - 90f else orientation.pitch + 90f
        } else {
            orientation.pitch
        }
        val bubbleDescription = stringResource(
            R.string.level_bubble_desc,
            formatAngle(pitchDeviation),
            formatAngle(orientation.roll),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (verticalMode) {
                // Vertical mode: a water/mercury tube level with the bubble
                // moving left-right only (roll), for checking plumb.
                TubeLevel(
                    roll = orientation.roll,
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .semantics {
                            contentDescription = bubbleDescription
                        },
                )
            } else {
                // Horizontal mode: the two-axis bubble (four directions).
                BubbleLevel(
                    pitch = orientation.pitch,
                    roll = orientation.roll,
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .semantics {
                            contentDescription = bubbleDescription
                        },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AngleReadout(
                    label = stringResource(
                        if (verticalMode) R.string.level_vertical_deviation else R.string.level_pitch,
                    ),
                    value = pitchDeviation,
                )
                AngleReadout(
                    label = stringResource(R.string.level_roll),
                    value = orientation.roll,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatusPill(
                available = calibration.isSet,
                label = stringResource(
                    if (calibration.isSet) R.string.level_calibrated_label else R.string.level_not_calibrated_label,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showCalibrationDialog = true }) {
                    Text(stringResource(R.string.level_calibrate))
                }
                TextButton(onClick = viewModel::resetCalibration) {
                    Text(stringResource(R.string.level_reset_calibration))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCalibrationDialog) {
        LevelCalibrationDialog(
            onSetZero = {
                viewModel.setZero()
                showCalibrationDialog = false
            },
            onDismiss = { showCalibrationDialog = false },
        )
    }
}

@Composable
private fun AngleReadout(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.level_degrees, formatAngle(value)),
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

/** Bubble level drawn on a canvas; the bubble follows pitch (y) and roll (x). */
@Composable
private fun BubbleLevel(
    pitch: Float,
    roll: Float,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) / 2f
        val center = this.center

        drawCircle(
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f),
            radius = radius,
        )
        drawCircle(
            color = surfaceColor.copy(alpha = 0.3f),
            radius = radius * 0.98f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
        )

        // Crosshair (four directions).
        drawLine(
            color = surfaceColor.copy(alpha = 0.3f),
            start = Offset(center.x - radius * 0.8f, center.y),
            end = Offset(center.x + radius * 0.8f, center.y),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = surfaceColor.copy(alpha = 0.3f),
            start = Offset(center.x, center.y - radius * 0.8f),
            end = Offset(center.x, center.y + radius * 0.8f),
            strokeWidth = 1.dp.toPx(),
        )

        // Center ring.
        drawCircle(
            color = primaryColor.copy(alpha = 0.6f),
            radius = 10.dp.toPx(),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
        )

        // Bubble: rolls right when the device rolls, pitches up when pitched.
        val maxOffset = radius * 0.55f
        val bubbleRadius = radius * 0.22f
        val x = (roll / 45f).coerceIn(-1f, 1f) * maxOffset
        val y = (-pitch / 45f).coerceIn(-1f, 1f) * maxOffset
        val isLevel = abs(x) < 0.03f * maxOffset && abs(y) < 0.03f * maxOffset

        drawCircle(
            color = if (isLevel) primaryColor else errorColor,
            radius = bubbleRadius,
            center = Offset(center.x + x, center.y + y),
        )
    }
}

/**
 * Vertical (plumb) mode: a water/mercury tube level. The bubble travels
 * left-right only, driven by roll — the tube is centered when the device is
 * held exactly upright with no side lean.
 */
@Composable
private fun TubeLevel(
    roll: Float,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = h / 2f

        // The vial: a horizontal capsule tube.
        val tubeHeight = h * 0.42f
        val tubeInset = w * 0.07f
        val tubeTop = cy - tubeHeight / 2f
        drawRoundRect(
            color = surfaceColor.copy(alpha = 0.16f),
            topLeft = Offset(tubeInset, tubeTop),
            size = Size(w - 2f * tubeInset, tubeHeight),
            cornerRadius = CornerRadius(tubeHeight / 2f),
        )
        drawRoundRect(
            color = surfaceColor.copy(alpha = 0.45f),
            topLeft = Offset(tubeInset, tubeTop),
            size = Size(w - 2f * tubeInset, tubeHeight),
            cornerRadius = CornerRadius(tubeHeight / 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
        )

        // Center reference marks (the "level" window in the middle).
        val tickHalf = tubeHeight * 0.34f
        val tickGap = 14.dp.toPx()
        drawLine(
            color = surfaceColor.copy(alpha = 0.7f),
            start = Offset(cy - tickGap, cy - tickHalf),
            end = Offset(cy - tickGap, cy + tickHalf),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = surfaceColor.copy(alpha = 0.7f),
            start = Offset(cy + tickGap, cy - tickHalf),
            end = Offset(cy + tickGap, cy + tickHalf),
            strokeWidth = 2.dp.toPx(),
        )

        // The mercury/bubble: moves left-right with roll.
        val bubbleRadius = tubeHeight * 0.26f
        val maxOffset = w / 2f - tubeInset - bubbleRadius - 6.dp.toPx()
        val x = (roll / 30f).coerceIn(-1f, 1f) * maxOffset
        val centered = abs(x) < tickGap * 0.6f

        drawCircle(
            color = if (centered) primaryColor else errorColor,
            radius = bubbleRadius,
            center = Offset(cy + x, cy),
        )
    }
}

@Composable
private fun LevelCalibrationDialog(
    onSetZero: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.level_calibrate_title)) },
        text = { Text(stringResource(R.string.level_calibrate_instructions)) },
        confirmButton = {
            Button(onClick = onSetZero) {
                Text(stringResource(R.string.level_set_zero))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun formatAngle(value: Float): String =
    String.format(Locale.US, "%.1f", value)
