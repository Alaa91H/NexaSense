package com.nexasense.domain.math

import org.junit.Assert.assertEquals
import org.junit.Test

class AngleMathTest {

    private val eps = 1e-3f

    @Test
    fun `normalizeTo360 keeps valid headings`() {
        assertEquals(0f, AngleMath.normalizeTo360(0f), eps)
        assertEquals(90f, AngleMath.normalizeTo360(90f), eps)
        assertEquals(180f, AngleMath.normalizeTo360(180f), eps)
        assertEquals(270f, AngleMath.normalizeTo360(270f), eps)
        assertEquals(359.9f, AngleMath.normalizeTo360(359.9f), eps)
    }

    @Test
    fun `normalizeTo360 wraps positive and negative angles`() {
        assertEquals(0f, AngleMath.normalizeTo360(360f), eps)
        assertEquals(0f, AngleMath.normalizeTo360(720f), eps)
        assertEquals(0f, AngleMath.normalizeTo360(-360f), eps)
        assertEquals(270f, AngleMath.normalizeTo360(-90f), eps)
        assertEquals(350f, AngleMath.normalizeTo360(-10f), eps)
        assertEquals(10f, AngleMath.normalizeTo360(370f), eps)
    }

    @Test
    fun `normalizeTo360 handles NaN and infinity`() {
        assertEquals(0f, AngleMath.normalizeTo360(Float.NaN), 0f)
        assertEquals(0f, AngleMath.normalizeTo360(Float.POSITIVE_INFINITY), 0f)
        assertEquals(0f, AngleMath.normalizeTo360(Float.NEGATIVE_INFINITY), 0f)
    }

    @Test
    fun `normalizeTo180 wraps into the signed range`() {
        assertEquals(180f, AngleMath.normalizeTo180(180f), eps)
        assertEquals(-170f, AngleMath.normalizeTo180(190f), eps)
        assertEquals(170f, AngleMath.normalizeTo180(-190f), eps)
        assertEquals(-90f, AngleMath.normalizeTo180(270f), eps)
        assertEquals(90f, AngleMath.normalizeTo180(-270f), eps)
    }

    @Test
    fun `angularDifference takes the short way`() {
        assertEquals(10f, AngleMath.angularDifference(5f, 355f), eps)
        assertEquals(-10f, AngleMath.angularDifference(355f, 5f), eps)
        assertEquals(180f, AngleMath.angularDifference(180f, 0f), eps)
        assertEquals(-179f, AngleMath.angularDifference(181f, 0f), eps)
    }

    @Test
    fun `lerpDegrees interpolates across the wrap boundary`() {
        assertEquals(0f, AngleMath.lerpDegrees(355f, 5f, 0.5f), eps)
        assertEquals(357.5f, AngleMath.lerpDegrees(355f, 5f, 0.25f), eps)
        assertEquals(2.5f, AngleMath.lerpDegrees(355f, 5f, 0.75f), eps)
        assertEquals(90f, AngleMath.lerpDegrees(0f, 180f, 0.5f), eps)
    }

    @Test
    fun `angularDistance is symmetric`() {
        assertEquals(10f, AngleMath.angularDistance(5f, 355f), eps)
        assertEquals(10f, AngleMath.angularDistance(355f, 5f), eps)
        assertEquals(2f, AngleMath.angularDistance(1f, 359f), eps)
    }
}
