package com.example.asssignment_4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.User
import com.example.asssignment_4.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        checkLoginStatus() // Check login status when ViewModel is created
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val response = authRepository.getProfile()
                if (response.isSuccessful) {
                    _currentUser.value = response.body()
                    _authError.value = null
                } else {
                    // Handle non-successful responses, e.g., 401 Unauthorized means not logged in
                    _currentUser.value = null
                     if (response.code() != 401) { // Don't show error just for not being logged in
                         _authError.value = "Auth Check Failed: ${response.code()}"
                     } else {
                         _authError.value = null // Clear error if it was just 401
                     }
                }
            } catch (e: Exception) {
                _currentUser.value = null
                _authError.value = "Error checking login: ${e.message}"
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            try {
                val response = authRepository.logout()
                if (response.isSuccessful || response.code() == 401) { // Treat 401 as successful logout
                    _currentUser.value = null
                    _authError.value = null
                    // Optionally clear cookies or other session data here if needed
                } else {
                    _authError.value = "Logout Failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _authError.value = "Error during logout: ${e.message}"
            }
        }
    }

    // TODO: Add loginUser and registerUser functions if needed for login/register screens
    // These would call authRepository.login/register and update _currentUser on success
}
