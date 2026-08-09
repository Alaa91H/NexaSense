package com.nexasense.domain.math

import org.junit.Assert.assertEquals
import org.junit.Test

class AngleMathAliasesTest {

    private val eps = 1e-3f

    @Test
    fun `normalizeDegrees wraps into 0 to 360`() {
        assertEquals(0f, AngleMath.normalizeDegrees(360f), eps)
        assertEquals(1f, AngleMath.normalizeDegrees(361f), eps)
        assertEquals(0f, AngleMath.normalizeDegrees(720f), eps)
        assertEquals(350f, AngleMath.normalizeDegrees(-10f), eps)
        assertEquals(90f, AngleMath.normalizeDegrees(-270f), eps)
        assertEquals(359.9f, AngleMath.normalizeDegrees(359.9f), eps)
        assertEquals(0f, AngleMath.normalizeDegrees(0f), eps)
    }

    @Test
    fun `normalizeSignedDegrees wraps into -180 to 180`() {
        assertEquals(0f, AngleMath.normalizeSignedDegrees(360f), eps)
        assertEquals(-170f, AngleMath.normalizeSignedDegrees(190f), eps)
        assertEquals(170f, AngleMath.normalizeSignedDegrees(-190f), eps)
        // The signed range is (-180, 180], so -180 normalizes to +180.
        assertEquals(180f, AngleMath.normalizeSignedDegrees(180f), eps)
        assertEquals(180f, AngleMath.normalizeSignedDegrees(-180f), eps)
    }

    @Test
    fun `shortestAngularDifference returns the short way`() {
        assertEquals(20f, AngleMath.shortestAngularDifference(10f, 350f), eps)
        assertEquals(-20f, AngleMath.shortestAngularDifference(350f, 10f), eps)
        assertEquals(0f, AngleMath.shortestAngularDifference(0f, 0f), eps)
        assertEquals(180f, AngleMath.shortestAngularDifference(180f, 0f), eps)
        // Both directions normalize to +180 in the (-180, 180] convention.
        assertEquals(180f, AngleMath.shortestAngularDifference(0f, 180f), eps)
    }

    @Test
    fun `aliases delegate to the canonical implementations`() {
        assertEquals(AngleMath.normalizeTo360(-10f), AngleMath.normalizeDegrees(-10f), 0f)
        assertEquals(AngleMath.normalizeTo180(190f), AngleMath.normalizeSignedDegrees(190f), 0f)
        assertEquals(AngleMath.angularDifference(10f, 350f), AngleMath.shortestAngularDifference(10f, 350f), 0f)
    }
}
