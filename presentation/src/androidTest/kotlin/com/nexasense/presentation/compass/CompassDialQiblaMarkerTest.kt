package com.nexasense.presentation.compass

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexasense.domain.engine.QiblaCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Instrumented UI test: the Kaaba marker on the compass dial must sit on the
 * same side as the turn hint.
 *
 * The hint ("turn right" / "turn left") is derived from
 * [QiblaCalculator.relativeQibla] — positive means the user must turn right.
 * The marker is placed at `bearing - heading` on the physical compass rose
 * (0° = top, clockwise, from [com.nexasense.domain.math.AngleMath.roseOffset]),
 * so a positive relative angle must place the marker to the RIGHT of the
 * dial center (physical pixels) and a negative one to the LEFT. Both the
 * hint and the marker are driven by the same relative angle, so this is the
 * invariant that keeps them consistent.
 *
 * The rose is layout-direction independent — a compass must not mirror — so
 * the geometry is asserted identically in LTR and RTL.
 */
@RunWith(AndroidJUnit4::class)
class CompassDialQiblaMarkerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dialDescription = "test-dial"
    private val kaabaEmoji = "\uD83D\uDD4B"

    private val dialSize = 320.dp

    private fun tolerancePx(): Float = with(composeRule.density) { (dialSize * 0.03f).toPx() }

    /** Renders the dial once and returns (dial bounds, marker bounds) in root px. */
    private fun measure(heading: Float, bearing: Float, rtl: Boolean): Pair<Rect, Rect> {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                CompassDial(
                    headingDegrees = heading,
                    modifier = Modifier.size(dialSize),
                    contentDescription = dialDescription,
                    tickColor = Color.Gray,
                    labelColor = Color.Gray,
                    markerColor = Color.Red,
                    dialColor = Color.LightGray,
                    qiblaBearingDegrees = bearing,
                )
            }
        }
        composeRule.waitForIdle()
        val dial = composeRule.onNodeWithContentDescription(dialDescription)
            .fetchSemanticsNode().boundsInRoot
        val marker = composeRule.onNodeWithText(kaabaEmoji)
            .fetchSemanticsNode().boundsInRoot
        return dial to marker
    }

    /**
     * Asserts the marker's screen side matches the sign of the relative angle
     * (the same value that produces the "turn right"/"turn left" hint).
     */
    private fun assertMarkerMatchesHint(heading: Float, bearing: Float, rtl: Boolean) {
        val relative = QiblaCalculator.relativeQibla(heading, bearing)
        val (dial, marker) = measure(heading, bearing, rtl)
        val dx = marker.center.x - dial.center.x
        val dy = marker.center.y - dial.center.y
        val tolerance = tolerancePx()

        // The marker always sits in the upper half of the dial for |relative| < 90°.
        assertTrue("marker should sit above the dial center (dy=$dy)", dy < 0f)

        when {
            relative > 2f -> assertTrue(
                "relative=$relative (turn right) but marker dx=$dx",
                dx > tolerance,
            )

            relative < -2f -> assertTrue(
                "relative=$relative (turn left) but marker dx=$dx",
                dx < -tolerance,
            )

            else -> assertTrue(
                "aligned (relative=$relative) but marker dx=$dx",
                abs(dx) <= tolerance,
            )
        }
    }

    @Test
    fun `turn right hint places the marker on the right side`() {
        // Bearing 30° ahead of heading 0° → relative +30° → "turn right".
        assertMarkerMatchesHint(heading = 0f, bearing = 30f, rtl = false)
    }

    @Test
    fun `turn left hint places the marker on the left side`() {
        // Bearing 330° is 30° behind heading 0° → relative −30° → "turn left".
        assertMarkerMatchesHint(heading = 0f, bearing = 330f, rtl = false)
    }

    @Test
    fun `aligned heading and bearing center the marker at the top`() {
        assertMarkerMatchesHint(heading = 0f, bearing = 0f, rtl = false)
    }

    @Test
    fun `marker side follows the hint at an arbitrary heading`() {
        // heading 120°, bearing 90° → relative −30° → marker on the left.
        assertMarkerMatchesHint(heading = 120f, bearing = 90f, rtl = false)
        // heading 120°, bearing 160° → relative +40° → marker on the right.
        assertMarkerMatchesHint(heading = 120f, bearing = 160f, rtl = false)
    }

    @Test
    fun `marker geometry is identical in LTR and RTL`() {
        val (ltrDial, ltrMarker) = measure(heading = 0f, bearing = 30f, rtl = false)
        val (rtlDial, rtlMarker) = measure(heading = 0f, bearing = 30f, rtl = true)

        assertEquals("dial must be centered identically in RTL", ltrDial.center.x, rtlDial.center.x, 1f)
        assertEquals("dial must be centered identically in RTL", ltrDial.center.y, rtlDial.center.y, 1f)
        // A compass must not mirror: the marker's physical pixel position is
        // exactly the same in both layout directions.
        assertEquals("marker x must not mirror in RTL", ltrMarker.center.x, rtlMarker.center.x, 1f)
        assertEquals("marker y must not mirror in RTL", ltrMarker.center.y, rtlMarker.center.y, 1f)
    }
}
