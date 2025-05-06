package com.example.asssignment_4.repository

import com.example.asssignment_4.model.AuthResponse
import com.example.asssignment_4.model.User
import retrofit2.Response

interface AuthRepository {
    suspend fun login(email: String, password: String): Response<AuthResponse>
    suspend fun register(email: String, password: String, fullName: String): Response<AuthResponse>
    suspend fun getProfile(): Response<User>
    suspend fun logout(): Response<Unit>
    suspend fun deleteAccount(): Response<Unit> // Added for account deletion
}
