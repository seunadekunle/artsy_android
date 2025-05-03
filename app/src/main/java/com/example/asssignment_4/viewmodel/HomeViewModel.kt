package com.example.asssignment_4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asssignment_4.model.Artist
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
                                Artist(
                                    id = result.links.self.href.split("/").last(),
                                    name = result.title,
                                    imageUrl = result.links.thumbnail.href,
                                    nationality = null,
                                    birthday = null,
                                    deathday = null,
                                    biography = result.description
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
