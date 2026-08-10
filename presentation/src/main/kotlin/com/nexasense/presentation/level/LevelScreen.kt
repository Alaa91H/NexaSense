package com.nexasense.presentation.level

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.components.EngineLifecycleEffect
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.StatusPill
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
                // moving left-right only (roll), plus a plumb gauge whose
                // vertical reference line and needle show the deviation from
                // vertical (pitch) visually.
                TubeLevel(
                    roll = orientation.roll,
                    pitchDeviation = pitchDeviation,
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .aspectRatio(1.35f)
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

    // Same smooth glide as the vertical mode: animate the bubble's normalized
    // position so it drifts naturally instead of snapping at sensor rate.
    val animatedXFactor by animateFloatAsState(
        targetValue = (roll / 45f).coerceIn(-1f, 1f),
        animationSpec = tween(durationMillis = LEVEL_ANIMATION_MILLIS),
        label = "bubbleX",
    )
    val animatedYFactor by animateFloatAsState(
        targetValue = (-pitch / 45f).coerceIn(-1f, 1f),
        animationSpec = tween(durationMillis = LEVEL_ANIMATION_MILLIS),
        label = "bubbleY",
    )

    // The centered indicator breathes like the plumb dot: a slow, gentle
    // pulse on its glow while the device is level — the same threshold the
    // level's haptic uses, so the light and the vibration are always in sync.
    val pulse by rememberInfiniteTransition(label = "flatPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flatPulseValue",
    )

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

        // Bubble: rolls right when the device rolls, pitches up when pitched,
        // following the animated position so it glides smoothly.
        val maxOffset = radius * 0.55f
        val bubbleRadius = radius * 0.22f
        val x = animatedXFactor * maxOffset
        val y = animatedYFactor * maxOffset
        val isLevel = abs(x) < 0.03f * maxOffset && abs(y) < 0.03f * maxOffset

        drawCircle(
            color = if (isLevel) primaryColor else errorColor,
            radius = bubbleRadius,
            center = Offset(center.x + x, center.y + y),
        )

        // Perfect-level indicator at the pivot: a small dot that lights up
        // (filled primary + soft pulsing glow) exactly when the device is
        // level on both axes — the same threshold the haptic uses, so the
        // light and the vibration are always in sync. Drawn after the bubble
        // so it stays visible on top of it.
        val perfectlyLevel = abs(pitch) < LEVEL_CENTERED_THRESHOLD_DEGREES &&
            abs(roll) < LEVEL_CENTERED_THRESHOLD_DEGREES
        if (perfectlyLevel) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.14f + 0.22f * pulse),
                radius = (9.dp + 6.dp * pulse).toPx(),
                center = center,
            )
        }
        drawCircle(
            color = if (perfectlyLevel) primaryColor else surfaceColor.copy(alpha = 0.55f),
            radius = 4.5.dp.toPx(),
            center = center,
            style = if (perfectlyLevel) {
                androidx.compose.ui.graphics.drawscope.Fill
            } else {
                androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            },
        )
    }
}

/**
 * Vertical (plumb) mode: a water/mercury tube level on top, plus a plumb
 * gauge below. The tube's bubble travels left-right only, driven by roll;
 * the gauge's needle rotates away from a dashed vertical reference line by
 * the deviation-from-vertical (pitch) angle — 0° is exactly plumb.
 */
