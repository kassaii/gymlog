package com.gymlog.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la pantalla de login.
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

// ViewModel de la pantalla de login.
class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()


    // Estado interno (mutable y privado). Solo el ViewModel puede modificarlo.
    private val _state = MutableStateFlow(LoginUiState())

    // Estado expuesto (inmutable y público). La Screen solo puede leerlo.
    // El patrón evita que la Screen modifique el estado del ViewModel por error.
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onLoginClicked() {
        viewModelScope.launch {
            // Indicar que está cargando y limpiar error previo.
            _state.update { it.copy(isLoading = true, error = null) }

            // Llamar al repositorio.
            val result = authRepository.login(
                email = _state.value.email,
                password = _state.value.password
            )

            // Gestionar el resultado.
            // Fold maneja éxito y fracaso.
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