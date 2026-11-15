
package com.gymlog.app.util

import java.util.Locale

// Provides methods to validate and normalize usernames
object UsernameValidator {

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{3,20}$")

    // Checks if a username meets the required format
    fun isValid(username: String): Boolean {
        return usernameRegex.matches(username)
    }

    // Converts a username to lowercase for consistent storage
    fun normalize(username: String): String {
        return username.lowercase(Locale.ROOT)
    }
}
