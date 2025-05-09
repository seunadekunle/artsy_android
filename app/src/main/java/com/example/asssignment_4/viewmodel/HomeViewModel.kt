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
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val authManager: AuthManager,
    private val apiService: ApiService
) : ViewModel() {

    companion object {
        // Global snackbar message flow that can be observed from anywhere in the app
        private val _globalSnackbarMessage = MutableSharedFlow<String>()
        val globalSnackbarMessage = _globalSnackbarMessage.asSharedFlow()
        
        // Function to emit a snackbar message from anywhere in the app
        suspend fun showSnackbar(message: String) {
            _globalSnackbarMessage.emit(message)
        }
    }

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

    // Local snackbar message flow for backward compatibility
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()
    
    // Forward local snackbar messages to the global flow
    private suspend fun showSnackbar(message: String) {
        _snackbarMessage.emit(message)
        // Also emit to global flow
        _globalSnackbarMessage.emit(message)
    }

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
                
                // First, check if this artist is in favorites to ensure correct status
                val isFavorite = _favouriteIds.value.contains(artistId)
                Log.d("HomeViewModel", "Artist $artistId favorite status before fetch: $isFavorite")
                
                // Check if we have a cached version that's still fresh
                val cachedArtist = artistDetailCache[artistId]
                if (cachedArtist != null && System.currentTimeMillis() - cachedArtist.second < 60000) {
                    Log.d("HomeViewModel", "Using cached artist details for $artistId")
                    // Make sure we update the favorite status even for cached artists
                    val updatedArtist = cachedArtist.first.copy(isFavorite = isFavorite)
                    _artistDetail.value = updatedArtist
                    // Update the cache with the correct favorite status
                    artistDetailCache[artistId] = Pair(updatedArtist, System.currentTimeMillis())
                } else {
                    // Fetch fresh data
                    Log.d("HomeViewModel", "Fetching fresh artist details for $artistId")
                    val artist = artistRepository.getArtistDetailsById(artistId)
                    if (artist != null) {
                        // Ensure favorite status is set correctly before caching
                        val artistWithCorrectFavoriteStatus = artist.copy(isFavorite = isFavorite)
                        // Cache the result with correct favorite status
                        artistDetailCache[artistId] = Pair(artistWithCorrectFavoriteStatus, System.currentTimeMillis())
                        _artistDetail.value = artistWithCorrectFavoriteStatus
                        Log.d("HomeViewModel", "Set artist detail with favorite status: $isFavorite")
                    } else {
                        _detailError.value = "Failed to fetch artist details"
                    }
                }
                
                // Fetch artworks for this artist
                try {
                    val artworksResponse = artistRepository.getArtistArtworks(artistId)
                    // Extract artworks from the embedded structure
                    _artistArtworks.value = artworksResponse.embedded?.artworks ?: emptyList()
                    Log.d("HomeViewModel", "Fetched ${_artistArtworks.value.size} artworks for artist $artistId")
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error fetching artworks: ${e.message}", e)
                    _artistArtworks.value = emptyList()
                }
                
                // Fetch similar artists
                fetchSimilarArtists(artistId, getAuthToken())
                
                // Ensure favorite status is correctly reflected in UI
                updateArtistFavoriteStatus(artistId, isFavorite)
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching artist details: ${e.message}")
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

    // Cache timestamp for favorites to avoid excessive refreshes
    private var lastFavoritesRefreshTime: Long = 0
    private val favoritesRefreshThreshold = 10 * 1000 // 10 seconds
    
    fun refreshFavoriteStatuses(forceRefresh: Boolean = false) {
        // Check if we've refreshed recently to avoid unnecessary network calls
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && currentTime - lastFavoritesRefreshTime < favoritesRefreshThreshold) {
            Log.d("HomeViewModel", "Skipping favorites refresh - recently updated")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Refreshing favorite statuses...")
                
                // Use Dispatchers.IO for network operations
                val favoritesResult = withContext(Dispatchers.IO) {
                    artistRepository.getFavourites()
                }
                
                if (favoritesResult.isSuccessful) {
                    val favorites = favoritesResult.body() ?: emptyList()
                    
                    // Update the last refresh timestamp
                    lastFavoritesRefreshTime = System.currentTimeMillis()
                    
                    // Update UI state immediately
                    _favourites.value = favorites
                    val favoriteIds = favorites.map { it.artistId }.toSet()
                    _favouriteIds.value = favoriteIds
                    
                    Log.d("HomeViewModel", "Favorites updated, count: ${favorites.size}")
                    
                    // Update the isFavorite field in all current lists
                    updateAllArtistsFavoriteStatus()
                    
                    // Mark that we need to refresh detailed information
                    if (_detailedFavorites.value.isEmpty() || 
                        _detailedFavorites.value.size != favorites.size ||
                        _detailedFavorites.value.any { it.second == null }) {
                        _needsRefresh.value = true
                    }
                } else {
                    // Handle specific error codes
                    when (favoritesResult.code()) {
                        401 -> {
                            Log.w("HomeViewModel", "Unauthorized (401) when fetching favorites - token may be expired")
                            // Clear favorites if unauthorized
                            _favourites.value = emptyList()
                            _favouriteIds.value = emptySet()
                        }
                        else -> {
                            Log.e("HomeViewModel", "Failed to refresh favorites: ${favoritesResult.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing favorites: ${e.message}")
            }
        }
    }

    suspend fun fetchFavoritesDetails(forceRefresh: Boolean = false) {
        if (_favourites.value.isEmpty()) {
            Log.d("HomeViewModel", "No favorites to fetch details for")
            _detailedFavorites.value = emptyList()
            return
        }
        
        try {
            Log.d("HomeViewModel", "Fetching details for ${_favourites.value.size} favorites, forceRefresh=$forceRefresh")
            
            val currentTime = System.currentTimeMillis()
            val cacheExpiry = if (forceRefresh) 0 else 15 * 60 * 1000 // Force refresh ignores cache
            
            // Results list to be built
            val detailedFavorites = mutableListOf<Pair<Favorite, Artist?>>()
            
            // Favorites that need details to be fetched
            val favoritesToFetch = mutableListOf<Favorite>()
            
            // First, use cache where available and immediately show cached results
            for (favorite in _favourites.value) {
                val cached = artistDetailCache[favorite.artistId]
                
                if (!forceRefresh && cached != null && (currentTime - cached.second < cacheExpiry)) {
                    // Cache hit - use cached artist detail
                    Log.d("HomeViewModel", "Cache hit for artist ${favorite.artistId}")
                    detailedFavorites.add(Pair(favorite, cached.first))
                } else {
                    // Cache miss or force refresh - need to fetch
                    favoritesToFetch.add(favorite)
                    // Add placeholder to maintain order
                    detailedFavorites.add(Pair(favorite, null))
                }
            }
            
            // Immediately update UI with cached results
            if (detailedFavorites.isNotEmpty()) {
                _detailedFavorites.value = detailedFavorites.toList()
                Log.d("HomeViewModel", "Updated UI with cached results first")
            }
            
            Log.d("HomeViewModel", "Cache hits: ${detailedFavorites.size - favoritesToFetch.size}, misses: ${favoritesToFetch.size}")
            
            // Fetch details for all cache misses in parallel using coroutines
            if (favoritesToFetch.isNotEmpty()) {
                val deferredResults = favoritesToFetch.map { favorite ->
                    viewModelScope.async(Dispatchers.IO) {
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
                }
                
                // Wait for all parallel requests to complete
                val fetchedResults = deferredResults.awaitAll()
                
                // Update the detailed favorites with the fetched results
                val updatedFavorites = detailedFavorites.map { (favorite, artist) ->
                    // If this was a placeholder (null artist), find the fetched result
                    if (artist == null) {
                        fetchedResults.find { it.first.artistId == favorite.artistId } ?: Pair(favorite, null)
                    } else {
                        // Keep the cached result
                        Pair(favorite, artist)
                    }
                }
                
                // Update the state with all detailed favorites
                _detailedFavorites.value = updatedFavorites
                Log.d("HomeViewModel", "Detailed favorites fully updated, count: ${updatedFavorites.size}")
                
                // Force a recomposition of any UI observing this state
                // This is a trick to ensure the UI updates even if the values are the same
                if (forceRefresh) {
                    delay(50) // Small delay
                    _detailedFavorites.value = updatedFavorites.toList()
                }
            }
            
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
                        showSnackbar("Added to Favorites")

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
                    showSnackbar("Removed from Favorites")
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
     * Updates the favorite status of an artist across all UI collections
     * to ensure consistent display everywhere
     */
    // Cache timeout values - shorter values for faster UI updates
    // Using existing favoritesRefreshThreshold but with updated value
    // Using existing lastFavoritesRefreshTime but with better initialization
    
    /**
     * Optimized global favorite status synchronization method that ensures favorites are consistent
     * across all screens and UI components with minimal network calls
     */
    fun synchronizeFavorites() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Fast synchronizing favorites across all screens")
                
                // First, immediately update UI with cached data for instant feedback
                updateAllArtistsFavoriteStatus()
                
                // Track if we did any network operations
                var didNetworkOperations = false
                
                // Refresh favorites with non-forced refresh for better performance
                Log.d("HomeViewModel", "Refreshing favorites from network")
                refreshFavoriteStatuses(false)
                didNetworkOperations = true
                
                // Refresh detailed favorites for the HomeScreen
                Log.d("HomeViewModel", "Refreshing detailed favorites")
                fetchFavoritesDetails(false)
                didNetworkOperations = true
                
                // Update UI again after network operations
                if (didNetworkOperations) {
                    updateAllArtistsFavoriteStatus()
                    // We don't emit a snackbar message here since this is just a background sync
                    // Snackbars should only be shown for explicit user actions like adding/removing favorites
                }
                
                Log.d("HomeViewModel", "Fast favorites sync complete")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error synchronizing favorites: ${e.message}")
                showSnackbar("Failed to sync favorites")
            }
        }
    }
    
    private fun updateArtistFavoriteStatus(artistId: String, isFavorite: Boolean) {
        Log.d("HomeViewModel", "Updating favorite status for artist $artistId to $isFavorite")
        
        viewModelScope.launch {
            // First immediately update favorite IDs set to ensure quick UI response
            if (isFavorite) {
                val updatedIds = _favouriteIds.value.toMutableSet()
                updatedIds.add(artistId)
                _favouriteIds.value = updatedIds
            } else {
                val updatedIds = _favouriteIds.value.toMutableSet()
                updatedIds.remove(artistId)
                _favouriteIds.value = updatedIds
            }
            
            // Update in search results
            val updatedSearchResults = _searchResults.value.map { artist ->
                if (artist.id == artistId) {
                    artist.copy(isFavorite = isFavorite)
                } else {
                    artist
                }
            }
            _searchResults.value = updatedSearchResults
            
            // Update in artist detail if it's the same artist - this affects the star in the app bar
            _artistDetail.value?.let { currentArtist ->
                if (currentArtist.id == artistId) {
                    // Create a new artist object to ensure state change is detected
                    val updatedArtist = currentArtist.copy(isFavorite = isFavorite)
                    Log.d("HomeViewModel", "Updated artist detail favorite status: ${updatedArtist.isFavorite}")
                    _artistDetail.value = updatedArtist
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
            
            // Update the cache to ensure consistency when navigating back to screens
            artistDetailCache.entries.forEach { (id, pair) ->
                if (id == artistId) {
                    val (artist, timestamp) = pair
                    artistDetailCache[id] = Pair(artist.copy(isFavorite = isFavorite), timestamp)
                }
            }
        }
        
        Log.d("HomeViewModel", "Favorite status updated for artist $artistId across all lists")
    }
    
    /**
     * Updates favorite status for all artists across all UI components
     * Made public so screens can use it for immediate UI updates
     */
    fun updateAllArtistsFavoriteStatus() {
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
    
    /**
     * Validates and fixes problematic artist image URLs
     * Especially focused on Picasso family artists based on memory info
     */
    suspend fun validateArtistImageUrls() {
        try {
            Log.d("HomeViewModel", "Validating artist image URLs after force close")
            
            // Process favorites first since they're important for the HomeScreen
            _favourites.value.forEach { favorite ->
                try {
                    // Check for known problematic URLs
                    if (favorite.artistImage.isNullOrEmpty() || 
                        !favorite.artistImage.startsWith("http")) {
                        Log.w("HomeViewModel", "Found invalid image URL for favorite: ${favorite.artistId}")
                        
                        // Try to get updated artist details to fix the URL
                        val artistDetails = artistRepository.getArtistDetailsById(favorite.artistId)
                        if (artistDetails != null && !artistDetails.imageUrl.isNullOrEmpty()) {
                            Log.d("HomeViewModel", "Fixed image URL for ${favorite.artistId}")
                        }
                    }
                    
                    // Special handling for Picasso family artists
                    if (favorite.artistName.contains("Picasso", ignoreCase = true)) {
                        Log.d("HomeViewModel", "Special handling for Picasso artist: ${favorite.artistId}")
                        // Force refresh this artist's details
                        artistRepository.getArtistDetailsById(favorite.artistId)
                    }
                } catch (e: Exception) {
                    // Just log errors and continue with the next artist
                    Log.e("HomeViewModel", "Error validating image for ${favorite.artistId}: ${e.message}")
                }
                
                // Small delay to avoid hammering the API
                delay(50)
            }
            
            // Force UI update by triggering a change in the detailed favorites list
            // This is the key to making the UI refresh immediately
            if (_detailedFavorites.value.isNotEmpty()) {
                Log.d("HomeViewModel", "Forcing UI update for detailed favorites")
                // Create a new list reference to trigger State change
                _detailedFavorites.value = _detailedFavorites.value.toList()
                // Also update the main favorites list
                _favourites.value = _favourites.value.toList()
            }
            
            Log.d("HomeViewModel", "Artist image URL validation complete")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error in validateArtistImageUrls: ${e.message}")
        }
    }
}
