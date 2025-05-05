package com.example.asssignment_4.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.Artwork
import com.example.asssignment_4.model.Gene
import com.example.asssignment_4.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"])

    // State for Artist Details
    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    // State for Artworks
    private val _artworks = MutableStateFlow<List<Artwork>>(emptyList())
    val artworks: StateFlow<List<Artwork>> = _artworks.asStateFlow()
    
    // State for Artwork Categories
    private val _artworkCategories = MutableStateFlow<Map<String, List<Gene>>>(emptyMap())
    val artworkCategories: StateFlow<Map<String, List<Gene>>> = _artworkCategories.asStateFlow()

    // Combined Loading State (can be split if needed)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error State
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchArtistDetailsAndArtworks()
    }

    private fun fetchArtistDetailsAndArtworks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch Artist Details using the correct method
                val artistResult = artistRepository.getArtistDetailsById(artistId)
                if (artistResult != null) {
                    _artist.value = artistResult
                } else {
                    _error.value = "Error fetching artist details: Artist not found or API error."
                }

                // Fetch Artworks using the correct method
                val artworksResult = artistRepository.getArtistArtworks(artistId)
                _artworks.value = artworksResult.embedded?.artworks ?: emptyList()
                if (_artworks.value.isEmpty() && _error.value == null) {
                    // Optionally set a specific message if artist is found but has no artworks
                    // _error.value = "Artist found, but no artworks listed."
                }

            } catch (e: Exception) {
                _error.value = "An unexpected error occurred: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetches categories for a specific artwork
     * @param artworkId The ID of the artwork to fetch categories for
     */
    fun fetchArtworkCategories(artworkId: String) {
        viewModelScope.launch {
            try {
                // Check if we already have categories for this artwork
                if (_artworkCategories.value.containsKey(artworkId)) {
                    return@launch // Skip if already fetched
                }
                
                val categories = artistRepository.getArtworkCategories(artworkId)
                
                // Update the map with new categories
                val updatedMap = _artworkCategories.value.toMutableMap()
                updatedMap[artworkId] = categories
                _artworkCategories.value = updatedMap
                
            } catch (e: Exception) {
                // Log error but don't update global error state to avoid disrupting the UI
                println("Error fetching categories for artwork $artworkId: ${e.message}")
            }
        }
    }
    
    /**
     * Gets categories for a specific artwork from the cached state
     * @param artworkId The ID of the artwork to get categories for
     * @return List of Gene objects representing categories, or empty list if none found
     */
    fun getArtworkCategories(artworkId: String): List<Gene> {
        return _artworkCategories.value[artworkId] ?: emptyList()
    }
}
