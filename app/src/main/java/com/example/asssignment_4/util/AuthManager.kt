package com.example.asssignment_4.util

import android.util.Log
import com.example.asssignment_4.model.User
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val cookieJar: PersistentCookieJar
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
            cookieJar.clearSession()
            tokenManager.clearAuthToken()
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
        cookieJar.clearSession()
        Log.d("AuthManager", "Auth state cleared, tokens removed, cookies cleared, manuallyLoggedOut=true")
    }
    
    /**
     * Emit an auth event to be observed by UI components
     */
    suspend fun emitAuthEvent(event: AuthManagerEvent) {
        _authEvent.emit(event)
    }
}
