package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName

data class Artwork(
    val id: String,
    val title: String?,
    val date: String?,
    val category: String?,
    val medium: String?,
    @SerializedName("_links") val links: ImageLinks? = null,
    val categories: List<Gene> = emptyList()
)

data class ImageLinks(
    val thumbnail: Link?,
    val image: Link?
)

data class ArtworkLinks(
    val self: Link? = null,
    val permalink: Link? = null
)
