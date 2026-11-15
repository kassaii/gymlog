
package com.gymlog.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.User
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

// Represents the current state of the profile screen
data class ProfileUiState(
    val user: User? = null,
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// Manages the current user's profile and workout history
class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val workoutRepository = WorkoutRepository()

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        observeProfile()
    }

    // Observes the current user's profile and workouts in real time
    // and updates the screen whenever their data changes
    private fun observeProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val uid = authRepository.getCurrentUserId()
            if (uid == null) {
                _state.update {
                    it.copy(isLoading = false, error = "Usuario no autenticado")
                }
                return@launch
            }

            workoutRepository.observeWorkoutsByUserId(uid).collectLatest { workouts ->
                _state.update { it.copy(workouts = workouts) }
            }
        }

        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch

            userRepository.observeUserProfile(uid).collectLatest { user ->
                if (user != null) {
                    _state.update {
                        it.copy(
                            user = user,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(isLoading = false, error = "Perfil no encontrado")
                    }
                }
            }
        }
    }
}
