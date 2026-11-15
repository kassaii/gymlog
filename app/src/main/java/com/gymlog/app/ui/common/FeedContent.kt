
package com.gymlog.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymlog.app.data.model.Workout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(
    workouts: List<Workout>,
    isLoading: Boolean,
    error: String?,
    weightUnit: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWorkoutClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Algo ha salido mal",
                        subtitle = error
                    )
                }
            }

            workouts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Outlined.FitnessCenter,
                        title = "Aún no hay entrenamientos",
                        subtitle = "Cuando alguien termine una sesión, aparecerá aquí."
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = workouts,
                        key = { it.id }
                    ) { workout ->
                        WorkoutCard(
                            workout = workout,
                            weightUnit = weightUnit,
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
