package com.example.asssignment_4.network

import com.example.asssignment_4.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth endpoints
    @POST("/api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<User>

    @POST("/api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<User>

    @GET("/api/auth/me")
    suspend fun getProfile(): Response<User>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>

    // Search
    @GET("/api/artists/search")
    suspend fun searchArtists(@Query("q") query: String): Response<SearchResponse>

    // Artist details
    @GET("/api/artist/{id}")
    suspend fun getArtistDetails(@Path("id") id: String): Response<Artist>

    @GET("/api/artists/{id}")
    suspend fun getArtistDetailsById(@Path("id") artistId: String): Artist

    @GET("/api/artists/{id}/artworks")
    suspend fun getArtistArtworks(@Path("id") artistId: String): List<Artwork>

    // Favourites
    @GET("/api/favourites")
    suspend fun getFavourites(): Response<List<Artist>>

    @POST("/api/favourites")
    suspend fun addFavourite(@Body favourite: FavouriteRequest): Response<Unit>

    @DELETE("/api/favourites")
    suspend fun removeFavourite(@Body favourite: FavouriteRequest): Response<Unit>
}

// --- Supporting request models (for login/register/favourite) ---
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val fullName: String, val email: String, val password: String)
data class FavouriteRequest(val artistId: String)
