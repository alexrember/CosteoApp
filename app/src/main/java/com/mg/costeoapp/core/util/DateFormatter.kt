package com.mg.costeoapp.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {

    private val locale = Locale.Builder().setLanguage("es").setRegion("SV").build()
    private val zone = ZoneId.systemDefault()

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)

    fun formatDate(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(dateFormatter)

    fun formatDateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(dateTimeFormatter)
}
