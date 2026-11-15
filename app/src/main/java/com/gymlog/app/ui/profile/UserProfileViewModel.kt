
package com.gymlog.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

// Represents the current state of another user's profile screen
data class UserProfileUiState(
    val user: User? = null,
    val workouts: List<Workout> = emptyList(),
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = false,
    val isLoading: Boolean = true,
    val isFollowLoading: Boolean = false,
    val error: String? = null
)

// Manages a user's profile, workout history and follow relationship
class UserProfileViewModel(
    private val targetUid: String
) : ViewModel() {

    private val userRepository = UserRepository()
    private val workoutRepository = WorkoutRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    init {
        observeProfile()
    }

    // Loads the selected user's workouts and observes their profile
    // and follow relationship for real-time updates
    private fun observeProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentUid = authRepository.getCurrentUserId()

            if (currentUid == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Usuario no autenticado"
                    )
                }
                return@launch
            }

            workoutRepository.getWorkoutsByUserId(targetUid).fold(
                onSuccess = { workouts ->
                    _state.update { it.copy(workouts = workouts) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )

            _state.update {
                it.copy(isOwnProfile = currentUid == targetUid)
            }

            launch {
                userRepository.observeUserProfile(targetUid).collectLatest { user ->
                    if (user != null) {
                        _state.update {
                            it.copy(
                                user = user,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Perfil no encontrado"
                            )
                        }
                    }
                }
            }

            if (currentUid != targetUid) {
                launch {
                    userRepository.observeIsFollowing(
                        currentUid,
                        targetUid
                    ).collectLatest { isFollowing ->
                        _state.update {
                            it.copy(isFollowing = isFollowing)
                        }
                    }
                }
            }
        }
    }

    // Follows or unfollows the selected user
    // and updates the loading or error state after the action
    fun toggleFollow() {
        val currentUid = authRepository.getCurrentUserId() ?: return

        if (currentUid == targetUid || _state.value.isFollowLoading) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(isFollowLoading = true, error = null)
            }

            val result = if (_state.value.isFollowing) {
                userRepository.unfollowUser(currentUid, targetUid)
            } else {
                userRepository.followUser(currentUid, targetUid)
            }

            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(isFollowLoading = false)
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isFollowLoading = false,
                            error = e.message
                        )
                    }
                }
            )
        }
    }
}

// Creates UserProfileViewModel instances for a specific user
class UserProfileViewModelFactory(
    private val userId: String
) : ViewModelProvider.Factory {

    // Creates a UserProfileViewModel using the provided user ID
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserProfileViewModel(userId) as T
    }
}
