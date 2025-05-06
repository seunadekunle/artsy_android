package com.example.asssignment_4.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val success: Boolean,
    val user: User? = null, // User might be null on error
    val token: String? = null, // Token might be null on error
    val message: String? = null // Optional message for errors
)
