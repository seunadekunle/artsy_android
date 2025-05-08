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
                        
                        // We found a valid cookie session
                        if (user != null) {
                            _currentUser.value = user
                            _userState.value = UserState.Success(user)
                            Log.d("AuthViewModel", "Restored session from cookie")
                        } else {
                            // User data is null, strange situation
                            _userState.value = UserState.NotLoggedIn
                            _currentUser.value = null
                            Log.d("AuthViewModel", "Auth status: Not logged in (no user data in profile response)")
                        }
                    } else {
                        // No valid cookie session
                        _userState.value = UserState.NotLoggedIn
                        _currentUser.value = null
                        Log.d("AuthViewModel", "Auth status: Not logged in (profile request failed)")
                    }
                } else {
                    // User manually logged out, respect that
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
            _isLoading.value = true
            _userState.value = UserState.Loading
            
            try {
                Log.d("AuthViewModel", "Fetching user profile...")
                val response = authRepository.getProfile()
                Log.d("AuthViewModel", "Profile response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful && response.body() != null) {
                    val userData = response.body()
                    Log.d("AuthViewModel", "Fetched profile: $userData")
                    
                    if (userData != null) {
                        Log.d("AuthViewModel", "User details - id: ${userData.id}, name: ${userData.fullName}, email: ${userData.email}")
                        Log.d("AuthViewModel", "User avatar URL: '${userData.avatarUrl}'")
                        
                        // Update both state flows
                        _currentUser.value = userData
                        _userState.value = UserState.Success(userData)
                        
                        Log.d("AuthViewModel", "Updated state - userState.value: ${_userState.value}")
                        return@launch
                    }
                }
                
                // Handle error cases
                val errorMsg = when (response.code()) {
                    401 -> {
                        handleUnauthorized()
                        "Session expired"
                    }
                    404 -> "User not found"
                    500 -> "Server error"
                    else -> response.errorBody()?.string() ?: "Unknown error"
                }
                
                Log.w("AuthViewModel", "Failed to fetch profile: $errorMsg")
                _userState.value = UserState.NotLoggedIn
                _currentUser.value = null
                _authError.value = errorMsg
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching profile: ${e.message}")
                e.printStackTrace()
                _userState.value = UserState.NotLoggedIn
                _currentUser.value = null
                _authError.value = e.message
            } finally {
                _isLoading.value = false
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
                    val authData = response.body()
                    val token = authData?.token
                    
                    // Get user from nested data structure
                    val userData = authData?.data?.user
                    Log.d("AuthViewModel", "Login response user: $userData")
                    Log.d("AuthViewModel", "User avatarUrl: ${userData?.avatarUrl}, fullName: ${userData?.fullName}")
                    Log.d("AuthViewModel", "JWT Token received: ${token != null}")
                    
                    // Mark as logged in if we have a token, even if user is null
                    if (token != null) {
                        authManager.saveAuthToken(token)
                        
                        if (userData != null) {
                            _currentUser.value = userData
                            _userState.value = UserState.Success(userData)
                        } else {
                            // User data is null, try to fetch profile separately
                            Log.d("AuthViewModel", "User data is null in login response, fetching profile...")
                            fetchUserProfile()
                        }
                        
                        // Emit success event
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Success("Logged in successfully"))
                        }
                        // Add delay before navigation
                        kotlinx.coroutines.delay(1500)
                    } else {
                        // No token received - this shouldn't happen with success=true
                        Log.w("AuthViewModel", "Login successful but no token received")
                        _authError.value = "Authentication error: No token received"
                        _userState.value = UserState.Error("Authentication error: No token received")
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Failure("Authentication error: No token received"))
                        }
                    }
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
                    val authData = response.body()
                    val token = authData?.token
                    val userData = authData?.data?.user
                    
                    Log.d("AuthViewModel", "Registration response user: $userData")
                    Log.d("AuthViewModel", "JWT Token received: ${token != null}")
                    
                    // Mark as registered and logged in if we have a token
                    if (token != null) {
                        authManager.saveAuthToken(token)
                        
                        // Set user if available, otherwise fetch profile
                        if (userData != null) {
                            _currentUser.value = userData
                            _userState.value = UserState.Success(userData)
                        } else {
                            // User data is null, try to fetch profile separately
                            Log.d("AuthViewModel", "User data is null in registration response, fetching profile...")
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
            _userState.value = UserState.Loading
            
            try {
                // First clear local auth state
                _currentUser.value = null
                _userState.value = UserState.NotLoggedIn
                authManager.clearAuthState() // Use AuthManager to clear session
                
                // Then make the logout API call
                val response = authRepository.logout()
                if (response.isSuccessful) {
                    // Emit success event
                    viewModelScope.launch {
                        delay(200) // Brief delay so UI can process the state change
                        authManager.emitAuthEvent(Success("Logged out successfully"))
                    }
                } else {
                    // Even if the API call fails, we keep the user logged out locally
                    Log.w("AuthViewModel", "Logout API call failed but user was logged out locally: ${response.code()}")
                    viewModelScope.launch {
                        delay(200)
                        authManager.emitAuthEvent(Success("Logged out successfully"))
                    }
                }
            } catch (e: Exception) {
                // Even if there's an error, we keep the user logged out locally
                Log.e("AuthViewModel", "Exception during logout but user was logged out locally", e)
                viewModelScope.launch {
                    delay(200)
                    authManager.emitAuthEvent(Success("Logged out successfully"))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes the user's account using the /api/auth/account endpoint
     * If successful, clears all auth state and redirects to login
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            _userState.value = UserState.Loading
            
            try {
                Log.d("AuthViewModel", "Attempting to delete user account")
                val response = authRepository.deleteAccount()
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("AuthViewModel", "Account deletion successful: ${responseBody?.message}")
                    
                    // Clear all user data and auth state
                    _currentUser.value = null
                    _userState.value = UserState.NotLoggedIn
                    authManager.clearAuthState() // Use AuthManager to clear session
                    
                    // Emit success event with message from server or default
                    val successMsg = responseBody?.message ?: "Deleted user successfully"
                    viewModelScope.launch {
                        delay(200) // Brief delay so UI can process the state change
                        authManager.emitAuthEvent(Success(successMsg))
                    }
                } else {
                    // Handle error cases with different status codes
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthViewModel", "Account deletion failed: ${response.code()} - $errorBody")
                    
                    // Try to parse the error response
                    val errorMsg = when (response.code()) {
                        401 -> "Authentication required. Please log in again."
                        403 -> "Session expired. Please log in again."
                        else -> {
                            try {
                                // Try to extract error message from response
                                response.errorBody()?.string()?.let { 
                                    if (it.contains("general")) {
                                        // Extract the specific error message if possible
                                        it.substringAfter("general\":").substringAfter("\"")
                                          .substringBefore("\"").takeIf { it.isNotBlank() }
                                    } else null
                                } ?: "Account deletion failed: ${response.code()}"
                            } catch (e: Exception) {
                                "Account deletion failed: ${response.code()}"
                            }
                        }
                    }
                    
                    _authError.value = errorMsg
                    _userState.value = UserState.Error(errorMsg)
                    
                    // If we got a 401/403, also handle the unauthorized state
                    if (response.code() == 401 || response.code() == 403) {
                        handleUnauthorized()
                    } else {
                        viewModelScope.launch {
                            authManager.emitAuthEvent(Failure(errorMsg))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception during account deletion", e)
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
