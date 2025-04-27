package com.example.asssignment_4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.* // Import necessary Flow operators
import kotlinx.coroutines.launch
import java.io.IOException // Import IOException

// Assuming Hilt setup for dependency injection
// If not using Hilt, adjust constructor injection accordingly

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
    private val _favouriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favouriteIds: StateFlow<Set<String>> = _favouriteIds.asStateFlow()

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
    }

    init {
        // Collect debounced search term changes to trigger API call
        loadFavourites() // Load initial favourites
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            _searchTerm
                .debounce(500L) // Wait 500ms after last input
                .filter { it.length >= 3 } // Only search if term is 3+ chars
                .distinctUntilChanged() // Only search if term actually changed
                .onEach { _isLoading.value = true } // Show loading indicator
                .mapLatest { term -> // Use mapLatest to cancel previous searches if new term arrives
                    try {
                        val response = artistRepository.searchArtists(term)
                        if (response.isSuccessful) {
                             Result.success(response.body() ?: emptyList())
                        } else {
                            Result.failure(IOException("API Error: ${response.code()} ${response.message()}"))
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
                .collect { result ->
                    _isLoading.value = false // Hide loading indicator
                    result.onSuccess { artists ->
                        _searchResults.value = artists
                        _error.value = null // Clear any previous error
                         if (artists.isEmpty()) {
                             _error.value = "No artists found for \"${_searchTerm.value}\""
                         }
                    }.onFailure { exception ->
                        _searchResults.value = emptyList() // Clear results on error
                        _error.value = "Search failed: ${exception.message ?: "Unknown error"}"
                    }
                }
        }
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

    fun toggleFavourite(artist: Artist) {
        val artistId = artist.id ?: return // Need an ID to toggle
        viewModelScope.launch {
            val isCurrentlyFav = _favouriteIds.value.contains(artistId)
            try {
                val response = if (isCurrentlyFav) { // Pass only artistId String
                    artistRepository.removeFavourite(artistId)
                } else {
                    artistRepository.addFavourite(artistId)
                }

                if (response.isSuccessful) {
                    // Reload favourites to get the updated list from the server
                    loadFavourites()
                } else {
                    _error.value = "Failed to update favourite: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error updating favourite: ${e.message}"
            }
        }
    }
}
