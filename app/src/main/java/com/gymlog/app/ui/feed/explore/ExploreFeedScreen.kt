package com.gymlog.app.ui.feed.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gymlog.app.data.model.User
import com.gymlog.app.ui.common.FeedContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFeedScreen(
    onWorkoutClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onSwitchToFollowing: () -> Unit,
    reloadKey: Boolean = false,
    viewModel: ExploreFeedViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    BackHandler(enabled = state.isSearchActive) {
        viewModel.onSearchToggle()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (state.isSearchActive) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = state.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onSearch = { },
                            expanded = state.isSearchActive,
                            onExpandedChange = { },
                            placeholder = { Text("Buscar usuarios...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::onSearchToggle) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                                }
                            }
                        )
                    },
                    expanded = state.isSearchActive,
                    onExpandedChange = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (state.isSearchingUsers) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (state.searchedUsers.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.searchedUsers) { user ->
                                UserSearchItem(user = user, onClick = { onUserClick(user.uid) })
                            }
                        }
                    } else if (state.searchQuery.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron usuarios", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                TopAppBar(
                    windowInsets = WindowInsets(0),
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Column {
                            Text("Explorar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Entrenos de la comunidad", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = onSwitchToFollowing) {
                            Icon(Icons.Default.People, contentDescription = "Ir a Seguidos")
                        }
                        IconButton(onClick = viewModel::onSearchToggle) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (!state.isSearchActive) {
            FeedContent(
                workouts = state.workouts,
                isLoading = state.isLoading,
                error = state.error,
                weightUnit = state.weightUnit,
                onWorkoutClick = onWorkoutClick,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun UserSearchItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = "Foto de ${user.username}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(user.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${user.followersCount} seguidores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}