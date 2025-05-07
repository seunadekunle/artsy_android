package com.example.asssignment_4.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.User
import com.example.asssignment_4.model.LoginRequest
import com.example.asssignment_4.model.RegisterRequest
import com.example.asssignment_4.repository.AuthRepository
import com.example.asssignment_4.util.AuthManager
import com.example.asssignment_4.util.AuthManagerEvent
import com.example.asssignment_4.util.AuthManagerEvent.Success
import com.example.asssignment_4.util.AuthManagerEvent.Failure
import com.example.asssignment_4.util.AuthManagerEvent.SessionExpired
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Add UserState sealed class for better state handling
sealed class UserState {
    data object Loading : UserState()
    data class Success(val user: User) : UserState()
    data object NotLoggedIn : UserState()
    data class Error(val message: String) : UserState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authManager: AuthManager
) : ViewModel() {

    // Update to use UserState instead of nullable User
    private val _userState = MutableStateFlow<UserState>(UserState.NotLoggedIn)
    val userState: StateFlow<UserState> = _userState.asStateFlow()
    
    // Keep currentUser for backward compatibility
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Use isLoggedIn from AuthManager
    val isLoggedIn = authManager.isLoggedIn

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Use authEvent from AuthManager
    val authEvent = authManager.authEvent

    init {
        // Check session on init
        checkLoginStatus()
    }
    
    /**
     * Handles 401 Unauthorized responses from the API
     * This method should be called when an API call fails with a 401 error
     * Delegates to AuthManager
     */
    fun handleUnauthorized() {
        viewModelScope.launch {
            _currentUser.value = null
            _userState.value = UserState.NotLoggedIn
            authManager.handleUnauthorized()
        }
    }

    // Use manuallyLoggedOut from AuthManager instead of local variable
    val manuallyLoggedOut = authManager.manuallyLoggedOut
    
    fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            _userState.value = UserState.Loading // Set loading state immediately
            
            try {
                // Use AuthManager to check if we have a token
                val hasToken = authManager.isLoggedIn.value
                val isManuallyLoggedOut = authManager.manuallyLoggedOut.value
                Log.d("AuthViewModel", "checkLoginStatus - Has token: $hasToken, manuallyLoggedOut: $isManuallyLoggedOut")
                
                if (hasToken) {
                    // We have a token, so we're logged in
                    fetchUserProfile()
                } else if (!manuallyLoggedOut.value) {
                    // Only try to restore cookie session if user hasn't manually logged out
                    Log.d("AuthViewModel", "No token, checking for cookie-based session")
                    
                    // Check cookie-based session
                    val response = authRepository.getProfile()
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()
                        Log.d("AuthViewModel", "Profile response user: $user")
                        Log.d("AuthViewModel", "User avatarUrl: ${user?.avatarUrl}, fullName: ${user?.fullName}")
                        
                        // Enhanced debug for profile image URL issues
                        if (user?.avatarUrl.isNullOrEmpty()) {
                            Log.w("AuthViewModel", "Profile image URL is null or empty! Raw response: ${response.raw().body}")
                        } else {
                            Log.d("AuthViewModel", "Profile image URL found: ${user?.avatarUrl}")
                        }
                        
                        if (user != null) {
                            _currentUser.value = user
                            _userState.value = UserState.Success(user)
                        } else {
                            _userState.value = UserState.NotLoggedIn
                        }
                        _authError.value = null
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Success("Session restored"))
                        }
                    } else {
                        _currentUser.value = null
                        _userState.value = UserState.NotLoggedIn
                        if (response.code() != 401) {
                            val errorMsg = "Auth Check Failed: ${response.code()}"
                            _authError.value = errorMsg
                            _userState.value = UserState.Error(errorMsg)
                            viewModelScope.launch {
                                authManager.emitAuthEvent(Failure(errorMsg))
                            }
                        } else {
                            // Clear session on 401
                            _authError.value = null
                            authManager.clearAuthState()
                        }
                    }
                } else {
                    Log.d("AuthViewModel", "User previously logged out manually, not attempting to restore session")
                    _currentUser.value = null
                    _userState.value = UserState.NotLoggedIn
                }
            } catch (e: Exception) {
                _currentUser.value = null
                val errorMsg = "Error checking login: ${e.message}"
                _authError.value = errorMsg
                _userState.value = UserState.Error(errorMsg)
                viewModelScope.launch {
                    authManager.emitAuthEvent(Failure(errorMsg))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun fetchUserProfile() {
        viewModelScope.launch {
            _userState.value = UserState.Loading // Set loading state
            try {
                Log.d("AuthViewModel", "Fetching user profile...")
                val response = authRepository.getProfile()
                if (response.isSuccessful) {
                    val user = response.body()
                    Log.d("AuthViewModel", "Fetched profile: $user")
                    if (user != null) {
                        Log.d("AuthViewModel", "User is not null, attempting to set _currentUser.value. User: $user")
                        _currentUser.value = user
                        _userState.value = UserState.Success(user)
                        Log.d("AuthViewModel", "Updated currentUser.value = ${_currentUser.value}")
                    } else {
                        Log.w("AuthViewModel", "User profile is null despite successful response")
                        _userState.value = UserState.Error("Profile data missing")
                    }
                } else {
                    Log.w("AuthViewModel", "Failed to fetch profile: ${response.code()}")
                    _userState.value = UserState.Error("Failed to fetch profile: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching profile: ${e.message}")
                _userState.value = UserState.Error("Error fetching profile: ${e.message}")
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            _userState.value = UserState.Loading
            try {
                val response = authRepository.login(email, password)
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.user
                    val token = response.body()?.token
                    
                    Log.d("AuthViewModel", "Login response user: $user")
                    Log.d("AuthViewModel", "User avatarUrl: ${user?.avatarUrl}, fullName: ${user?.fullName}")
                    Log.d("AuthViewModel", "JWT Token received: ${token != null}")
                    
                    // Enhanced debug for profile image URL issues
                    if (user?.avatarUrl.isNullOrEmpty()) {
                        Log.w("AuthViewModel", "LOGIN: Profile image URL is null or empty!")
                    } else {
                        Log.d("AuthViewModel", "LOGIN: Profile image URL found: ${user?.avatarUrl}")
                    }
                    
                    // Mark as logged in if we have a token, even if user is null
                    if (token != null) {
                        authManager.saveAuthToken(token)
                        
                        // Set user if available, otherwise fetch profile
                        if (user != null) {
                            _currentUser.value = user
                            _userState.value = UserState.Success(user)
                        } else {
                            // User data is null, try to fetch profile separately
                            Log.d("AuthViewModel", "User data is null in login response, fetching profile...")
                            fetchUserProfile()
                        }
                        
                        // Emit success event
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Success("Logged in successfully"))
                        }
                    } else {
                        // No token received - this shouldn't happen with success=true
                        Log.w("AuthViewModel", "Login successful but no token received")
                        _authError.value = "Authentication error: No token received"
                        _userState.value = UserState.Error("Authentication error: No token received")
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Failure("Authentication error: No token received"))
                        }
                    }
                    // Add delay before navigation
                    kotlinx.coroutines.delay(1500)
                } else {
                    val errorMsg = response.body()?.message ?: "Login failed: ${response.code()}"
                    _authError.value = errorMsg
                    _userState.value = UserState.Error(errorMsg)
                    viewModelScope.launch {
                        authManager.emitAuthEvent(Failure(errorMsg))
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Login error: ${e.message}"
                _authError.value = errorMsg
                _userState.value = UserState.Error(errorMsg)
                viewModelScope.launch {
                    authManager.emitAuthEvent(Failure(errorMsg))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            _userState.value = UserState.Loading
            try {
                val response = authRepository.register(email, password, name)
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.user
                    val token = response.body()?.token
                    
                    Log.d("AuthViewModel", "Register response user: $user")
                    Log.d("AuthViewModel", "User avatarUrl: ${user?.avatarUrl}, fullName: ${user?.fullName}")
                    Log.d("AuthViewModel", "JWT Token received: ${token != null}")
                    
                    // Enhanced debug for profile image URL issues
                    if (user?.avatarUrl.isNullOrEmpty()) {
                        Log.w("AuthViewModel", "REGISTER: Profile image URL is null or empty!")
                    } else {
                        Log.d("AuthViewModel", "REGISTER: Profile image URL found: ${user?.avatarUrl}")
                    }
                    
                    // Mark as logged in if we have a token, even if user is null
                    if (token != null) {
                        authManager.saveAuthToken(token)
                        
                        // Set user if available, otherwise fetch profile
                        if (user != null) {
                            _currentUser.value = user
                            _userState.value = UserState.Success(user)
                        } else {
                            // User data is null, try to fetch profile separately
                            Log.d("AuthViewModel", "User data is null in register response, fetching profile...")
                            fetchUserProfile()
                        }
                        
                        // Emit success event through AuthManager
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Success("Registered successfully"))
                        }
                        // Add delay before navigation
                        kotlinx.coroutines.delay(1500)
                    } else {
                        // No token received - this shouldn't happen with success=true
                        Log.w("AuthViewModel", "Registration successful but no token received")
                        _authError.value = "Authentication error: No token received"
                        _userState.value = UserState.Error("Authentication error: No token received")
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Failure("Authentication error: No token received"))
                        }
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Registration failed: ${response.code()}"
                    _authError.value = errorMsg
                    _userState.value = UserState.Error(errorMsg)
                    viewModelScope.launch {
                        authManager.emitAuthEvent(Failure(errorMsg))
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Registration error: ${e.message}"
                _authError.value = errorMsg
                _userState.value = UserState.Error(errorMsg)
                viewModelScope.launch {
                    authManager.emitAuthEvent(Failure(errorMsg))
                }
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
                // The manuallyLoggedOut flag will be set by AuthManager.clearAuthState()
                
                val response = authRepository.logout()
                if (response.isSuccessful || response.code() == 401) { // Treat 401 as success if already logged out
                    _currentUser.value = null
                    _userState.value = UserState.NotLoggedIn
                    _authError.value = null
                    authManager.clearAuthState() // Use AuthManager to clear session
                    viewModelScope.launch {
                        authManager.emitAuthEvent(Success("Logged out successfully"))
                    }
                } else {
                    val errorMsg = "Logout Failed: ${response.code()}"
                    _authError.value = errorMsg
                    _userState.value = UserState.Error(errorMsg)
                    viewModelScope.launch {
                        authManager.emitAuthEvent(Failure(errorMsg))
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error during logout: ${e.message}"
                _authError.value = errorMsg
                _userState.value = UserState.Error(errorMsg)
                viewModelScope.launch {
                    authManager.emitAuthEvent(Failure(errorMsg))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            _userState.value = UserState.Loading
            try {
                val response = authRepository.deleteAccount()
                if (response.isSuccessful) {
                    _currentUser.value = null
                    _userState.value = UserState.NotLoggedIn
                    authManager.clearAuthState() // Use AuthManager to clear session
                    viewModelScope.launch {
                        authManager.emitAuthEvent(Success("Deleted user successfully"))
                    }
                } else {
                    val errorMsg = "Account deletion failed: ${response.code()} - ${response.message()}"
                    _authError.value = errorMsg
                    _userState.value = UserState.Error(errorMsg)
                    viewModelScope.launch {
                        authManager.emitAuthEvent(Failure(errorMsg))
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error deleting account: ${e.message}"
                _authError.value = errorMsg
                _userState.value = UserState.Error(errorMsg)
                viewModelScope.launch {
                    authManager.emitAuthEvent(Failure(errorMsg))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
