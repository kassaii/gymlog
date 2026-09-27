package com.gymlog.app.data.repository

import com.gymlog.app.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Repositorio encargado de la autenticación del usuario.

class AuthRepository {

    // Instancia que permite gestionar la autenticación.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Instancia del servicio Firestore para gestionar la base de datos.
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Atajo para acceder a la colección de usuarios.
    private val usersCollection = firestore.collection("users")

    // Función que permite el registro del usuario.
    // Se utiliza suspend y await() para evitar bloquear el hilo principal
    // Se utiliza Result para envolver el resultado en forma de éxito (User) o fallo (excepción).
    suspend fun register(email: String, password: String, username: String): Result<User> {
        return try {
            // Comprobar la existencia del username.
            val usernameLower = username.lowercase()
            val existing = usersCollection
                .whereEqualTo("usernameLower", usernameLower)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya está en uso"))
            }

            // Crear cuenta en Firebase Auth.
            // Se devuelve un error si el User es null.
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("No se pudo crear el usuario"))

            // Registrar usuario en Firestore.
            val newUser = User(
                uid = uid,
                email = email,
                username = username,
                usernameLower = usernameLower,
                weightUnit = "kg",
                createdAt = System.currentTimeMillis(),
                followersCount = 0,
                followingCount = 0
            )

            // Guardar el perfil del usuario en Firestore.
            usersCollection.document(uid).set(newUser).await()

            Result.success(newUser)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Función que permite el inicio de sesión del usuario.
    suspend fun login(email: String, password: String): Result<User> {
        return try {

            // Se obtiene el uid mediante el correo y contraseña.
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Credenciales incorrectas"))

            // Conversión del documento de Firestore a objeto.
            // Se devuelve un error en caso de que el documento del usuario no exista en Firestore.
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
                ?: return Result.failure(Exception("Perfil de usuario no encontrado"))

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cierra sesión del usuario.
    fun logout() {
        auth.signOut()
    }

    // Devuelve el uid del usuario logueado o null si no hay un usuario logueado.
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}