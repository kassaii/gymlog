package com.gymlog.app.util

object WeightFormatter {

    private const val KG_to_LB = 2.20462

    fun kgToLb(kg: Double): Double = kg * KG_to_LB

    fun lbToKg(lb: Double): Double = lb / KG_to_LB

    // Convierte un peso (kg) a la unidad del usuario y le da formato.
    fun format(weightInKg: Double, unit: String): String {
        return if (unit == "kg") {
            "${"%.1f".format(weightInKg)} kg"
        } else {
            "${"%.1f".format(kgToLb(weightInKg))} lb"
        }
    }
}