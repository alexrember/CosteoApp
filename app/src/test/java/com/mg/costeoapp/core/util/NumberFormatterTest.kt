package com.mg.costeoapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun `entero sin decimales`() {
        assertEquals("946", 946.0.formatDisplay())
        assertEquals("0", 0.0.formatDisplay())
        assertEquals("1", 1.0.formatDisplay())
    }

    @Test
    fun `decimales relevantes se muestran`() {
        assertEquals("13.75", 13.75.formatDisplay())
        assertEquals("0.5", 0.5.formatDisplay())
        assertEquals("2.5", 2.5.formatDisplay())
    }

    @Test
    fun `floating point noise se limpia`() {
        assertEquals("45", 44.999999999997.formatDisplay())
        assertEquals("1.5", 1.499999999999.formatDisplay())
        assertEquals("9.2", 9.200000000001.formatDisplay())
        assertEquals("768.6", 768.5999999998.formatDisplay())
    }

    @Test
    fun `maximo 3 decimales`() {
        assertEquals("1.235", 1.23456.formatDisplay())
        assertEquals("0.333", 0.33333.formatDisplay())
    }

    @Test
    fun `sin ceros innecesarios`() {
        assertEquals("1.5", 1.500.formatDisplay())
        assertEquals("10", 10.000.formatDisplay())
        assertEquals("0.1", 0.100.formatDisplay())
    }
}
