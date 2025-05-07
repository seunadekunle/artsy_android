package com.example.asssignment_4.model

import kotlinx.serialization.Serializable


data class RegisterRequest(
    val email: String,
    val password: String,
    val fullname: String
)
