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
import androidx.compose.ui.geometry.Offset
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
    onBack: () -> Unit,
) {
    val viewModel: LevelViewModel = viewModel(initializer = {
        LevelViewModel(
            levelEngine = container.levelEngine,
            calibrationStore = container.calibrationStore,
        )
    })
    val orientation by viewModel.orientation.collectAsStateWithLifecycle()
    val isAvailable by viewModel.isAvailable.collectAsStateWithLifecycle()
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()

    EngineLifecycleEffect(active = true, onStateChanged = viewModel::setActive)

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
                    stringResource(R.string.level_unavailable),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(R.string.level_unavailable_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@ScreenScaffold
        }

        val bubbleDescription = stringResource(
            R.string.level_bubble_desc,
            formatAngle(orientation.pitch),
            formatAngle(orientation.roll),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AngleReadout(
                    label = stringResource(R.string.level_pitch),
                    value = orientation.pitch,
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

        // Crosshair.
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
