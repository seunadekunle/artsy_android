package com.example.asssignment_4.model

import kotlinx.serialization.Serializable


data class LoginRequest(
    val email: String,
    val password: String
)
