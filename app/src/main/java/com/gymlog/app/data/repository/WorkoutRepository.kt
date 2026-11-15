package com.gymlog.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gymlog.app.data.model.Exercise
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.model.WorkoutExercise
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Handles workout storage and progress tracking in Firestore
class WorkoutRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val workoutsCollection = firestore.collection("workouts")

    // Saves a new workout in Firestore and compares its sets with the previous workout
    // to calculate progress before saving
    suspend fun saveWorkout(
        userId: String,
        username: String,
        userPhotoUrl: String,
        title: String,
        description: String,
        durationMinutes: Int,
        totalVolume: Double,
        totalSets: Int,
        exercises: List<WorkoutExercise>
    ): Result<Workout> {
        return try {
            if (FirebaseAuth.getInstance().currentUser?.uid != userId) {
                return Result.failure(IllegalStateException("No autorizado"))
            }
            val docRef = workoutsCollection.document()

            val previousWorkout = getPreviousWorkout(
                userId = userId,
                beforeTimestamp = System.currentTimeMillis()
            ).getOrNull()

            val exercisesWithProgress = exercises.map { currentExercise ->
                val previousExercise = previousWorkout?.exercises?.find {
                    it.exerciseId == currentExercise.exerciseId ||
                            it.exerciseName == currentExercise.exerciseName
                }

                val updatedSets = currentExercise.sets.mapIndexed { index, currentSet ->
                    if (currentSet.isWarmup || previousExercise == null) {
                        currentSet
                    } else {
                        val previousSet = previousExercise.sets.getOrNull(index)
                        if (previousSet != null && !previousSet.isWarmup) {
                            val weightDiff = currentSet.weight - previousSet.weight
                            val repsDiff = currentSet.reps - previousSet.reps
                            currentSet.copy(
                                progressDiff = if (weightDiff != 0.0) weightDiff else null,
                                repsProgressDiff = if (repsDiff != 0) repsDiff else null
                            )
                        } else {
                            currentSet
                        }
                    }
                }

                currentExercise.copy(sets = updatedSets)
            }

            val progressCount = exercisesWithProgress
                .flatMap { it.sets }
                .count { it.progressDiff != null && it.progressDiff > 0 }

            val workout = Workout(
                id = docRef.id,
                userId = userId,
                username = username,
                userPhotoUrl = userPhotoUrl,
                title = title,
                description = description,
                date = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                totalVolume = totalVolume,
                totalSets = totalSets,
                progressCount = progressCount,
                exercises = exercisesWithProgress
            )

            docRef.set(workout).await()

            Result.success(workout)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Loads the most recent workouts from all users
    // ordered by date for the Explore feed
    suspend fun getRecentWorkouts(limit: Long = 50): Result<List<Workout>> {
        return try {
            val snapshot = workoutsCollection
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val workouts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)?.copy(id = doc.id)
            }

            Result.success(workouts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Loads workouts from followed users in groups
    // and sorts the results by date
    suspend fun getWorkoutsByFollowingIds(followingIds: List<String>): Result<List<Workout>> {
        return try {
            if (followingIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val allWorkouts = mutableListOf<Workout>()

            followingIds.chunked(10).forEach { chunk ->
                val snapshot = workoutsCollection
                    .whereIn("userId", chunk)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val workouts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Workout::class.java)?.copy(id = doc.id)
                }
                allWorkouts.addAll(workouts)
            }

            val sorted = allWorkouts.sortedByDescending { it.date }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Deletes a workout from Firestore after checking
    // that it belongs to the signed-in user
    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(IllegalStateException("No autorizado"))
            val ref = workoutsCollection.document(workoutId)
            if (ref.get().await().getString("userId") != uid) {
                return Result.failure(IllegalStateException("No puedes eliminar este entrenamiento"))
            }
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Loads the exercise library from Firestore
    // and returns the exercises sorted by name
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

    // Loads all workouts created by a specific user
    // ordered from newest to oldest
    suspend fun getWorkoutsByUserId(userId: String): Result<List<Workout>> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val workouts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)?.copy(id = doc.id)
            }

            Result.success(workouts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listens for changes to a user's workouts in real time
    // and emits the updated list
    fun observeWorkoutsByUserId(userId: String): Flow<List<Workout>> = callbackFlow {
        val listener = workoutsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val workouts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Workout::class.java)?.copy(id = doc.id)
                    }
                    trySend(workouts)
                }
            }
        awaitClose { listener.remove() }
    }

    // Finds a workout in Firestore using its document ID
    suspend fun getWorkoutById(workoutId: String): Result<Workout> {
        return try {
            val document = workoutsCollection.document(workoutId).get().await()
            val workout = document.toObject(Workout::class.java)?.copy(id = document.id)
            if (workout != null) {
                Result.success(workout)
            } else {
                Result.failure(Exception("Workout not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Finds the most recent workout saved by a user
    // before a given timestamp
    private suspend fun getPreviousWorkout(userId: String, beforeTimestamp: Long): Result<Workout?> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("userId", userId)
                .whereLessThan("date", beforeTimestamp)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val workout = snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(Workout::class.java)?.copy(id = doc.id)
            }
            Result.success(workout)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Loads recent workouts and collects the latest working sets
    // for each exercise and set position
    suspend fun getPreviousSetsByExercise(
        userId: String,
        limit: Long = 50
    ): Result<Map<String, Map<Int, ExerciseSet>>> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val workouts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)?.copy(id = doc.id)
            }

            val result = mutableMapOf<String, MutableMap<Int, ExerciseSet>>()

            workouts.forEach { workout ->
                workout.exercises.forEach { exercise ->
                    val workingSets = exercise.sets.filter { !it.isWarmup }
                    val exerciseMap = result.getOrPut(exercise.exerciseName) { mutableMapOf() }

                    workingSets.forEachIndexed { index, set ->
                        if (!exerciseMap.containsKey(index)) {
                            exerciseMap[index] = set
                        }
                    }
                }
            }

            val finalMap: Map<String, Map<Int, ExerciseSet>> = result.mapValues { it.value.toMap() }
            Result.success(finalMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
