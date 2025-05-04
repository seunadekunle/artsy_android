package com.example.asssignment_4.model

/**
 * Link model representing hypermedia links from the API
 * Used by multiple models to reference related resources
 */
data class Link(
    val href: String
)

/**
 * Templated link model for links that can be parameterized
 */
data class TemplatedLink(
    val href: String,
    val templated: Boolean = false
)
