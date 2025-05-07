package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("fullname")
    val fullName: String? = null,

    val email: String? = null,

    @SerializedName("profileImageUrl")
    val avatarUrl: String? = null,

    @SerializedName("favorites")
    val favourites: List<String>? = null
)
