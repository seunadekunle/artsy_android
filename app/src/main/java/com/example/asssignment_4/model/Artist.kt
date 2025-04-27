package com.example.asssignment_4.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val nationality: String?,
    val birthday: String?,
    val deathday: String?,
    val imageUrl: String?,
    val biography: String?
)
