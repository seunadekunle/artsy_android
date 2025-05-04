package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName

data class Gene(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    @SerializedName("_links") val links: GeneLinks? = null
)

data class GeneLinks(
    val self: Link? = null,
    val thumbnail: Link? = null,
    val permalink: Link? = null
)
