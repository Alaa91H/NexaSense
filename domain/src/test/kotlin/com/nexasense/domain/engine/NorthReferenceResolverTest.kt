package com.nexasense.domain.engine

import com.nexasense.domain.model.NorthReference
import org.junit.Assert.assertEquals
import org.junit.Test

class NorthReferenceResolverTest {

    @Test
    fun `automatic uses true north when declination is available`() {
        assertEquals(
            NorthReference.TRUE_NORTH,
            NorthReferenceResolver.effective(NorthReference.AUTOMATIC, declinationAvailable = true),
        )
    }

    @Test
    fun `automatic falls back to magnetic north without declination`() {
        assertEquals(
            NorthReference.MAGNETIC_NORTH,
            NorthReferenceResolver.effective(NorthReference.AUTOMATIC, declinationAvailable = false),
        )
    }

    @Test
    fun `true north stays true when declination is available`() {
        assertEquals(
            NorthReference.TRUE_NORTH,
            NorthReferenceResolver.effective(NorthReference.TRUE_NORTH, declinationAvailable = true),
        )
    }

    @Test
    fun `true north falls back to magnetic without declination`() {
        assertEquals(
            NorthReference.MAGNETIC_NORTH,
            NorthReferenceResolver.effective(NorthReference.TRUE_NORTH, declinationAvailable = false),
        )
    }

    @Test
    fun `magnetic north always stays magnetic`() {
        assertEquals(
            NorthReference.MAGNETIC_NORTH,
            NorthReferenceResolver.effective(NorthReference.MAGNETIC_NORTH, declinationAvailable = true),
        )
        assertEquals(
            NorthReference.MAGNETIC_NORTH,
            NorthReferenceResolver.effective(NorthReference.MAGNETIC_NORTH, declinationAvailable = false),
        )
    }
}
