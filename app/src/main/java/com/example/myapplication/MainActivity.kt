package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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

    NavHost(navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onNavigateToDashboard = { navController.navigate("dashboard") }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToPemeriksaan = { navController.navigate("pemeriksaan") },
                onNavigateToDataAnak = { navController.navigate("data_anak") },
                onNavigateToImunisasi = { navController.navigate("imunisasi") }
            )
        }
        composable("pemeriksaan") {
            PemeriksaanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHasil = { navController.navigate("hasil") }
            )
        }
        composable("hasil") {
            HasilScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("imunisasi") {
            ImunisasiScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("data_anak") {
            DataAnakScreen(
                onAnakClick = { anak -> navController.navigate("riwayat/${anak.nama}") },
                onTambahClick = { navController.navigate("daftar_anak_baru") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("daftar_anak_baru") {
            DaftarAnakBaruScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHubung = { navController.navigate("hubung_orang_tua") }
            )
        }
        composable("hubung_orang_tua") {
            HubungOrangTuaScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = {
                    // Navigate back to dashboard or success screen
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "riwayat/{namaAnak}",
            arguments = listOf(navArgument("namaAnak") { type = NavType.StringType })
        ) { backStack ->
            RiwayatScreen(
                namaAnak = backStack.arguments?.getString("namaAnak") ?: "",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPemeriksaan = { navController.navigate("pemeriksaan") },
                onNavigateToImunisasi = { navController.navigate("imunisasi") }
            )
        }
    }
}
