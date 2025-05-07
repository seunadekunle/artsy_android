package com.example.asssignment_4.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AuthResponse(
    val success: Boolean = false,
    
    val user: User? = null, // User might be null on error
    
    @JsonNames("data")
    val userData: User? = null, // Some APIs wrap user in a data field
    
    val token: String? = null, // Token might be null on error
    
    val message: String? = null, // Optional message for errors
    
    @JsonNames("error")
    val errorMessage: String? = null // Alternative error field
)
