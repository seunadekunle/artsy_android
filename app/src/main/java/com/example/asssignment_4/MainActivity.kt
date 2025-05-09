package com.example.asssignment_4

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.asssignment_4.ui.navigation.AppNavGraph
import com.example.asssignment_4.ui.theme.YourAppTheme
import com.example.asssignment_4.util.AppLifecycleObserver
import com.example.asssignment_4.viewmodel.AuthViewModel
import com.example.asssignment_4.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the application.
 * Uses AppLifecycleObserver to handle background/foreground transitions
 * and ensures data refreshes when app is force-closed and reopened.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "AppStatePrefs"
        private const val KEY_LAST_EXIT_TIME = "lastExitTimestamp"
        private const val FORCE_CLOSE_THRESHOLD = 1000 // ms to detect force close vs normal exit
    }
    
    // Inject the lifecycle observer that will handle data refreshing
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver
    
    // Get ViewModel instances
    private val authViewModel: AuthViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    
    // Shared preferences to track app state between sessions
    private lateinit var preferences: SharedPreferences
    
    // Flag to track if this is a cold start or resuming
    private val isColdStart = mutableStateOf(true)
    private val wasForceClose = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        
        // Initialize preferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // Check if this is a force-close restart
        checkForForceClose()
        
        // Apply splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Register the lifecycle observer
        lifecycle.addObserver(appLifecycleObserver)
        
        // Directly update ViewModels to force UI refresh on cold start
        if (isColdStart.value) {
            Log.d(TAG, "Cold start detected - directly updating ViewModels")
            refreshDataInViewModels(wasForceCloseDetected = wasForceClose.value)
            isColdStart.value = false
        }
        
        // Create a state to track initial loading for overlay
        val isInitialLoading = mutableStateOf(true)
        
        // Handle configuration changes (like theme changes)
        if (savedInstanceState != null) {
            Log.d(TAG, "Configuration change detected - preserving state")
            // No need to refresh data on configuration changes
            isInitialLoading.value = false
        }
        
        // Force data refresh kicks off initial loading
        if (isColdStart.value || wasForceClose.value) {
            Log.d(TAG, "Initial loading started")
            isInitialLoading.value = true
            
            // Launch a coroutine to reset loading state after a delay
            lifecycleScope.launch {
                // Give time for data to refresh
                delay(1500) // Short delay for better user experience
                isInitialLoading.value = false
                Log.d(TAG, "Initial loading complete")
            }
        }
        
        setContent {
            YourAppTheme {
                val navController = rememberNavController()
                
                // Create a global SnackbarHostState that will be shared across all screens
                val snackbarHostState = remember { SnackbarHostState() }
                
                // Collect global snackbar messages
                LaunchedEffect(Unit) {
                    HomeViewModel.globalSnackbarMessage.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }
                
                // Global loading state for tracking data refresh
                val isLoading = remember { isInitialLoading }
                
                // Use LaunchedEffect to ensure Compose updates with fresh data
                LaunchedEffect(key1 = "initial_data_load") {
                    Log.d(TAG, "LaunchedEffect triggered for initial data load")
                    if (wasForceClose.value) {
                        // Special handling for Picasso family artists as mentioned in memory
                        Log.d(TAG, "Ensuring special handling for problematic image URLs")
                        // Force UI update by refreshing favorites with detailed data
                        homeViewModel.fetchFavoritesDetails(true)
                    }
                    
                    // Reset loading state after data refresh
                    delay(800)  // Give time for UI to update
                    isLoading.value = false
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isLoading.value,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    
                    // Main app content with global Scaffold and SnackbarHost
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Surface(color = MaterialTheme.colorScheme.background) {
                            AppNavGraph(
                                navController = navController,
                                paddingValues = innerPadding,
                                snackbarHostState = snackbarHostState
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Refresh data directly in ViewModels to ensure UI updates
     */
    private fun refreshDataInViewModels(wasForceCloseDetected: Boolean) {
        lifecycleScope.launch {
            try {
                // First refresh auth state
                Log.d(TAG, "Directly refreshing authentication state")
                authViewModel.checkLoginStatus()
                
                // Then refresh favorites with force flag
                Log.d(TAG, "Directly refreshing favorites data")
                homeViewModel.refreshFavoriteStatuses(true)
                
                // Short delay to ensure auth check completes
                kotlinx.coroutines.delay(150)
                
                // Refresh detailed favorites information
                Log.d(TAG, "Directly refreshing detailed favorites data")
                homeViewModel.fetchFavoritesDetails(true)
                
                // Also do background image URL validation for Picasso family artists
                if (wasForceCloseDetected) {
                    Log.d(TAG, "Force close detected - performing extra validation for image URLs")
                    // Specific handling for image loading issues mentioned in memory
                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        homeViewModel.validateArtistImageUrls()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during ViewModel refresh: ${e.message}")
            }
        }
    }
    
    /**
     * Check if the app was force-closed by looking at the timestamps
     */
    private fun checkForForceClose() {
        val lastExitTime = preferences.getLong(KEY_LAST_EXIT_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        if (lastExitTime > 0) {
            val timeSinceExit = currentTime - lastExitTime
            
            // If the app closed less than the threshold time ago and we're
            // already in onCreate, this is likely a force close scenario
            if (timeSinceExit < FORCE_CLOSE_THRESHOLD) {
                Log.d(TAG, "Force close detected! Last exit was only ${timeSinceExit}ms ago")
                // Force a complete data refresh
                appLifecycleObserver.resetState()
                wasForceClose.value = true
            } else {
                Log.d(TAG, "Normal restart after ${timeSinceExit}ms")
                wasForceClose.value = false
            }
        } else {
            Log.d(TAG, "First launch detected")
            wasForceClose.value = false
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
        
        // When resuming, make sure to refresh data if needed
        if (!isColdStart.value) { // Skip if this is the initial launch
            lifecycleScope.launch {
                homeViewModel.refreshFavoriteStatuses(false)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Save the current time when the app is paused
        // This will help detect force close vs normal close
        preferences.edit().putLong(KEY_LAST_EXIT_TIME, System.currentTimeMillis()).apply()
        Log.d(TAG, "Activity paused, saved exit timestamp")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove the lifecycle observer to prevent memory leaks
        lifecycle.removeObserver(appLifecycleObserver)
        Log.d(TAG, "Activity destroyed")
    }
}