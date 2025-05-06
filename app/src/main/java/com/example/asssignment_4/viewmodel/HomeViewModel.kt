package com.example.asssignment_4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.ArtistLinks
import com.example.asssignment_4.model.Artwork
import com.example.asssignment_4.model.Gene
import com.example.asssignment_4.model.Link
import com.example.asssignment_4.model.SearchResponse
import com.example.asssignment_4.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import android.util.Log

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Artist>>(emptyList())
    val searchResults: StateFlow<List<Artist>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Favourites State
    private val _favourites = MutableStateFlow<List<Artist>>(emptyList())
    val favourites: StateFlow<List<Artist>> = _favourites.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _favouriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favouriteIds: StateFlow<Set<String>> = _favouriteIds.asStateFlow()

    // --- Artist Detail and Artworks State ---
    private val _artistDetail = MutableStateFlow<Artist?>(null)
    val artistDetail: StateFlow<Artist?> = _artistDetail.asStateFlow()

    private val _artistArtworks = MutableStateFlow<List<Artwork>>(emptyList())
    val artistArtworks: StateFlow<List<Artwork>> = _artistArtworks.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    // Similar Artists State
    private val _similarArtists = MutableStateFlow<List<Artist>>(emptyList())
    val similarArtists: StateFlow<List<Artist>> = _similarArtists.asStateFlow()

    fun fetchSimilarArtists(artistId: String, authToken: String?) {
        viewModelScope.launch {
            try {
                _isDetailLoading.value = true
                val response = artistRepository.getSimilarArtists(artistId, authToken)
                if (response.isSuccessful) {
                    _similarArtists.value = response.body() ?: emptyList()
                } else {
                    _detailError.value = "Failed to fetch similar artists"
                    _similarArtists.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching similar artists: ${e.message}")
                _detailError.value = "Error loading similar artists: ${e.message}"
                _similarArtists.value = emptyList()
            } finally {
                _isDetailLoading.value = false
            }
        }
    }
    
    // Artwork Categories State
    private val _artworkCategories = MutableStateFlow<Map<String, List<Gene>>>(emptyMap())
    val artworkCategories: StateFlow<Map<String, List<Gene>>> = _artworkCategories.asStateFlow()
    // ----------------------------------------

    // Debounce search term changes
    @OptIn(FlowPreview::class)
    fun setSearchTerm(term: String) {
        _searchTerm.value = term
        // Clear previous results immediately if search term is short
        if (term.length < 3) {
             _searchResults.value = emptyList()
             _error.value = null // Clear error too
             _isLoading.value = false // Stop loading indicator
             return // Don't trigger search yet
        }

        viewModelScope.launch {
            try {
                // Call API and update results
                _isLoading.value = true
                try {
                    val response = artistRepository.searchArtists(term)
                    if (response.isSuccessful) {
                        val searchResponse = response.body()
                        if (searchResponse != null) {
                            // Convert SearchResult to Artist objects
                            _searchResults.value = searchResponse._embedded.results.map { result ->
                                val artistId = result.links.self.href.split("/").last()
                                val imageUrl = if (result.links.thumbnail.href == "/assets/shared/missing_image.png") {
                                    null // Use a default placeholder in UI instead
                                } else {
                                    result.links.thumbnail.href
                                }
                                
                                // Create links object
                                val artistLinks = ArtistLinks(
                                    self = Link(href = result.links.self.href),
                                    permalink = Link(href = result.links.permalink.href),
                                    thumbnail = Link(href = result.links.thumbnail.href)
                                )
                                
                                Artist(
                                    id = artistId,
                                    name = result.title,
                                    imageUrl = imageUrl,
                                    nationality = null,
                                    birthday = null,
                                    deathday = null,
                                    biography = result.description,
                                    links = artistLinks,
                                    isFavorite = _favouriteIds.value.contains(artistId)
                                )
                            }
                        } else {
                            _searchResults.value = emptyList()
                        }
                    } else {
                        _error.value = "Failed to search artists"
                    }
                } catch (e: Exception) {
                    _error.value = e.message ?: "An error occurred"
                } finally {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            }
        }
    }

    fun setError(message: String) {
        _error.value = message
        _searchResults.value = emptyList()
        _isLoading.value = false
    }

    // --- Function to fetch artist details and artworks ---
    fun fetchArtistDetailsAndArtworks(artistId: String) {
        viewModelScope.launch {
            _isDetailLoading.value = true
            _detailError.value = null
            _artistDetail.value = null // Clear previous data
            _artistArtworks.value = emptyList() // Clear previous data

            try {
                // Fetch details and artworks concurrently (or sequentially if needed)
                // For simplicity, fetching sequentially here
                val artistResponse = artistRepository.getArtistDetailsById(artistId)
                _artistDetail.value = artistResponse

                val artworksResponse = artistRepository.getArtistArtworks(artistId)
                // Extract the list from the nested structure, handle potential nulls
                _artistArtworks.value = artworksResponse.embedded?.artworks ?: emptyList()

            } catch (e: IOException) {
                _detailError.value = "Network error fetching details. Please check connection."
                _artistDetail.value = null
                _artistArtworks.value = emptyList()
            } catch (e: Exception) {
                _detailError.value = "Error fetching details: ${e.message ?: "Unknown error"}"
                _artistDetail.value = null
                _artistArtworks.value = emptyList()
            } finally {
                _isDetailLoading.value = false
            }
        }
    }
    // ----------------------------------------------------

    // --- Function to clear artist detail state --- 
    fun clearArtistDetails() {
        _artistDetail.value = null
        _artistArtworks.value = emptyList()
        _detailError.value = null
        _isDetailLoading.value = false
        _artworkCategories.value = emptyMap()
        _similarArtists.value = emptyList()
    }
    
    /**
     * Fetches categories for a specific artwork
     * @param artworkId The ID of the artwork to fetch categories for
     */
    fun fetchArtworkCategories(artworkId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "Starting to fetch categories for artwork: $artworkId")
                
                // Check if we already have categories for this artwork
                if (_artworkCategories.value.containsKey(artworkId)) {
                    android.util.Log.d("HomeViewModel", "Categories already cached for artwork: $artworkId")
                    return@launch // Skip if already fetched
                }
                
                android.util.Log.d("HomeViewModel", "Calling repository for categories for artwork: $artworkId")
                val categories = artistRepository.getArtworkCategories(artworkId)
                android.util.Log.d("HomeViewModel", "Retrieved ${categories.size} categories for artwork: $artworkId")
                
                // Update the map with new categories
                val updatedMap = _artworkCategories.value.toMutableMap()
                updatedMap[artworkId] = categories
                _artworkCategories.value = updatedMap
                
                android.util.Log.d("HomeViewModel", "Updated categories state with ${categories.size} categories for artwork: $artworkId")
                
            } catch (e: Exception) {
                // Log error but don't update global error state to avoid disrupting the UI
                android.util.Log.e("HomeViewModel", "Error fetching categories for artwork $artworkId: ${e.message}", e)
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
    // --------------------------------------------

    init {
        // Load initial favourites
        loadFavourites()
    }

    private fun loadFavourites() {
        viewModelScope.launch {
            try {
                val response = artistRepository.getFavourites() // Assuming this returns Response<List<Artist>>
                if (response.isSuccessful) {
                    val favList = response.body() ?: emptyList()
                    _favourites.value = favList
                    _favouriteIds.value = favList.mapNotNull { it.id }.toSet()
                } else {
                     _error.value = "Failed to load favourites: ${response.code()}" // Handle error appropriately
                }
            } catch (e: Exception) {
                 _error.value = "Error loading favourites: ${e.message}"
                 _favourites.value = emptyList() // Clear on error
                 _favouriteIds.value = emptySet()
            }
        }
    }

    fun addFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                val response = artistRepository.addFavourite(artistId)
                if (response.isSuccessful) {
                    loadFavourites()
                } else {
                    _error.value = "Failed to add to favorites: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error adding to favorites: ${e.message}"
            }
        }
    }

    fun removeFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                val response = artistRepository.removeFavourite(artistId)
                if (response.isSuccessful) {
                    loadFavourites()
                } else {
                    _error.value = "Failed to remove from favorites: ${response.code()}"
                }
            } catch (e: Exception) {
            }
        }
    }
}
