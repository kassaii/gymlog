package com.gymlog.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gymlog.app.ui.auth.LoginScreen
import com.gymlog.app.ui.auth.RegisterScreen
import com.gymlog.app.ui.main.MainScreen
import com.gymlog.app.ui.profile.UserListScreen
import com.gymlog.app.ui.profile.UserListType
import com.gymlog.app.ui.profile.UserListViewModel
import com.gymlog.app.ui.profile.UserListViewModelFactory
import com.gymlog.app.ui.profile.UserProfileScreen
import com.gymlog.app.ui.profile.UserProfileViewModel
import com.gymlog.app.ui.profile.UserProfileViewModelFactory
import com.gymlog.app.ui.settings.SettingsScreen
import com.gymlog.app.ui.workout.CreateWorkoutScreen
import com.gymlog.app.ui.workout.CreateWorkoutViewModel
import com.gymlog.app.ui.workout.WorkoutDetailScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    createWorkoutViewModel: CreateWorkoutViewModel,
    startDestination: String = Routes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                navController = navController,
                createWorkoutViewModel = createWorkoutViewModel
            )
        }

        composable(Routes.CREATE_WORKOUT) {
            CreateWorkoutScreen(
                viewModel = createWorkoutViewModel,
                onWorkoutSaved = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                    }
                },
                onMinimize = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.WORKOUT_DETAIL,
            arguments = listOf(navArgument(Routes.WORKOUT_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString(Routes.WORKOUT_ID) ?: ""
            WorkoutDetailScreen(
                workoutId = workoutId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument(Routes.USER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Routes.USER_ID) ?: ""

            val viewModel: UserProfileViewModel = viewModel(
                factory = UserProfileViewModelFactory(userId)
            )

            UserProfileScreen(
                onBack = { navController.popBackStack() },
                onWorkoutClick = { workoutId ->
                    navController.navigate(Routes.workoutDetail(workoutId))
                },
                onFollowersClick = {
                    navController.navigate(Routes.userList(userId, "followers"))
                },
                onFollowingClick = {
                    navController.navigate(Routes.userList(userId, "following"))
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Routes.USER_LIST,
            arguments = listOf(
                navArgument(Routes.USER_ID) { type = NavType.StringType },
                navArgument(Routes.LIST_TYPE) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Routes.USER_ID) ?: ""
            val listTypeStr = backStackEntry.arguments?.getString(Routes.LIST_TYPE) ?: "following"
            val listType = UserListType.valueOf(listTypeStr.uppercase())

            val viewModel: UserListViewModel = viewModel(
                factory = UserListViewModelFactory(userId, listType)
            )

            UserListScreen(
                listType = listType,
                onBack = { navController.popBackStack() },
                onUserClick = { clickedUserId ->
                    navController.navigate(Routes.userProfile(clickedUserId))
                },
                viewModel = viewModel
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}