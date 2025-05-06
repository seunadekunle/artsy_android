package com.example.asssignment_4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.asssignment_4.ui.navigation.AppNavGraph
import com.example.asssignment_4.ui.theme.YourAppTheme // Replace with your theme package
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // hook the system splash
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            YourAppTheme() {
                val navController = rememberNavController()
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavGraph(
                        navController = navController,
                        paddingValues = PaddingValues(0.dp)
                    )
                }
            }
        }
    }
}