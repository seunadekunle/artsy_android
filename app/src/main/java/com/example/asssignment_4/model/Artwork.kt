package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName

data class Artwork(
    val id: String,
    val title: String?,
    val date: String?,
    val category: String?,
    val medium: String?,
    val imageUrl: String?,
    @SerializedName("_links") val links: ArtworkLinks? = null,
    val categories: List<Gene> = emptyList()
)

data class ArtworkLinks(
    val self: Link? = null,
    val thumbnail: Link? = null,
    val image: Link? = null,
    val permalink: Link? = null
)
