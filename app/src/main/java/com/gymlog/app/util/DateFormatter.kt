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

    fun formatSocialDateTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val time = timeFormat.format(calendar.time)

        return when {
            diff < 60_000 -> "Ahora mismo"
            diff < 3_600_000 -> "Hace ${diff / 60_000} min"
            diff < 86_400_000 -> "Hoy a las $time"
            diff < 172_800_000 -> "Ayer a las $time"
            else -> {
                val dateFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                "${dateFormat.format(calendar.time)} · $time"
            }
        }
    }
}