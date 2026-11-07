package com.gymlog.app.data.repository

import com.gymlog.app.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gymlog.app.util.Constants
import kotlinx.coroutines.tasks.await

// Repository responsible for user authentication
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // Registers a new user in Firebase Auth and creates their profile in Firestore
    suspend fun register(email: String, password: String, username: String): Result<User> {
        return try {
            val usernameLower = username.lowercase()
            val existing = usersCollection
                .whereEqualTo("usernameLower", usernameLower)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya está en uso"))
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("No se pudo crear el usuario"))

            val newUser = User(
                uid = uid,
                email = email,
                username = username,
                usernameLower = usernameLower,
                photoUrl = Constants.DEFAULT_USER_PHOTO,
                weightUnit = "kg",
                createdAt = System.currentTimeMillis(),
                followersCount = 0,
                followingCount = 0
            )

            usersCollection.document(uid).set(newUser).await()

            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logs in a user and retrieves their profile from Firestore
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Credenciales incorrectas"))

            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
                ?: return Result.failure(Exception("Perfil de usuario no encontrado"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Signs out the current user
    fun logout() {
        auth.signOut()
    }

    // Returns the current user's ID or null if not logged in
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}