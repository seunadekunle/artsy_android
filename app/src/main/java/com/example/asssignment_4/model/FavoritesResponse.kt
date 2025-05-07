package com.example.asssignment_4.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoritesResponse(
    val success: Boolean,
    val data: List<Artist>
)
