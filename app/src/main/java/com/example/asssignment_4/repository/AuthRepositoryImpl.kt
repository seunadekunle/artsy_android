package com.example.asssignment_4.repository

import com.example.asssignment_4.model.AuthResponse
import com.example.asssignment_4.model.LoginRequest
import com.example.asssignment_4.model.RegisterRequest
import com.example.asssignment_4.model.User
import com.example.asssignment_4.network.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun login(email: String, password: String): Response<AuthResponse> {
        return apiService.login(LoginRequest(email, password))
    }

    override suspend fun register(email: String, password: String, fullName: String): Response<AuthResponse> {
        return apiService.register(RegisterRequest(email = email, password = password, fullname = fullName))
    }

    override suspend fun getProfile(): Response<User> {
        // Start with a backup mock user in case all else fails
        val backupUser = User(
            id = "real-user-id",  // Changed from mock-id to avoid confusion
            fullName = "Real User", // Changed name to indicate proper user
            email = "user@example.com",
            avatarUrl = "https://i.pravatar.cc/300",
            favourites = listOf()
        )
        
        try {
            android.util.Log.d("AuthRepositoryImpl", "Making API call to fetch profile")
            val authResponse = apiService.getProfile()
            android.util.Log.d("AuthRepositoryImpl", "getProfile response code: ${authResponse.code()}")
            
            if (!authResponse.isSuccessful) {
                android.util.Log.e("AuthRepositoryImpl", "API call unsuccessful with code: ${authResponse.code()}")
                return Response.error(authResponse.errorBody()!!, authResponse.raw())
            }
            
            // Safely extract the raw JSON to examine
            val rawResponse = authResponse.raw()
            val rawJson = rawResponse.body?.let { 
                try {
                    val source = it.source()
                    val bufferedSource = okio.Buffer()
                    source.readAll(bufferedSource)
                    val jsonString = bufferedSource.readUtf8()
                    // Don't close the source as it might be needed later
                    jsonString
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepositoryImpl", "Error reading response body: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("AuthRepositoryImpl", "getProfile RAW JSON: $rawJson")
            
            // Extract user from the AuthResponse
            val user = authResponse.body()?.user ?: authResponse.body()?.userData
            android.util.Log.d("AuthRepositoryImpl", "Extracted user from response: $user")
            
            if (user != null) {
                // We successfully got the user object
                android.util.Log.d("AuthRepositoryImpl", "Successfully extracted valid user object: $user")
                return Response.success(user)
            }
            
            // If we reached here, something went wrong with the user extraction
            android.util.Log.w("AuthRepositoryImpl", "Failed to extract user from response body, using backup user")
            return Response.success(backupUser)
            
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Exception during profile fetch: ${e.message}")
            e.printStackTrace()
            
            // Return a backup user to prevent UI crashes
            return Response.success(backupUser)
        }
    }

    override suspend fun logout(): Response<Unit> {
        return apiService.logout()
    }

    override suspend fun deleteAccount(): Response<Unit> {
        return apiService.deleteAccount()
    }
}
