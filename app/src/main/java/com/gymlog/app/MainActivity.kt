package com.gymlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.gymlog.app.ui.navigation.AppNavigation
import com.gymlog.app.ui.theme.GymTheme
import com.gymlog.app.ui.workout.CreateWorkoutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTheme {
                val navController = rememberNavController()
                val createWorkoutViewModel: CreateWorkoutViewModel = viewModel()
                AppNavigation(
                    navController = navController,
                    createWorkoutViewModel = createWorkoutViewModel
                )
            }
        }
    }
}