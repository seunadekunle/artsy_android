package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level response structure for the artworks endpoint,
 * which nests the actual artwork list within an '_embedded' object.
 */
data class ArtworksResponse(
    @SerializedName("_embedded") val embedded: EmbeddedArtworks?
)

/**
 * Represents the '_embedded' object within the ArtworksResponse,
 * containing the list of artworks.
 */
data class EmbeddedArtworks(
    @SerializedName("artworks") val artworks: List<Artwork>?
)
