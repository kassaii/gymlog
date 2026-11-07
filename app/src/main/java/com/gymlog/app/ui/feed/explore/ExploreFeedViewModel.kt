package com.gymlog.app.ui.feed.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreFeedUiState(
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = ""
)

class ExploreFeedViewModel : ViewModel() {

    private val workoutRepository = WorkoutRepository()

    private val _state = MutableStateFlow(ExploreFeedUiState())
    val state: StateFlow<ExploreFeedUiState> = _state.asStateFlow()

    init {
        loadFeed()
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

    fun onSearchToggle() {
        _state.update {
            if (it.isSearchActive) {
                it.copy(isSearchActive = false, searchQuery = "")
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}