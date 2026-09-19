package com.voiceshield.ai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceshield.ai.domain.model.User
import com.voiceshield.ai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val currentUser: User? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }

    fun login(email: String, pass: String, onNavigateHome: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter both email and password.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = authRepository.login(email.trim(), pass)
            res.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    onNavigateHome()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = err.localizedMessage ?: "Login failed")
                }
            )
        }
    }

    fun loginAsDev(onNavigateHome: () -> Unit) {
        login("dev@voiceshield.local", "devpassword123", onNavigateHome)
    }

    fun register(name: String, email: String, pass: String, confirmPass: String, onNavigateHome: () -> Unit) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "All fields are required.")
            return
        }
        if (pass != confirmPass) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = authRepository.register(name.trim(), email.trim(), pass)
            res.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    onNavigateHome()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = err.localizedMessage ?: "Registration failed")
                }
            )
        }
    }

    fun resetPassword(email: String, onSuccessMessage: (String) -> Unit) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your registered email.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = authRepository.resetPassword(email.trim())
            res.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccessMessage("Password reset link sent to $email")
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = err.localizedMessage ?: "Reset failed")
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
