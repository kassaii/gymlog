package com.gymlog.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.gymlog.app.data.model.Exercise
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.model.WorkoutExercise
import kotlinx.coroutines.tasks.await

class WorkoutRepository {

    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()

    // Shortcut to the "workouts" collection
    private val workoutsCollection = firestore.collection("workouts")

    // Saves a workout in Firestore
    suspend fun saveWorkout(
        userId: String,
        username: String,
        userPhotoUrl: String,
        title: String,
        durationMinutes: Int,
        totalVolume: Double,
        totalSets: Int,
        exercises: List<WorkoutExercise>
    ): Result<Workout> {
        return try {
            val workout = Workout(
                userId = userId,
                username = username,
                userPhotoUrl = userPhotoUrl,
                title = title,
                date = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                totalVolume = totalVolume,
                totalSets = totalSets,
                exercises = exercises
            )

            // Add the document
            val docRef = workoutsCollection.add(workout).await()

            // Workout with the generated ID
            val savedWorkout = workout.copy(id = docRef.id)

            Result.success(savedWorkout)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExercises(): Result<List<Exercise>> {
        return try {
            val snapshot = firestore.collection("exercises").get().await()

            val exercises = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Exercise::class.java)?.copy(id = doc.id)
            }

            val sortedExercises = exercises.sortedBy { it.name }
            Result.success(sortedExercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}