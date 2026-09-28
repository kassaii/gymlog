package com.gymlog.app.data.model

// Clase que representa un ejercicio dentro de un entrenamiento.
data class WorkoutExercise(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val exercisePhotoUrl: String = "",
    val sets: List<ExerciseSet> = emptyList()
)