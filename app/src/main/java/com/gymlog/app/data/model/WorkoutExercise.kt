package com.gymlog.app.data.model

// Class representing an exercise within a workout
data class WorkoutExercise(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val exercisePhotoUrl: String = "",
    val sets: List<ExerciseSet> = emptyList()
)