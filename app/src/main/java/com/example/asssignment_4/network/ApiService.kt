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
    suspend fun getProfile(
        @Header("Authorization") authToken: String,
        @Header("Accept") acceptType: String = "application/json"
    ): Response<AuthResponse>
    
    // Overloaded version that will use the token from the interceptor if available
    @GET("/api/auth/me")
    suspend fun getProfile(): Response<AuthResponse>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>

    @DELETE("/api/auth/account")
    suspend fun deleteAccount(): Response<AuthResponse>

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

    // Favorites
    @GET("/api/favorites/")
    suspend fun getFavourites(): Response<List<Favorite>>

    @POST("/api/favorites/{artistId}")
    suspend fun addFavourite(@Path("artistId") artistId: String): Response<Favorite>

    @DELETE("/api/favorites/{artistId}")
    suspend fun removeFavourite(@Path("artistId") artistId: String): Response<Map<String, String>>

    // Similar Artists
    @GET("/api/artists/{id}/similar")
    suspend fun getSimilarArtists(@Path("id") artistId: String): Response<SimilarArtistsResponse>

    @GET("/api/artists/{id}/similar")
    suspend fun getSimilarArtists(
        @Path("id") artistId: String,
        @Header("Authorization") authToken: String
    ): Response<SimilarArtistsResponse>
}
