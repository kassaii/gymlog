package com.gymlog.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.Exercise
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.WorkoutExercise
import com.gymlog.app.data.repository.AuthRepository
import com.gymlog.app.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class CreateWorkoutUiState(
    val title: String = "",
    val description: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val discardSuccess: Boolean = false,

    val isExerciseDialogVisible: Boolean = false,
    val isLoadingExercises: Boolean = false,
    val availableExercises: List<Exercise> = emptyList(),
    val selectedExerciseIds: Set<String> = emptySet(),

    val searchQuery: String = "",
    val selectedMuscleGroup: String? = null,

    val expandedSet: Pair<Int, Int>? = null,

    // exerciseName -> (setIndex -> set) with the most recent data for each position
    val previousSetsByExercise: Map<String, Map<Int, ExerciseSet>> = emptyMap()
)

class CreateWorkoutViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(CreateWorkoutUiState())
    val state: StateFlow<CreateWorkoutUiState> = _state.asStateFlow()

    // Reference to the timer to cancel it when needed
    private var timerJob: Job? = null

    init {
        loadExercises()
        loadPreviousSets()
    }

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

    fun reloadExercisesIfNeeded() {
        if (_state.value.availableExercises.isEmpty() && !_state.value.isLoadingExercises) {
            loadExercises()
        }
        loadPreviousSets()
    }

    private fun loadPreviousSets() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            workoutRepository.getPreviousSetsByExercise(uid).fold(
                onSuccess = { map ->
                    _state.update { it.copy(previousSetsByExercise = map) }
                },
                onFailure = { }
            )
        }
    }

    fun startWorkoutIfNeeded() {
        if (_state.value.exercises.isEmpty() && _state.value.elapsedSeconds == 0) {
            _state.update { it.copy(isTimerRunning = true) }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000.milliseconds)
                if (_state.value.isTimerRunning) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun onPauseTimer() {
        _state.update { it.copy(isTimerRunning = false) }
    }

    fun onResumeTimer() {
        _state.update { it.copy(isTimerRunning = true) }
    }

    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _state.update { it.copy(description = description) }
    }

    fun onToggleSetExpansion(exerciseIndex: Int, setIndex: Int) {
        _state.update { currentState ->
            val current = currentState.expandedSet
            if (current != null && current.first == exerciseIndex && current.second == setIndex) {
                currentState.copy(expandedSet = null)
            } else {
                currentState.copy(expandedSet = exerciseIndex to setIndex)
            }
        }
    }

    fun onShowExerciseDialog(show: Boolean) {
        _state.update {
            it.copy(
                isExerciseDialogVisible = show,
                selectedExerciseIds = emptySet(),
                searchQuery = "",
                selectedMuscleGroup = null
            )
        }
    }

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

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onMuscleGroupFilterChange(muscleGroup: String?) {
        _state.update { it.copy(selectedMuscleGroup = muscleGroup) }
    }

    fun onConfirmAddExercises() {
        val currentState = _state.value
        val exercisesToAdd = currentState.availableExercises.filter {
            currentState.selectedExerciseIds.contains(it.id)
        }

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
            val firstNewExerciseIndex = it.exercises.size
            it.copy(
                exercises = it.exercises + newWorkoutExercises,
                isExerciseDialogVisible = false,
                expandedSet = if (newWorkoutExercises.isNotEmpty()) {
                    firstNewExerciseIndex to 0
                } else {
                    it.expandedSet
                }
            )
        }
    }

    fun onRemoveExercise(exerciseIndex: Int) {
        _state.update { currentState ->
            val exerciseToRemove = currentState.exercises.getOrNull(exerciseIndex)?.exerciseName
            val newExercises = currentState.exercises.filterIndexed { i, _ -> i != exerciseIndex }
            val newExpanded = if (currentState.expandedSet?.first == exerciseIndex) null else currentState.expandedSet

            val newPreviousMap = exerciseToRemove?.let {
                currentState.previousSetsByExercise - it
            } ?: currentState.previousSetsByExercise

            currentState.copy(
                exercises = newExercises,
                expandedSet = newExpanded,
                previousSetsByExercise = newPreviousMap
            )
        }
    }

    fun onMoveExercise(fromIndex: Int, toIndex: Int) {
        _state.update { currentState ->
            val exercises = currentState.exercises.toMutableList()
            if (fromIndex in exercises.indices && toIndex in exercises.indices) {
                val item = exercises.removeAt(fromIndex)
                exercises.add(toIndex, item)
            }
            val newExpanded = currentState.expandedSet?.let { (ex, set) ->
                when {
                    ex == fromIndex -> toIndex to set
                    ex == toIndex -> fromIndex to set
                    else -> ex to set
                }
            }
            currentState.copy(exercises = exercises, expandedSet = newExpanded)
        }
    }

    fun onAddSet(exerciseIndex: Int) {
        _state.update {
            val updated = it.exercises.toMutableList()
            val exercise = updated[exerciseIndex]
            val newSetIndex = exercise.sets.size
            updated[exerciseIndex] = exercise.copy(
                sets = exercise.sets + ExerciseSet(reps = 0, weight = 0.0)
            )
            it.copy(
                exercises = updated,
                expandedSet = exerciseIndex to newSetIndex
            )
        }
    }

    fun onRemoveSet(exerciseIndex: Int, setIndex: Int) {
        _state.update { currentState ->
            val updated = currentState.exercises.toMutableList()
            val exercise = updated[exerciseIndex]
            val newSets = exercise.sets.filterIndexed { i, _ -> i != setIndex }
            updated[exerciseIndex] = exercise.copy(sets = newSets)

            val newExpanded = currentState.expandedSet?.let { (ex, set) ->
                when {
                    ex != exerciseIndex -> ex to set
                    set == setIndex -> null
                    set > setIndex -> ex to (set - 1)
                    else -> ex to set
                }
            }
            currentState.copy(exercises = updated, expandedSet = newExpanded)
        }
    }

    fun onSetChange(exerciseIndex: Int, setIndex: Int, reps: Int, weight: Double) {
        _state.update {
            val updated = it.exercises.toMutableList()
            val exercise = updated[exerciseIndex]
            val updatedSets = exercise.sets.toMutableList()
            updatedSets[setIndex] = updatedSets[setIndex].copy(reps = reps, weight = weight)
            updated[exerciseIndex] = exercise.copy(sets = updatedSets)
            it.copy(exercises = updated)
        }
    }

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
            val wasCompleted = set.isCompleted

            val newSet = if (!wasCompleted) {
                // If completing the set and it has no real data, use ghost data
                val ghostSets = currentState.previousSetsByExercise[exercise.exerciseName]

                // Count how many working sets (non-warmup) were completed before this one
                val workingSetsBefore = updatedSets
                    .take(setIndex)
                    .count { !it.isWarmup && it.isCompleted }

                val ghost = if (set.isWarmup) {
                    null
                } else {
                    ghostSets?.get(workingSetsBefore)
                }

                // Use ghost weight/reps if the current set is empty
                val weightToUse = if (set.weight == 0.0 && ghost != null) ghost.weight else set.weight
                val repsToUse = if (set.reps == 0 && ghost != null) ghost.reps else set.reps

                set.copy(
                    isCompleted = true,
                    weight = weightToUse,
                    reps = repsToUse
                )
            } else {
                set.copy(isCompleted = false)
            }

            updatedSets[setIndex] = newSet
            updatedExercises[exerciseIndex] = exercise.copy(sets = updatedSets)

            var newExpanded = currentState.expandedSet
            if (!wasCompleted) {
                val nextIndex = updatedSets.drop(setIndex + 1).indexOfFirst { !it.isCompleted }
                newExpanded = if (nextIndex != -1) {
                    exerciseIndex to (setIndex + 1 + nextIndex)
                } else {
                    null
                }
            }

            currentState.copy(exercises = updatedExercises, expandedSet = newExpanded)
        }
    }

    fun onFinishWorkout() {
        val currentState = _state.value

        if (currentState.exercises.isEmpty()) {
            _state.update { it.copy(error = "Add at least one exercise") }
            return
        }

        val finalTitle = if (currentState.title.isBlank()) {
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            "Entreno a las ${formatter.format(java.util.Date())}"
        } else {
            currentState.title
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isTimerRunning = false) }

            val uid = authRepository.getCurrentUserId()!!
            val user = authRepository.getUserProfile(uid).getOrThrow()

            val workingExercises = currentState.exercises.map { exercise ->
                val ghostSets = currentState.previousSetsByExercise[exercise.exerciseName]

                val processedSets = exercise.sets
                    .filter { !it.isWarmup }
                    .mapIndexed { index, set ->
                        val ghost = ghostSets?.get(index)
                        val weightToUse = if (set.weight == 0.0 && ghost != null) ghost.weight else set.weight
                        val repsToUse = if (set.reps == 0 && ghost != null) ghost.reps else set.reps

                        set.copy(
                            weight = weightToUse,
                            reps = repsToUse,
                            isCompleted = true
                        )
                    }

                exercise.copy(sets = processedSets)
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
                title = finalTitle,
                description = currentState.description,
                durationMinutes = durationMinutes,
                totalVolume = totalVolume,
                totalSets = totalSets,
                exercises = workingExercises
            )

            result
                .onSuccess {
                    timerJob?.cancel()
                    _state.value = CreateWorkoutUiState(saveSuccess = true)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun onSaveHandled() {
        _state.update { it.copy(saveSuccess = false) }
    }

    fun onDiscardWorkout() {
        timerJob?.cancel()
        timerJob = null
        _state.value = CreateWorkoutUiState(discardSuccess = true)
    }

    fun onDiscardHandled() {
        _state.update { it.copy(discardSuccess = false) }
    }
}