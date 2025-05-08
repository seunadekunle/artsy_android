package com.example.asssignment_4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.ArtistLinks
import com.example.asssignment_4.model.Artwork
import com.example.asssignment_4.model.Favorite
import com.example.asssignment_4.model.Gene
import com.example.asssignment_4.model.Link
import com.example.asssignment_4.model.SearchResponse
import com.example.asssignment_4.model.SimilarArtistsResponse
import com.example.asssignment_4.repository.ArtistRepository
import com.example.asssignment_4.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import android.util.Log
import com.example.asssignment_4.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val authManager: AuthManager,
    private val apiService: ApiService
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
    private val _favourites = MutableStateFlow<List<Favorite>>(emptyList())
    val favourites: StateFlow<List<Favorite>> = _favourites.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _favouriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favouriteIds: StateFlow<Set<String>> = _favouriteIds.asStateFlow()
    
    // Detailed information for favorites
    private val _detailedFavorites = MutableStateFlow<List<Pair<Favorite, Artist?>>>(emptyList())
    val detailedFavorites: StateFlow<List<Pair<Favorite, Artist?>>> = _detailedFavorites.asStateFlow()
    
    // Flag to track when we need to refresh data
    private val _needsRefresh = MutableStateFlow(true)
    val needsRefresh: StateFlow<Boolean> = _needsRefresh.asStateFlow()

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

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    fun fetchSimilarArtists(artistId: String, authToken: String?) {
        viewModelScope.launch {
            try {
                _isDetailLoading.value = true
                val response = artistRepository.getSimilarArtists(artistId, authToken)
                if (response.isSuccessful) {
                    val similarArtistsResponse = response.body()
                    if (similarArtistsResponse != null) {
                        _similarArtists.value = similarArtistsResponse.artists
                    } else {
                        _similarArtists.value = emptyList()
                        _detailError.value = "No similar artists found"
                    }
                } else {
                    _detailError.value = "Failed to fetch similar artists: ${response.code()}"
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
    }
    
    /**
     * Clears the search results and resets the search state
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _error.value = null
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

    // State-clearing function
    fun clearState() {
        _searchResults.value = emptyList()
        _error.value = null
        _artistDetail.value = null
        _artistArtworks.value = emptyList()
        _detailError.value = null
        _similarArtists.value = emptyList()
    }
    
    /**
     * Call this function when returning to screens that show favorites or artists
     * to ensure the UI reflects the latest favorite status changes
     */
    fun refreshFavoriteStatuses() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Refreshing favorite statuses")
            _needsRefresh.value = false
            
            // Refresh favorites from API
            getFavorites()
            
            // Then update all artists with current favorite status
            updateAllArtistsFavoriteStatus()
            
            // Fetch detailed information for favorites
            fetchFavoritesDetails()
        }
    }
    
    /**
     * Fetch detailed artist information for each favorite
     */
    suspend fun fetchFavoritesDetails() {
        try {
            val favorites = _favourites.value
            
            // Use async to fetch artist details in parallel
            val deferredArtists = favorites.map { favorite ->
                viewModelScope.async(Dispatchers.IO) {
                    try {
                        val response = artistRepository.getArtistDetailsById(favorite.artistId)
                        if (response != null) {
                            Pair(favorite, response)
                        } else {
                            Log.e("HomeViewModel", "No artist details found for ${favorite.artistId}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error fetching artist: ${e.message}")
                        null
                    }
                }
            }

            // Await all results and filter out nulls
            val results = deferredArtists.awaitAll().filterNotNull()
            _detailedFavorites.value = results
            Log.d("HomeViewModel", "Updated detailed favorites list with ${results.size} items")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching favorite details: ${e.message}")
        }
    }
    
    /**
     * Mark that favorite data needs refreshing (call this when changing favorite status)
     */
    fun markNeedsRefresh() {
        _needsRefresh.value = true
    }

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
        // Load favourites on init
        getFavorites()
        
        // Set up automatic refresh when login state changes
        viewModelScope.launch {
            authManager.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    Log.d("HomeViewModel", "Auth state changed to logged in, refreshing favorites")
                    refreshFavoriteStatuses()
                } else {
                    // Clear favorites when logged out
                    _favourites.value = emptyList()
                    _favouriteIds.value = emptySet()
                    _detailedFavorites.value = emptyList()
                }
            }
        }
        
        // Start periodic refresh of detailed favorites
        startAutoRefreshDetailedFavorites()
    }

    fun startAutoRefreshDetailedFavorites() {
        viewModelScope.launch {
            if (authManager.isLoggedIn.value) {
                // Run in a separate coroutine to avoid blocking UI
                launch {
                    try {
                        fetchFavoritesDetails()
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error refreshing favorites: ${e.message}")
                    }
                }
            }
        }
    }

    fun getFavorites() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Loading favorites...")
                val response = apiService.getFavourites()
                if (response.isSuccessful) {
                    val favorites = response.body() ?: emptyList()
                    _favourites.value = favorites
                    _favouriteIds.value = favorites.map { it.artistId }.toSet()
                    Log.d("HomeViewModel", "Loaded ${favorites.size} favorites: ${favorites.map { it.artistId }}")
                    Log.d("HomeViewModel", "Updated favorite IDs: ${_favouriteIds.value}")
                    
                    // Update isFavorited property in all artist lists
                    updateAllArtistsFavoriteStatus()
                } else {
                    val statusCode = response.code()
                    Log.e("HomeViewModel", "Failed to load favorites: $statusCode")
                    
                    if (statusCode == 401) {
                        // Token expired or invalid, notify auth system
                        Log.w("HomeViewModel", "Unauthorized (401) when loading favorites - token may be expired")
                        viewModelScope.launch {
                            authManager.handleUnauthorized()
                        }
                        // Clear favorites since we're not authorized
                        _favourites.value = emptyList()
                        _favouriteIds.value = emptySet()
                    } else {
                        _error.value = "Failed to load favourites: $statusCode"
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading favorites: ${e.message}")
                _error.value = "Error loading favourites: ${e.message}"
            }
        }
    }

    fun addFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Adding favorite: $artistId")
                val response = artistRepository.addFavourite(artistId)
                if (response.isSuccessful) {
                    response.body()?.let { favorite ->
                        // Update local state immediately
                        _favourites.value = _favourites.value + favorite
                        _favouriteIds.value = _favouriteIds.value + favorite.artistId
                        Log.d("HomeViewModel", "Successfully added favorite: ${favorite.artistId}")
                        
                        // Update favorite status in all artist lists
                        updateArtistFavoriteStatus(favorite.artistId, true)
                        
                        // Mark that we need to refresh detailed information
                        markNeedsRefresh()
                        _snackbarMessage.emit("Added to Favorites")
                    }
                } else {
                    val statusCode = response.code()
                    Log.e("HomeViewModel", "Failed to add favorite: $statusCode")
                    
                    if (statusCode == 401) {
                        // Token expired or invalid, notify auth system
                        Log.w("HomeViewModel", "Unauthorized (401) when adding favorite - token may be expired")
                        viewModelScope.launch {
                            authManager.handleUnauthorized()
                        }
                        // Clear favorites since we're not authorized
                        _favourites.value = emptyList()
                        _favouriteIds.value = emptySet()
                    } else {
                        _error.value = "Failed to add favourite: $statusCode"
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error adding favorite: ${e.message}")
                _error.value = "Error adding favourite: ${e.message}"
            }
        }
    }

    fun removeFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Removing favorite: $artistId")
                val response = apiService.removeFavourite(artistId)
                if (response.isSuccessful) {
                    // Update local state immediately
                    _favourites.value = _favourites.value.filter { it.artistId != artistId }
                    _favouriteIds.value = _favouriteIds.value - artistId
                    Log.d("HomeViewModel", "Successfully removed favorite: $artistId")
                    
                    // Update favorite status in all artist lists
                    updateArtistFavoriteStatus(artistId, false)
                    
                    // Mark that we need to refresh detailed information
                    markNeedsRefresh()
                    _snackbarMessage.emit("Removed from Favorites")
                } else {
                    val statusCode = response.code()
                    Log.e("HomeViewModel", "Failed to remove favorite: $statusCode")
                    
                    if (statusCode == 401) {
                        // Token expired or invalid, notify auth system
                        Log.w("HomeViewModel", "Unauthorized (401) when removing favorite - token may be expired")
                        viewModelScope.launch {
                            authManager.handleUnauthorized()
                        }
                    } else {
                        _error.value = "Failed to remove favourite: $statusCode"
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error removing favorite: ${e.message}")
                _error.value = "Error removing favourite: ${e.message}"
            }
        }
    }

    /**
     * Updates the isFavorite property of an artist across all lists
     * @param artistId ID of the artist to update
     * @param isFavorite New favorite status
     */
    private fun updateArtistFavoriteStatus(artistId: String, isFavorite: Boolean) {
        Log.d("HomeViewModel", "Updating favorite status for artist $artistId to $isFavorite")
        
        // Update in search results
        val updatedSearchResults = _searchResults.value.map { artist ->
            if (artist.id == artistId) {
                artist.copy(isFavorite = isFavorite)
            } else {
                artist
            }
        }
        _searchResults.value = updatedSearchResults
        
        // Update in artist detail if it's the same artist
        _artistDetail.value?.let { currentArtist ->
            if (currentArtist.id == artistId) {
                _artistDetail.value = currentArtist.copy(isFavorite = isFavorite)
            }
        }
        
        // Update in similar artists list
        val updatedSimilarArtists = _similarArtists.value.map { artist ->
            if (artist.id == artistId) {
                artist.copy(isFavorite = isFavorite)
            } else {
                artist
            }
        }
        _similarArtists.value = updatedSimilarArtists
        
        Log.d("HomeViewModel", "Favorite status updated for artist $artistId across all lists")
    }
    
    /**
     * Updates isFavorited property for all artists in all lists based on current favorites
     */
    private fun updateAllArtistsFavoriteStatus() {
        Log.d("HomeViewModel", "Updating favorite status for all artists based on current favorites: ${_favouriteIds.value}")
        
        // Update search results
        val updatedSearchResults = _searchResults.value.map { artist ->
            artist.copy(isFavorite = _favouriteIds.value.contains(artist.id))
        }
        _searchResults.value = updatedSearchResults
        
        // Update artist detail if any
        _artistDetail.value?.let { currentArtist ->
            _artistDetail.value = currentArtist.copy(
                isFavorite = _favouriteIds.value.contains(currentArtist.id)
            )
        }
        
        // Update similar artists list
        val updatedSimilarArtists = _similarArtists.value.map { artist ->
            artist.copy(isFavorite = _favouriteIds.value.contains(artist.id))
        }
        _similarArtists.value = updatedSimilarArtists
        
        Log.d("HomeViewModel", "Favorite status updated for all artists")
    }

    /**
     * Gets the current auth token
     * @return The auth token or null if not authenticated
     */
    fun getAuthToken(): String? {
        return authManager.getAuthToken()
    }
}
