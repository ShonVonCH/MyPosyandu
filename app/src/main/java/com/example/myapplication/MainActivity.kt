package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
    val formViewModel : FormDataViewModel = viewModel()
    val anakViewModel : AnakViewModel     = viewModel()
    val selectedAnak  = remember { mutableStateOf<AnakData?>(null) }

    // ── Guard global: hanya boleh 1 navigasi dalam satu waktu ──────────
    // Setelah klik, isNavigating = true. Reset otomatis saat back stack berubah.
    val isNavigating = remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Setiap kali halaman berubah (back stack entry berubah), buka kunci
    androidx.compose.runtime.LaunchedEffect(backStackEntry) {
        isNavigating.value = false
    }

    // Helper: navigate hanya kalau sedang tidak dalam proses navigasi
    fun safeNavigate(block: () -> Unit) {
        if (isNavigating.value) return
        isNavigating.value = true
        block()
    }

    NavHost(navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onNavigateToDashboard = { role ->
                    safeNavigate {
                        val dest = if (role == "kader") "dashboard" else "dashboard_orangtua"
                        navController.navigate(dest) {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToDataAnak = { safeNavigate { navController.navigate("data_anak") { launchSingleTop = true } } },
                onNavigateToLaporan  = { }
            )
        }

        composable("dashboard_orangtua") {
            DashboardOrangTuaScreen(
                onNavigateToDetailAnak = { namaAnak ->
                    safeNavigate { navController.navigate("riwayat/$namaAnak") { launchSingleTop = true } }
                }
            )
        }

        composable("pemeriksaan") {
            PemeriksaanScreen(
                onNavigateBack    = { navController.popBackStack() },
                onNavigateToHasil = { },
                onSimpan          = { bb, tb -> anakViewModel.simpanHasilPemeriksaan(bb, tb) }
            )
        }

        composable("imunisasi") {
            ImunisasiScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("data_anak") {
            DataAnakScreen(
                viewModel      = formViewModel,
                anakViewModel  = anakViewModel,
                onAnakClick    = { anak ->
                    safeNavigate {
                        selectedAnak.value = anak
                        anakViewModel.setAnakAktif(anak.nama)
                        navController.navigate("riwayat/${anak.nama}") { launchSingleTop = true }
                    }
                },
                onTambahClick  = {
                    safeNavigate {
                        formViewModel.formAnak       = FormAnakData()
                        formViewModel.formOrangTua   = FormOrangTuaData()
                        formViewModel.dariKonfirmasi = false
                        navController.navigate("daftar_anak_baru") { launchSingleTop = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("daftar_anak_baru") {
            DaftarAnakBaruScreen(
                viewModel          = formViewModel,
                dariKonfirmasi     = false,
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToHubung = { safeNavigate { navController.navigate("hubung_orang_tua") { launchSingleTop = true } } }
            )
        }

        composable("daftar_anak_perbaiki") {
            DaftarAnakBaruScreen(
                viewModel          = formViewModel,
                dariKonfirmasi     = true,
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToHubung = { safeNavigate { navController.navigate("hubung_orang_tua") { launchSingleTop = true } } }
            )
        }

        composable("hubung_orang_tua") {
            HubungOrangTuaScreen(
                viewModel      = formViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = { safeNavigate { navController.navigate("konfirmasi_data") { launchSingleTop = true } } }
            )
        }

        composable("konfirmasi_data") {
            KonfirmasiDataScreen(
                viewModel         = formViewModel,
                onNavigateBack    = { navController.popBackStack() },
                onSimpanClicked   = {
                    safeNavigate {
                        formViewModel.simpanAnak()
                        navController.navigate("sukses_daftar") { launchSingleTop = true }
                    }
                },
                onPerbaikiClicked = {
                    safeNavigate { navController.navigate("daftar_anak_perbaiki") { launchSingleTop = true } }
                }
            )
        }

        composable("sukses_daftar") {
            SuksesDaftarScreen(
                viewModel        = formViewModel,
                onNavigateBack   = { navController.popBackStack() },
                onSelesaiClicked = {
                    safeNavigate {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route     = "riwayat/{namaAnak}",
            arguments = listOf(navArgument("namaAnak") { type = NavType.StringType })
        ) { backStack ->
            val namaAnak = backStack.arguments?.getString("namaAnak") ?: ""
            val bb by anakViewModel.beratBadanTerakhir.collectAsState()
            val tb by anakViewModel.tinggiBadanTerakhir.collectAsState()
            val anak = selectedAnak.value

            RiwayatScreen(
                namaAnak                = namaAnak,
                umurBulan               = anak?.umurBulan    ?: 0,
                jenisKelamin            = anak?.jenisKelamin ?: "-",
                beratBadanTerakhir      = bb,
                tinggiBadanTerakhir     = tb,
                onNavigateBack          = { navController.popBackStack() },
                onNavigateToPemeriksaan = { safeNavigate { navController.navigate("pemeriksaan") { launchSingleTop = true } } },
                onNavigateToImunisasi   = { safeNavigate { navController.navigate("imunisasi") { launchSingleTop = true } } }
            )
        }
    }
}