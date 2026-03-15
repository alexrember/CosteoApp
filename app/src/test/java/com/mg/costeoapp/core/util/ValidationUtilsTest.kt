package com.mg.costeoapp.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun isValidName_minimo_2_caracteres() {
        assertFalse(ValidationUtils.isValidName(""))
        assertFalse(ValidationUtils.isValidName("A"))
        assertFalse(ValidationUtils.isValidName(" "))
        assertTrue(ValidationUtils.isValidName("AB"))
        assertTrue(ValidationUtils.isValidName("Tienda Grande"))
    }

    @Test
    fun isValidName_trim_antes_de_validar() {
        assertFalse(ValidationUtils.isValidName("  A  ")) // trim = "A" (1 char)
        assertTrue(ValidationUtils.isValidName("  AB  ")) // trim = "AB" (2 chars)
    }

    @Test
    fun isPositiveNumber() {
        assertTrue(ValidationUtils.isPositiveNumber("1"))
        assertTrue(ValidationUtils.isPositiveNumber("0.5"))
        assertTrue(ValidationUtils.isPositiveNumber("100"))
        assertFalse(ValidationUtils.isPositiveNumber("0"))
        assertFalse(ValidationUtils.isPositiveNumber("-1"))
        assertFalse(ValidationUtils.isPositiveNumber("abc"))
        assertFalse(ValidationUtils.isPositiveNumber(""))
    }

    @Test
    fun isNonNegativeNumber() {
        assertTrue(ValidationUtils.isNonNegativeNumber("0"))
        assertTrue(ValidationUtils.isNonNegativeNumber("5.5"))
        assertFalse(ValidationUtils.isNonNegativeNumber("-1"))
        assertFalse(ValidationUtils.isNonNegativeNumber("abc"))
    }

    @Test
    fun isValidPercentage() {
        assertTrue(ValidationUtils.isValidPercentage(0))
        assertTrue(ValidationUtils.isValidPercentage(50))
        assertTrue(ValidationUtils.isValidPercentage(100))
        assertFalse(ValidationUtils.isValidPercentage(-1))
        assertFalse(ValidationUtils.isValidPercentage(101))
    }
}
