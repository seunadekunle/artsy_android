package com.example.asssignment_4.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.asssignment_4.ui.navigation.AppNavGraph
import com.example.asssignment_4.ui.navigation.Screen
import com.example.asssignment_4.ui.screens.SearchResultCard
import com.example.asssignment_4.ui.theme.YourAppTheme
import com.example.asssignment_4.ui.theme.artsyBlue
import com.example.asssignment_4.ui.theme.artsyLightBlue
import com.example.asssignment_4.viewmodel.AuthViewModel
import com.example.asssignment_4.viewmodel.HomeViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScaffold(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val isLoggedIn = currentUser != null

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isSearchScreen = currentRoute == Screen.Search.route
    val homeViewModel: HomeViewModel = hiltViewModel()
    var searchTerm by remember { mutableStateOf("") }

    // Observe search results
    val searchResults by homeViewModel.searchResults.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val error by homeViewModel.error.collectAsState()

    // Trigger search when text changes
    LaunchedEffect(searchTerm) {
        homeViewModel.setSearchTerm(searchTerm)
    }

    // Animation for screen transitions
    val enterTransition = remember {
        slideInHorizontally(initialOffsetX = { it }) + fadeIn()
    }
    val exitTransition = remember {
        slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    }

    Scaffold(
        topBar = {
            if (isSearchScreen) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                modifier = Modifier.padding(end = 8.dp),
                                tint = Color.Gray
                            )
                            TextField(
                                value = searchTerm,
                                onValueChange = { searchTerm = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search for artists...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = artsyBlue,
                    )
                )
            } else {
                TopAppBar(
                    modifier = Modifier.background(artsyLightBlue),
                    title = { Text("Artist Search") },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Outlined.Person, contentDescription = "Account")
                            }
                            if (isLoggedIn) {
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Log out") },
                                        onClick = {
                                            authViewModel.logoutUser()
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete account") },
                                        onClick = { showMenu = false }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = artsyBlue,
                    )
                )
            }
        },
        containerColor = artsyLightBlue,
        content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (isSearchScreen) {
                    Column {
                        // Search results
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            }
                        } else if (error != null) {
                            Text(
                                text = error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else if (searchResults.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                items(searchResults) { artist ->
                                    SearchResultCard(
                                        artist = artist,
                                        onClick = {
                                            navController.navigate(
                                                Screen.ArtistDetail.createRoute(artist.id)
                                            )
                                        }
                                    )
                                }
                            }
                        } else if (searchTerm.isNotBlank()) {
                            Text(
                                text = "No results found for '$searchTerm'",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    AppNavGraph(navController = navController)
                }
            }
        }
    )
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldPreview() {
    YourAppTheme(darkTheme = false) {
        MainScaffold(
            navController = rememberNavController(),
            isDarkTheme = false,
            onToggleTheme = {}
        )
    }
}
