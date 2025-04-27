package com.example.asssignment_4.repository

import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.User
import com.example.asssignment_4.model.Artwork
import com.example.asssignment_4.model.Gene
import com.example.asssignment_4.network.ApiService
import com.example.asssignment_4.network.FavouriteRequest

class ArtistRepository(private val api: ApiService) {
    suspend fun searchArtists(query: String) = api.searchArtists(query)
    suspend fun getArtistDetails(id: String) = api.getArtistDetails(id)
    suspend fun getFavourites() = api.getFavourites()
    suspend fun addFavourite(artistId: String) = api.addFavourite(FavouriteRequest(artistId))
    suspend fun removeFavourite(artistId: String) = api.removeFavourite(FavouriteRequest(artistId))
}

class AuthRepository(private val api: ApiService) {
    suspend fun login(email: String, password: String) = api.login(com.example.asssignment_4.network.LoginRequest(email, password))
    suspend fun register(fullName: String, email: String, password: String) = api.register(com.example.asssignment_4.network.RegisterRequest(fullName, email, password))
    suspend fun getProfile() = api.getProfile()
    suspend fun logout() = api.logout()
}
