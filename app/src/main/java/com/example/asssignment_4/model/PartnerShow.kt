package com.example.asssignment_4.model

// Placeholder for the structure of a Partner Show
data class PartnerShow(
    val id: String, // Example field
    val name: String // Example field
    // Add other relevant fields based on the actual API response
)

// Placeholder for the response structure of the /api/shows endpoint
data class PartnerShowResponse(
    // Assuming the API returns a list or some embedded structure
    val shows: List<PartnerShow>? // Example field
    // Adjust based on the actual API response
)
