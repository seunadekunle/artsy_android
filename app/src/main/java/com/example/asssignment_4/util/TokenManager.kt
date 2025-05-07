package com.example.asssignment_4.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val USER_TOKEN = "user_token"
        private const val MANUAL_LOGOUT = "manual_logout"
    }

    /**
     * Saves the JWT authentication token
     * 
     * @param token The JWT token to save
     */
    fun saveAuthToken(token: String?) {
        if (token.isNullOrBlank()) {
            Log.w("TokenManager", "Attempted to save null or empty token")
            return
        }
        
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
        Log.d("TokenManager", "JWT token saved successfully")
    }

    /**
     * Retrieves the stored JWT auth token from SharedPreferences
     * @return The JWT token string, or null if not found
     */
    fun getAuthToken(): String? {
        val token = prefs.getString(USER_TOKEN, null)
        
        if (token.isNullOrBlank()) {
            Log.d("TokenManager", "No JWT token found in preferences")
            return null
        }
        
        Log.d("TokenManager", "JWT token retrieved from preferences")
        return token
    }
    
    // Keep old method name for backward compatibility
    @Deprecated("Use getAuthToken() instead", ReplaceWith("getAuthToken()"))
    fun fetchAuthToken(): String? = getAuthToken()

    /**
     * Clears the auth token from preferences
     */
    fun clearAuthToken() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.apply()
        Log.d("TokenManager", "JWT token cleared from preferences")
    }
    
    /**
     * Sets the manual logout state in preferences
     * @param value Whether the user has manually logged out
     */
    fun setManualLogoutState(value: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(MANUAL_LOGOUT, value)
        editor.apply()
        Log.d("TokenManager", "Manual logout state set to: $value")
    }
    
    /**
     * Gets the manual logout state from preferences
     * @return Whether the user has manually logged out
     */
    fun getManualLogoutState(): Boolean {
        val state = prefs.getBoolean(MANUAL_LOGOUT, false)
        Log.d("TokenManager", "Retrieved manual logout state: $state")
        return state
    }
}
