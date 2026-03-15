package com.mg.costeoapp.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    private val locale = Locale.Builder().setLanguage("es").setRegion("SV").build()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", locale)
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", locale)

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))
}
