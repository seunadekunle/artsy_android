package com.example.asssignment_4.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
// Import the shared Link classes

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val nationality: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    val imageUrl: String? = null,
    val biography: String? = null,
    @SerialName("_links") val links: ArtistLinks? = null,
    var isFavorite: Boolean = false
)

@Serializable
data class ArtistLinks(
    val thumbnail: Link? = null,
    val image: TemplatedLink? = null,
    val self: Link? = null,
    val permalink: Link? = null,
    val artworks: Link? = null,
    val published_artworks: Link? = null,
    val similar_artists: Link? = null,
    val similar_contemporary_artists: Link? = null,
    val genes: Link? = null
)

// Link and TemplatedLink classes moved to dedicated Link.kt file
