package com.nexasense.domain.engine

import com.nexasense.domain.model.CardinalDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class DeclinationEngineTest {

    private val eps = 1e-3f

    @Test
    fun `no declination means true equals magnetic`() {
        assertEquals(45f, DeclinationEngine.trueHeading(45f, 0f), eps)
    }

    @Test
    fun `east declination adds to the heading`() {
        assertEquals(10f, DeclinationEngine.trueHeading(0f, 10f), eps)
        assertEquals(100f, DeclinationEngine.trueHeading(90f, 10f), eps)
    }

    @Test
    fun `west declination subtracts from the heading`() {
        assertEquals(350f, DeclinationEngine.trueHeading(0f, -10f), eps)
        assertEquals(170f, DeclinationEngine.trueHeading(180f, -10f), eps)
    }

    @Test
    fun `wrap-around across 360`() {
        assertEquals(10f, DeclinationEngine.trueHeading(350f, 20f), eps)
        assertEquals(350f, DeclinationEngine.trueHeading(10f, -20f), eps)
        assertEquals(0f, DeclinationEngine.trueHeading(359f, 1f), eps)
    }

    @Test
    fun `inverse conversion recovers the magnetic heading`() {
        assertEquals(0f, DeclinationEngine.magneticHeading(10f, 10f), eps)
        assertEquals(350f, DeclinationEngine.magneticHeading(350f, 0f), eps)
    }

    @Test
    fun `cardinal directions map correctly`() {
        assertEquals(CardinalDirection.N, CardinalDirection.fromDegrees(0f))
        assertEquals(CardinalDirection.NE, CardinalDirection.fromDegrees(45f))
        assertEquals(CardinalDirection.E, CardinalDirection.fromDegrees(90f))
        assertEquals(CardinalDirection.SE, CardinalDirection.fromDegrees(135f))
        assertEquals(CardinalDirection.S, CardinalDirection.fromDegrees(180f))
        assertEquals(CardinalDirection.SW, CardinalDirection.fromDegrees(225f))
        assertEquals(CardinalDirection.W, CardinalDirection.fromDegrees(270f))
        assertEquals(CardinalDirection.NW, CardinalDirection.fromDegrees(315f))
        assertEquals(CardinalDirection.N, CardinalDirection.fromDegrees(359.9f))
        assertEquals(CardinalDirection.N, CardinalDirection.fromDegrees(22.4f))
        assertEquals(CardinalDirection.NE, CardinalDirection.fromDegrees(22.5f))
    }
}
