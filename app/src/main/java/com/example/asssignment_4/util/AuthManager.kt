package com.example.asssignment_4.util

import android.util.Log
import com.example.asssignment_4.model.User
import com.example.asssignment_4.repository.AuthRepository
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication state event types for the UI to respond to
 */
sealed class AuthManagerEvent {
    data class Success(val message: String) : AuthManagerEvent()
    data class Failure(val message: String) : AuthManagerEvent()
    data object SessionExpired : AuthManagerEvent()
}

/**
 * Centralized manager for authentication state that can be injected into any ViewModel
 * without creating circular dependencies
 */
@Singleton
class AuthManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val cookieJar: PersistentCookieJar,
    private val authRepository: AuthRepository
) {
    // Authentication state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    // Events from authentication operations
    private val _authEvent = MutableSharedFlow<AuthManagerEvent>()
    val authEvent: SharedFlow<AuthManagerEvent> = _authEvent.asSharedFlow()
    
    // Flag to prevent multiple session expired events
    private var hasSessionExpiredEventFired = false
    
    // Flag to track if user has manually logged out
    private val _manuallyLoggedOut = MutableStateFlow(false)
    val manuallyLoggedOut: StateFlow<Boolean> = _manuallyLoggedOut.asStateFlow()
    
    // User profile cache
    private var cachedUserProfile: User? = null
    private var lastProfileFetchTime = 0L
    private val PROFILE_CACHE_TTL = 5 * 60 * 1000 // 5 minutes in milliseconds
    
    // Should be called on app start to initialize auth state from token
    init {
        val hasToken = tokenManager.getAuthToken() != null
        _isLoggedIn.value = hasToken
        
        // Get manual logout state from SharedPreferences
        val wasManuallyLoggedOut = tokenManager.getManualLogoutState()
        _manuallyLoggedOut.value = wasManuallyLoggedOut
        
        Log.d("AuthManager", "Init auth state: hasToken=$hasToken, manuallyLoggedOut=$wasManuallyLoggedOut")
    }
    
    /**
     * Handle unauthorized (401) responses by clearing tokens and notifying UI
     */
    suspend fun handleUnauthorized() {
        if (!hasSessionExpiredEventFired) {
            Log.w("AuthManager", "Session expired, clearing auth state")
            _isLoggedIn.value = false
            tokenManager.clearAuthToken()
            
            // Clear all cookies to ensure server-side session is terminated
            try {
                cookieJar.clear()
                Log.d("AuthManager", "Cookie jar completely cleared during unauthorized handling")
            } catch (e: Exception) {
                Log.e("AuthManager", "Error clearing cookie jar during unauthorized handling: ${e.message}")
            }
            
            // Also clear the standard CookieManager as a fallback
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.CookieManager.getInstance().flush()
                Log.d("AuthManager", "System CookieManager cleared during unauthorized handling")
            } catch (e: Exception) {
                Log.e("AuthManager", "Error clearing system cookies during unauthorized handling: ${e.message}")
            }
            
            _authEvent.emit(AuthManagerEvent.SessionExpired)
            hasSessionExpiredEventFired = true
            
            // Reset flag after a delay to allow for future session expirations
            kotlinx.coroutines.delay(10000) // Wait 10 seconds before allowing another event
            hasSessionExpiredEventFired = false
        }
    }
    
    /**
     * Save auth token received during login/registration and update login state
     */
    fun saveAuthToken(token: String?) {
        if (!token.isNullOrBlank()) {
            tokenManager.saveAuthToken(token)
            _isLoggedIn.value = true
            _manuallyLoggedOut.value = false  // Reset manual logout flag on explicit login
            tokenManager.setManualLogoutState(false) // Save to preferences
            Log.d("AuthManager", "Auth token saved, setting manuallyLoggedOut=false")
        }
    }
    
    /**
     * Clear authentication state during logout
     */
    fun clearAuthState() {
        _isLoggedIn.value = false
        _manuallyLoggedOut.value = true
        tokenManager.clearAuthToken()
        tokenManager.setManualLogoutState(true) // Save logout state to preferences
        
        // Clear all cookies to ensure server-side session is terminated
        try {
            cookieJar.clear()
            Log.d("AuthManager", "Cookie jar completely cleared")
        } catch (e: Exception) {
            Log.e("AuthManager", "Error clearing cookie jar: ${e.message}")
        }
        
        // Also clear the standard CookieManager as a fallback
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            Log.d("AuthManager", "System CookieManager cleared")
        } catch (e: Exception) {
            Log.e("AuthManager", "Error clearing system cookies: ${e.message}")
        }
        
        Log.d("AuthManager", "Auth state cleared, tokens removed, cookies cleared, manuallyLoggedOut=true")
    }
    
    /**
     * Emit an auth event to be observed by UI components
     */
    suspend fun emitAuthEvent(event: AuthManagerEvent) {
        _authEvent.emit(event)
    }

    /**
     * Get the current auth token
     * @return The JWT token string, or null if not authenticated
     */
    fun getAuthToken(): String? {
        return tokenManager.getAuthToken()
    }
    
    /**
     * Check if the current token is still valid by making a lightweight API call
     * Updates the logged in state based on the result
     * 
     * @return true if the token is valid, false otherwise
     */
    suspend fun checkTokenValidity(): Boolean {
        val token = getAuthToken()
        if (token == null) {
            _isLoggedIn.value = false
            return false
        }
        
        try {
            // Make a profile request to check token validity
            val response = authRepository.getProfile()
            if (response.isSuccessful && response.body() != null) {
                // If successful, update cached profile
                updateCachedProfile(response)
                
                // Ensure logged in state is correct
                if (!_isLoggedIn.value) {
                    _isLoggedIn.value = true
                    Log.d("AuthManager", "Token check: Valid token, updating isLoggedIn to true")
                }
                return true
            } else {
                // If unauthorized, clear auth state
                if (response.code() == 401) {
                    Log.w("AuthManager", "Token check: Token is invalid (401)")
                    handleUnauthorized()
                } else {
                    Log.w("AuthManager", "Token check: Server error ${response.code()}")
                }
                return false
            }
        } catch (e: Exception) {
            // Network errors or other exceptions
            Log.e("AuthManager", "Token check failed: ${e.message}")
            // Don't automatically clear auth on network errors
            return false
        }
    }
    
    /**
     * Update the cached user profile from a successful API response
     */
    private fun updateCachedProfile(response: Response<User>) {
        response.body()?.let { user ->
            cachedUserProfile = user
            lastProfileFetchTime = System.currentTimeMillis()
            
            // Log profile details for debugging image issues
            Log.d("AuthManager", "Profile updated - fullName: ${user.fullName}, email: ${user.email}")
            
            // Based on memory, User model has @SerialName("profileImageUrl") annotation for avatarUrl
            Log.d("AuthManager", "Profile image URL: ${user.avatarUrl ?: "null"}")
            
            // Check for missing profile image URL based on memory info
            if (user.avatarUrl.isNullOrEmpty()) {
                Log.w("AuthManager", "User profile has no image URL - may indicate serialization issue")
            }
        }
    }
    
    /**
     * Get the current user profile information
     * Uses cached version if available and not expired
     * 
     * @return The User object or null if not logged in
     */
    suspend fun getCurrentUserProfile(): User? {
        val token = getAuthToken() ?: return null
        
        // Return cached profile if it's fresh enough
        val now = System.currentTimeMillis()
        if (cachedUserProfile != null && now - lastProfileFetchTime < PROFILE_CACHE_TTL) {
            Log.d("AuthManager", "Using cached profile (age: ${now - lastProfileFetchTime}ms)")
            return cachedUserProfile
        }
        
        // Otherwise fetch fresh profile
        try {
            val response = authRepository.getProfile()
            if (response.isSuccessful && response.body() != null) {
                updateCachedProfile(response)
                return cachedUserProfile
            } else {
                Log.w("AuthManager", "Failed to get profile: ${response.code()}")
                if (response.code() == 401) {
                    handleUnauthorized()
                }
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error getting profile: ${e.message}")
        }
        
        // Return cached profile as fallback even if outdated
        return cachedUserProfile
    }
}
