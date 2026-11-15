package com.gymlog.app.data.repository

import com.gymlog.app.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gymlog.app.util.UsernameValidator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val usersCollection = firestore.collection("users")
    private val usernamesCollection = firestore.collection("usernames")
    private val userPrivateCollection = firestore.collection("userPrivate")
    private val followsCollection = firestore.collection("follows")

    // Gets the number of followers and followed users
    // using Firestore count queries
    private suspend fun loadFollowCounts(uid: String): Pair<Int, Int> {
        val followers = followsCollection
            .whereEqualTo("followingId", uid)
            .count()
            .get(com.google.firebase.firestore.AggregateSource.SERVER)
            .await()
            .count.toInt()

        val following = followsCollection
            .whereEqualTo("followerId", uid)
            .count()
            .get(com.google.firebase.firestore.AggregateSource.SERVER)
            .await()
            .count.toInt()

        return followers to following
    }

    // Loads a user profile and follow counts, adding private data
    // only when the profile belongs to the signed-in user
    private suspend fun loadUser(uid: String): User? {
        val document = usersCollection.document(uid).get().await()
        val publicUser = document.toObject(User::class.java) ?: return null

        val (followersCount, followingCount) = loadFollowCounts(uid)

        val user = publicUser.copy(
            email = "",
            weightUnit = "kg",
            followingIds = emptyList(),
            followersCount = followersCount,
            followingCount = followingCount
        )

        if (auth.currentUser?.uid != uid) {
            return user
        }

        val privateDocument = userPrivateCollection.document(uid).get().await()

        if (!privateDocument.exists()) {
            throw IllegalStateException("Datos privados del usuario no encontrados")
        }

        return user.copy(
            email = privateDocument.getString("email") ?: "",
            weightUnit = privateDocument.getString("weightUnit") ?: "kg"
        )
    }

    // Gets a user profile by ID and returns the result
    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val user = loadUser(uid)
                ?: return Result.failure(Exception("Perfil no encontrado"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listens for profile and follow changes in real time
    // and includes private data only for the signed-in user
    fun observeUserProfile(uid: String): Flow<User?> = callbackFlow {
        var publicUser: User? = null
        var privateEmail: String? = null
        var privateWeightUnit: String? = null
        var publicLoaded = false
        var privateLoaded = auth.currentUser?.uid != uid
        var followersCount = 0
        var followingCount = 0

        // Combines the latest profile data and sends it to the observer
        fun emitProfile() {
            if (!publicLoaded || !privateLoaded) return

            val user = publicUser

            if (user == null) {
                trySend(null)
                return
            }

            val result = user.copy(
                email = "",
                weightUnit = "kg",
                followingIds = emptyList(),
                followersCount = followersCount,
                followingCount = followingCount
            )

            if (auth.currentUser?.uid == uid) {
                trySend(
                    result.copy(
                        email = privateEmail ?: "",
                        weightUnit = privateWeightUnit ?: "kg"
                    )
                )
            } else {
                trySend(result)
            }
        }

        val publicListener = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                publicUser = snapshot?.toObject(User::class.java)
                publicLoaded = true
                emitProfile()
            }

        val privateListener = if (auth.currentUser?.uid == uid) {
            userPrivateCollection.document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        close(IllegalStateException("Datos privados no encontrados"))
                        return@addSnapshotListener
                    }

                    privateEmail = snapshot.getString("email")
                    privateWeightUnit = snapshot.getString("weightUnit")
                    privateLoaded = true
                    emitProfile()
                }
        } else {
            null
        }

        val followersListener = followsCollection
            .whereEqualTo("followingId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                followersCount = snapshot?.size() ?: 0
                emitProfile()
            }

        val followingListener = followsCollection
            .whereEqualTo("followerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                followingCount = snapshot?.size() ?: 0
                emitProfile()
            }

        awaitClose {
            publicListener.remove()
            privateListener?.remove()
            followersListener.remove()
            followingListener.remove()
        }
    }

    // Listens for changes to the signed-in user's weight unit
    fun observeWeightUnit(uid: String): Flow<String> = callbackFlow {
        if (auth.currentUser?.uid != uid) {
            close(IllegalStateException("No autorizado"))
        } else {
            val listener = userPrivateCollection.document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        close(IllegalStateException("Datos privados no encontrados"))
                        return@addSnapshotListener
                    }

                    trySend(snapshot.getString("weightUnit") ?: "kg")
                }

            awaitClose { listener.remove() }
        }
    }

    // Searches for up to 20 users whose usernames start with the query
    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            if (query.isBlank()) return Result.success(emptyList())

            val queryLower = UsernameValidator.normalize(query)

            val snapshot = usersCollection
                .orderBy("usernameLower")
                .startAt(queryLower)
                .endAt(queryLower + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { document ->
                val user = document.toObject(User::class.java)
                    ?: return@mapNotNull null

                user.copy(
                    email = "",
                    weightUnit = "kg",
                    followingIds = emptyList(),
                    followersCount = 0,
                    followingCount = 0
                )
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Checks the new username and updates its reservation
    // and the user profile in one Firestore transaction
    suspend fun updateUsername(uid: String, newUsername: String): Result<Unit> {
        return try {
            if (auth.currentUser?.uid != uid) {
                return Result.failure(Exception("No autorizado"))
            }

            if (!UsernameValidator.isValid(newUsername)) {
                return Result.failure(
                    Exception("El nombre debe tener entre 3 y 20 caracteres y solo contener letras, números o _")
                )
            }

            val newUsernameLower = UsernameValidator.normalize(newUsername)
            val userRef = usersCollection.document(uid)
            val newUsernameRef = usernamesCollection.document(newUsernameLower)

            val existing = usersCollection
                .whereEqualTo("usernameLower", newUsernameLower)
                .limit(1)
                .get()
                .await()

            if (existing.documents.any { it.id != uid }) {
                return Result.failure(Exception("El nombre de usuario ya está en uso"))
            }

            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)

                if (!userSnapshot.exists()) {
                    throw Exception("Perfil no encontrado")
                }

                val oldUsernameLower = userSnapshot.getString("usernameLower")
                    ?: throw Exception("Nombre de usuario actual no encontrado")

                val newUsernameSnapshot = transaction.get(newUsernameRef)

                if (newUsernameSnapshot.exists() &&
                    newUsernameSnapshot.getString("uid") != uid
                ) {
                    throw Exception("El nombre de usuario ya está en uso")
                }

                val oldUsernameRef = usernamesCollection.document(oldUsernameLower)

                if (oldUsernameLower != newUsernameLower) {
                    val oldUsernameSnapshot = transaction.get(oldUsernameRef)

                    if (oldUsernameSnapshot.exists() &&
                        oldUsernameSnapshot.getString("uid") != uid
                    ) {
                        throw Exception("La reserva del nombre actual no pertenece al usuario")
                    }

                    if (oldUsernameSnapshot.exists()) {
                        transaction.delete(oldUsernameRef)
                    }
                }

                if (!newUsernameSnapshot.exists()) {
                    transaction.set(newUsernameRef, mapOf("uid" to uid))
                }

                transaction.update(
                    userRef,
                    "username", newUsername,
                    "usernameLower", newUsernameLower
                )

                Unit
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Checks the selected unit and saves it in the user's private data
    suspend fun updateWeightUnit(uid: String, unit: String): Result<Unit> {
        return try {
            if (auth.currentUser?.uid != uid) {
                return Result.failure(Exception("No autorizado"))
            }

            if (unit !in listOf("kg", "lbs")) {
                return Result.failure(Exception("Unidad de peso no válida"))
            }

            userPrivateCollection.document(uid)
                .update("weightUnit", unit)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets the IDs of users followed by the given user
    suspend fun getFollowingIds(uid: String): Result<List<String>> {
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followerId", uid)
                .get()
                .await()

            Result.success(
                snapshot.documents.mapNotNull {
                    it.getString("followingId")
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets the IDs of users who follow the given user
    suspend fun getFollowersIds(uid: String): Result<List<String>> {
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followingId", uid)
                .get()
                .await()

            Result.success(
                snapshot.documents.mapNotNull {
                    it.getString("followerId")
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listens for changes to whether one user follows another
    fun observeIsFollowing(currentUid: String, targetUid: String): Flow<Boolean> =
        callbackFlow {
            if (auth.currentUser?.uid != currentUid) {
                close(IllegalStateException("No autorizado"))
            } else {
                val followId = "${currentUid}_${targetUid}"

                val listener = followsCollection.document(followId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        trySend(snapshot?.exists() == true)
                    }

                awaitClose { listener.remove() }
            }
        }

    // Creates a follow if it does not already exist
    suspend fun followUser(currentUid: String, targetUid: String): Result<Unit> {
        if (currentUid == targetUid) {
            return Result.failure(Exception("No puedes seguirte a ti mismo"))
        }

        return try {
            if (auth.currentUser?.uid != currentUid) {
                return Result.failure(Exception("No autorizado"))
            }

            val targetDocument = usersCollection.document(targetUid).get().await()

            if (!targetDocument.exists()) {
                return Result.failure(Exception("Usuario objetivo no encontrado"))
            }

            val followId = "${currentUid}_${targetUid}"
            val followRef = followsCollection.document(followId)

            firestore.runTransaction { transaction ->
                val existing = transaction.get(followRef)

                if (!existing.exists()) {
                    transaction.set(
                        followRef,
                        mapOf(
                            "followerId" to currentUid,
                            "followingId" to targetUid,
                            "createdAt" to System.currentTimeMillis()
                        )
                    )
                }

                Unit
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Removes the follow between two users
    suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit> {
        return try {
            if (auth.currentUser?.uid != currentUid) {
                return Result.failure(Exception("No autorizado"))
            }

            val followId = "${currentUid}_${targetUid}"
            followsCollection.document(followId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}