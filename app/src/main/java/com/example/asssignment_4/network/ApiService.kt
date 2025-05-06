package com.example.asssignment_4.network

import com.example.asssignment_4.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth endpoints
    @POST("/api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @GET("/api/auth/me")
    suspend fun getProfile(): Response<User>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>

    @DELETE("/api/auth/me") // Endpoint for deleting the authenticated user's account
    suspend fun deleteAccount(): Response<Unit>

    // Search
    @GET("/api/artists/search")
    suspend fun searchArtists(@Query("q") query: String): Response<SearchResponse>

    // Artist details
    @GET("/api/artist/{id}")
    suspend fun getArtistDetails(@Path("id") id: String): Response<Artist>

    @GET("/api/artists/{id}")
    suspend fun getArtistDetailsById(@Path("id") artistId: String): Response<Artist>

    @GET("/api/artists/{id}/artworks")
    suspend fun getArtistArtworks(@Path("id") artistId: String): ArtworksResponse
    
    // Artwork categories
    @GET("/api/artworks/{id}/categories")
    suspend fun getArtworkCategories(@Path("id") artworkId: String): Response<GenesResponse>

    // Favourites
    @GET("/api/favourites")
    suspend fun getFavourites(): Response<List<Artist>>

    @POST("/api/favourites")
    suspend fun addFavourite(@Body favourite: FavouriteRequest): Response<Unit>

    @DELETE("/api/favourites")
    suspend fun removeFavourite(@Body favourite: FavouriteRequest): Response<Unit>

    // Similar Artists
    @GET("/api/artists/{id}/similar")
    suspend fun getSimilarArtists(@Path("id") artistId: String): Response<List<Artist>>

    @GET("/api/artists/{id}/similar")
    suspend fun getSimilarArtists(
        @Path("id") artistId: String,
        @Header("Authorization") authToken: String
    ): Response<List<Artist>>
}
