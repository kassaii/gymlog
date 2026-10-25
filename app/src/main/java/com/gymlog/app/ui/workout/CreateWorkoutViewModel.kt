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
import kotlin.time.Duration.Companion.milliseconds

data class CreateWorkoutUiState(
    val title: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val discardSuccess: Boolean = false,

    // Dialog and exercises state
    val isExerciseDialogVisible: Boolean = false,
    val isLoadingExercises: Boolean = false,
    val availableExercises: List<Exercise> = emptyList(),
    val selectedExerciseIds: Set<String> = emptySet(),

    // Filters for the dialog
    val searchQuery: String = "",
    val selectedMuscleGroup: String? = null
)

class CreateWorkoutViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(CreateWorkoutUiState())
    val state: StateFlow<CreateWorkoutUiState> = _state.asStateFlow()

    init {
        startTimer()
        loadExercises()
    }

    // Load exercises from Firestore once
    private fun loadExercises() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingExercises = true) }
            workoutRepository.getExercises().fold(
                onSuccess = { exercises ->
                    _state.update { it.copy(availableExercises = exercises, isLoadingExercises = false) }
                },
                onFailure = { error ->
                    _state.update { it.copy(error = error.message, isLoadingExercises = false) }
                }
            )
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000.milliseconds)
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

    // Open/Close dialog
    fun onShowExerciseDialog(show: Boolean) {
        _state.update {
            it.copy(
                isExerciseDialogVisible = show,
                // Clear selection and filters when opening/closing
                selectedExerciseIds = emptySet(),
                searchQuery = "",
                selectedMuscleGroup = null
            )
        }
    }

    // Handle selection of an exercise
    fun onToggleExerciseSelection(exerciseId: String) {
        _state.update { currentState ->
            val currentSelection = currentState.selectedExerciseIds
            val newSelection = if (currentSelection.contains(exerciseId)) {
                currentSelection - exerciseId
            } else {
                currentSelection + exerciseId
            }
            currentState.copy(selectedExerciseIds = newSelection)
        }
    }

    // Handle search query change
    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    // Handle muscle group filter change
    fun onMuscleGroupFilterChange(muscleGroup: String?) {
        _state.update { it.copy(selectedMuscleGroup = muscleGroup) }
    }

    // Confirm selection and add to workout
    fun onConfirmAddExercises() {
        val currentState = _state.value
        // Filter the available exercises to get the full objects of the selected ones
        val exercisesToAdd = currentState.availableExercises.filter {
            currentState.selectedExerciseIds.contains(it.id)
        }

        // Add them to the current workout
        val newWorkoutExercises = exercisesToAdd.map { exercise ->
            WorkoutExercise(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                exercisePhotoUrl = exercise.photoUrl,
                muscleGroup = exercise.muscleGroup,
                sets = listOf(ExerciseSet(reps = 0, weight = 0.0))
            )
        }

        _state.update {
            it.copy(
                exercises = it.exercises + newWorkoutExercises,
                isExerciseDialogVisible = false // Close dialog
            )
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

    // Move exercise up or down
    fun onMoveExercise(fromIndex: Int, toIndex: Int) {
        _state.update { currentState ->
            val exercises = currentState.exercises.toMutableList()
            if (fromIndex in exercises.indices && toIndex in exercises.indices) {
                val item = exercises.removeAt(fromIndex)
                exercises.add(toIndex, item)
            }
            currentState.copy(exercises = exercises)
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
            // Usamos copy() para preservar isWarmup e isCompleted
            updatedSets[setIndex] = updatedSets[setIndex].copy(reps = reps, weight = weight)
            updated[exerciseIndex] = exercise.copy(sets = updatedSets)
            it.copy(exercises = updated)
        }
    }

    // Toggle set type between normal and warmup
    fun onToggleWarmup(exerciseIndex: Int, setIndex: Int) {
        _state.update { currentState ->
            val updatedExercises = currentState.exercises.toMutableList()
            val exercise = updatedExercises[exerciseIndex]
            val updatedSets = exercise.sets.toMutableList()
            val set = updatedSets[setIndex]

            updatedSets[setIndex] = set.copy(isWarmup = !set.isWarmup)
            updatedExercises[exerciseIndex] = exercise.copy(sets = updatedSets)

            currentState.copy(exercises = updatedExercises)
        }
    }

    fun onToggleCheck(exerciseIndex: Int, setIndex: Int) {
        _state.update { currentState ->
            val updatedExercises = currentState.exercises.toMutableList()
            val exercise = updatedExercises[exerciseIndex]
            val updatedSets = exercise.sets.toMutableList()
            val set = updatedSets[setIndex]
            updatedSets[setIndex] = set.copy(isCompleted = !set.isCompleted)
            updatedExercises[exerciseIndex] = exercise.copy(sets = updatedSets)
            currentState.copy(exercises = updatedExercises)
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

            val workingExercises = currentState.exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.filter { !it.isWarmup })
            }.filter { it.sets.isNotEmpty() }

            val durationMinutes = currentState.elapsedSeconds / 60
            val totalVolume = workingExercises.sumOf { ex ->
                ex.sets.sumOf { set -> set.reps * set.weight }
            }
            val totalSets = workingExercises.sumOf { it.sets.size }

            val result = workoutRepository.saveWorkout(
                userId = uid,
                username = user.username,
                userPhotoUrl = user.photoUrl,
                title = currentState.title,
                durationMinutes = durationMinutes,
                totalVolume = totalVolume,
                totalSets = totalSets,
                exercises = workingExercises
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