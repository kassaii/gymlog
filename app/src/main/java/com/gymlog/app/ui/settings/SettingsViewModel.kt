package com.gymlog.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.model.User
import com.gymlog.app.data.repository.AuthRepository
import com.gymlog.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showUsernameDialog: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val newUsername: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val uid = authRepository.getCurrentUserId()
            if (uid == null) {
                _state.update { it.copy(isLoading = false, error = "Usuario no autenticado") }
                return@launch
            }
            userRepository.getUserProfile(uid).fold(
                onSuccess = { user ->
                    _state.update { it.copy(user = user, isLoading = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun onShowUsernameDialog(show: Boolean) {
        _state.update {
            it.copy(
                showUsernameDialog = show,
                newUsername = if (show) it.user?.username ?: "" else "",
                error = null
            )
        }
    }

    fun onShowPasswordDialog(show: Boolean) {
        _state.update {
            it.copy(
                showPasswordDialog = show,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                error = null
            )
        }
    }

    fun onNewUsernameChange(username: String) {
        _state.update { it.copy(newUsername = username) }
    }

    fun onCurrentPasswordChange(password: String) {
        _state.update { it.copy(currentPassword = password) }
    }

    fun onNewPasswordChange(password: String) {
        _state.update { it.copy(newPassword = password) }
    }

    fun onConfirmPasswordChange(password: String) {
        _state.update { it.copy(confirmPassword = password) }
    }

    fun updateUsername() {
        val newUsername = _state.value.newUsername.trim()
        if (newUsername.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }
            val uid = authRepository.getCurrentUserId() ?: return@launch

            userRepository.updateUsername(uid, newUsername).fold(
                onSuccess = {
                    _state.update { it.copy(isUpdating = false, showUsernameDialog = false, updateSuccess = true) }
                    loadUser()
                },
                onFailure = { e ->
                    _state.update { it.copy(isUpdating = false, error = e.message) }
                }
            )
        }
    }

    fun updatePassword() {
        val currentPassword = _state.value.currentPassword
        val newPassword = _state.value.newPassword
        val confirmPassword = _state.value.confirmPassword

        if (currentPassword.isEmpty()) {
            _state.update { it.copy(error = "Introduce tu contraseña actual") }
            return
        }

        if (newPassword.length < 6) {
            _state.update { it.copy(error = "La nueva contraseña debe tener al menos 6 caracteres") }
            return
        }

        if (newPassword != confirmPassword) {
            _state.update { it.copy(error = "Las contraseñas nuevas no coinciden") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }
            authRepository.updatePasswordWithReauth(currentPassword, newPassword).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            showPasswordDialog = false,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            updateSuccess = true
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isUpdating = false, error = e.message) }
                }
            )
        }
    }

    fun updateWeightUnit(useLbs: Boolean) {
        val uid = authRepository.getCurrentUserId() ?: return
        val newUnit = if (useLbs) "lbs" else "kg"

        viewModelScope.launch {
            userRepository.updateWeightUnit(uid, newUnit).fold(
                onSuccess = {
                    _state.update { it.copy(user = it.user?.copy(weightUnit = newUnit)) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun onUpdateHandled() {
        _state.update { it.copy(updateSuccess = false) }
    }
}