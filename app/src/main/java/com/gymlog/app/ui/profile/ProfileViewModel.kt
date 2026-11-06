package com.gymlog.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.User
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.repository.AuthRepository
import com.gymlog.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val workoutRepository = WorkoutRepository()

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val uid = authRepository.getCurrentUserId()
            if (uid == null) {
                _state.update { it.copy(isLoading = false, error = "Usuario no autenticado") }
                return@launch
            }

            // Load user profile
            val userResult = authRepository.getUserProfile(uid)
            if (userResult.isFailure) {
                _state.update { it.copy(isLoading = false, error = userResult.exceptionOrNull()?.message) }
                return@launch
            }

            // Load user workouts
            val workoutsResult = workoutRepository.getWorkoutsByUserId(uid)
            if (workoutsResult.isFailure) {
                _state.update { it.copy(isLoading = false, error = workoutsResult.exceptionOrNull()?.message) }
                return@launch
            }

            _state.update {
                it.copy(
                    user = userResult.getOrNull(),
                    workouts = workoutsResult.getOrDefault(emptyList()),
                    isLoading = false
                )
            }
        }
    }

    fun refresh() {
        loadProfile()
    }
}