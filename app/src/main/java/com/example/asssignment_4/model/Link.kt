package com.example.asssignment_4.model

import kotlinx.serialization.Serializable

/**
 * Link model representing hypermedia links from the API
 * Used by multiple models to reference related resources
 */
@Serializable
data class Link(
    val href: String
)

/**
 * Templated link model for links that can be parameterized
 */
@Serializable
data class TemplatedLink(
    val href: String,
    val templated: Boolean = false
)
