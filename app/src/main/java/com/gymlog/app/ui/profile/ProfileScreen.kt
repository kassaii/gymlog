
package com.gymlog.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gymlog.app.data.model.User
import com.gymlog.app.ui.common.EmptyStateView
import com.gymlog.app.ui.common.ProfileStats
import com.gymlog.app.ui.common.StatCard
import com.gymlog.app.ui.common.WorkoutCard
import com.gymlog.app.util.WeightFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onWorkoutClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val totalHours = state.workouts.sumOf { it.durationMinutes } / 60.0
    val totalVolume = state.workouts.sumOf { it.totalVolume }
    val totalSets = state.workouts.sumOf { it.totalSets }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (state.user != null) {
            val user = state.user!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 16.dp
                )
            ) {
                item {
                    ProfileHeader(user = user)
                }

                item {
                    ProfileStats(
                        workoutsCount = state.workouts.size,
                        followersCount = user.followersCount,
                        followingCount = user.followingCount,
                        onFollowersClick = onFollowersClick,
                        onFollowingClick = onFollowingClick
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            label = "Horas",
                            value = if (totalHours < 1) {
                                "${(totalHours * 60).toInt()} min"
                            } else {
                                "${"%.1f".format(totalHours)} h"
                            }
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.FitnessCenter,
                            label = "Volumen",
                            value = WeightFormatter.formatCompact(
                                totalVolume,
                                user.weightUnit
                            )
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.List,
                            label = "Series",
                            value = "$totalSets"
                        )
                    }
                }

                item {
                    Text(
                        text = "Historial de entrenos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.workouts.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.FitnessCenter,
                            title = "Aún no tienes entrenamientos",
                            subtitle = "Los entrenamientos que completes aparecerán aquí"
                        )
                    }
                } else {
                    items(state.workouts) { workout ->
                        WorkoutCard(
                            workout = workout,
                            weightUnit = user.weightUnit,
                            onClick = {
                                onWorkoutClick(workout.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = "Profile picture of ${user.username}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.username,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
