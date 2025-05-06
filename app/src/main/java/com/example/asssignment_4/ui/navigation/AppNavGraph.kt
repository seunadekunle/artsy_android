package com.example.asssignment_4.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.asssignment_4.ui.screens.*
import com.example.asssignment_4.viewmodel.LoginViewModel

// Define the navigation routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: String): String {
            return "artist_detail/$artistId"
        }
    }

    object Login : Screen("login")
    object Register : Screen("register")
    object Favourites : Screen("favourites")
    // Add other screens if needed
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen(navController = navController)
        }

        composable(
            route = Screen.Search.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it })
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            }
        ) {
            SearchScreen(navController = navController)
        }

        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(
                navArgument("artistId") { type = NavType.StringType }
            ),
            enterTransition = {
                fadeIn()
            },
            exitTransition = {
                fadeOut()
            },
            popEnterTransition = {
                fadeIn()
            },
            popExitTransition = {
                fadeOut()
            }
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ArtistDetailScreen(
                navController = navController,
                artistId = artistId,
                paddingValues = paddingValues
            )
        }

        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            LoginScreen(
                navController = navController,
                onRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.Register.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            RegisterScreen(
                navController = navController,
                onLogin = { navController.navigate(Screen.Login.route) },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.Favourites.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            FavouritesScreen(navController = navController)
        }
    }
}
