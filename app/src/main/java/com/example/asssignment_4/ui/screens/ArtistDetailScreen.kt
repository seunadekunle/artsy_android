package com.example.asssignment_4.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.asssignment_4.R
import com.example.asssignment_4.ui.theme.artsyBlue
import com.example.asssignment_4.ui.theme.artsyDarkBlue
import com.example.asssignment_4.viewmodel.HomeViewModel
import com.example.asssignment_4.viewmodel.AuthViewModel
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.Artwork
import com.example.asssignment_4.model.Gene
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.asssignment_4.ui.components.CategoryDialog
import com.example.asssignment_4.ui.components.SearchResultCard
import com.example.asssignment_4.ui.navigation.Screen

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArtistDetailScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    artistId: String,
    paddingValues: PaddingValues
) {

    // Effect for fetching artist data when screen is first displayed
    LaunchedEffect(artistId, authViewModel.currentUser.value) { // Keyed by currentUser to re-fetch on login/logout
        // Call the ViewModel function to fetch data
        if (artistId.isNotEmpty()) { // Ensure ID is valid before fetching
            viewModel.fetchArtistDetailsAndArtworks(artistId)
            // Fetch similar artists if user is logged in
            if (authViewModel.currentUser.value != null) {
                viewModel.fetchSimilarArtists(artistId = artistId, authToken = null) // Pass null, rely on cookie jar
            }
        }
    }
    
    // Effect that runs when leaving the screen to mark data for refresh
    DisposableEffect(artistId) {
        onDispose {
            // Mark that favorites data needs refresh when navigating away from this screen
            // This will ensure that any changes made here are reflected in other screens
            viewModel.markNeedsRefresh()
            Log.d("ArtistDetailScreen", "Marked data for refresh when leaving screen")
        }
    }

    // Clear state when the screen is disposed (leaves the composition)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearArtistDetails()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    // Collect states from ViewModel
    val isLoading = viewModel.isDetailLoading.collectAsState().value
    val error = viewModel.detailError.collectAsState().value ?: ""
    val artist = viewModel.artistDetail.collectAsState().value
    val artworks = viewModel.artistArtworks.collectAsState().value
    
    // Check login status and favorites
    val currentUser = authViewModel.currentUser.collectAsState().value
    val isLoggedIn = authViewModel.isLoggedIn.collectAsState().value
    val favouriteIds = viewModel.favouriteIds.collectAsState().value
    val isFavorite = artist?.id?.let { favouriteIds.contains(it) } ?: false
    val similarArtists = viewModel.similarArtists.collectAsState().value

    // Derive display values from the collected artist state
    val artistSubtitle = formatArtistInfo(artist = artist)
    val artistBio = artist?.biography ?: ""

    LaunchedEffect(artistId, selectedTab, isLoggedIn) { // Removed tokenValue from key
        if (isLoggedIn && selectedTab == 3) { // Similar tab
            viewModel.fetchSimilarArtists(artistId = artistId, authToken = null) // Pass null, rely on cookie jar
        }
    }

    // State for category dialog
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedArtworkForDialog by remember { mutableStateOf<Artwork?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = artist?.name ?: "Loading...",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (isLoggedIn && artist != null) {
                        IconButton(
                            onClick = {
                                if (isFavorite) {
                                    viewModel.removeFavorite(artist.id)
                                } else {
                                    viewModel.addFavorite(artist.id)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding -> // Padding provided by this Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from THIS Scaffold
                .background(artsyBlue.copy(alpha = 0.05f))
        ) {
            // Define tab data with icons and titles (conditionally include Similar tab if logged in)
            val tabs = if (isLoggedIn) {
                listOf(
                    TabItem("Details", Icons.Outlined.Info),
                    TabItem("Artworks", Icons.Outlined.AccountBox),
                    TabItem("Similar", Icons.Outlined.People)
                )
            } else {
                listOf(
                    TabItem("Details", Icons.Outlined.Info),
                    TabItem("Artworks", Icons.Outlined.AccountBox)
                )
            }
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.onSecondary,
                contentColor = MaterialTheme.colorScheme.secondary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                divider = { Divider(thickness = 1.dp, color = Color.Gray) }
            ) {
                tabs.forEachIndexed { idx, tab ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { 
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        text = { 
                            Text(
                                text = tab.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                letterSpacing = (-0.15).sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.padding(vertical = 0.dp)
                    )
                }
            }

            // Content area for the selected tab
            when (selectedTab) {
                0 -> { // Details tab
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.onSecondary
                    ) {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = artsyBlue)
                            }
                        } else if (error.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error: $error", color = Color.Red)
                            }
                        } else {
                            Column(modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Artist Name with big, bold font - matching the mockup
                                Text(
                                    text = artist?.name ?: "Artist Name",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                                
                                // Artist subtitle with special characters, increased font weight and larger size
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = artistSubtitle.replace(" • ", " \u2022 "),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                                
                                // Artist biography from the mockup
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = artistBio,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    lineHeight = 24.sp,
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }
                }
                1 -> { // Artworks tab
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = artsyBlue)
                        }
                    } else if (error.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: $error", color = Color.Red)
                        }
                    } else if (artworks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "No artworks",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(artworks) { artwork ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary)
                                ) {
                                    Column { // Revert: Let Column naturally wrap content
                                        // Display artwork image if available
                                        val imageUrlTemplate = artwork.links?.image?.href ?: artwork.links?.thumbnail?.href
                                        val imageUrl = imageUrlTemplate?.replace("{image_version}", "medium") // Replace placeholder

                                        if (imageUrl != null && imageUrl != "/assets/shared/missing_image.png") {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = artwork.title,
                                                contentScale = ContentScale.FillWidth, // Scale to fill width, adjusting height
                                                error = painterResource(id = R.drawable.artsy_logo),
                                                placeholder = painterResource(id = R.drawable.artsy_logo),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                            )
                                        } else {
                                            // Display placeholder with colored background
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f) // Use aspect ratio for placeholder consistency
                                                    .fillMaxWidth()
                                                    .background(artsyBlue.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.artsy_logo),
                                                    contentDescription = "No Image",
                                                    modifier = Modifier.size(80.dp),
                                                    tint = artsyBlue
                                                )
                                            }
                                        }
                                        
                                        // Artwork title - left-aligned as in the mockup
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp), // Add horizontal padding
                                            text = buildString {
                                                append(artwork.title ?: "Untitled")
                                                artwork.date?.let { date ->
                                                    if (date.isNotBlank()) {
                                                        append(", ")
                                                        append(date)
                                                    }
                                                }
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        Spacer(Modifier.height(4.dp))
                                        
                                        // "View categories" button as shown in mockup
                                        Spacer(Modifier.height(8.dp)) // Reduce space before button slightly
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Button(
                                                onClick = { // Update local state
                                                    selectedArtworkForDialog = artwork
                                                    showCategoryDialog = true
                                                },
                                                shape = RoundedCornerShape(50), // Make button rounded
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                            ) {
                                                Text(
                                                    "View Categories",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSecondary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp)) // Ensure padding at the bottom of the card
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Similar artists tab (only shown if logged in)
                    if (isLoggedIn) {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = artsyBlue)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (isLoading) {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                } else if (similarArtists.isEmpty()) {
                                    Text(
                                        text = "No similar artists found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(similarArtists) { artist ->
                                            SearchResultCard(
                                                artist = artist,
                                                isLoggedIn = true,
                                                isFavorite = favouriteIds.contains(artist.id),
                                                onFavoriteClick = {
                                                    if (favouriteIds.contains(artist.id)) {
                                                        viewModel.removeFavorite(artist.id)
                                                        viewModel.markNeedsRefresh()
                                                    } else {
                                                        viewModel.addFavorite(artist.id)
                                                        viewModel.markNeedsRefresh()
                                                    }
                                                },
                                                onClick = {
                                                    // Navigate to the artist detail screen
                                                    navController.navigate(
                                                        Screen.ArtistDetail.createRoute(artist.id)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback if tab is selected but user not logged in
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Please log in to see similar artists",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { /* Navigate to login */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = artsyBlue)
                                ) {
                                    Text("Log In")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
  
    // --- Category Dialog --- 
    // Use local state variables for conditional display
    if (showCategoryDialog && selectedArtworkForDialog != null) {
        Log.d("ArtistDetailScreen", "Showing dialog for artwork: ${selectedArtworkForDialog?.id}, title: ${selectedArtworkForDialog?.title}")
        
        // Track loading state for categories
        var isLoadingCategories by remember { mutableStateOf(true) }
        var categoriesLoaded by remember { mutableStateOf<List<Gene>>(emptyList()) }
        
        // Fetch categories for this artwork when dialog is shown
        LaunchedEffect(selectedArtworkForDialog?.id) {
            isLoadingCategories = true
            selectedArtworkForDialog?.id?.let { artworkId ->
                Log.d("ArtistDetailScreen", "LaunchedEffect triggering fetchArtworkCategories for: $artworkId")
                viewModel.fetchArtworkCategories(artworkId)
                
                // Get categories after fetching - slight delay to ensure fetch completes
                delay(500) // Short delay to allow fetch to complete
                val cats = viewModel.getArtworkCategories(artworkId)
                Log.d("ArtistDetailScreen", "Got ${cats.size} categories for artwork: $artworkId")
                categoriesLoaded = cats
                isLoadingCategories = false
            }
        }
        
        Log.d("ArtistDetailScreen", "Displaying dialog with loading state: $isLoadingCategories")
        
        CategoryDialog(
            categories = categoriesLoaded,
            isLoading = isLoadingCategories,
            onDismiss = { 
                Log.d("ArtistDetailScreen", "Dismissing category dialog")
                showCategoryDialog = false 
            }
        )
    }
}

fun formatArtistInfo(artist: Artist?): String {
    val details = mutableListOf<String?>()
    details.add(artist?.nationality)
    val birth = artist?.birthday
    val death = artist?.deathday
    if (birth != null || death != null) {
        details.add("${birth ?: "?"} - ${death ?: "?"}")
    }
    return details.filterNotNull().filter { it.isNotEmpty() }.joinToString(", ")
}


// Define a data class for tab items with icons
data class TabItem(val title: String, val icon: ImageVector)

@Preview(showBackground = true)
@Composable
fun ArtistDetailScreenPreview() {
    ArtistDetailScreen(
        navController = rememberNavController(),
        artistId = "123",
        paddingValues = PaddingValues(0.dp), // Provide dummy padding
        authViewModel = hiltViewModel() // Will be replaced with a preview mock in non-Hilt preview
    )
}
