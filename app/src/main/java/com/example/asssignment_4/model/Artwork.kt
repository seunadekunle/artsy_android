package com.example.asssignment_4.model

import kotlinx.serialization.Serializable

@Serializable
data class Artwork(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val categories: List<Gene>
)
