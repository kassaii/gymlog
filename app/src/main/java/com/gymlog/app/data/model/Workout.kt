package com.gymlog.app.data.model

// Represents a workout
data class Workout(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0L,
    val durationMinutes: Int = 0,
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val progressCount: Int = 0,
    val exercises: List<WorkoutExercise> = emptyList()
)