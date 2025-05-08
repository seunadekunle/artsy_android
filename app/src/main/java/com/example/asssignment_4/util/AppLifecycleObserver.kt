package com.example.asssignment_4.util

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.asssignment_4.repository.ArtistRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Lifecycle observer that handles app foreground/background transitions
 * and triggers data refreshes when the app returns to the foreground.
 */
@ActivityRetainedScoped
class AppLifecycleObserver @Inject constructor(
    private val application: Application,
    private val authManager: AuthManager,
    private val artistRepository: ArtistRepository
) : DefaultLifecycleObserver {
    
    // Application scope for long-running operations
    private val appScope = CoroutineScope(Dispatchers.Main.immediate)
    
    // Track if app is returning from background
    private var isReturningFromBackground = false
    
    // Timestamp of when the app went to background
    private var lastBackgroundedTime: Long = 0
    
    // Flag to track if a refresh is in progress to prevent duplicates
    private val isRefreshing = AtomicBoolean(false)
    
    // Threshold to consider app as truly backgrounded (500ms)
    private val BACKGROUND_THRESHOLD = 500L
    
    init {
        // Monitor process lifecycle for true foreground/background transitions
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Process comes to foreground
                val timeInBackground = System.currentTimeMillis() - lastBackgroundedTime
                if (lastBackgroundedTime > 0 && timeInBackground > BACKGROUND_THRESHOLD) {
                    Log.d(TAG, "Process returned to foreground after ${timeInBackground}ms")
                    triggerDataRefresh(true)
                }
            }
            
            override fun onStop(owner: LifecycleOwner) {
                // Process goes to background
                lastBackgroundedTime = System.currentTimeMillis()
                Log.d(TAG, "Process went to background")
            }
        })
        
        // Also register activity lifecycle callbacks for more granular detection
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedActivities = 0
            
            override fun onActivityStarted(activity: android.app.Activity) {
                if (startedActivities == 0) {
                    // App comes to foreground (activity level)
                    val timeInBackground = System.currentTimeMillis() - lastBackgroundedTime
                    if (lastBackgroundedTime > 0 && timeInBackground > BACKGROUND_THRESHOLD) {
                        Log.d(TAG, "Activity returned to foreground after ${timeInBackground}ms")
                        // Only do a light refresh here as ProcessLifecycleOwner will handle the main refresh
                        triggerDataRefresh(false)
                    }
                }
                startedActivities++
            }
            
            override fun onActivityStopped(activity: android.app.Activity) {
                startedActivities--
                if (startedActivities == 0) {
                    // App goes to background (activity level)
                    Log.d(TAG, "All activities stopped")
                }
            }
            
            // Implement required methods
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }
    
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "Activity onStart")
        val currentTime = System.currentTimeMillis()
        
        // If returning from a true background state, refresh data
        if (currentTime - lastBackgroundedTime > BACKGROUND_THRESHOLD) {
            isReturningFromBackground = true
            triggerDataRefresh(true)
        }
    }
    
    override fun onResume(owner: LifecycleOwner) {
        Log.d(TAG, "Activity onResume")
        if (isReturningFromBackground) {
            isReturningFromBackground = false
            triggerDataRefresh(true)
        } else {
            // Always do a light refresh when resuming
            triggerDataRefresh(false)
        }
    }
    
    /**
     * Public method that can be called to force a data refresh
     * Used when app is cold-started after force close
     */
    fun forceRefreshData() {
        Log.d(TAG, "Force refresh data requested")
        triggerDataRefresh(true)
    }
    
    /**
     * Reset internal state - called when app is force-closed and reopened
     */
    fun resetState() {
        Log.d(TAG, "Resetting state after force close")
        isReturningFromBackground = false
        lastBackgroundedTime = 0
        isRefreshing.set(false)
        
        // Clear any tokens or cookies that might have expired
        appScope.launch {
            try {
                authManager.checkTokenValidity()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking token validity: ${e.message}")
            }
        }
    }
    
    /**
     * Trigger data refresh operations
     */
    private fun triggerDataRefresh(forceRefresh: Boolean) {
        // Use atomic boolean to prevent multiple simultaneous refreshes
        if (!isRefreshing.compareAndSet(false, true)) {
            Log.d(TAG, "Refresh already in progress, skipping")
            return
        }
        
        Log.d(TAG, "Triggering data refresh, forceRefresh=$forceRefresh")
        
        // Use appScope to ensure this survives configuration changes
        appScope.launch {
            try {
                // First check auth status
                refreshAuthStatus()
                
                // Then refresh favorites data
                refreshFavorites(forceRefresh)
                
                Log.d(TAG, "Data refresh triggered")
            } catch (e: Exception) {
                Log.e(TAG, "Error during data refresh: ${e.message}")
            } finally {
                // Mark as no longer refreshing
                isRefreshing.set(false)
            }
        }
    }
    
    /**
     * Refresh authentication status
     */
    private suspend fun refreshAuthStatus() {
        try {
            // This will update the isLoggedIn state flow in AuthManager
            val token = authManager.getAuthToken()
            Log.d(TAG, "Auth status refreshed, has token: ${token != null}")
            
            // Check profile image URL status for debugging
            if (token != null) {
                val profile = authManager.getCurrentUserProfile()
                profile?.let {
                    Log.d(TAG, "User profile has image URL: ${it.avatarUrl ?: "null"}")
                    if (it.avatarUrl.isNullOrEmpty()) {
                        Log.w(TAG, "User has no profile image URL - might need to fetch updated profile")
                    }
                }
            }
            
            // Add delay to ensure snackbar messages are visible as mentioned in memory
            delay(200)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing auth status: ${e.message}")
        }
    }
    
    /**
     * Refresh favorites data
     */
    private suspend fun refreshFavorites(forceRefresh: Boolean) {
        try {
            // Only fetch favorites if we have a valid auth token
            if (authManager.isLoggedIn.value) {
                // Get favorite ids
                val response = artistRepository.getFavourites()
                if (response.isSuccessful) {
                    val favorites = response.body() ?: emptyList()
                    Log.d(TAG, "Favorites refreshed: ${favorites.size} items")
                    
                    // Add detailed logging for favorite operations as mentioned in memory
                    favorites.forEach { favorite -> 
                        Log.d(TAG, "Favorite: id=${favorite.artistId}, name=${favorite.artistName}, image=${favorite.artistImage}")
                    }
                    
                    // Fetch detailed info for each favorite in background
                    if (forceRefresh && favorites.isNotEmpty()) {
                        appScope.launch(Dispatchers.IO) {
                            favorites.forEach { favorite ->
                                try {
                                    val artistDetails = artistRepository.getArtistDetailsById(favorite.artistId)
                                    
                                    // Special handling for image URLs as mentioned in memory
                                    artistDetails?.let { artist ->
                                        // Log image URL for debugging
                                        Log.d(TAG, "Artist ${artist.name} (${artist.id}) image URL: ${artist.imageUrl}")
                                        
                                        // Special handling for Picasso family artists
                                        if (artist.name?.contains("Picasso", ignoreCase = true) == true) {
                                            Log.d(TAG, "Special handling for Picasso family artist")
                                        }
                                        
                                        // Check for different URL formats and edge cases
                                        if (!artist.imageUrl.isNullOrEmpty()) {
                                            val hasProperUrlScheme = artist.imageUrl?.startsWith("http") == true ||
                                                    artist.imageUrl?.startsWith("https") == true
                                            if (!hasProperUrlScheme) {
                                                Log.w(TAG, "Artist has invalid image URL format: ${artist.imageUrl}")
                                            }
                                        }
                                    }
                                    
                                    Log.d(TAG, "Detailed info fetched for ${favorite.artistId}")
                                } catch (e: Exception) {
                                    // Just log errors but continue
                                    Log.e(TAG, "Error fetching details for ${favorite.artistId}: ${e.message}")
                                }
                                // Short delay to avoid hammering the API
                                delay(100)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Failed to refresh favorites: ${response.code()}")
                }
            } else {
                Log.d(TAG, "Skipping favorites refresh - not logged in")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in refreshFavorites: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "AppLifecycleObserver"
    }
}
