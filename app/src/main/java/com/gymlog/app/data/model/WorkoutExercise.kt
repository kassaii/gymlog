package com.gymlog.app.data.model

data class WorkoutExercise(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val exercisePhotoUrl: String = "",
    val muscleGroup: String = "",
    val sets: List<ExerciseSet> = emptyList()
)