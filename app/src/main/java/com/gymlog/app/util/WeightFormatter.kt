package com.gymlog.app.util

object WeightFormatter {

    private const val KG_to_LB = 2.20462

    fun kgToLb(kg: Double): Double = kg * KG_to_LB

    fun lbToKg(lb: Double): Double = lb / KG_to_LB

    // Converts a weight (kg) to the user's unit and formats it
    fun format(weightInKg: Double, unit: String): String {
        val value = if (unit == "kg") weightInKg else kgToLb(weightInKg)
        return if (value == value.toInt().toDouble()) {
            "${value.toInt()} $unit"
        } else {
            "${"%.1f".format(value)} $unit"
        }
    }
}