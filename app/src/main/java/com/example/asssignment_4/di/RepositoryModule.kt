package com.example.asssignment_4.di

import com.example.asssignment_4.network.ApiService
import com.example.asssignment_4.repository.AuthRepository
import com.example.asssignment_4.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(apiService: ApiService): AuthRepository {
        return AuthRepositoryImpl(apiService)
    }

    // Add other repository bindings here if needed
}
