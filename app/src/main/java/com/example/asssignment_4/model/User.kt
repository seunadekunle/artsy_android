package com.example.asssignment_4.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String?,
    val favourites: List<String> // List of Artist IDs
)
