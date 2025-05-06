package com.example.asssignment_4.repository

import android.util.Log
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.ArtworksResponse
import com.example.asssignment_4.model.FavouriteRequest
import com.example.asssignment_4.model.Gene
import com.example.asssignment_4.model.PartnerShowResponse
import com.example.asssignment_4.model.SearchResponse
import com.example.asssignment_4.network.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(private val api: ApiService) {
    suspend fun searchArtists(query: String): Response<SearchResponse> = api.searchArtists(query)
    suspend fun getArtistDetails(id: String) = api.getArtistDetails(id)
    suspend fun getFavourites() = api.getFavourites()
    suspend fun addFavourite(artistId: String) = api.addFavourite(FavouriteRequest(artistId))
    suspend fun removeFavourite(artistId: String) = api.removeFavourite(FavouriteRequest(artistId))

    // Add function to get artist details by ID (using new endpoint)
    suspend fun getArtistDetailsById(artistId: String): Artist? {
        val response = api.getArtistDetailsById(artistId)
        return if (response.isSuccessful) {
            response.body()
        } else {
            // Log error or handle specific codes if needed
            null // Return null if API call fails
        }
    }

    // Add function to get artist artworks by ID (using new endpoint)
    suspend fun getArtistArtworks(artistId: String): ArtworksResponse = api.getArtistArtworks(artistId)
    
    // Add function to get artwork categories
    suspend fun getArtworkCategories(artworkId: String): List<Gene> {
        Log.d("ArtistRepository", "Fetching categories for artwork: $artworkId")
        return try {
            val response = api.getArtworkCategories(artworkId)
            if (response.isSuccessful) {
                // Extract the list of genes from the nested structure
                val genes = response.body()?.embedded?.genes ?: emptyList()
                Log.d("ArtistRepository", "Successfully fetched ${genes.size} categories for artwork: $artworkId")
                genes
            } else {
                Log.e("ArtistRepository", "Error fetching categories: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            // Log the exception for detailed debugging
            Log.e("ArtistRepository", "Exception fetching categories: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPartnerShows(partnerId: String): PartnerShowResponse? {
        // This function is not implemented in the original code
        // You may need to implement it according to your requirements
        TODO("Not yet implemented")
    }

    suspend fun getSimilarArtists(
        artistId: String,
        authToken: String? = null
    ): Response<List<Artist>> {
        return try {
            if (authToken != null) {
                api.getSimilarArtists(artistId, "Bearer $authToken")
            } else {
                api.getSimilarArtists(artistId)
            }
        } catch (e: Exception) {
            Log.e("ArtistRepository", "Exception fetching similar artists: ${e.message}", e)
            throw e
        }
    }
}
