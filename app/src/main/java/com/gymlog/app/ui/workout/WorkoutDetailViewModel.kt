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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val weightUnit: String = "kg"
)

class WorkoutDetailViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(WorkoutDetailUiState())
    val state: StateFlow<WorkoutDetailUiState> = _state.asStateFlow()

    fun loadWorkout(workoutId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                userRepository.getUserProfile(uid).onSuccess { user ->
                    _state.update { it.copy(weightUnit = user.weightUnit) }
                }
            }

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

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, deleteError = null) }

            workoutRepository.deleteWorkout(workoutId)
                .onSuccess {
                    _state.update { it.copy(isDeleting = false, deleteSuccess = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isDeleting = false, deleteError = e.message) }
                }
        }
    }

    fun onDeleteHandled() {
        _state.update { it.copy(deleteSuccess = false) }
    }
}