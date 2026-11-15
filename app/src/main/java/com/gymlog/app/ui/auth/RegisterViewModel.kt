
package com.gymlog.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Represents the current state of the register screen
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registerSuccess: Boolean = false
)

// Manages the register screen state and user registration
class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    // Updates the email entered by the user in the register form
    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email) }
    }

    // Updates the password entered by the user in the register form
    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password) }
    }

    // Updates the username entered by the user in the register form
    fun onUsernameChange(username: String) {
        _state.update { it.copy(username = username) }
    }

    // Validates the entered data and attempts to register a new user
    // through the repository, updating the screen state with the result
    fun onRegisterClicked() {
        if (_state.value.isLoading) return

        val email = _state.value.email
        val password = _state.value.password
        val username = _state.value.username

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
            _state.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.register(email, password, username)

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, registerSuccess = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }
}
