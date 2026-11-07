package com.gymlog.app.data.repository

import com.gymlog.app.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Repository responsible for user data
class UserRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // Gets a user's profile from their uid
    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
                ?: return Result.failure(Exception("Perfil no encontrado"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Searches for users by username prefix
    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            if (query.isBlank()) return Result.success(emptyList())

            val queryLower = query.lowercase()
            val snapshot = usersCollection
                .orderBy("usernameLower")
                .startAt(queryLower)
                .endAt(queryLower + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets the list of user IDs that the given user follows
    suspend fun getFollowingIds(uid: String): Result<List<String>> {
        return try {
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user?.followingIds ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}