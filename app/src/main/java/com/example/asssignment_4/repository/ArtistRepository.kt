package com.example.asssignment_4.repository

import com.example.asssignment_4.model.*
import com.example.asssignment_4.network.ApiService
import com.example.asssignment_4.network.FavouriteRequest
import retrofit2.Response

class ArtistRepository(private val api: ApiService) {
    suspend fun searchArtists(query: String): Response<SearchResponse> = api.searchArtists(query)
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
