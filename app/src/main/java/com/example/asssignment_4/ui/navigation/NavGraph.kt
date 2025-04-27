package com.example.asssignment_4.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.example.asssignment_4.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object ArtistDetail : Screen("artistDetail/{artistId}") {
        fun createRoute(artistId: String) = "artistDetail/$artistId"
    }
    object Login : Screen("login")
    object Register : Screen("register")
    object Favourites : Screen("favourites")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.ArtistDetail.route) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ArtistDetailScreen(navController, artistId)
        }
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                onRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { navController.navigate(Screen.Home.route) { 
                // Clear the back stack up to Login screen
                popUpTo(Screen.Login.route) { inclusive = true } 
            } }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                onLogin = { navController.navigate(Screen.Login.route) },
                onRegisterSuccess = { navController.navigate(Screen.Home.route) { 
                // Clear the back stack up to Register screen
                popUpTo(Screen.Register.route) { inclusive = true } 
            } }
            )
        }
        composable(Screen.Favourites.route) { FavouritesScreen(navController) }
    }
}
