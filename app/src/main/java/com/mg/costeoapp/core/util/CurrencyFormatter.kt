package com.mg.costeoapp.core.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun fromCents(cents: Long): String = formatter.format(cents / 100.0)

    fun toCents(dollars: String): Long? {
        val cleaned = dollars.replace("$", "").replace(",", "").trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        return (value * 100).toLong()
    }
}
