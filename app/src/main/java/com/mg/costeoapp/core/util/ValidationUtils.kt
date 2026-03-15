package com.mg.costeoapp.core.util

object ValidationUtils {

    fun isValidName(name: String): Boolean = name.trim().length >= 2

    fun isPositiveNumber(value: String): Boolean {
        val number = value.toDoubleOrNull() ?: return false
        return number > 0
    }

    fun isNonNegativeNumber(value: String): Boolean {
        val number = value.toDoubleOrNull() ?: return false
        return number >= 0
    }

    fun isValidPercentage(value: Int): Boolean = value in 0..100
}
