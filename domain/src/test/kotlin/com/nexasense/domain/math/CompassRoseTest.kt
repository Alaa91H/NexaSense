package com.nexasense.domain.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the compass-rose convention used by the dial and the Qibla marker:
 * 0° = top, 90° = right, 180° = bottom, 270° = left, measured clockwise,
 * in PHYSICAL screen coordinates — identical in LTR and RTL layouts.
 *
 * Regression: the rose was previously drawn with 0° at the RIGHT (a 90°
 * clockwise rotation of the whole frame). The Kaaba then sat 90° clockwise
 * from its true bearing — e.g. a Qibla on the user's right rendered on the
 * left, and turning toward it visibly moved it away ("Qibla on the left,
 * tilt the device left → it moves away").
 */
class CompassRoseTest {

    private val cx = 100f
    private val cy = 100f
    private val r = 50f

    @Test fun `north 0 degrees is at the top`() {
        val (x, y) = AngleMath.roseOffset(cx, cy, r, 0f)
        assertEquals(cx, x, 1e-3f)
        assertEquals(cy - r, y, 1e-3f)
    }

    @Test fun `east 90 degrees is at the right`() {
        val (x, y) = AngleMath.roseOffset(cx, cy, r, 90f)
        assertEquals(cx + r, x, 1e-3f)
        assertEquals(cy, y, 1e-3f)
    }

    @Test fun `south 180 degrees is at the bottom`() {
        val (x, y) = AngleMath.roseOffset(cx, cy, r, 180f)
        assertEquals(cx, x, 1e-3f)
        assertEquals(cy + r, y, 1e-3f)
    }

    @Test fun `west 270 degrees is at the left`() {
        val (x, y) = AngleMath.roseOffset(cx, cy, r, 270f)
        assertEquals(cx - r, x, 1e-3f)
        assertEquals(cy, y, 1e-3f)
    }

    @Test fun `northeast 45 degrees is top-right`() {
        val (x, y) = AngleMath.roseOffset(cx, cy, r, 45f)
        assertTrue(x > cx)
        assertTrue(y < cy)
    }

    @Test fun `qibla to the right of the device renders on the right side`() {
        // bearing - heading = 90 → the Qibla is 90° clockwise from where the
        // device points → the marker must sit on the RIGHT of the dial.
        val (x, y) = AngleMath.roseOffset(0f, 0f, 1f, 90f)
        assertTrue(x > 0f)
        assertEquals(0f, y, 1e-3f)
    }

    @Test fun `turning toward the displayed qibla brings it to the top`() {
        // Marker angle = bearing - heading. Device points east (90°), Qibla
        // due west (270°): the marker renders at the bottom (behind).
        var heading = 90f
        val bearing = 270f
        val markerRose = AngleMath.normalizeTo360(bearing - heading)
        assertEquals(180f, markerRose, 1e-3f)

        // Turning RIGHT (heading increases, toward the west) must decrease
        // the marker's rose angle toward 0 — the top, where the device
        // points once aligned. Before the fix the marker sat 90° clockwise
        // of this, so a turn toward it visibly moved it away instead.
        heading = 180f
        val markerRoseAfter = AngleMath.normalizeTo360(bearing - heading)
        assertEquals(90f, markerRoseAfter, 1e-3f)
        assertTrue(markerRoseAfter < markerRose)
    }

    @Test fun `rose is physical - the dial must not mirror in RTL`() {
        // The helper has no layout-direction input: it returns physical
        // coordinates, so the dial and marker render identically in LTR and
        // RTL (a compass must not mirror).
        val (x, y) = AngleMath.roseOffset(cx, cy, r, 45f)
        assertEquals(cx + r * 0.70710677f, x, 1e-2f)
        assertEquals(cy - r * 0.70710677f, y, 1e-2f)
    }
}
