package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.filled.ArrowBack
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.viewmodel.HomeViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArtistDetailScreen(
    navController: NavHostController,
    artistId: String,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(artistId) {
        println("Fetching details for artist ID: $artistId") // Placeholder
    }

    val isLoading = false // Replace with viewModel.isLoading.collectAsState()
    val error = null // Replace with viewModel.error.collectAsState()
    val artist: Artist? = null // Replace with actual artist data from state
    val artistName = artist?.name ?: "Artist Name" // Placeholder
    val artistSubtitle = "${artist?.nationality ?: "Nationality"}, ${artist?.birthday ?: "Dates"}" // Placeholder
    val artistBio = artist?.biography ?: "Biography loading..." // Placeholder

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = artistName,
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

                    1 -> {
                        var showCategoryDialog by remember { mutableStateOf(false) }
                        val artworkUrl = "https://www.artic.edu/iiif/2/9e3c6d7e-2d2f-3b0a-4b6c-7e3e5d2c1b4a/full/843,/0/default.jpg" // Placeholder
                        val artworkTitle = "Artwork Title" // Placeholder

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(artworkUrl),
                                contentDescription = artworkTitle,
                                modifier = Modifier
                                    .height(250.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(artworkTitle, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { showCategoryDialog = true }) {
                                Text("View categories")
                            }
                        }
                        if (showCategoryDialog) {
                            AlertDialog(
                                onDismissRequest = { showCategoryDialog = false },
                                confirmButton = {
                                    Button(onClick = { showCategoryDialog = false }) {
                                        Text("Close")
                                    }
                                },
                                title = { Text("Categories") },
                                text = { Text("Placeholder categories...") } // Placeholder text
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArtistDetailScreenPreview() {
    ArtistDetailScreen(
        navController = rememberNavController(),
        artistId = "123"
    )
}