@Composable
private fun TubeLevel(
    roll: Float,
    pitchDeviation: Float,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val textMeasurer = rememberTextMeasurer()
    val tickLabelStyle = remember(surfaceColor) {
        TextStyle(color = surfaceColor.copy(alpha = 0.7f), fontSize = 11.sp)
    }
    // Measured once, outside the draw lambda (same convention as CompassDial).
    val measuredTickLabels = remember(textMeasurer, tickLabelStyle) {
        listOf(10, 20, 30, 40).associateWith { degree ->
            textMeasurer.measure(degree.toString(), tickLabelStyle)
        }
    }

    // The raw sensor values arrive at up to 50 Hz (GAME rate) and jump
    // sample to sample, which makes the mercury bubble and the plumb needle
    // snap between positions. Animate both with the same short tween so the
    // whole vertical mode glides smoothly, like a real liquid, instead of
    // jumping.
    val animatedBubbleFactor by animateFloatAsState(
        targetValue = (roll / 30f).coerceIn(-1f, 1f),
        animationSpec = tween(durationMillis = LEVEL_ANIMATION_MILLIS),
        label = "tubeBubble",
    )
    val animatedNeedleDegrees by animateFloatAsState(
        targetValue = pitchDeviation.coerceIn(-50f, 50f),
        animationSpec = tween(durationMillis = LEVEL_ANIMATION_MILLIS),
        label = "plumbNeedle",
    )

    // The perfect-plumb indicator breathes: a slow, gentle pulse on its glow
    // (radius + alpha) so the centered state is unmistakable at a glance.
    // The pulse value is only consumed while plumb, so it has no effect
    // otherwise.
    val pulse by rememberInfiniteTransition(label = "plumbPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "plumbPulseValue",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // ---------- Horizontal mercury tube (roll, left-right) ----------
        val tubeHeight = h * 0.24f
        val tubeCenterY = h * 0.20f
        val tubeInset = w * 0.07f
        val tubeTop = tubeCenterY - tubeHeight / 2f
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

        // Center reference marks (the "level" window), symmetric around the
        // tube's horizontal center.
        val tickHalf = tubeHeight * 0.34f
        val tickGap = 14.dp.toPx()
        drawLine(
            color = surfaceColor.copy(alpha = 0.7f),
            start = Offset(cx - tickGap, tubeCenterY - tickHalf),
            end = Offset(cx - tickGap, tubeCenterY + tickHalf),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = surfaceColor.copy(alpha = 0.7f),
            start = Offset(cx + tickGap, tubeCenterY - tickHalf),
            end = Offset(cx + tickGap, tubeCenterY + tickHalf),
            strokeWidth = 2.dp.toPx(),
        )

        // The mercury/bubble: moves left-right with the (animated) roll,
        // symmetric around the tube's horizontal center.
        val bubbleRadius = tubeHeight * 0.26f
        val maxOffset = w / 2f - tubeInset - bubbleRadius - 6.dp.toPx()
        val x = animatedBubbleFactor * maxOffset
        val centered = abs(x) < tickGap * 0.6f

        drawCircle(
            color = if (centered) primaryColor else errorColor,
            radius = bubbleRadius,
            center = Offset(cx + x, tubeCenterY),
        )

        // ---------- Plumb gauge: dashed vertical reference + needle ----------
        val pivot = Offset(cx, h * 0.56f)
        val needleLength = h * 0.30f

        // The 0° reference: a dashed vertical line through the pivot.
        drawLine(
            color = surfaceColor.copy(alpha = 0.55f),
            start = pivot,
            end = Offset(pivot.x, pivot.y + needleLength),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
        )

        // Degree scale: ticks every 10° (major every 20°) with labels.
        val tickOuter = needleLength
        val tickInner = needleLength * 0.86f
        for (degree in -50..50 step 10) {
            if (degree == 0) continue
            val major = degree % 20 == 0
            drawLine(
                color = surfaceColor.copy(alpha = if (major) 0.6f else 0.35f),
                start = plumbPoint(pivot, tickInner, degree.toFloat()),
                end = plumbPoint(pivot, tickOuter, degree.toFloat()),
                strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
            )
            if (major) {
                val labelPosition = plumbPoint(pivot, needleLength * 1.06f, degree.toFloat())
                measuredTickLabels[abs(degree)]?.let { measured ->
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            labelPosition.x - measured.size.width / 2f,
                            labelPosition.y - measured.size.height / 2f,
                        ),
                    )
                }
            }
        }

        // The needle: rotates away from the reference by the (animated)
        // deviation from vertical. Green (primary) when plumb (within ±2°),
        // red otherwise.
        val aligned = abs(animatedNeedleDegrees) <= PLUMB_ALIGNED_DEGREES
        val needleColor = if (aligned) primaryColor else errorColor
        val needleEnd = plumbPoint(pivot, needleLength, animatedNeedleDegrees)
        drawLine(
            color = needleColor,
            start = pivot,
            end = needleEnd,
            strokeWidth = 3.dp.toPx(),
        )
        drawCircle(color = needleColor, radius = 5.dp.toPx(), center = needleEnd)

        // Perfect-plumb indicator on the reference line: a small dot at the
        // pivot that lights up (filled primary + soft glow) exactly when the
        // device is perfectly plumb — the same threshold the level's haptic
        // uses, so the light and the vibration are always in sync.
        val perfectlyPlumb = abs(pitchDeviation) < LEVEL_CENTERED_THRESHOLD_DEGREES &&
            abs(roll) < LEVEL_CENTERED_THRESHOLD_DEGREES
        if (perfectlyPlumb) {
            // Pulsing glow: radius and alpha breathe together, so the
            // centered state visibly throbs instead of sitting still.
            drawCircle(
                color = primaryColor.copy(alpha = 0.14f + 0.22f * pulse),
                radius = (9.dp + 6.dp * pulse).toPx(),
                center = pivot,
            )
        }
        drawCircle(
            color = if (perfectlyPlumb) primaryColor else surfaceColor.copy(alpha = 0.55f),
            radius = 4.5.dp.toPx(),
            center = pivot,
            style = if (perfectlyPlumb) {
                androidx.compose.ui.graphics.drawscope.Fill
            } else {
                androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            },
        )
    }
}

/** Duration of the level's smooth glide (ms) — bubbles and plumb needle. */
private const val LEVEL_ANIMATION_MILLIS = 180

/** Point at [degrees] clockwise from straight-down, [length] from [pivot]. */
private fun plumbPoint(pivot: Offset, length: Float, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    return Offset(
        x = pivot.x + length * sin(radians).toFloat(),
        y = pivot.y + length * cos(radians).toFloat(),
    )
}

/** Deviation within which the plumb needle reads as aligned. */
private const val PLUMB_ALIGNED_DEGREES = 2f

/**
 * Threshold for the perfect-level indicators (plumb dot and flat dot),
 * matching the level's haptic centered zone (both axes) so the light and
 * the vibration are in sync.
 */
private const val LEVEL_CENTERED_THRESHOLD_DEGREES = 1.5f

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
