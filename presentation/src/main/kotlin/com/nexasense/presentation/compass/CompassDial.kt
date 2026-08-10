package com.nexasense.presentation.compass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexasense.domain.model.CompassStyle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A large compass dial with a fixed top marker. The dial rotates opposite to
 * the heading so the N label always points at the measured direction.
 *
 * The dial ring is drawn at 80% of the canvas so there is room for the Qibla
 * Kaaba marker, which sits just outside the ring at the Qibla bearing.
 *
 * Three professional styles are supported (see [CompassStyle]): classic
 * (2° minor + 30° major ticks, 8-point cardinal labels), azimuth (numbered
 * degree dial, military/aviation style) and minimal (clean, only the 4 main
 * cardinal points). The granular flags [showCardinalLabels], [showDegreeTicks]
 * and [showDegreeNumbers] fine-tune what each style draws.
 */
@Composable
fun CompassDial(
    headingDegrees: Float,
    modifier: Modifier = Modifier,
    contentDescription: String,
    tickColor: Color,
    labelColor: Color,
    markerColor: Color,
    dialColor: Color,
    style: CompassStyle = CompassStyle.CLASSIC,
    showCardinalLabels: Boolean = true,
    showDegreeTicks: Boolean = true,
    showDegreeNumbers: Boolean = false,
    qiblaBearingDegrees: Float? = null,
) {
    val animatedHeading by animateFloatAsState(
        targetValue = headingDegrees,
        animationSpec = tween(durationMillis = 180),
        label = "compassHeading",
    )
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember(labelColor) { TextStyle(color = labelColor, fontSize = 18.sp) }
    val numberStyle = remember(labelColor) {
        TextStyle(color = labelColor.copy(alpha = 0.85f), fontSize = 13.sp)
    }
    val labels = remember(style) {
        when (style) {
            CompassStyle.CLASSIC -> cardinalLabels8
            CompassStyle.AZIMUTH, CompassStyle.MINIMAL -> cardinalLabels4
        }
    }
    // Measure the (static) labels once. Measuring text inside the Canvas draw
    // lambda would re-run measures on every animation frame.
    val measuredLabels = remember(textMeasurer, labelStyle, labels) {
        labels.associate { (degree, label) -> degree to textMeasurer.measure(label, labelStyle) }
    }
    val measuredNumbers = remember(textMeasurer, numberStyle) {
        degreeNumbers.associate { (degree, label) -> degree to textMeasurer.measure(label, numberStyle) }
    }

    Canvas(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = this.center
        // The dial ring is 80% of the canvas so the Qibla Kaaba fits outside.
        val dialRadius = radius * 0.80f

        drawCircle(color = dialColor, radius = dialRadius)
        drawCircle(
            color = tickColor.copy(alpha = 0.35f),
            radius = dialRadius * 0.97f,
            style = Stroke(width = 1.dp.toPx()),
        )

        rotate(degrees = -animatedHeading, pivot = center) {
            if (showDegreeTicks) {
                drawTicks(center, dialRadius, tickColor, style)
            }
            // The azimuth style is defined by its numbered ring; the other
            // styles show numbers only when explicitly enabled.
            if (style == CompassStyle.AZIMUTH || showDegreeNumbers) {
                degreeNumbers.forEach { (degree, _) ->
                    val position = polar(center, dialRadius * numberRadius(style), degree.toFloat())
                    rotate(degrees = animatedHeading, pivot = position) {
                        val measured = measuredNumbers[degree] ?: return@rotate
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                position.x - measured.size.width / 2f,
                                position.y - measured.size.height / 2f,
                            ),
                        )
                    }
                }
            }
            if (showCardinalLabels) {
                labels.forEach { (degree, _) ->
                    val position = polar(center, dialRadius * labelRadius(style), degree.toFloat())
                    rotate(degrees = animatedHeading, pivot = position) {
                        val measured = measuredLabels[degree] ?: return@rotate
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                position.x - measured.size.width / 2f,
                                position.y - measured.size.height / 2f,
                            ),
                        )
                    }
                }
            }
        }

        // Fixed marker showing the device heading at the top.
        val marker = Path().apply {
            moveTo(center.x - 10.dp.toPx(), center.y - dialRadius * 0.78f)
            lineTo(center.x + 10.dp.toPx(), center.y - dialRadius * 0.78f)
            lineTo(center.x, center.y - dialRadius * 0.66f)
            close()
        }
        drawPath(path = marker, color = markerColor)

        // Center hub.
        drawCircle(color = markerColor, radius = 5.dp.toPx(), center = center)

        // Qibla marker: the Kaaba sits just outside the dial ring at the
        // bearing, drawn inside the rotating frame so it stays at the correct
        // position relative to the (rotating) dial. The bearing must already
        // be in the heading's north reference.
        qiblaBearingDegrees?.let { bearing ->
            rotate(degrees = -animatedHeading, pivot = center) {
                val position = polar(center, dialRadius * 1.16f, bearing)
                drawKaaba(center = position, size = 22.dp.toPx())
            }
        }
    }
}

