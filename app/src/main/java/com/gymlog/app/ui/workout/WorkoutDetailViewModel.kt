
package com.gymlog.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.repository.AuthRepository
import com.gymlog.app.data.repository.UserRepository
import com.gymlog.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Represents the current state of the workout detail screen
data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val weightUnit: String = "kg"
)

// Manages workout details
class WorkoutDetailViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(WorkoutDetailUiState())
    val state: StateFlow<WorkoutDetailUiState> = _state.asStateFlow()

    // Observes the current user's weight unit
    // and updates the screen when the preference changes
    private fun observeWeightUnit() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            userRepository.observeWeightUnit(uid).collectLatest { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }
    }

    // Loads a workout by its ID and updates the screen
    // with its details or an error if loading fails
    fun loadWorkout(workoutId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            observeWeightUnit()

            val result = workoutRepository.getWorkoutById(workoutId)

            result.fold(
                onSuccess = { workout ->
                    _state.update {
                        it.copy(workout = workout, isLoading = false, error = null)
                    }
                },
                onFailure = { exception ->
                    _state.update {
                        it.copy(
                            workout = null,
                            isLoading = false,
                            error = exception.message ?: "Failed to load workout"
                        )
                    }
                }
            )
        }
    }

    // Deletes a workout only if it belongs to the current user
    // and updates the deletion status after the request
    fun deleteWorkout(workoutId: String) {
        if (_state.value.isDeleting) return

        val uid = authRepository.getCurrentUserId()
        if (uid == null || _state.value.workout?.id != workoutId || _state.value.workout?.userId != uid) {
            _state.update {
                it.copy(deleteError = "No puedes eliminar este entrenamiento")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, deleteError = null) }

            workoutRepository.deleteWorkout(workoutId)
                .onSuccess {
                    _state.update {
                        it.copy(isDeleting = false, deleteSuccess = true)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isDeleting = false, deleteError = e.message)
                    }
                }
        }
    }

    // Resets the deletion success flag after the screen handles it
    fun onDeleteHandled() {
        _state.update { it.copy(deleteSuccess = false) }
    }
}
