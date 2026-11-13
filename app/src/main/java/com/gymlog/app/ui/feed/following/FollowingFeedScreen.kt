package com.gymlog.app.ui.feed.following

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymlog.app.ui.common.FeedContent
import com.gymlog.app.ui.common.UserSearchView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingFeedScreen(
    onWorkoutClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onSwitchToExplore: () -> Unit,
    viewModel: FollowingFeedViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSearchResults()
        onPauseOrDispose { }
    }

    BackHandler(enabled = state.isSearchActive) {
        viewModel.onSearchToggle()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (!state.isSearchActive) {
                TopAppBar(
                    windowInsets = WindowInsets(0),
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Column {
                            Text("Siguiendo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Entrenos de tus seguidos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = onSwitchToExplore) {
                            Icon(Icons.Default.Public, contentDescription = "Ir a Explorar")
                        }
                        IconButton(onClick = viewModel::onSearchToggle) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (state.isSearchActive) {
            UserSearchView(
                query = state.searchQuery,
                users = state.searchedUsers,
                isSearching = state.isSearchingUsers,
                followingIds = state.myFollowingIds,
                onQueryChange = viewModel::onSearchQueryChange,
                onClose = viewModel::onSearchToggle,
                onUserClick = onUserClick,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            FeedContent(
                workouts = state.workouts,
                isLoading = state.isLoading,
                error = state.error,
                weightUnit = state.weightUnit,
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::onRefresh,
                onWorkoutClick = onWorkoutClick,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}