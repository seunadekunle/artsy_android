package com.example.asssignment_4.model

import kotlinx.serialization.Serializable

@Serializable
data class Gene(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?
)
