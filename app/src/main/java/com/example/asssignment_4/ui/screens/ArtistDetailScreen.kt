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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.rememberNavController

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArtistDetailScreen(navController: NavHostController, artistId: String) {
    // Mock loading state
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var fav by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    // Mock artist data
    val artistName = "Pablo Picasso"
    val artistSubtitle = "Spanish, 1881 - 1973"
    val artistBio = "Born in Málaga, Spain, in 1881, Pablo Picasso showed an early passion for drawing... (truncated)"
    val artworkUrl = "https://www.artic.edu/iiif/2/9e3c6d7e-2d2f-3b0a-4b6c-7e3e5d2c1b4a/full/843,/0/default.jpg"
    val artworkTitle = "View of Vétheuil, 1880"
    val similarArtists = listOf(
        "Georges Braque" to "https://example.com/braque.jpg",
        "Juan Gris" to "https://example.com/gris.jpg",
        "Francisco Bores" to "https://example.com/bores.jpg"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fav = !fav }) {
                        Icon(
                            imageVector = if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (fav) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val tabTitles = listOf("Details", "Artworks", "Similar")
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Loading...", modifier = Modifier.padding(top = 16.dp))
                    }
                }
                selectedTab == 0 -> {
                    // Details Tab
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        item {
                            Text(artistName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(artistSubtitle, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                            Text(
                                text = artistBio,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }
                selectedTab == 1 -> {
                    // Artworks Tab
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
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
                                title = {
                                    Text("Categories")
                                },
                                text = {
                                    Column {
                                        Text("1860–1969")
                                        Text("All art, design, decorative art, and architecture created from roughly 1860 to 1969.")
                                    }
                                }
                            )
                        }
                    }
                }
                selectedTab == 2 -> {
                    // Similar Tab
                    if (similarArtists.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No similar artists found.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(similarArtists.size) { idx ->
                                val (name, imgUrl) = similarArtists[idx]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF5F5F5))
                                        .clickable { /* Navigate to artist detail */ }
                                ) {
                                    Image(
                                        painter = painterResource(android.R.drawable.ic_menu_gallery), // Replace with Coil in real app
                                        contentDescription = name,
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
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
