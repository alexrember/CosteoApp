package com.mg.costeoapp.feature.sync.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappersTest {

    // -----------------------------------------------------------------------
    // Timestamp conversion (kept — these helpers are still in EntityMappers)
    // -----------------------------------------------------------------------

    @Test
    fun `epochMillisToIso convierte correctamente`() {
        val millis = 1700000000000L
        val iso = epochMillisToIso(millis)
        assertTrue("ISO string debe contener T: $iso", iso.contains("T"))
        assertTrue("ISO string debe contener Z: $iso", iso.contains("Z"))
    }

    @Test
    fun `isoToEpochMillis convierte correctamente`() {
        val iso = "2023-11-14T22:13:20Z"
        val millis = isoToEpochMillis(iso)
        assertEquals(1700000000000L, millis)
    }

    @Test
    fun `roundtrip epoch to ISO y de vuelta`() {
        val original = 1700000000000L
        val iso = epochMillisToIso(original)
        val back = isoToEpochMillis(iso)
        assertEquals(original, back)
    }

    @Test
    fun `epoch cero convierte a ISO correctamente`() {
        val iso = epochMillisToIso(0L)
        assertEquals("1970-01-01T00:00:00Z", iso)
    }
}
