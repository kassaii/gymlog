package com.gymlog.app.util

object NumberFormatter {
    // Converts a Double to a string for text inputs (empty if zero)
    fun toInputText(value: Double): String = when {
        value == 0.0 -> ""
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> value.toString()
    }

    // Converts a Double to a display string (0 if zero, formatted if decimal)
    fun toDisplay(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
}