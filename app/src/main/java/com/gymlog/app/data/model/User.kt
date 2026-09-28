package com.gymlog.app.data.model

 // Clase que representa a un usuario de la aplicación.
 // El uid es el identificador único que genera Firebase.
 // usernameLower es la version de username en minúsculas
 // para poder realizar búsquedas insensibles a mayúsculas y minúsculas en Firestore.

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val weightUnit: String = "kg",
    val createdAt: Long = 0L,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)