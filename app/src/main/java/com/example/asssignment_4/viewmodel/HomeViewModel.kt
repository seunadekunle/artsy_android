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

    private val artistDetailCache = mutableMapOf<String, Pair<Artist, Long>>()

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

    // For artwork categories (genes)
    private val _artworkCategories = MutableStateFlow<Map<String, List<Gene>>>(emptyMap())
    
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

    fun fetchSimilarArtists(artistId: String, authToken: String?) {
        viewModelScope.launch {
            try {
                _isDetailLoading.value = true
                val response = artistRepository.getSimilarArtists(artistId, authToken)
                if (response.isSuccessful) {
                    val similarArtists = response.body()?.artists ?: emptyList()
                    
                    // Update favorite status for all similar artists
                    val updatedSimilarArtists = similarArtists.map { artist ->
                        artist.copy(isFavorite = _favouriteIds.value.contains(artist.id))
                    }
                    
                    _similarArtists.value = updatedSimilarArtists
                } else {
                    _error.value = "Failed to load similar artists: ${response.code()}"
                }
                
                _isDetailLoading.value = false
                
            } catch (e: Exception) {
                _isDetailLoading.value = false
                _error.value = "Error loading similar artists: ${e.message}"
            }
        }
    }

    @OptIn(FlowPreview::class)
    fun setSearchTerm(term: String) {
        _searchTerm.value = term
        
        if (term.isEmpty()) {
            clearSearchResults()
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Wait a bit to avoid excessive API calls while typing
                delay(400)
                
                // If the term has changed since we started the delay, abort
                if (term != _searchTerm.value) {
                    return@launch
                }
                
                val response = artistRepository.searchArtists(_searchTerm.value)
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    // Extract artists from the search response structure
                    // The actual structure depends on your API, adjust as needed
                    val results = searchResponse?._embedded?.results ?: emptyList()
                    
                    // Convert search results to Artist objects
                    val artists = results.mapNotNull { result ->
                        // This conversion depends on your actual model structure
                        // You may need to adjust this based on your API response
                        try {
                            Artist(
                                id = result.links.permalink.href.substringAfterLast("/"),
                                name = result.title,
                                biography = result.description,
                                imageUrl = result.links.thumbnail.href
                            )
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Error converting search result to artist: ${e.message}")
                            null
                        }
                    }
                    
                    // Update isFavorite state based on current favorite IDs
                    val updatedArtists = artists.map { artist ->
                        artist.copy(isFavorite = _favouriteIds.value.contains(artist.id))
                    }
                    
                    _searchResults.value = updatedArtists
                } else {
                    _error.value = "Search failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Search error: ${e.message}"
                Log.e("HomeViewModel", "Search error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _error.value = null
        _isLoading.value = false
    }

    fun fetchArtistDetailsAndArtworks(artistId: String) {
        viewModelScope.launch {
            try {
                _isDetailLoading.value = true
                _detailError.value = null
                
                // Fetch from cache if available
                val cachedArtist = artistDetailCache[artistId]?.first
                if (cachedArtist != null) {
                    Log.d("HomeViewModel", "Using cached artist details for $artistId")
                    _artistDetail.value = cachedArtist
                    fetchSimilarArtists(artistId, authManager.getAuthToken())
                    return@launch
                }
                
                val response = artistRepository.getArtistDetailsById(artistId)
                if (response != null) {
                    // Update with favorite status
                    val isFavorite = _favouriteIds.value.contains(response.id)
                    _artistDetail.value = response.copy(isFavorite = isFavorite)
                    
                    // Fetch artworks for this artist
                    try {
                        val artworksResponse = artistRepository.getArtistArtworks(artistId)
                        // Extract artworks from the embedded structure
                        _artistArtworks.value = artworksResponse.embedded?.artworks ?: emptyList()
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error fetching artworks: ${e.message}", e)
                        _artistArtworks.value = emptyList()
                    }
                    
                    // Fetch similar artists
                    fetchSimilarArtists(artistId, authManager.getAuthToken())
                } else {
                    _detailError.value = "Failed to load artist details"
                }
            } catch (e: Exception) {
                _detailError.value = "Error: ${e.message}"
                Log.e("HomeViewModel", "Error fetching artist details: ${e.message}", e)
            } finally {
                _isDetailLoading.value = false
            }
        }
    }

    fun clearState() {
        _searchResults.value = emptyList()
        _artistDetail.value = null
        _artistArtworks.value = emptyList()
        _similarArtists.value = emptyList()
        _error.value = null
        _detailError.value = null
    }

    fun refreshFavoriteStatuses() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Refreshing favorite statuses...")
                val favoritesResult = artistRepository.getFavourites()
                if (favoritesResult.isSuccessful) {
                    val favorites = favoritesResult.body() ?: emptyList()
                    _favourites.value = favorites
                    _favouriteIds.value = favorites.map { it.artistId }.toSet()
                    
                    Log.d("HomeViewModel", "Favorites updated, count: ${favorites.size}")
                    
                    // Update the isFavorite field in all current lists
                    updateAllArtistsFavoriteStatus()
                } else {
                    Log.e("HomeViewModel", "Failed to refresh favorites: ${favoritesResult.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing favorites: ${e.message}")
            }
        }
    }

    suspend fun fetchFavoritesDetails() {
        if (_favourites.value.isEmpty()) {
            Log.d("HomeViewModel", "No favorites to fetch details for")
            _detailedFavorites.value = emptyList()
            return
        }
        
        try {
            Log.d("HomeViewModel", "Fetching details for ${_favourites.value.size} favorites")
            
            val currentTime = System.currentTimeMillis()
            val cacheExpiry = 5 * 60 * 1000 // 5 minutes in milliseconds
            
            // Results list to be built
            val detailedFavorites = mutableListOf<Pair<Favorite, Artist?>>()
            
            // Favorites that need details to be fetched
            val favoritesToFetch = mutableListOf<Favorite>()
            
            // First, use cache where available
            for (favorite in _favourites.value) {
                val cached = artistDetailCache[favorite.artistId]
                
                if (cached != null && (currentTime - cached.second < cacheExpiry)) {
                    // Cache hit - use cached artist detail
                    Log.d("HomeViewModel", "Cache hit for artist ${favorite.artistId}")
                    detailedFavorites.add(Pair(favorite, cached.first))
                } else {
                    // Cache miss - need to fetch
                    favoritesToFetch.add(favorite)
                }
            }
            
            Log.d("HomeViewModel", "Cache hits: ${detailedFavorites.size}, misses: ${favoritesToFetch.size}")
            
            // Fetch details for all cache misses in parallel
            if (favoritesToFetch.isNotEmpty()) {
                val fetchedDetails = favoritesToFetch.map { favorite ->
                    try {
                        Log.d("HomeViewModel", "Fetching details for favorite: ${favorite.artistId}")
                        val artist = artistRepository.getArtistDetailsById(favorite.artistId)
                        if (artist != null) {
                            // Cache the result
                            artistDetailCache[favorite.artistId] = Pair(artist, currentTime)
                        }
                        Pair(favorite, artist)
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error fetching artist details: ${e.message}")
                        Pair(favorite, null)
                    }
                }
                detailedFavorites.addAll(fetchedDetails)
            }
            
            // Update the state with all detailed favorites
            _detailedFavorites.value = detailedFavorites
            Log.d("HomeViewModel", "Detailed favorites updated, count: ${detailedFavorites.size}")
            
            // Reset the refresh flag
            _needsRefresh.value = false
            
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error in fetchFavoritesDetails: ${e.message}")
        }
    }

    fun markNeedsRefresh() {
        _needsRefresh.value = true
    }

    fun clearArtistDetails() {
        _artistDetail.value = null
        _artistArtworks.value = emptyList()
        _similarArtists.value = emptyList()
        _isDetailLoading.value = false
        _detailError.value = null
    }

    fun fetchArtworkCategories(artworkId: String) {
        viewModelScope.launch {
            try {
                // Avoid fetching if already cached
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
    
    fun getArtworkCategories(artworkId: String): List<Gene> {
        return _artworkCategories.value[artworkId] ?: emptyList()
    }

    fun startAutoRefreshDetailedFavorites() {
        viewModelScope.launch {
            while (isActive) {
                if (_needsRefresh.value) {
                    Log.d("HomeViewModel", "Auto-refreshing favorite details")
                    fetchFavoritesDetails()
                }
                
                // Check again after delay
                delay(10000) // 10 seconds
            }
        }
    }

    fun getFavorites() {
        viewModelScope.launch {
            try {
                val response = artistRepository.getFavourites()
                if (response.isSuccessful) {
                    val favorites = response.body() ?: emptyList()
                    _favourites.value = favorites
                    _favouriteIds.value = favorites.map { it.artistId }.toSet()
                    
                    Log.d("HomeViewModel", "Favorites loaded, count: ${favorites.size}")
                    
                    // Make sure favorite status is consistently reflected in all lists
                    updateAllArtistsFavoriteStatus()
                    
                    // Mark that we need to refresh detailed information
                    markNeedsRefresh()
                } else {
                    Log.e("HomeViewModel", "Failed to load favorites: ${response.code()}")
                    
                    if (response.code() == 401) {
                        viewModelScope.launch {
                            authManager.handleUnauthorized()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading favorites: ${e.message}")
            }
        }
    }

    fun addFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                val response = artistRepository.addFavourite(artistId)
                if (response.isSuccessful) {
                    response.body()?.let { favorite ->
                        // Update local state immediately
                        val updatedFavorites = _favourites.value.toMutableList().apply {
                            add(favorite)
                        }
                        _favourites.value = updatedFavorites
                        
                        val updatedIds = _favouriteIds.value.toMutableSet().apply {
                            add(favorite.artistId)
                        }
                        _favouriteIds.value = updatedIds
                        
                        Log.d("HomeViewModel", "Successfully added favorite: ${favorite.artistId}")

                        // Update favorite status in all artist lists
                        updateArtistFavoriteStatus(favorite.artistId, true)

                        // Mark that we need to refresh detailed information
                        markNeedsRefresh()
                        _snackbarMessage.emit("Added to Favorites")

                        // Fetch and cache artist detail in the background
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val detail = artistRepository.getArtistDetailsById(favorite.artistId)
                                if (detail != null) {
                                    val now = System.currentTimeMillis()
                                    artistDetailCache[favorite.artistId] = Pair(detail, now)
                                    Log.d("HomeViewModel", "Cached artist detail for ${favorite.artistId} after favoriting")
                                }
                            } catch (e: Exception) {
                                Log.e("HomeViewModel", "Failed to fetch/cache artist detail for ${favorite.artistId}: ${e.message}")
                            }
                        }
                    }
                } else {
                    Log.e("HomeViewModel", "Failed to add favorite: ${response.code()}")
                    _error.value = "Failed to add favorite: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error adding favorite: ${e.message}")
                _error.value = "Error adding favorite: ${e.message}"
            }
        }
    }

    fun removeFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Removing favorite: $artistId")
                val response = artistRepository.removeFavourite(artistId)
                if (response.isSuccessful) {
                    // Update local state immediately
                    _favourites.value = _favourites.value.filter { it.artistId != artistId }
                    
                    val updatedIds = _favouriteIds.value.toMutableSet()
                    updatedIds.remove(artistId)
                    _favouriteIds.value = updatedIds
                    
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

    fun getAuthToken(): String? {
        return authManager.getAuthToken()
    }
}
