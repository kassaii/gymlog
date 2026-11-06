package com.gymlog.app.ui.navigation

object Routes {

    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Main
    const val MAIN = "main"

    // Workouts
    const val CREATE_WORKOUT = "create_workout"
    const val WORKOUT_ID = "workoutId"
    const val WORKOUT_DETAIL = "workout_detail/{$WORKOUT_ID}"
    fun workoutDetail(workoutId: String) = "workout_detail/$workoutId"

    // Tabs (inside MainScreen)
    const val PROFILE = "profile"
    const val EXPLORE = "explore"
    const val FOLLOWING = "following"

    // Settings
    const val SETTINGS = "settings"
}