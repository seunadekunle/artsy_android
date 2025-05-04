package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.asssignment_4.R
import com.example.asssignment_4.viewmodel.HomeViewModel
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.model.Artwork
import com.example.asssignment_4.ui.theme.artsyBlue

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArtistDetailScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
    artistId: String,
    paddingValues: PaddingValues
) {
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(artistId) {
        // Call the ViewModel function to fetch data
        if (artistId.isNotEmpty()) { // Ensure ID is valid before fetching
            viewModel.fetchArtistDetailsAndArtworks(artistId)
        }
    }

    // Collect states from ViewModel
    val isLoading = viewModel.isDetailLoading.collectAsState().value
    val error = viewModel.detailError.collectAsState().value
    val artist = viewModel.artistDetail.collectAsState().value
    val artworks = viewModel.artistArtworks.collectAsState().value

    // Derive display values from the collected artist state
    val artistSubtitle = if (artist != null) {
        // Format subtitle only if artist data is available
        listOfNotNull(artist.nationality, artist.birthday).joinToString(", ")
    } else {
        " " // Show empty or loading state
    }
    val artistBio = artist?.biography ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val tabTitles = listOf("Details", "Artworks")
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
        } else {
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            // Artist Image from imageUrl or links.thumbnail
                            val imageUrl = artist?.imageUrl ?: artist?.links?.thumbnail?.href
                            if (imageUrl != null && imageUrl != "/assets/shared/missing_image.png") {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = artist?.name,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(bottom = 16.dp),
                                    contentScale = ContentScale.Fit,
                                    placeholder = painterResource(id = R.drawable.artsy_logo),
                                    error = painterResource(id = R.drawable.artsy_logo)
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        
                        item {
                            Text(
                                text = artist?.name ?: "Loading...",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = artistSubtitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = artistBio,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                1 -> { // Artworks Tab
                    // Replace placeholder with LazyColumn for artworks
                    if (artworks.isEmpty() && !isLoading) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No artworks found for this artist.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(artworks) { artwork ->
                                // Card layout for each artwork
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        // Determine best image URL
                                        val imageUrl = artwork.imageUrl
                                            ?: artwork.links?.thumbnail?.href
                                            ?: "/assets/images/artsy_logo.svg"
                                            
                                        // Skip displaying default placeholder URL
                                        if (imageUrl != "/assets/shared/missing_image.png") {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = artwork.title,
                                                modifier = Modifier
                                                    .height(200.dp)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(id = R.drawable.artsy_logo),
                                                error = painterResource(id = R.drawable.artsy_logo)
                                            )
                                        } else {
                                            // Display placeholder with colored background
                                            Box(
                                                modifier = Modifier
                                                    .height(200.dp)
                                                    .fillMaxWidth()
                                                    .background(artsyBlue.copy(alpha = 0.3f))
                                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
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
                                        
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            artwork.title ?: "Untitled",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        
                                        // Add date if available
                                        if (artwork.date != null && artwork.date.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = artwork.date,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }
                                        
                                        Spacer(Modifier.height(12.dp))
                                        // Add Button to view categories if needed (requires another API call)
                                        // Button(onClick = { /* TODO: Fetch and show categories */ }) {
                                        //     Text("View Categories")
                                        // }
                                        // Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatArtistInfo(artist: Artist): String {
    val details = mutableListOf<String?>()
    details.add(artist.nationality)
    val birth = artist.birthday
    val death = artist.deathday
    if (birth != null || death != null) {
        details.add("(${birth ?: "?"} - ${death ?: "?"})")
    }
    return details.filterNotNull().filter { it.isNotEmpty() }.joinToString(", ")
}


@Preview(showBackground = true)
@Composable
fun ArtistDetailScreenPreview() {
    ArtistDetailScreen(
        navController = rememberNavController(),
        artistId = "123",
        paddingValues = PaddingValues(0.dp) // Provide dummy padding
    )
}
