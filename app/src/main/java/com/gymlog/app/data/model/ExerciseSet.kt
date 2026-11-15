package com.gymlog.app.data.model

import com.google.firebase.firestore.PropertyName

// Class representing a set within an exercise
// Contains the set data
data class ExerciseSet(
    val reps: Int = 0,
    val weight: Double = 0.0,

    @get:PropertyName("warmup")
    @set:PropertyName("warmup")
    var isWarmup: Boolean = false,

    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var isCompleted: Boolean = false,

    val progressDiff: Double? = null,
    val repsProgressDiff: Int? = null
)