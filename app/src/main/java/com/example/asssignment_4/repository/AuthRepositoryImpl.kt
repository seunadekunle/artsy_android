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
        return apiService.getProfile()
    }

    override suspend fun logout(): Response<Unit> {
        return apiService.logout()
    }

    override suspend fun deleteAccount(): Response<Unit> {
        return apiService.deleteAccount()
    }
}
