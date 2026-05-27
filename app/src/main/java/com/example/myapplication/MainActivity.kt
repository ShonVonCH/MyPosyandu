package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Menggunakan MaterialTheme (Material 2)
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        // Halaman Pertama
        composable("dashboard") {
            DashboardScreen(
                onNavigateToPemeriksaan = {
                    navController.navigate("pemeriksaan")
                },
                onNavigateToDataAnak = {
                    navController.navigate("data_anak")
                }
            )
        }
        
        // Halaman Kedua
        composable("pemeriksaan") {
            PemeriksaanScreen(
                onNavigateBack = {
                    navController.popBackStack() // Kembali ke halaman sebelumnya
                }
            )
        }

        // Halaman Ketiga
        composable("data_anak") {
            DataAnakScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = {
                    navController.navigate("detail_anak")
                }
            )
        }

        // Halaman Detail
        composable("detail_anak") {
            DetailAnakScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
