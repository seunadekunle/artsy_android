package com.example.asssignment_4.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class User(
    @SerialName("_id")
    @JsonNames("id")
    val id: String? = null,

    @SerialName("fullname")
    @JsonNames("fullName")
    val fullName: String? = null,

    val email: String? = null,

    @SerialName("profileImageUrl")
    @JsonNames("avatarUrl")
    val avatarUrl: String? = null,

    val favourites: List<String>? = null
)
