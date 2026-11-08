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

data class UserProfileUiState(
    val user: User? = null,
    val workouts: List<Workout> = emptyList(),
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = false,
    val isLoading: Boolean = true,
    val isFollowLoading: Boolean = false,
    val error: String? = null
)

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

    private fun observeProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentUid = authRepository.getCurrentUserId() ?: return@launch

            // Load workouts once
            val workoutsResult = workoutRepository.getWorkoutsByUserId(targetUid)

            // Observe target profile
            userRepository.observeUserProfile(targetUid).collectLatest { user ->
                // Observe current user to know if following
                userRepository.observeUserProfile(currentUid).collectLatest { currentUser ->
                    val isFollowing = currentUser?.followingIds?.contains(targetUid) ?: false
                    val isOwnProfile = currentUid == targetUid

                    if (user != null) {
                        _state.update {
                            it.copy(
                                user = user,
                                workouts = workoutsResult.getOrDefault(it.workouts),
                                isFollowing = isFollowing,
                                isOwnProfile = isOwnProfile,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Perfil no encontrado") }
                    }
                }
            }
        }
    }

    fun toggleFollow() {
        val currentUid = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _state.update { it.copy(isFollowLoading = true) }

            val result = if (_state.value.isFollowing) {
                userRepository.unfollowUser(currentUid, targetUid)
            } else {
                userRepository.followUser(currentUid, targetUid)
            }

            result.onSuccess {
                _state.update { it.copy(isFollowLoading = false) }
            }.onFailure { e ->
                _state.update { it.copy(isFollowLoading = false, error = e.message) }
            }
        }
    }
}

class UserProfileViewModelFactory(
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserProfileViewModel(userId) as T
    }
}