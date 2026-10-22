package com.gymlog.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Login screen state
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

// Login screen ViewModel
class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()


    // Internal state (mutable and private). Only the ViewModel can modify it
    private val _state = MutableStateFlow(LoginUiState())

    // Exposed state (immutable and public). The Screen can only read it
    // The pattern prevents the Screen from modifying the ViewModel's state by mistake
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onLoginClicked() {
        viewModelScope.launch {
            // Set loading to true and clear previous error
            _state.update { it.copy(isLoading = true, error = null) }

            // Call the repository
            val result = authRepository.login(
                email = _state.value.email,
                password = _state.value.password
            )

            // Handle the result
            // Fold handles both success and failure
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