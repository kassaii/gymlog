package com.gymlog.app.data.model

// Represents follow relationship between two users
data class Follow(
    val followerId: String = "",
    val followingId: String = "",
    val createdAt: Long = 0L
)