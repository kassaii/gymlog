
package com.gymlog.app.data.repository

import com.gymlog.app.data.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gymlog.app.util.Constants
import com.gymlog.app.util.UsernameValidator
import kotlinx.coroutines.tasks.await

// Handles user authentication and account registration using Firebase
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val usernamesCollection = firestore.collection("usernames")
    private val userPrivateCollection = firestore.collection("userPrivate")

    // Registers a new user after checking the username
    // and creates their account and profile in Firebase
    suspend fun register(email: String, password: String, username: String): Result<User> {
        return try {
            if (!UsernameValidator.isValid(username)) {
                return Result.failure(
                    Exception("El nombre debe tener entre 3 y 20 caracteres y solo contener letras, números o _")
                )
            }

            val usernameLower = UsernameValidator.normalize(username)

            val existing = usersCollection
                .whereEqualTo("usernameLower", usernameLower)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya está en uso"))
            }

            val usernameRef = usernamesCollection.document(usernameLower)

            if (usernameRef.get().await().exists()) {
                return Result.failure(Exception("El nombre de usuario ya está en uso"))
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("No se pudo crear el usuario"))

            try {
                createUserDocuments(uid, email, username)
            } catch (error: Exception) {
                try {
                    auth.currentUser?.takeIf { it.uid == uid }?.delete()?.await()
                } catch (_: Exception) {
                }
                throw error
            }

            val user = loadAuthenticatedUser(uid)
                ?: return Result.failure(Exception("No se pudo recuperar el perfil"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Creates the public profile, private data and username reservation
    // in a single Firestore transaction
    private suspend fun createUserDocuments(
        uid: String,
        email: String,
        username: String
    ) {
        val usernameLower = UsernameValidator.normalize(username)

        val userRef = usersCollection.document(uid)
        val privateRef = userPrivateCollection.document(uid)
        val usernameRef = usernamesCollection.document(usernameLower)

        val publicUser = mapOf(
            "uid" to uid,
            "username" to username,
            "usernameLower" to usernameLower,
            "photoUrl" to Constants.DEFAULT_USER_PHOTO,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val usernameSnapshot = transaction.get(usernameRef)

            if (userSnapshot.exists()) {
                throw Exception("El perfil de usuario ya existe")
            }

            if (usernameSnapshot.exists()) {
                throw Exception("El nombre de usuario ya está en uso")
            }

            transaction.set(
                usernameRef,
                mapOf("uid" to uid)
            )

            transaction.set(userRef, publicUser)

            transaction.set(
                privateRef,
                mapOf(
                    "email" to email,
                    "weightUnit" to "kg"
                )
            )

            Unit
        }.await()
    }

    // Loads the authenticated user's public and private information
    // from Firestore and combines it into a User object
    private suspend fun loadAuthenticatedUser(uid: String): User? {
        if (auth.currentUser?.uid != uid) {
            throw IllegalStateException("No autorizado")
        }

        val publicDocument = usersCollection.document(uid).get().await()
        val publicUser = publicDocument.toObject(User::class.java)
            ?: return null

        val privateDocument = userPrivateCollection.document(uid).get().await()

        if (!privateDocument.exists()) {
            throw IllegalStateException("Datos privados del usuario no encontrados")
        }

        return publicUser.copy(
            email = privateDocument.getString("email") ?: "",
            weightUnit = privateDocument.getString("weightUnit") ?: "kg",
            followingIds = emptyList(),
            followersCount = 0,
            followingCount = 0
        )
    }

    // Signs in a user with their email and password
    // and loads their profile from Firestore
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Credenciales incorrectas"))

            val user = loadAuthenticatedUser(uid)
                ?: return Result.failure(Exception("Perfil de usuario no encontrado"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Verifies the current password before updating it
    // with the new password in Firebase Authentication
    suspend fun updatePasswordWithReauth(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("No hay usuario logueado"))

            val email = user.email
                ?: return Result.failure(Exception("El usuario no tiene email"))

            val credential = EmailAuthProvider.getCredential(email, currentPassword)

            user.reauthenticate(credential).await()

            user.updatePassword(newPassword).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Signs out the current user from Firebase Authentication
    fun logout() {
        auth.signOut()
    }

    // Returns the current user's ID or null if no user is signed in
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