/**
 * Draws a professional Kaaba: the black cube with its gold kiswah band and
 * door, used as the Qibla marker outside the dial ring. Fixed brand colors —
 * the Kaaba is black with gold in reality, independent of the app theme.
 */
private fun DrawScope.drawKaaba(center: Offset, size: Float) {
    val kaabaBlack = Color(0xFF1B1C20)
    val gold = Color(0xFFD4AF37)
    val goldSoft = Color(0xFFC9A227)
    val half = size / 2f

    // Roof parapet: a slightly wider gold line on top.
    drawRoundRect(
        color = gold,
        topLeft = Offset(center.x - half * 1.05f, center.y - half * 0.66f),
        size = Size(size * 1.10f, size * 0.14f),
        cornerRadius = CornerRadius(size * 0.05f),
    )
    // The cube body (slightly taller than wide, like the real Kaaba).
    drawRoundRect(
        color = kaabaBlack,
        topLeft = Offset(center.x - half * 0.72f, center.y - half * 0.52f),
        size = Size(size * 1.44f, size * 1.14f),
        cornerRadius = CornerRadius(size * 0.06f),
    )
    // The kiswah band (hizam) wrapping the upper third.
    drawRoundRect(
        color = gold,
        topLeft = Offset(center.x - half * 0.72f, center.y - half * 0.10f),
        size = Size(size * 1.44f, size * 0.20f),
        cornerRadius = CornerRadius(size * 0.04f),
    )
    // A thin second gold line below the band.
    drawRoundRect(
        color = goldSoft,
        topLeft = Offset(center.x - half * 0.72f, center.y + half * 0.14f),
        size = Size(size * 1.44f, size * 0.06f),
        cornerRadius = CornerRadius(size * 0.02f),
    )
    // The door (Bab al-Kaaba).
    drawRoundRect(
        color = gold,
        topLeft = Offset(center.x - half * 0.20f, center.y + half * 0.26f),
        size = Size(size * 0.40f, size * 0.30f),
        cornerRadius = CornerRadius(size * 0.05f),
    )
}

private fun DrawScope.drawTicks(center: Offset, radius: Float, tickColor: Color, style: CompassStyle) {
    val minorStep = when (style) {
        CompassStyle.CLASSIC -> 2
        CompassStyle.AZIMUTH -> 5
        CompassStyle.MINIMAL -> 30
    }
    for (degree in 0 until 360 step minorStep) {
        val major = degree % 30 == 0
        val outer = radius * if (major) 0.90f else 0.96f
        val inner = radius * if (major) 0.78f else 0.905f
        drawLine(
            color = if (major) tickColor else tickColor.copy(alpha = 0.45f),
            start = polar(center, outer, degree.toFloat()),
            end = polar(center, inner, degree.toFloat()),
            strokeWidth = if (major) 2.5.dp.toPx() else 1.dp.toPx(),
        )
    }
}

private fun labelRadius(style: CompassStyle): Float = when (style) {
    CompassStyle.CLASSIC -> 0.62f
    CompassStyle.AZIMUTH -> 0.44f
    CompassStyle.MINIMAL -> 0.70f
}

private fun numberRadius(style: CompassStyle): Float = when (style) {
    CompassStyle.CLASSIC -> 0.50f
    CompassStyle.AZIMUTH -> 0.64f
    CompassStyle.MINIMAL -> 0.50f
}

private fun DrawScope.polar(center: Offset, radius: Float, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    return Offset(
        x = center.x + radius * cos(radians).toFloat(),
        y = center.y + radius * sin(radians).toFloat(),
    )
}

private val cardinalLabels8: List<Pair<Int, String>> = listOf(
    0 to "N",
    45 to "NE",
    90 to "E",
    135 to "SE",
    180 to "S",
    225 to "SW",
    270 to "W",
    315 to "NW",
)

private val cardinalLabels4: List<Pair<Int, String>> = listOf(
    0 to "N",
    90 to "E",
    180 to "S",
    270 to "W",
)

private val degreeNumbers: List<Pair<Int, String>> =
    (0 until 360 step 30).map { it to it.toString() }
