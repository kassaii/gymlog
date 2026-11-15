package com.gymlog.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.Exercise
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.WorkoutExercise
import com.gymlog.app.data.repository.AuthRepository
import com.gymlog.app.data.repository.UserRepository
import com.gymlog.app.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Represents the current state of the workout creation screen
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

    val previousSetsByExercise: Map<String, Map<Int, ExerciseSet>> = emptyMap(),
    val weightUnit: String = "kg"
)

// Manages workout creation, exercises, sets, and the workout timer
class CreateWorkoutViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(CreateWorkoutUiState())
    val state: StateFlow<CreateWorkoutUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadExercises()
        loadPreviousSets()
        observeWeightUnit()
    }

    // Observes the user's weight unit and updates it when it changes
    private fun observeWeightUnit() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            userRepository.observeWeightUnit(uid).collectLatest { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }
    }

    // Loads the exercise library and updates the available exercises
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

    // Reloads exercises if the list is empty and refreshes previous sets
    fun reloadExercisesIfNeeded() {
        if (_state.value.availableExercises.isEmpty() && !_state.value.isLoadingExercises) {
            loadExercises()
        }
        loadPreviousSets()
    }

    // Loads the latest saved sets for each exercise to use as previous values
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

    // Starts the workout timer when no exercises or elapsed time are recorded
    fun startWorkoutIfNeeded() {
        if (_state.value.exercises.isEmpty() && _state.value.elapsedSeconds == 0) {
            _state.update { it.copy(isTimerRunning = true) }
            startTimer()
        }
    }

    // Starts a coroutine that counts elapsed workout seconds while running
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

    // Pauses the workout timer without resetting the elapsed time
    fun onPauseTimer() {
        _state.update { it.copy(isTimerRunning = false) }
    }

    // Resumes counting elapsed workout time
    fun onResumeTimer() {
        _state.update { it.copy(isTimerRunning = true) }
    }

    // Updates the workout title in the screen state
    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title) }
    }

    // Updates the workout description in the screen state
    fun onDescriptionChange(description: String) {
        _state.update { it.copy(description = description) }
    }

    // Expands the selected set or collapses it if already expanded
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

    // Opens or closes the exercise picker and resets its filters
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

    // Adds or removes an exercise from the current selection
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

    // Updates the search text used to filter available exercises
    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    // Updates the selected muscle group filter
    fun onMuscleGroupFilterChange(muscleGroup: String?) {
        _state.update { it.copy(selectedMuscleGroup = muscleGroup) }
    }

    // Adds selected exercises with an initial set and expands the first new set
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

    // Removes an exercise and clears its previous-set data from the state
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

    // Moves an exercise to a new position and adjusts the expanded set
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

    // Adds an empty set to an exercise and expands the new set
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

    // Removes a set from an exercise and adjusts the expanded set
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

    // Updates the repetitions and weight of a selected set
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

    // Marks a set as a warm-up or a working set
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

    // Marks a set complete or incomplete, fills missing previous values, and advances to the next set
    fun onToggleCheck(exerciseIndex: Int, setIndex: Int) {
        _state.update { currentState ->
            val updatedExercises = currentState.exercises.toMutableList()
            val exercise = updatedExercises[exerciseIndex]
            val updatedSets = exercise.sets.toMutableList()
            val set = updatedSets[setIndex]
            val wasCompleted = set.isCompleted

            val newSet = if (!wasCompleted) {
                val ghostSets = currentState.previousSetsByExercise[exercise.exerciseName]

                val workingSetsBefore = updatedSets
                    .take(setIndex)
                    .count { !it.isWarmup && it.isCompleted }

                val ghost = if (set.isWarmup) {
                    null
                } else {
                    ghostSets?.get(workingSetsBefore)
                }

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

    // Validates and saves the workout with its working sets, duration, and total volume
    fun onFinishWorkout() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.saveSuccess) return

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

            val uid = authRepository.getCurrentUserId()
            if (uid == null) {
                _state.update { it.copy(isLoading = false, error = "Usuario no autenticado") }
                return@launch
            }
            val userResult = userRepository.getUserProfile(uid)
            val user = userResult.getOrNull()
            if (user == null) {
                _state.update { it.copy(isLoading = false, error = userResult.exceptionOrNull()?.message ?: "No se pudo cargar el perfil") }
                return@launch
            }

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

    // Clears the workout saved confirmation flag
    fun onSaveHandled() {
        _state.update { it.copy(saveSuccess = false) }
    }

    // Stops the timer and resets the workout with a discard confirmation
    fun onDiscardWorkout() {
        timerJob?.cancel()
        timerJob = null
        _state.value = CreateWorkoutUiState(discardSuccess = true)
    }

    // Clears the workout discarded confirmation flag
    fun onDiscardHandled() {
        _state.update { it.copy(discardSuccess = false) }
    }
}
