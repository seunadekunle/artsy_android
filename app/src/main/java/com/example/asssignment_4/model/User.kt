package com.example.asssignment_4.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("_id")
    val id: String,
    @SerialName("fullname")
    val fullName: String,
    val email: String,
    val avatarUrl: String? = null,
    val favourites: List<String>? = null
)
