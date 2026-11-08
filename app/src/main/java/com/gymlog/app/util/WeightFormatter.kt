package com.gymlog.app.util

import java.util.Locale

object WeightFormatter {

    private const val KG_TO_LB = 2.20462

    fun kgToLb(kg: Double): Double = kg * KG_TO_LB

    fun lbToKg(lb: Double): Double = lb / KG_TO_LB

    // Converts a weight (kg) to the user's unit and formats it
    fun format(weightInKg: Double, unit: String): String {
        val value = if (unit == "kg") weightInKg else kgToLb(weightInKg)
        return if (value == value.toInt().toDouble()) {
            "${value.toInt()} $unit"
        } else {
            "${"%.1f".format(value)} $unit"
        }
    }

    // Formats a large volume compactly (1287.5 kg -> 1.3k kg)
    fun formatCompact(volumeInKg: Double, unit: String): String {
        val value = if (unit == "kg") volumeInKg else kgToLb(volumeInKg)

        return if (value < 1000) {
            "${value.toInt()} $unit"
        } else {
            val compactValue = value / 1000.0
            String.format(Locale.getDefault(), "%.1fk %s", compactValue, unit)
        }
    }

    // Formats a weight for display in inputs (converts kg to user unit)
    fun formatForInput(weightInKg: Double, unit: String): String {
        if (weightInKg <= 0.0) return ""
        val value = if (unit == "kg") weightInKg else kgToLb(weightInKg)
        return if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    // Parses a weight input (in user unit) to kg for storage
    fun parseFromInput(input: String, unit: String): Double {
        val value = input.toDoubleOrNull() ?: return 0.0
        return if (unit == "kg") value else lbToKg(value)
    }

    // Formats a weight difference (in kg) to the user's unit
    fun formatDiff(diffInKg: Double, unit: String): String {
        val value = if (unit == "kg") diffInKg else kgToLb(diffInKg)
        return if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }
}