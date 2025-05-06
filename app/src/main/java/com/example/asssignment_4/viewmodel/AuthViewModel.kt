package com.example.asssignment_4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.User
import com.example.asssignment_4.model.LoginRequest
import com.example.asssignment_4.model.RegisterRequest
import com.example.asssignment_4.repository.AuthRepository
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthEvent {
    data class Success(val message: String) : AuthEvent()
    data class Failure(val message: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cookieJar: PersistentCookieJar
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent: SharedFlow<AuthEvent> = _authEvent.asSharedFlow()

    init {
        // Check session on init
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                val response = authRepository.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    _currentUser.value = response.body()
                    _authError.value = null
                    _authEvent.emit(AuthEvent.Success("Session restored"))
                } else {
                    _currentUser.value = null
                    if (response.code() != 401) {
                        val errorMsg = "Auth Check Failed: ${response.code()}"
                        _authError.value = errorMsg
                        _authEvent.emit(AuthEvent.Failure(errorMsg))
                    } else {
                        // Clear session on 401
                        _authError.value = null
                        cookieJar.clearSession()
                    }
                }
            } catch (e: Exception) {
                _currentUser.value = null
                val errorMsg = "Error checking login: ${e.message}"
                _authError.value = errorMsg
                _authEvent.emit(AuthEvent.Failure(errorMsg))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                val response = authRepository.login(email, password)
                if (response.isSuccessful && response.body()?.success == true) {
                    _currentUser.value = response.body()?.user
                    // PersistentCookieJar handles cookie persistence automatically
                    _authEvent.emit(AuthEvent.Success("Logged in successfully"))
                    // Add delay before navigation
                    kotlinx.coroutines.delay(1500)
                } else {
                    val errorMsg = response.body()?.message ?: "Login failed: ${response.code()}"
                    _authError.value = errorMsg
                    _authEvent.emit(AuthEvent.Failure(errorMsg))
                }
            } catch (e: Exception) {
                val errorMsg = "Login error: ${e.message}"
                _authError.value = errorMsg
                _authEvent.emit(AuthEvent.Failure(errorMsg))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                val response = authRepository.register(email, password, name)
                if (response.isSuccessful && response.body()?.success == true) {
                    _currentUser.value = response.body()?.user
                    // PersistentCookieJar handles cookie persistence automatically
                    _authEvent.emit(AuthEvent.Success("Registered successfully"))
                    // Add delay before navigation
                    kotlinx.coroutines.delay(1500)
                } else {
                    val errorMsg = response.body()?.message ?: "Registration failed: ${response.code()}"
                    _authError.value = errorMsg
                    _authEvent.emit(AuthEvent.Failure(errorMsg))
                }
            } catch (e: Exception) {
                val errorMsg = "Registration error: ${e.message}"
                _authError.value = errorMsg
                _authEvent.emit(AuthEvent.Failure(errorMsg))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                val response = authRepository.logout()
                if (response.isSuccessful || response.code() == 401) { // Treat 401 as success if already logged out
                    _currentUser.value = null
                    _authError.value = null
                    cookieJar.clearSession()
                    _authEvent.emit(AuthEvent.Success("Logged out successfully"))
                } else {
                    val errorMsg = "Logout Failed: ${response.code()}"
                    _authError.value = errorMsg
                    _authEvent.emit(AuthEvent.Failure(errorMsg))
                }
            } catch (e: Exception) {
                val errorMsg = "Error during logout: ${e.message}"
                _authError.value = errorMsg
                _authEvent.emit(AuthEvent.Failure(errorMsg))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                // Assuming authRepository.deleteAccount() will be created
                val response = authRepository.deleteAccount()
                if (response.isSuccessful) {
                    _currentUser.value = null
                    cookieJar.clearSession()
                    _authEvent.emit(AuthEvent.Success("Deleted user successfully"))
                } else {
                    val errorMsg = "Account deletion failed: ${response.code()} - ${response.message()}"
                    _authError.value = errorMsg
                    _authEvent.emit(AuthEvent.Failure(errorMsg))
                }
            } catch (e: Exception) {
                val errorMsg = "Error deleting account: ${e.message}"
                _authError.value = errorMsg
                _authEvent.emit(AuthEvent.Failure(errorMsg))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
