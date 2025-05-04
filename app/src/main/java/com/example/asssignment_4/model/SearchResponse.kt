package com.example.asssignment_4.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("total_count") val totalCount: Int,
    val offset: Int,
    val q: String,
    @SerializedName("_embedded") val _embedded: SearchEmbedded
)

data class SearchEmbedded(
    val results: List<SearchResult>
)

data class SearchResult(
    val type: String,
    val title: String,
    val description: String?,
    @SerializedName("og_type") val ogType: String,
    @SerializedName("_links") val links: SearchLinks
)

data class SearchLinks(
    val self: Link,
    val permalink: Link,
    val thumbnail: Link
)

// Link class moved to dedicated Link.kt file
