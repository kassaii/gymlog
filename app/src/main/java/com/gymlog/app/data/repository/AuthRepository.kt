package com.gymlog.app.data.repository

import com.gymlog.app.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gymlog.app.util.Constants
import kotlinx.coroutines.tasks.await

// Repository responsible for user authentication

class AuthRepository {

    // Instance to manage authentication
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Firestore service instance to manage the database
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Shortcut to access the users collection
    private val usersCollection = firestore.collection("users")

    // Function to register a new user
    // Uses suspend and await() to avoid blocking the main thread
    // Uses Result to wrap the outcome as success (User) or failure (exception)
    suspend fun register(email: String, password: String, username: String): Result<User> {
        return try {
            // Check if the username already exists
            val usernameLower = username.lowercase()
            val existing = usersCollection
                .whereEqualTo("usernameLower", usernameLower)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya está en uso"))
            }

            // Create account in Firebase Auth
            // Returns an error if the User is null
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("No se pudo crear el usuario"))

            // Register user in Firestore
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

            // Save the user profile in Firestore
            usersCollection.document(uid).set(newUser).await()

            Result.success(newUser)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Function to log in a user
    suspend fun login(email: String, password: String): Result<User> {
        return try {

            // Get the uid using email and password
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Credenciales incorrectas"))

            // Convert Firestore document to object
            // Returns an error if the user document does not exist in Firestore
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
                ?: return Result.failure(Exception("Perfil de usuario no encontrado"))

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    // Logs out the user
    fun logout() {
        auth.signOut()
    }

    // Returns the logged-in user's uid or null if no user is logged in
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}