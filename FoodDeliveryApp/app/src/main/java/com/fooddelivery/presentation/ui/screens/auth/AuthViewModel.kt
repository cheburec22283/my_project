package com.fooddelivery.presentation.ui.screens.auth

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    /** Возвращает true, если пользователь уже залогинен (используется в SplashScreen). */
    fun isAuthenticated(): Boolean = authRepository.isAuthenticated()

    fun signIn() {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState.Error("Заполните email и пароль")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            authRepository.signIn(email.trim(), password)
                .onSuccess { _state.value = AuthUiState.Success }
                .onFailure { _state.value = AuthUiState.Error(it.message ?: "Ошибка входа") }
        }
    }

    fun signUp() {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() -> {
                _state.value = AuthUiState.Error("Заполните все поля")
                return
            }
            password.length < 6 -> {
                _state.value = AuthUiState.Error("Пароль должен быть не менее 6 символов")
                return
            }
            password != confirmPassword -> {
                _state.value = AuthUiState.Error("Пароли не совпадают")
                return
            }
        }
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            authRepository.signUp(name.trim(), email.trim(), password)
                .onSuccess { _state.value = AuthUiState.Success }
                .onFailure { _state.value = AuthUiState.Error(it.message ?: "Ошибка регистрации") }
        }
    }

    fun reset() { _state.value = AuthUiState.Idle }
}
