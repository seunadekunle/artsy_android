package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(navController: NavHostController) {
    // Mock data
    val isLoggedIn = true
    val favorites = listOf("Claude Monet", "Pablo Picasso")
    val searchResults = listOf(
        Triple("Pablo Picasso", "https://uploads0.wikiart.org/images/pablo-picasso.jpg", true),
        Triple("PIC (Partners in Crime)", "https://www.publicdomainpictures.net/pictures/320000/velka/background-image.png", false),
        Triple("Francis Picabia", "https://uploads0.wikiart.org/images/francis-picabia.jpg", false)
    )
    var search by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist Search") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Avatar")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Log out") }, onClick = { showMenu = false })
                            DropdownMenuItem(text = { Text("Delete account") }, onClick = { showMenu = false })
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Date
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
            )
            // Favorites Section
            if (isLoggedIn) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 2.dp)
                )
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        favorites.forEachIndexed { idx, fav ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fav, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Text("10 seconds ago", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            if (idx < favorites.size - 1) Divider()
                        }
                        Text(
                            "Powered by Artsy",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)
                        )
                    }
                }
            }
            // Search Bar
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("Search artists...") },
                singleLine = true
            )
            // Search Results
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 60.dp)
            ) {
                items(searchResults.size) { idx ->
                    val (name, imgUrl, isFav) = searchResults[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { /* Navigate to artist detail */ },
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Image(
                                painter = painterResource(android.R.drawable.ic_menu_gallery), // Replace with Coil
                                contentDescription = name,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { /* Toggle favorite */ }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) Color(0xFFFFC107) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

