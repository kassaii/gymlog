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

    // Tabs inside MainScreen
    const val PROFILE = "profile"
    const val EXPLORE = "explore"
    const val FOLLOWING = "following"

    // User Profiles
    const val USER_ID = "userId"
    const val USER_PROFILE = "user_profile/{$USER_ID}"
    fun userProfile(userId: String) = "user_profile/$userId"

    // User List
    const val LIST_TYPE = "listType"
    const val USER_LIST = "user_list/{$USER_ID}/{$LIST_TYPE}"
    fun userList(userId: String, listType: String) = "user_list/$userId/$listType"

    // Settings
    const val SETTINGS = "settings"
}