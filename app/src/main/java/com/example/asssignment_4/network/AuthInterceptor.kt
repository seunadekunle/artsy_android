package com.example.asssignment_4.network

import android.util.Log
import com.example.asssignment_4.util.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor for adding JWT token to API requests that require authentication
 * Works alongside cookie-based authentication handled by PersistentCookieJar
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    /**
     * Determines if the given path requires JWT token authentication
     * 
     * @param path The encoded URL path to check
     * @return True if the path requires authentication, false otherwise
     */
    private fun shouldAddAuthHeader(path: String): Boolean {
        // Add authorization header for these protected endpoints
        return path.contains("/api/favorites") ||
               path.contains("/api/auth/me") // profile endpoints
               // Add other protected endpoints as needed
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Check if this request requires authentication
        val requiresAuth = shouldAddAuthHeader(originalRequest.url.encodedPath)
        val requestBuilder = originalRequest.newBuilder()
        
        if (requiresAuth) {
            val token = tokenManager.getAuthToken()
            
            if (!token.isNullOrBlank()) {
                Log.d("AuthInterceptor", "Adding Authorization header for: ${originalRequest.url}")
                requestBuilder.header("Authorization", "Bearer $token")
            } else {
                Log.w("AuthInterceptor", "No token available for authenticated endpoint: ${originalRequest.url}")
            }
        } else {
            Log.d("AuthInterceptor", "Skipping auth for non-protected endpoint: ${originalRequest.url}")
        }
        
        // Keep original headers and method
        val request = requestBuilder
            .method(originalRequest.method, originalRequest.body)
            .build()
            
        return chain.proceed(request)
    }
}
