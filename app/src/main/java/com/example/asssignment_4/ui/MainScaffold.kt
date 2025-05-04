package com.example.asssignment_4.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextDecoration

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
    val isLoginScreen = currentRoute == Screen.Login.route
    val isSignupScreen = currentRoute == Screen.Register.route
    val homeViewModel: HomeViewModel = hiltViewModel()

    // Check if the current route is the ArtistDetail screen
    // Note: Comparing against the route *template* from Screen.ArtistDetail.route
    val isDetailScreen = currentRoute == Screen.ArtistDetail.route 
    val detailArtistName = if (isDetailScreen) {
        // Extract artist name from arguments
        navBackStackEntry?.arguments?.getString("artistName")?.replace("%20", " ") ?: "Artist Detail"
    } else {
        null
    }

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
            // Conditionally display TopAppBar based on route
            when {
                isSearchScreen -> {
                    // Search TopAppBar (existing logic)
                    TopAppBar(
                        title = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = LocalContentColor.current.copy(alpha = 0.75f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(modifier = Modifier.weight(1f)) {
                                        BasicTextField(
                                            value = searchTerm,
                                            onValueChange = {
                                                searchTerm = it
                                                if (it.isNotEmpty()) {
                                                    homeViewModel.setSearchTerm(it)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 6.dp),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.W500,
                                                textDecoration = TextDecoration.Underline,
                                                color = LocalContentColor.current
                                            ),
                                            cursorBrush = SolidColor(LocalContentColor.current)
                                        )
                                        if (searchTerm.isEmpty()) {
                                            Text(
                                                text = "Search artists…",
                                                fontSize = 22.sp,
                                                color = LocalContentColor.current.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }
                                    }

                                    IconButton(onClick = {
                                        homeViewModel.setSearchTerm("")
                                        searchTerm = ""
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = LocalContentColor.current.copy(alpha = 1f)
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
                isDetailScreen && detailArtistName != null -> {
                    // Artist Detail TopAppBar
                    TopAppBar(
                        title = { Text(detailArtistName) }, // Use extracted name
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = artsyBlue, // Keep consistent color
                        )
                    )
                }
                      currentRoute == Screen.Home.route -> {
                    TopAppBar(
                        title = {
                            Text(
                                "Artist Search",
                                style = MaterialTheme.typography.titleLarge
                                    .copy(fontWeight = FontWeight.Medium)
                            ) },

                        actions = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Add Search Icon Button HERE
                                IconButton(onClick = { navController.navigate(Screen.Search.route)}) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search")
                                }
                                // Profile Icon/Menu Button (existing)
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = if (isLoggedIn) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    if (isLoggedIn) {
                                        DropdownMenuItem(text = { Text("Favorites") }, onClick = { 
                                            navController.navigate(Screen.Favourites.route)
                                            showMenu = false
                                         })
                                        DropdownMenuItem(text = { Text("Logout") }, onClick = { authViewModel.logoutUser() ; showMenu = false })
                                    } else {
                                        DropdownMenuItem(text = { Text("Login") }, onClick = { navController.navigate(Screen.Login.route); showMenu = false })
                                        DropdownMenuItem(text = { Text("Register") }, onClick = { navController.navigate(Screen.Register.route) ; showMenu = false })
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = artsyBlue,
                        )
                    )
                }
                else -> {
                    // Default TopAppBar (for Home, Login, etc.)
                    TopAppBar(
                        title = { Text("Artsy App") },
                        actions = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = if (isLoggedIn) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    if (isLoggedIn) {
                                        DropdownMenuItem(text = { Text("Favorites") }, onClick = { 
                                            navController.navigate(Screen.Favourites.route)
                                            showMenu = false
                                         })
                                        DropdownMenuItem(text = { Text("Logout") }, onClick = { authViewModel.logoutUser() ; showMenu = false })
                                    } else {
                                        DropdownMenuItem(text = { Text("Login") }, onClick = { navController.navigate(Screen.Login.route); showMenu = false })
                                        DropdownMenuItem(text = { Text("Register") }, onClick = { navController.navigate(Screen.Register.route) ; showMenu = false })
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (currentRoute != Screen.Home.route) { // Show back arrow if not on Home
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            } else {
                                IconButton(onClick = { navController.navigate(Screen.Search.route)}) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = artsyBlue,
                        )
                    )
                }
            }
        },
        containerColor = artsyLightBlue,
        content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (isSearchScreen) {
                    Column {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            }
                        }
                        else if (searchResults.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    AppNavGraph(navController = navController, paddingValues = innerPadding)
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
