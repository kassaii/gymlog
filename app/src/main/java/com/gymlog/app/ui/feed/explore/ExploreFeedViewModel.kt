
package com.gymlog.app.ui.feed.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.User
import com.gymlog.app.data.model.Workout
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

// Represents the current state of the explore feed screen
data class ExploreFeedUiState(
    val workouts: List<Workout> = emptyList(),
    val searchedUsers: List<User> = emptyList(),
    val myFollowingIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSearchingUsers: Boolean = false,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val weightUnit: String = "kg"
)

// Manages the explore feed, user search and following information
class ExploreFeedViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(ExploreFeedUiState())
    val state: StateFlow<ExploreFeedUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    private var myFollowingIds: Set<String> = emptySet()
    private var currentUserId: String? = null

    init {
        loadFeed()
        loadUserContext()
        observeWeightUnit()
    }

    // Observes changes to the user's weight unit
    // and updates the feed when the unit changes
    private fun observeWeightUnit() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch

            userRepository.observeWeightUnit(uid).collectLatest { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }
    }

    // Loads the current user's ID and followed users
    // to identify which search results are already followed
    private fun loadUserContext() {
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUserId()

            currentUserId?.let { uid ->
                userRepository.getFollowingIds(uid).onSuccess { ids ->
                    myFollowingIds = ids.toSet()
                    _state.update { it.copy(myFollowingIds = myFollowingIds) }
                }
            }
        }
    }

    // Loads the most recent workouts from all users
    // and updates the explore feed with the results
    fun loadFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            workoutRepository.getRecentWorkouts().fold(
                onSuccess = { workouts ->
                    _state.update { it.copy(workouts = workouts, isLoading = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    // Refreshes the explore feed by loading the latest workouts
    // without displaying the initial loading state
    fun onRefresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            workoutRepository.getRecentWorkouts().fold(
                onSuccess = { workouts ->
                    _state.update { it.copy(workouts = workouts, isRefreshing = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isRefreshing = false, error = e.message) }
                }
            )
        }
    }

    // Opens or closes the user search
    // and clears the search results when closing it
    fun onSearchToggle() {
        searchJob?.cancel()

        _state.update {
            if (it.isSearchActive) {
                it.copy(
                    isSearchActive = false,
                    searchQuery = "",
                    searchedUsers = emptyList(),
                    isSearchingUsers = false
                )
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    // Searches for users after a short delay to avoid unnecessary requests
    // and sorts the results by followed users and username
    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }

        searchJob?.cancel()

        if (query.isBlank()) {
            _state.update {
                it.copy(
                    searchedUsers = emptyList(),
                    isSearchingUsers = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300.milliseconds)

            _state.update { it.copy(isSearchingUsers = true) }

            userRepository.searchUsers(query).fold(
                onSuccess = { users ->
                    val filteredUsers = users.filter { it.uid != currentUserId }

                    val sortedUsers = filteredUsers.sortedWith(
                        compareByDescending<User> {
                            myFollowingIds.contains(it.uid)
                        }.thenBy {
                            it.username.lowercase()
                        }
                    )

                    _state.update {
                        it.copy(
                            searchedUsers = sortedUsers,
                            isSearchingUsers = false
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            error = e.message,
                            isSearchingUsers = false
                        )
                    }
                }
            )
        }
    }

    // Runs the current user search again
    // to update the displayed results
    fun refreshSearchResults() {
        val query = _state.value.searchQuery

        if (query.isNotBlank()) {
            onSearchQueryChange(query)
        }
    }
}
