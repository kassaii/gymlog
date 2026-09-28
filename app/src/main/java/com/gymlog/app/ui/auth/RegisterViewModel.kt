package com.gymlog.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la pantalla de register.
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registerSuccess: Boolean = false
)

// ViewModel de la pantalla de register.
class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()


    // Estado interno (mutable y privado). Solo el ViewModel puede modificarlo.
    private val _state = MutableStateFlow(RegisterUiState())

    // Estado expuesto (inmutable y público). La Screen solo puede leerlo.
    // El patrón evita que la Screen modifique el estado del ViewModel por error.
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onUsernameChange(username: String) {
        _state.update { it.copy(username = username) }
    }

    fun onRegisterClicked() {
        val email = _state.value.email
        val password = _state.value.password
        val username = _state.value.username

        // Validaciones extra
        if (email.isBlank()) {
            _state.update { it.copy(error = "El email no puede estar vacío") }
            return
        }

        if (!email.contains("@") || !email.contains(".")) {
            _state.update { it.copy(error = "El email no es válido") }
            return
        }

        if (password.length < 6) {
            _state.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
            return
        }

        if (username.isBlank()) {
            _state.update { it.copy(error = "El nombre de usuario no puede estar vacío") }
            return
        }

        viewModelScope.launch {

            // Indicar que está cargando y limpiar error previo.
            _state.update { it.copy(isLoading = true, error = null) }

            // Llamar al repositorio.
            val result = authRepository.register(email, password, username)

            // Gestionar el resultado.
            // Fold maneja éxito y fracaso.
            result.fold(
                onSuccess = { _state.update { it.copy(isLoading = false, registerSuccess = true) } },
                onFailure = { error -> _state.update { it.copy(isLoading = false, error = error.message) } }
            )
        }
    }
}