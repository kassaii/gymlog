package com.gymlog.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.Exercise
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.WorkoutExercise
import com.gymlog.app.data.repository.AuthRepository
import com.gymlog.app.data.repository.WorkoutRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateWorkoutUiState(
    val title: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val discardSuccess: Boolean = false
)

class CreateWorkoutViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(CreateWorkoutUiState())
    val state: StateFlow<CreateWorkoutUiState> = _state.asStateFlow()

    init {
        startTimer()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_state.value.isTimerRunning) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    // Pause timer
    fun onPauseTimer() {
        _state.update { it.copy(isTimerRunning = false) }
    }

    // Resume timer
    fun onResumeTimer() {
        _state.update { it.copy(isTimerRunning = true) }
    }

    // Change workout title
    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title) }
    }

    // Add exercise from library
    fun onAddExercise(exercise: Exercise) {
        val workoutExercise = WorkoutExercise(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            exercisePhotoUrl = exercise.photoUrl,
            sets = emptyList()
        )
        _state.update {
            it.copy(exercises = it.exercises + workoutExercise)
        }
    }

    // Remove exercise from workout
    fun onRemoveExercise(exerciseIndex: Int) {
        _state.update {
            it.copy(
                exercises = it.exercises.filterIndexed { i, _ -> i != exerciseIndex }
            )
        }
    }

    // Add empty set to exercise
    fun onAddSet(exerciseIndex: Int) {
        _state.update {
            val updated = it.exercises.toMutableList()
            val exercise = updated[exerciseIndex]
            updated[exerciseIndex] = exercise.copy(
                sets = exercise.sets + ExerciseSet(reps = 0, weight = 0.0)
            )
            it.copy(exercises = updated)
        }
    }

    // Remove set from exercise
    fun onRemoveSet(exerciseIndex: Int, setIndex: Int) {
        _state.update {
            val updated = it.exercises.toMutableList()
            val exercise = updated[exerciseIndex]
            updated[exerciseIndex] = exercise.copy(
                sets = exercise.sets.filterIndexed { i, _ -> i != setIndex }
            )
            it.copy(exercises = updated)
        }
    }

    // Update set reps and weight
    fun onSetChange(exerciseIndex: Int, setIndex: Int, reps: Int, weight: Double) {
        _state.update {
            val updated = it.exercises.toMutableList()
            val exercise = updated[exerciseIndex]
            val updatedSets = exercise.sets.toMutableList()
            updatedSets[setIndex] = ExerciseSet(reps = reps, weight = weight)
            updated[exerciseIndex] = exercise.copy(sets = updatedSets)
            it.copy(exercises = updated)
        }
    }

    // Finish and save workout to Firestore
    fun onFinishWorkout() {
        val currentState = _state.value

        if (currentState.exercises.isEmpty()) {
            _state.update { it.copy(error = "Add at least one exercise") }
            return
        }

        if (currentState.title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isTimerRunning = false) }

            val uid = authRepository.getCurrentUserId()!!
            val user = authRepository.getUserProfile(uid).getOrThrow()

            val durationMinutes = currentState.elapsedSeconds / 60
            val totalVolume = currentState.exercises.sumOf { ex ->
                ex.sets.sumOf { set -> set.reps * set.weight }
            }
            val totalSets = currentState.exercises.sumOf { it.sets.size }

            val result = workoutRepository.saveWorkout(
                userId = uid,
                username = user.username,
                userPhotoUrl = user.photoUrl,
                title = currentState.title,
                durationMinutes = durationMinutes,
                totalVolume = totalVolume,
                totalSets = totalSets,
                exercises = currentState.exercises
            )

            // Handle result using fold
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, saveSuccess = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    // Discard workout without saving
    fun onDiscardWorkout() {
        _state.update {
            CreateWorkoutUiState(discardSuccess = true)
        }
    }
}