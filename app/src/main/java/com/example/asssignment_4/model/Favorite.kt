package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName


data class Favorite(
    @SerializedName("_id") val id: String,
    val userId: String,
    val artistId: String,
    val artistName: String,
    val artistImage: String,
    val createdAt: String
)
