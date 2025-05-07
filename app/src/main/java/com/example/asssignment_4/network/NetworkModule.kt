package com.example.asssignment_4.network

import android.content.Context
import android.util.Log
import com.example.asssignment_4.BuildConfig
import com.example.asssignment_4.repository.ArtistRepository
import com.example.asssignment_4.repository.AuthRepository
import com.example.asssignment_4.util.TokenManager
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
//    private const val BASE_URL = "https://artsy-3-456511.uc.r.appspot.com"
     private const val BASE_URL = "http://10.0.0.76:8080"

    @Provides
    @Singleton
    fun provideCookieJar(@ApplicationContext context: Context): PersistentCookieJar {
        return PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: PersistentCookieJar, 
        authInterceptor: AuthInterceptor,
        tokenManager: TokenManager
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .addInterceptor(authInterceptor) // Add our new AuthInterceptor
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .method(original.method, original.body)
                chain.proceed(requestBuilder.build())
            }
            // Add authenticator to handle 401 Unauthorized responses
            .authenticator { _, response ->
                if (response.code == 401) {
                    Log.w("NetworkModule", "Received 401 Unauthorized response from ${response.request.url}")
                    // Clear token on 401 responses
                    tokenManager.clearAuthToken()
                }
                // Don't retry the request - return null to indicate authentication failed
                null
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideArtistRepository(apiService: ApiService): ArtistRepository {
        return ArtistRepository(apiService)
    }

    // Remove the conflicting @Provides for AuthRepository
    // The binding is handled by @Binds in RepositoryModule.kt
//    @Provides
//    @Singleton
//    fun provideAuthRepository(apiService: ApiService): AuthRepository {
//        return AuthRepository(apiService)
//    }
}
