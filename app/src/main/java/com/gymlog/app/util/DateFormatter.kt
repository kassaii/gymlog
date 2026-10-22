package com.gymlog.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    // Formats a timestamp (long) to "26/09/2026 · 18:30"
    fun formatDateTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())
        return format.format(date)
    }
}