package com.nexasense.presentation.compass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A large compass dial: 2° ticks, 30° major ticks and cardinal labels, with a
 * fixed top marker. The dial rotates opposite to the heading so the N label
 * always points at the measured direction.
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
    qiblaBearingDegrees: Float? = null,
    qiblaColor: Color = com.nexasense.presentation.theme.NexaSenseColors.QiblaMarker,
) {
    val animatedHeading by animateFloatAsState(
        targetValue = headingDegrees,
        animationSpec = tween(durationMillis = 180),
        label = "compassHeading",
    )
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 18.sp)

    Canvas(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = this.center

        drawCircle(color = dialColor, radius = radius)
        drawCircle(
            color = tickColor.copy(alpha = 0.35f),
            radius = radius * 0.97f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )

        rotate(degrees = -animatedHeading, pivot = center) {
            for (degree in 0 until 360 step 2) {
                val major = degree % 30 == 0
                val angle = degree.toFloat()
                val outer = radius * if (major) 0.90f else 0.96f
                val inner = radius * if (major) 0.78f else 0.905f
                drawLine(
                    color = if (major) tickColor else tickColor.copy(alpha = 0.45f),
                    start = polar(center, outer, angle),
                    end = polar(center, inner, angle),
                    strokeWidth = if (major) 2.5.dp.toPx() else 1.dp.toPx(),
                )
            }
            cardinalLabels.forEach { (degree, label) ->
                val position = polar(center, radius * 0.62f, degree.toFloat())
                rotate(degrees = animatedHeading, pivot = position) {
                    val measured = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(
                            position.x - measured.size.width / 2f,
                            position.y - measured.size.height / 2f,
                        ),
                        style = labelStyle,
                    )
                }
            }
        }

        // Fixed marker showing the device heading at the top.
        val marker = Path().apply {
            moveTo(center.x - 10.dp.toPx(), center.y - radius * 0.78f)
            lineTo(center.x + 10.dp.toPx(), center.y - radius * 0.78f)
            lineTo(center.x, center.y - radius * 0.66f)
            close()
        }
        drawPath(path = marker, color = markerColor)

        // Center hub.
        drawCircle(color = markerColor, radius = 5.dp.toPx(), center = center)

        // Qibla marker: drawn inside the rotating frame at the bearing, so it
        // stays at the correct position relative to the (rotating) dial. The
        // bearing must already be in the heading's north reference.
        qiblaBearingDegrees?.let { bearing ->
            rotate(degrees = -animatedHeading, pivot = center) {
                val position = polar(center, radius * 0.72f, bearing)
                val qiblaMarker = Path().apply {
                    moveTo(position.x, position.y - 8.dp.toPx())
                    lineTo(position.x + 8.dp.toPx(), position.y + 8.dp.toPx())
                    lineTo(position.x - 8.dp.toPx(), position.y + 8.dp.toPx())
                    close()
                }
                drawPath(path = qiblaMarker, color = qiblaColor)
                drawCircle(color = qiblaColor, radius = 2.5.dp.toPx(), center = position)
            }
        }
    }
}

private fun DrawScope.polar(center: Offset, radius: Float, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    return Offset(
        x = center.x + radius * cos(radians).toFloat(),
        y = center.y + radius * sin(radians).toFloat(),
    )
}

private val cardinalLabels: List<Pair<Int, String>> = listOf(
    0 to "N",
    45 to "NE",
    90 to "E",
    135 to "SE",
    180 to "S",
    225 to "SW",
    270 to "W",
    315 to "NW",
)
