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

    // Update user's username
    suspend fun updateUsername(uid: String, newUsername: String): Result<Unit> {
        return try {
            val newUsernameLower = newUsername.lowercase()
            val existing = usersCollection.whereEqualTo("usernameLower", newUsernameLower).get().await()
            if (!existing.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya está en uso"))
            }
            usersCollection.document(uid).update(
                "username", newUsername,
                "usernameLower", newUsernameLower
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update user's weight unit
    suspend fun updateWeightUnit(uid: String, unit: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("weightUnit", unit).await()
            Result.success(Unit)
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

    // Follows a user using a batch to update both users atomically
    suspend fun followUser(currentUid: String, targetUid: String): Result<Unit> {
        if (currentUid == targetUid) {
            return Result.failure(Exception("No puedes seguirte a ti mismo"))
        }

        return try {
            val currentUserRef = usersCollection.document(currentUid)
            val targetUserRef = usersCollection.document(targetUid)

            val currentUser = currentUserRef.get().await().toObject(User::class.java)
                ?: return Result.failure(Exception("Usuario actual no encontrado"))

            val targetUser = targetUserRef.get().await().toObject(User::class.java)
                ?: return Result.failure(Exception("Usuario objetivo no encontrado"))

            if (currentUser.followingIds.contains(targetUid)) {
                return Result.success(Unit)
            }

            val newFollowingIds = currentUser.followingIds + targetUid
            val newFollowersCount = targetUser.followersCount + 1

            val batch = firestore.batch()
            batch.update(currentUserRef, "followingIds", newFollowingIds)
            batch.update(currentUserRef, "followingCount", newFollowingIds.size)
            batch.update(targetUserRef, "followersCount", newFollowersCount)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Unfollows a user using a batch
    suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit> {
        return try {
            val currentUserRef = usersCollection.document(currentUid)
            val targetUserRef = usersCollection.document(targetUid)

            val currentUser = currentUserRef.get().await().toObject(User::class.java)
                ?: return Result.failure(Exception("Usuario actual no encontrado"))

            val targetUser = targetUserRef.get().await().toObject(User::class.java)
                ?: return Result.failure(Exception("Usuario objetivo no encontrado"))

            if (!currentUser.followingIds.contains(targetUid)) {
                return Result.success(Unit)
            }

            val newFollowingIds = currentUser.followingIds - targetUid
            val newFollowersCount = (targetUser.followersCount - 1).coerceAtLeast(0)

            val batch = firestore.batch()
            batch.update(currentUserRef, "followingIds", newFollowingIds)
            batch.update(currentUserRef, "followingCount", newFollowingIds.size)
            batch.update(targetUserRef, "followersCount", newFollowersCount)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}