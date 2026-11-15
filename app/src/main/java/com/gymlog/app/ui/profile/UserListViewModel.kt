
package com.gymlog.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.User
import com.gymlog.app.data.repository.AuthRepository
import com.gymlog.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Defines what to display followers or followed users
enum class UserListType {
    FOLLOWERS,
    FOLLOWING
}

// Represents the current state of the user list screen
data class UserListUiState(
    val allUsers: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val searchQuery: String = "",
    val currentUserFollowingIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// Manages follower and following lists, user search and follow actions
class UserListViewModel(
    private val targetUid: String,
    private val listType: UserListType
) : ViewModel() {

    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(UserListUiState())
    val state: StateFlow<UserListUiState> = _state.asStateFlow()

    init {
        loadUsers()
    }

    // Loads the followers or followed users of the selected profile
    // and checks which users are followed by the current user
    fun loadUsers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentUid = authRepository.getCurrentUserId()
            if (currentUid == null) {
                _state.update {
                    it.copy(isLoading = false, error = "Usuario no autenticado")
                }
                return@launch
            }

            userRepository.getFollowingIds(currentUid).onSuccess { myFollowingIds ->
                _state.update {
                    it.copy(currentUserFollowingIds = myFollowingIds.toSet())
                }
            }

            val idsResult = when (listType) {
                UserListType.FOLLOWING -> userRepository.getFollowingIds(targetUid)
                UserListType.FOLLOWERS -> userRepository.getFollowersIds(targetUid)
            }

            if (idsResult.isFailure) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = idsResult.exceptionOrNull()?.message
                    )
                }
                return@launch
            }

            val userIds = idsResult.getOrDefault(emptyList())
            if (userIds.isEmpty()) {
                _state.update {
                    it.copy(
                        allUsers = emptyList(),
                        filteredUsers = emptyList(),
                        isLoading = false
                    )
                }
                return@launch
            }

            val users = mutableListOf<User>()
            userIds.forEach { id ->
                userRepository.getUserProfile(id).onSuccess { user ->
                    users.add(user)
                }
            }

            _state.update {
                it.copy(
                    allUsers = users,
                    filteredUsers = users,
                    isLoading = false
                )
            }
        }
    }

    // Filters the loaded users by username
    // and updates the list as the search query changes
    fun onSearchQueryChange(query: String) {
        _state.update { currentState ->
            val filtered = if (query.isBlank()) {
                currentState.allUsers
            } else {
                currentState.allUsers.filter {
                    it.username.contains(query, ignoreCase = true)
                }
            }

            currentState.copy(
                searchQuery = query,
                filteredUsers = filtered
            )
        }
    }

    // Follows or unfollows a user depending on the current relationship
    // and updates the following list when the action succeeds
    fun toggleFollow(userId: String) {
        val currentUid = authRepository.getCurrentUserId() ?: return
        val isCurrentlyFollowing = _state.value.currentUserFollowingIds.contains(userId)

        viewModelScope.launch {
            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(currentUid, userId)
            } else {
                userRepository.followUser(currentUid, userId)
            }

            result.onSuccess {
                _state.update { currentState ->
                    val newFollowingIds = if (isCurrentlyFollowing) {
                        currentState.currentUserFollowingIds - userId
                    } else {
                        currentState.currentUserFollowingIds + userId
                    }

                    currentState.copy(currentUserFollowingIds = newFollowingIds)
                }
            }
        }
    }
}

// Creates UserListViewModel instances with the selected user and list type
class UserListViewModelFactory(
    private val userId: String,
    private val listType: UserListType
) : ViewModelProvider.Factory {

    // Creates a UserListViewModel using the provided user ID and list type
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserListViewModel(userId, listType) as T
    }
}
