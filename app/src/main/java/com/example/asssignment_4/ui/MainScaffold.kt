package com.example.asssignment_4.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.asssignment_4.ui.navigation.AppNavGraph
import com.example.asssignment_4.ui.navigation.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScaffold(navController: NavHostController, isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist Search") },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        },
        content = { padding ->
            AppNavGraph(navController = navController)
        }
    )
}
