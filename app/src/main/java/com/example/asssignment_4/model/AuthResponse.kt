package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    val success: Boolean = false,
    val data: UserData? = null,
    val message: String? = null,
    val token: String? = null,
    @SerializedName("errors")
    val error: ErrorResponse? = null
)

data class UserData(
    val user: User? = null
)

data class ErrorResponse(
    val general: String? = null
)
