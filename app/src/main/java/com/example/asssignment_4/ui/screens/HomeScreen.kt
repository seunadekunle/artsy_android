package com.example.asssignment_4.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.asssignment_4.R
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.User
import com.example.asssignment_4.ui.navigation.Screen
import com.example.asssignment_4.ui.components.ArtistRow
import com.example.asssignment_4.ui.components.SearchResultCard
import com.example.asssignment_4.ui.theme.artsyBlue
import com.example.asssignment_4.ui.theme.lightArtsyBlue
import com.example.asssignment_4.util.AuthManagerEvent
import com.example.asssignment_4.util.AuthManagerEvent.SessionExpired
import com.example.asssignment_4.viewmodel.AuthViewModel
import com.example.asssignment_4.viewmodel.HomeViewModel
import com.example.asssignment_4.viewmodel.UserState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val isLoading by homeViewModel.isLoading.collectAsState()
    val needsRefresh by homeViewModel.needsRefresh.collectAsState()
    val favourites by homeViewModel.favourites.collectAsState()
    val favouriteIds by homeViewModel.favouriteIds.collectAsState()
    val detailedFavorites by homeViewModel.detailedFavorites.collectAsState()
    
    // Use the safe getUserState() method instead of direct state access
    val userState = authViewModel.getUserState()
    
    val currentUser = when (userState) {
        is UserState.Success -> userState.user
        else -> null
    }
    
    // Fix isLoggedIn check to better determine actual login state
    val isTokenPresent = authViewModel.isLoggedIn.collectAsState(initial = false).value
    // Only consider user logged in if we have both a token AND a valid user
    val isLoggedIn = isTokenPresent && currentUser != null
    
    // Debug login state
    LaunchedEffect(isTokenPresent, currentUser) {
        Log.d("HomeScreen", "Login state: isTokenPresent=$isTokenPresent, currentUser=${currentUser != null}, final isLoggedIn=$isLoggedIn")
    }
    // Preload favorite artist details as soon as HomeScreen is visible and user is logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            homeViewModel.fetchFavoritesDetails()
        }
    }
    
    val focusManager = LocalFocusManager.current

    // Only log userState in debug builds and when not in error state
    if (userState !is UserState.Error || !userState.toString().contains("Error checking login")) {
        LaunchedEffect(userState) {
            Log.d("HomeScreen", "Current userState in HomeScreen UI = $userState")
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect auth events for snackbar messages
    LaunchedEffect(Unit) {
        authViewModel.authEvent.collect { event ->
            when (event) {
                is AuthManagerEvent.Success -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AuthManagerEvent.Failure -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    // Refresh favorites when login state changes
    LaunchedEffect(isLoggedIn) {
        Log.d("HomeScreen", "User logged in, refreshing favorites")
        homeViewModel.getFavorites()
    }

    // This effect observes authentication events but only shows messages if relevant
    LaunchedEffect(authViewModel) {
        authViewModel.authEvent.collect { event ->
            // Only show auth-related messages if they're relevant to the current state
            when (event) {
                is AuthManagerEvent.Success -> {
                    // Only show success messages if we're logged in or it's a login-related message
                    if (isTokenPresent || event.message.contains("logged in", ignoreCase = true) ||
                        event.message.contains("registered", ignoreCase = true)) {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is AuthManagerEvent.Failure -> {
                    // Only show error messages if they're login-related
                    if (event.message.contains("login", ignoreCase = true) ||
                        event.message.contains("auth", ignoreCase = true) ||
                        event.message.contains("password", ignoreCase = true) ||
                        event.message.contains("user", ignoreCase = true)) {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is AuthManagerEvent.SessionExpired -> {
                    // Only show session expired message if we previously had a token
                    if (isTokenPresent) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Session expired",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Fast two-phase approach for immediate UI updates followed by background refresh
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Fast initial rendering of favorites")
        // Phase 1: Immediate UI update from cache
        homeViewModel.updateAllArtistsFavoriteStatus()
        
        // Phase 2: Background refresh
        homeViewModel.synchronizeFavorites()
    }
    
    // Optimized refresh when needsRefresh flag is set
    LaunchedEffect(needsRefresh) {
        if (needsRefresh) {
            Log.d("HomeScreen", "Fast refresh due to needsRefresh flag")
            homeViewModel.synchronizeFavorites()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Artist Search",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            if (isLoggedIn) {
                                when (userState) {
                                    is UserState.Loading -> {
                                        // Show loading indicator for profile
                                        IconButton(onClick = {}) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    is UserState.Success -> {
                                         val user = (userState as UserState.Success).user
                                        // Debug log the user avatar URL before loading
                                        Log.d("HomeScreen", "Loading user profile image, avatar URL: '${user.avatarUrl}'")
                                        
                                        IconButton(onClick = { showMenu = true }) {
                                            // Use default URL if avatar is missing
                                            val imageUrl = if (!user.avatarUrl.isNullOrBlank()) {
                                                user.avatarUrl
                                            } else {
                                                Log.w("HomeScreen", "Using fallback avatar image")
                                                "https://d32dm0rphc51dk.cloudfront.net/28zn9h0gSTJzP9RqXNIJhw/square.jpg"
                                            }
                                            
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(imageUrl)
                                                    .crossfade(true)
                                                    .placeholder(R.drawable.ic_person_placeholder)
                                                    .error(R.drawable.ic_person_placeholder)
                                                    .build(),
                                                contentDescription = "Profile",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                                onSuccess = { Log.d("HomeScreen", "Profile image loaded successfully") },
                                                onError = { Log.e("HomeScreen", "Error loading profile image: $it") }
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Log out", fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    scope.launch {
                                                        authViewModel.logoutUser()
                                                        showMenu = false
                                                        snackbarHostState.showSnackbar("Logged out successfully")
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete Account", fontWeight = FontWeight.Medium, color = Color.Red) },
                                                onClick = {
                                                    authViewModel.deleteAccount()
                                                    showMenu = false
                                                }
                                            )
                                        }
                                    }
                                    else -> {
                                        // Error or not logged in state, show login icon
                                        IconButton(onClick = { navController.navigate(Screen.Login.route) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Person,
                                                contentDescription = "Profile",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            } else {
                                IconButton(onClick = { navController.navigate(Screen.Login.route) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Date
            val today = remember { java.time.LocalDate.now() }
            val formattedDate = remember(today) {
                today.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"))
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xDF555555),
                fontWeight = FontWeight.W500,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isLoggedIn) 12.dp else 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .align(Alignment.Center)
                    )
                }
                if (!isLoggedIn) {
                    // Login button
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { navController.navigate(Screen.Login.route) },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                "Log in to see favorites",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                } else {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (favourites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                "No Favorites",
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W400
                            )
                        }

                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favourites, key = { it.id }) { favorite ->
                                // Find detailed artist info if available
                                val detailedArtist = detailedFavorites.find { it.first.artistId == favorite.artistId }?.second
                                
                                val artist = Artist(
                                    id = favorite.artistId,
                                    name = favorite.artistName,
                                    nationality = detailedArtist?.nationality,
                                    birthday = detailedArtist?.birthday,
                                    deathday = detailedArtist?.deathday,
                                    imageUrl = favorite.artistImage,
                                    biography = detailedArtist?.biography,
                                    isFavorite = true
                                )
                                
                                ArtistRow(
                                    artist = artist, 
                                    onClick = { 
                                        navController.navigate(Screen.ArtistDetail.createRoute(artist.id))
                                    },
                                    onFavoriteToggle = { artistId, isFavorite ->
                                        if (!isFavorite) {
                                            // Remove from favorites
                                            homeViewModel.removeFavorite(artistId)
                                            // Mark that we need a refresh
                                            homeViewModel.markNeedsRefresh()
                                        }
                                    },
                                    timestamp = favorite.createdAt // Pass the timestamp from the favorite object
                                )
                            }
                        }
                    }
                }
                // Powered by Artsy attribution
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Powered by Artsy",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .clickable {
                                val intent =
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://artsy.org"))
                                navController.context.startActivity(intent)
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorMessage(message: String = "An error occurred") {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}
