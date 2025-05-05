package com.example.asssignment_4.model

import com.example.asssignment_4.model.Link
import com.google.gson.annotations.SerializedName

// Data class for the _links object within a Gene
data class GeneLinks(
    val thumbnail: Link?,
    val image: Link? // Might contain templated URL
    // Add other links if needed (self, permalink, etc.)
)

// Updated Gene data class
data class Gene(
    val id: String,
    val name: String,
    @SerializedName("display_name") val displayName: String?,
    val description: String?,
    @SerializedName("_links") val links: GeneLinks? // Added links field
    // Add other relevant fields if necessary, based on the full JSON
)

// Data class for the overall response structure
data class GenesResponse(
    @SerializedName("_embedded") val embedded: EmbeddedGenes?
    // Add other top-level fields like _links or total_count if needed
)

// Data class for the _embedded part containing the list of genes
data class EmbeddedGenes(
    val genes: List<Gene>?
)
