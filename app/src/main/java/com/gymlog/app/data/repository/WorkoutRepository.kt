package com.gymlog.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gymlog.app.data.model.Exercise
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.model.WorkoutExercise
import kotlinx.coroutines.tasks.await

class WorkoutRepository {

    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()

    // Shortcut to the "workouts" collection
    private val workoutsCollection = firestore.collection("workouts")

    // Saves a workout in Firestore with a pre-generated document id.
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
            // Generate a new document reference to get a unique id beforehand
            val docRef = workoutsCollection.document()

            // Fetch the previous workout for this user (if any)
            val previousWorkout = getPreviousWorkout(
                userId = userId,
                beforeTimestamp = System.currentTimeMillis()
            ).getOrNull()

            // Calculate progressDiff for each set
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

            // Calculate total progress count (sets with positive progress)
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

            // Save the workout
            docRef.set(workout).await()

            Result.success(workout)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets the most recent workouts from all users (for the Explore feed)
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

    // Gets workouts from a list of userIds (the users the current user follows).
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

    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            workoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
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

    // Gets all workouts of a specific user, ordered by date descending
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

    // Gets a workout by id
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

    // Gets the most recent workout for a user that was saved before the given timestamp
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

    // Returns a map of exerciseName.
    // Iterates workouts from most recent to oldest, filling in the most recent
    // set found for each position so older sets can fill gaps if the most recent
    // workout had fewer sets.
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

            // Safety
            val finalMap: Map<String, Map<Int, ExerciseSet>> = result.mapValues { it.value.toMap() }
            Result.success(finalMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}