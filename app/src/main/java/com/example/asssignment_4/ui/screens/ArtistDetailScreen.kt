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
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArtistDetailScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    artistId: String,
    paddingValues: PaddingValues
) {

    LaunchedEffect(artistId) {
        // Call the ViewModel function to fetch data
        if (artistId.isNotEmpty()) { // Ensure ID is valid before fetching
            viewModel.fetchArtistDetailsAndArtworks(artistId)
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
    
    // Check login status
    val currentUser = authViewModel.currentUser.collectAsState().value
    val isLoggedIn = currentUser != null

    // Derive display values from the collected artist state
    val artistSubtitle = formatArtistInfo(artist = artist)
    val artistBio = artist?.biography ?: ""

    // State for category dialog
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedArtworkForDialog by remember { mutableStateOf<Artwork?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artist?.name ?: "Loading...") }, 
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add favorite star icon button (only if logged in)
                    if (isLoggedIn) {
                        IconButton(onClick = { /* Toggle favorite */ }) {
                            Icon(
                                imageVector = Icons.Filled.StarBorder,
                                contentDescription = "Favorite",
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
                                                fontSize = 18.sp
                                            ),
                                            textAlign = TextAlign.Center,
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
                                Text(
                                    text = "Similar artists feature coming soon",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
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


@Composable
fun CategoryDialog(
    categories: List<Gene>,
    isLoading: Boolean = false,
    onDismiss: () -> Unit
) {
    Log.d("ArtistDetailScreen", "CategoryDialog: isLoading=$isLoading, categories=${categories.size}")

    // State for LazyRow
    val lazyListState = rememberLazyListState()
    // Coroutine scope for scrolling
    val coroutineScope = rememberCoroutineScope()
    // Track current visible index
    var currentIndex by remember { mutableStateOf(0) }

    // Main Dialog shell
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .fillMaxWidth(1f)
                .heightIn(min = 550.dp, max = 595.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 16.dp),
                    fontWeight = FontWeight(499)
                )
                Spacer(Modifier.height(16.dp))

                // Content Area with LazyRow
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Loading",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        categories.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No categories available", color = Color.Gray)
                            }
                        }
                        else -> {
                            val showPrevious = currentIndex > 0
                            val showNext = currentIndex < categories.size - 1

                            val navigateToPrevious = {
                                if (showPrevious) {
                                    currentIndex--
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(currentIndex)
                                    }
                                }
                            }
                            val navigateToNext = {
                                if (showNext) {
                                    currentIndex++
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(currentIndex)
                                    }
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyRow(
                                    state = lazyListState,
                                    modifier = Modifier
                                        .width(480.dp)
                                        .padding(horizontal = 25.dp),
                                    contentPadding = PaddingValues(
                                        start = 15.dp,
                                        end = 12.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(categories.size) { index ->
                                        val gene = categories[index]
                                        Card(
                                            modifier = Modifier
                                                .fillParentMaxHeight(0.95f)
                                                .width(240.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                AsyncImage(
                                                    model = gene.links?.thumbnail?.href,
                                                    contentDescription = gene.name,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(0.35f),
                                                    contentScale = ContentScale.Crop,
                                                    placeholder = painterResource(id = R.drawable.artsy_logo),
                                                    error = painterResource(id = R.drawable.artsy_logo)
                                                )

                                                Text(
                                                    text = gene.displayName ?: gene.name,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 19.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp)
                                                )

                                                Column(
                                                    modifier = Modifier
                                                        .weight(0.65f)
                                                        .fillMaxWidth()
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                    horizontalAlignment = Alignment.Start
                                                ) {
                                                    Text(
                                                        text = gene.description ?: "No description available.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight(499),
                                                        textAlign = TextAlign.Start
                                                    )
                                                    Spacer(Modifier.height(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                        onClick = navigateToPrevious,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 7.dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous Category",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                 IconButton(
                                        onClick = navigateToNext,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 10.dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next Category",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                            }
                        }
                    }
                }

                // Footer with Close Button
                Spacer(Modifier.height(5.dp))

                Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Close", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    }
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
