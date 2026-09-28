package com.gymlog.app.data.model

// Clase que representa una serie dentro de un ejercicio.
// Contiene los datos de la serie.
data class ExerciseSet(
    val reps: Int = 0,
    val weight: Double = 0.0
)