package com.example.asssignment_4.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavouriteRequest(
    @SerialName("artistId")
    val artistId: String
)
