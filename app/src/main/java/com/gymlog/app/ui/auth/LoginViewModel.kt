
package com.gymlog.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Represents the current state of the login screen
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

// Manages the login screen state and user authentication
class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // Updates the email entered by the user in the login form
    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email) }
    }

    // Updates the password entered by the user in the login form
    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password) }
    }

    // Tries to log in using the entered credentials
    // and updates the screen state depending on the result
    fun onLoginClicked() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.login(
                email = _state.value.email,
                password = _state.value.password
            )

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, loginSuccess = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }
}
