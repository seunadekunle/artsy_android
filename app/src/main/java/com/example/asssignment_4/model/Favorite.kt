package com.example.asssignment_4.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Favorite(
    @SerialName("_id") val id: String,
    val userId: String,
    val artistId: String,
    val artistName: String,
    val artistImage: String,
    val createdAt: String
)
