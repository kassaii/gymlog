package com.gymlog.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gymlog.app.ui.feed.explore.ExploreFeedScreen
import com.gymlog.app.ui.feed.following.FollowingFeedScreen
import com.gymlog.app.ui.navigation.Routes
import com.gymlog.app.ui.profile.ProfileScreen
import com.gymlog.app.ui.workout.CreateWorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    createWorkoutViewModel: CreateWorkoutViewModel,
    reloadFeed: Boolean = false
) {
    var currentTab by rememberSaveable { mutableStateOf("home") }

    val feedNavController = rememberNavController()

    val workoutState by createWorkoutViewModel.state.collectAsStateWithLifecycle()
    val isWorkoutActive = workoutState.exercises.isNotEmpty() || workoutState.elapsedSeconds > 0

    Scaffold(
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarItem(
                        icon = Icons.Default.Home,
                        label = "Inicio",
                        isSelected = currentTab == "home",
                        onClick = { currentTab = "home" }
                    )

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                navController.navigate(Routes.CREATE_WORKOUT)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWorkoutActive) Icons.Default.FitnessCenter else Icons.Default.Add,
                            contentDescription = if (isWorkoutActive) "Return to workout" else "Create workout",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    BottomBarItem(
                        icon = Icons.Default.Person,
                        label = "Perfil",
                        isSelected = currentTab == "profile",
                        onClick = { currentTab = "profile" }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                "home" -> {
                    NavHost(
                        navController = feedNavController,
                        startDestination = "explore",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("explore") {
                            ExploreFeedScreen(
                                onWorkoutClick = { workoutId ->
                                    navController.navigate(Routes.workoutDetail(workoutId))
                                },
                                onUserClick = { userId ->
                                    navController.navigate(Routes.userProfile(userId))
                                },
                                onSwitchToFollowing = {
                                    feedNavController.navigate("following")
                                },
                                reloadKey = reloadFeed
                            )
                        }
                        composable("following") {
                            FollowingFeedScreen(
                                onWorkoutClick = { workoutId ->
                                    navController.navigate(Routes.workoutDetail(workoutId))
                                },
                                onUserClick = { userId ->
                                    navController.navigate(Routes.userProfile(userId))
                                },
                                onSwitchToExplore = {
                                    feedNavController.navigate("explore") {
                                        popUpTo("explore") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
                "profile" -> ProfileScreen(
                    onWorkoutClick = { workoutId ->
                        navController.navigate(Routes.workoutDetail(workoutId))
                    },
                    onSettingsClick = {
                        navController.navigate(Routes.SETTINGS)
                    },
                    reloadKey = reloadFeed
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}