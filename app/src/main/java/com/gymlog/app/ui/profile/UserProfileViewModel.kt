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
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            fetchProfileData()
        }
    }

    private suspend fun fetchProfileData() {
        _state.update { it.copy(isLoading = true, error = null) }

        val currentUid = authRepository.getCurrentUserId() ?: return

        val userResult = userRepository.getUserProfile(targetUid)
        if (userResult.isFailure) {
            _state.update { it.copy(isLoading = false, error = userResult.exceptionOrNull()?.message) }
            return
        }

        val workoutsResult = workoutRepository.getWorkoutsByUserId(targetUid)

        val currentUserResult = userRepository.getUserProfile(currentUid)
        val isFollowing = currentUserResult.getOrNull()?.followingIds?.contains(targetUid) ?: false

        val isOwnProfile = currentUid == targetUid

        _state.update {
            it.copy(
                user = userResult.getOrNull(),
                workouts = workoutsResult.getOrDefault(emptyList()),
                isFollowing = isFollowing,
                isOwnProfile = isOwnProfile,
                isLoading = false
            )
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
                fetchProfileData()
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