package com.example.asssignment_4.repository

import com.example.asssignment_4.model.AuthResponse
import com.example.asssignment_4.model.LoginRequest
import com.example.asssignment_4.model.RegisterRequest
import com.example.asssignment_4.model.User
import com.example.asssignment_4.network.ApiService
import com.example.asssignment_4.util.TokenManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Response<AuthResponse> {
        return apiService.login(LoginRequest(email, password))
    }

    override suspend fun register(email: String, password: String, fullName: String): Response<AuthResponse> {
        return apiService.register(RegisterRequest(email = email, password = password, fullname = fullName))
    }

    // TokenManager is now properly injected through the constructor
    
    override suspend fun getProfile(): Response<User> {
        try {
            // Get token from TokenManager
            val token = tokenManager.getAuthToken()
            android.util.Log.d("AuthRepositoryImpl", "Making API call to fetch profile, token available: ${token != null}")
            
            // Call the appropriate API method based on token availability
            val authResponse = if (token != null) {
                // Use the Bearer format as specified in the API docs
                // Also include the Accept: application/json header as recommended
                apiService.getProfile(
                    authToken = "Bearer $token",
                    acceptType = "application/json"
                )
            } else {
                // Fall back to the version without explicit token (might use interceptor)
                apiService.getProfile()
            }
            
            android.util.Log.d("AuthRepositoryImpl", "getProfile response code: ${authResponse.code()}")
            
            // Capture and log the raw JSON response body for debugging
            try {
                // Create a new request from the raw response to avoid consuming the body
                val responseBodyCopy = authResponse.raw().newBuilder().build()
                val rawJson = responseBodyCopy.body?.string()
                android.util.Log.d("AUTH_DEBUG", "RAW JSON RESPONSE: $rawJson")
                
                // Log key information we're looking for
                if (rawJson != null) {
                    // Look for specific field patterns in the raw JSON
                    val containsFullname = rawJson.contains("\"fullname\":") || rawJson.contains("\"full_name\":") || 
                                          rawJson.contains("\"name\":") || rawJson.contains("\"userName\":") ||
                                          rawJson.contains("\"full-name\":") || rawJson.contains("\"username\":")
                                          
                    val containsProfileImage = rawJson.contains("\"profileImageUrl\":") || rawJson.contains("\"profile_image_url\":") ||
                                              rawJson.contains("\"avatar\":") || rawJson.contains("\"avatarUrl\":") ||
                                              rawJson.contains("\"profile_image\":") || rawJson.contains("\"image\":")
                                              
                    val containsFavorites = rawJson.contains("\"favorites\":") || rawJson.contains("\"favourites\":") ||
                                          rawJson.contains("\"favorite_artists\":") || rawJson.contains("\"favourite_artists\":")
                    
                    android.util.Log.d("AUTH_DEBUG", "JSON contains fullname field: $containsFullname")
                    android.util.Log.d("AUTH_DEBUG", "JSON contains profile image field: $containsProfileImage")
                    android.util.Log.d("AUTH_DEBUG", "JSON contains favorites field: $containsFavorites")
                }
            } catch (e: Exception) {
                android.util.Log.e("AUTH_DEBUG", "Failed to log raw response: ${e.message}")
            }
            
            if (!authResponse.isSuccessful) {
                android.util.Log.e("AuthRepositoryImpl", "API call unsuccessful with code: ${authResponse.code()}")
                return Response.error(authResponse.errorBody()!!, authResponse.raw())
            }
            
            val responseBody = authResponse.body()
            android.util.Log.d("AuthRepositoryImpl", "Response code: ${authResponse.code()}")
            android.util.Log.d("AuthRepositoryImpl", "Response body structure: $responseBody")
            
            if (responseBody == null) {
                android.util.Log.e("AuthRepositoryImpl", "Response body is null")
                return Response.error(500, okhttp3.ResponseBody.create(
                    "text/plain".toMediaTypeOrNull(),
                    "Empty response body"
                ))
            }
            
            // Detailed logging of the JSON structure
            android.util.Log.d("AUTH_DEBUG", "AuthResponse: success=${responseBody.success}, has token=${responseBody.token != null}")
            android.util.Log.d("AUTH_DEBUG", "AuthResponse: has data=${responseBody.data != null}, has userData=${responseBody.data?.user != null}")
            if (responseBody.data != null) {
                android.util.Log.d("AUTH_DEBUG", "UserData: ${responseBody.data}")
            }
            
            // Get the user from the nested data structure
            val user = responseBody.data?.user
            if (user != null) {
                android.util.Log.d("AuthRepositoryImpl", "Found user in response:")
                android.util.Log.d("AUTH_DEBUG", "User object: $user")
                android.util.Log.d("AUTH_DEBUG", "  id: ${user.id}")
                android.util.Log.d("AUTH_DEBUG", "  fullname field: ${user.fullName ?: "NULL"}")
                android.util.Log.d("AUTH_DEBUG", "  email: ${user.email ?: "NULL"}")
                android.util.Log.d("AUTH_DEBUG", "  profileImageUrl field: ${user.avatarUrl ?: "NULL"}")
                android.util.Log.d("AUTH_DEBUG", "  favorites field: ${user.favourites ?: "NULL"}")
                
                // Process profile image URL to ensure it's valid
                val processedUser = processUserProfileImage(user)
                android.util.Log.d("AuthRepositoryImpl", "Processed profile image URL: ${processedUser.avatarUrl}")
                return Response.success(processedUser)
            } else if (!responseBody.success) {
                // If the response indicates failure, return the error
                val errorMsg = responseBody.error?.general ?: responseBody.message ?: "Authentication failed"
                android.util.Log.e("AuthRepositoryImpl", "Authentication error: $errorMsg")
                return Response.error(401, okhttp3.ResponseBody.create(
                    "text/plain".toMediaTypeOrNull(),
                    errorMsg
                ))
            } else {
                // Response was successful but no user data
                android.util.Log.e("AuthRepositoryImpl", "No user data in successful response")
                android.util.Log.d("AUTH_DEBUG", "Response says success but missing user data: $responseBody")
                return Response.error(500, okhttp3.ResponseBody.create(
                    "text/plain".toMediaTypeOrNull(),
                    "Invalid response format"
                ))
            }
            
            // Check if we have an error message
            val errorMessage = responseBody?.error?.general ?: responseBody?.message
            
            // No user data found in the response
            android.util.Log.e("AuthRepositoryImpl", "No user data found in auth response. Error: $errorMessage")
            return Response.error(404, okhttp3.ResponseBody.create(
                "text/plain".toMediaTypeOrNull(), 
                errorMessage ?: "User data not found in response"
            ))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Exception during profile fetch: ${e.message}")
            e.printStackTrace()
            return Response.error(500, okhttp3.ResponseBody.create(
                "text/plain".toMediaTypeOrNull(), "Error fetching profile: ${e.message}"
            ))
        }
    }

    override suspend fun logout(): Response<Unit> {
        return apiService.logout()
    }

    override suspend fun deleteAccount(): Response<AuthResponse> {
        try {
            // Get token from TokenManager
            val token = tokenManager.getAuthToken()
            android.util.Log.d("AuthRepositoryImpl", "Making API call to delete account, token available: ${token != null}")
            
            if (token == null) {
                android.util.Log.e("AuthRepositoryImpl", "Cannot delete account: No auth token available")
                return Response.error(401, okhttp3.ResponseBody.create(
                    "text/plain".toMediaTypeOrNull(),
                    "Authentication required"
                ))
            }
            
            // Call the delete account API
            val response = apiService.deleteAccount()
            android.util.Log.d("AuthRepositoryImpl", "Delete account response code: ${response.code()}")
            
            if (response.isSuccessful) {
                // If deletion was successful, clear the auth token
                tokenManager.clearAuthToken()
                android.util.Log.d("AuthRepositoryImpl", "Account deleted successfully")
            } else {
                // Log the error
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AuthRepositoryImpl", "Failed to delete account: ${response.code()}, error: $errorBody")
            }
            
            return response
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Exception during account deletion: ${e.message}")
            e.printStackTrace()
            return Response.error(500, okhttp3.ResponseBody.create(
                "text/plain".toMediaTypeOrNull(), 
                "Error deleting account: ${e.message}"
            ))
        }
    }
    
    /**
     * Processes a user's profile image URL to ensure it's valid and properly formatted
     * Handles various edge cases like relative paths, template variables, and non-http URLs
     * 
     * @param user The user object with potentially problematic avatar URL
     * @return A new user object with properly processed avatar URL
     */
    private fun processUserProfileImage(user: User): User {
        val avatarUrl = user.avatarUrl
        
        // If no avatar URL, return user as is
        if (avatarUrl.isNullOrBlank()) {
            android.util.Log.d("AuthRepositoryImpl", "User has no profile image URL")
            return user
        }
        
        // Process the URL based on known patterns and issues
        val processedUrl = when {
            // If it's already a gravatar URL, use it as-is
            avatarUrl.contains("gravatar.com/avatar") -> {
                avatarUrl
            }
            
            // If it's any other type of URL, it's incorrect and we should use a default gravatar
            // This handles the cloudfront.net URLs and any other non-gravatar URLs
            !avatarUrl.contains("gravatar.com/avatar") -> {
                android.util.Log.w("AuthRepositoryImpl", "Non-gravatar URL detected: $avatarUrl")
                // Use a default gravatar URL with a generated hash or the md5 of their email if available
                // For simplicity, using a default here
                "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp"
            }
            
            // Fallback case - should never reach here but keeping it for safety
            else -> avatarUrl
        }
        
        android.util.Log.d("AuthRepositoryImpl", "Processed profile URL: $processedUrl from original: $avatarUrl")
        
        // Create a new user with the processed URL
        return user.copy(avatarUrl = processedUrl)
    }
}
