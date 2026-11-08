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

data class ExploreFeedUiState(
    val workouts: List<Workout> = emptyList(),
    val searchedUsers: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSearchingUsers: Boolean = false,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val weightUnit: String = "kg"
)

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

    private fun observeWeightUnit() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            userRepository.observeWeightUnit(uid).collectLatest { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }
    }

    private fun loadUserContext() {
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUserId()
            currentUserId?.let { uid ->
                userRepository.getFollowingIds(uid).onSuccess { ids ->
                    myFollowingIds = ids.toSet()
                }
            }
        }
    }

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

    fun onSearchToggle() {
        _state.update {
            if (it.isSearchActive) {
                it.copy(isSearchActive = false, searchQuery = "", searchedUsers = emptyList())
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }

        searchJob?.cancel()

        if (query.isBlank()) {
            _state.update { it.copy(searchedUsers = emptyList(), isSearchingUsers = false) }
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
                        }
                            .thenByDescending {
                                it.followersCount
                            }
                            .thenBy {
                                it.username.lowercase()
                            }
                    )
                    _state.update { it.copy(searchedUsers = sortedUsers, isSearchingUsers = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isSearchingUsers = false) }
                }
            )
        }
    }

    fun refreshSearchResults() {
        val query = _state.value.searchQuery
        if (query.isNotBlank()) {
            onSearchQueryChange(query)
        }
    }
}