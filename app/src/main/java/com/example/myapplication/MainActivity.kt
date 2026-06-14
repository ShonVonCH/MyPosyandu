package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DatabaseHelper(applicationContext).readableDatabase.close()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController  = rememberNavController()
    val formViewModel  : FormDataViewModel = viewModel()
    val anakViewModel  : AnakViewModel     = viewModel()

    val isNavigating   = remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(backStackEntry) { isNavigating.value = false }

    fun safeNavigate(block: () -> Unit) {
        if (isNavigating.value) return
        isNavigating.value = true
        block()
    }

    NavHost(navController, startDestination = "login") {

        // ── LOGIN ────────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                formViewModel             = formViewModel,
                onNavigateToDashboard     = { _ ->
                    safeNavigate {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToDashboardOrtu = { username ->
                    safeNavigate {
                        navController.navigate("dashboard_orangtua/$username") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // ── DASHBOARD KADER ──────────────────────────────────────────────────
// ── DASHBOARD KADER ──────────────────────────────────────────────────
        composable("dashboard") {
            val context = LocalContext.current

            var totalAnak      by remember { mutableStateOf(0) }
            var anakHadir      by remember { mutableStateOf(0) }
            var hadirBulan     by remember { mutableStateOf(0) }
            var jadwalBulanIni by remember { mutableStateOf(0) }

            LaunchedEffect(Unit) {
                val dashRepo = DashboardRepository(context)
                totalAnak      = dashRepo.getTotalAnak()
                anakHadir      = dashRepo.getAnakHadirHariIni()
                hadirBulan     = dashRepo.getAnakHadirBulanIni()
                jadwalBulanIni = dashRepo.getJadwalBulanIni()

                Log.d("DASHBOARD_DEBUG", "totalAnak=$totalAnak, anakHadir=$anakHadir, hadirBulan=$hadirBulan, jadwalBulan=$jadwalBulanIni")
            }

            DashboardScreen(
                totalAnak            = totalAnak,
                anakHadir            = anakHadir,
                anakHadirBulan       = hadirBulan,
                jadwalBulanIni       = jadwalBulanIni,
                onNavigateToDataAnak = {
                    safeNavigate { navController.navigate("data_anak") { launchSingleTop = true } }
                },
                onNavigateToLaporan  = {
                    safeNavigate { navController.navigate("laporan") { launchSingleTop = true } }
                },
                onNavigateToPanggil  = {   // ← TAMBAH INI
                    safeNavigate { navController.navigate("antrian_kader") { launchSingleTop = true } }
                }
            )
        }

        // ── DASHBOARD ORANG TUA ──────────────────────────────────────────────
        composable(
            route     = "dashboard_orangtua/{username}",
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStack ->
            val username = backStack.arguments?.getString("username") ?: ""
            val context  = LocalContext.current

            var anakList by remember { mutableStateOf<List<AnakData>>(emptyList()) }

            LaunchedEffect(username) {
                val db = DatabaseHelper(context).readableDatabase
                val cursorOrtu = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ORTU_ID}, ${DatabaseHelper.COL_ORTU_NAMA} " +
                            "FROM ${DatabaseHelper.TABLE_ORTU} WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ?",
                    arrayOf(username)
                )
                var ortuId = ""; var namaOrtu = ""
                if (cursorOrtu.moveToFirst()) {
                    ortuId   = cursorOrtu.getString(0) ?: ""
                    namaOrtu = cursorOrtu.getString(1) ?: ""
                }
                cursorOrtu.close()

                if (ortuId.isNotBlank()) {
                    val cursorAnak = db.rawQuery(
                        "SELECT ${DatabaseHelper.COL_ANAK_ID}, ${DatabaseHelper.COL_ANAK_NAMA}, " +
                                "${DatabaseHelper.COL_ANAK_TGL_LAHIR}, ${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} " +
                                "FROM ${DatabaseHelper.TABLE_ANAK} WHERE ${DatabaseHelper.COL_ANAK_ORTU_ID} = ?",
                        arrayOf(ortuId)
                    )
                    val list = mutableListOf<AnakData>()
                    while (cursorAnak.moveToNext()) {
                        val id       = cursorAnak.getString(0) ?: continue
                        val nama     = cursorAnak.getString(1) ?: ""
                        val tglLahir = cursorAnak.getString(2) ?: ""
                        val gender   = cursorAnak.getString(3) ?: "-"
                        list.add(AnakData(
                            id           = id,
                            nama         = nama,
                            umurBulan    = hitungUmurBulan(tglLahir),
                            tanggal      = formatTanggalSingkat(tglLahir),
                            namaOrangTua = namaOrtu,
                            jenisKelamin = when (gender.trim().lowercase()) {
                                "laki-laki", "l" -> "L"
                                "perempuan", "p" -> "P"
                                else             -> gender.ifBlank { "-" }
                            }
                        ))
                    }
                    cursorAnak.close()
                    anakList = list
                }
                db.close()
            }

            DashboardOrangTuaScreen(
                username               = username,
                onNavigateToDetailAnak = { anakId ->
                    safeNavigate {
                        navController.navigate("detail_anak_ringkasan/$anakId") { launchSingleTop = true }
                    }
                },
                onNavigateToTicket = {
                    safeNavigate { navController.navigate("antrian_ortu") { launchSingleTop = true } }
                },
                onNavigateToFood = {
                    safeNavigate { navController.navigate("menu_sehat") { launchSingleTop = true } }
                }
            )
        }

        composable("laporan") {
            val username = formViewModel.loggedInOrangTuaUsername   // atau ambil dari kader

            LaporanScreen(
                onNavigateBack      = { navController.popBackStack() },
                onNavigateToHome    = {
                    safeNavigate {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToPanggil = {
                    safeNavigate {
                        navController.navigate("antrian_kader") { launchSingleTop = true }
                    }
                },
                onNavigateToUser    = {
                    // TODO: sambungkan ke halaman User/Profil jika sudah ada
                }
            )
        }

        // ── ANTRIAN KADER ────────────────────────────────────────────────────
        composable("antrian_kader") {
            AntrianKaderScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    safeNavigate {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToPanggil = {
                    // Sudah di halaman panggil
                },
                onNavigateToLaporan = {
                    safeNavigate {
                        navController.navigate("laporan") { launchSingleTop = true }
                    }
                }
            )
        }

        // ── ANTRIAN ORTU ─────────────────────────────────────────────────────
        composable("antrian_ortu") {
            val username = formViewModel.loggedInOrangTuaUsername
            val ortuId   = formViewModel.loggedInOrangTuaId
            AntrianOrtuScreen(
                userId             = ortuId,
                onNavigateToHome   = {
                    safeNavigate {
                        navController.navigate("dashboard_orangtua/$username") { launchSingleTop = true }
                    }
                },
                onNavigateToTicket = {
                    safeNavigate { navController.navigate("tiket_antrian") { launchSingleTop = true } }
                }
            )
        }

        // ── TIKET ANTRIAN ────────────────────────────────────────────────────
        composable("tiket_antrian") {
            val username = formViewModel.loggedInOrangTuaUsername
            TiketAntrianScreen(
                username            = username,
                onNavigateToHome    = {
                    safeNavigate {
                        navController.navigate("dashboard_orangtua/$username") { launchSingleTop = true }
                    }
                },
                onNavigateToTicket  = { },
                onNavigateToFood    = {
                    safeNavigate { navController.navigate("menu_sehat") { launchSingleTop = true } }
                },
                onNavigateToProfile = { }
            )
        }

        // ── MENU SEHAT ───────────────────────────────────────────────────────
        composable("menu_sehat") {
            val username = formViewModel.loggedInOrangTuaUsername
            MenuSehatScreen(
                onNavigateToAgeGroup = { ageGroup ->
                    val encodedAge = android.net.Uri.encode(ageGroup)
                    safeNavigate { navController.navigate("menu_list/$encodedAge") }
                },
                onNavigateToDetail = { id ->
                    safeNavigate { navController.navigate("menu_detail/$id") }
                },
                onNavigateToHome = {
                    navController.navigate("dashboard_orangtua/$username") {
                        popUpTo("dashboard_orangtua/$username") { inclusive = true }
                    }
                },
                onNavigateToTicket = {
                    navController.navigate("antrian_ortu") { launchSingleTop = true }
                },
                onNavigateToProfile = { /* TODO */ }
            )
        }

        composable(
            route = "menu_list/{ageGroup}",
            arguments = listOf(navArgument("ageGroup") { type = NavType.StringType })
        ) { backStack ->
            val username = formViewModel.loggedInOrangTuaUsername
            val ageGroup = backStack.arguments?.getString("ageGroup") ?: ""
            MenuListScreen(
                ageGroup = ageGroup,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    safeNavigate { navController.navigate("menu_detail/$id") }
                },
                onNavigateToHome = {
                    navController.navigate("dashboard_orangtua/$username") {
                        popUpTo("dashboard_orangtua/$username") { inclusive = true }
                    }
                },
                onNavigateToTicket = {
                    navController.navigate("antrian_ortu") { launchSingleTop = true }
                },
                onNavigateToProfile = { /* TODO */ }
            )
        }

        composable(
            route = "menu_detail/{menuId}",
            arguments = listOf(navArgument("menuId") { type = NavType.StringType })
        ) { backStack ->
            val username = formViewModel.loggedInOrangTuaUsername
            val menuId = backStack.arguments?.getString("menuId") ?: ""
            MenuDetailScreen(
                menuId = menuId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate("dashboard_orangtua/$username") {
                        popUpTo("dashboard_orangtua/$username") { inclusive = true }
                    }
                },
                onNavigateToTicket = {
                    navController.navigate("antrian_ortu") { launchSingleTop = true }
                },
                onNavigateToProfile = { /* TODO */ }
            )
        }

        // ── DETAIL ANAK (ORTU) ───────────────────────────────────────────────
        composable(
            route     = "detail_anak_ringkasan/{anakId}",
            arguments = listOf(navArgument("anakId") { type = NavType.StringType })
        ) { backStack ->
            val anakId = backStack.arguments?.getString("anakId") ?: ""

            DetailAnakRingkasanScreen(
                anakId                  = anakId,
                onNavigateBack          = { navController.popBackStack() },
                onNavigateToPemeriksaan = {
                    safeNavigate {
                        navController.navigate("pemeriksaan_ortu/$anakId") { launchSingleTop = true }
                    }
                },
                onNavigateToVaksin      = {
                    safeNavigate {
                        navController.navigate("vaksin_ortu/$anakId") { launchSingleTop = true }
                    }
                }
            )
        }

        // ── PEMERIKSAAN ORTU ─────────────────────────────────────────────────
        composable(
            route     = "pemeriksaan_ortu/{anakId}",
            arguments = listOf(navArgument("anakId") { type = NavType.StringType })
        ) { backStack ->
            val anakId = backStack.arguments?.getString("anakId") ?: ""

            PemeriksaanOrtuScreen(
                anakId                = anakId,
                onNavigateBack        = { navController.popBackStack() },
                onNavigateToRingkasan = {
                    navController.popBackStack("detail_anak_ringkasan/$anakId", inclusive = false)
                },
                onNavigateToVaksin    = {
                    safeNavigate {
                        navController.navigate("vaksin_ortu/$anakId") { launchSingleTop = true }
                    }
                }
            )
        }

        // ── VAKSIN ORTU ──────────────────────────────────────────────────────
        composable(
            route     = "vaksin_ortu/{anakId}",
            arguments = listOf(navArgument("anakId") { type = NavType.StringType })
        ) { backStack ->
            val anakId = backStack.arguments?.getString("anakId") ?: ""

            VaksinOrtuScreen(
                anakId                  = anakId,
                onNavigateBack          = { navController.popBackStack() },
                onNavigateToRingkasan   = {
                    navController.popBackStack("detail_anak_ringkasan/$anakId", inclusive = false)
                },
                onNavigateToPemeriksaan = {
                    navController.popBackStack("pemeriksaan_ortu/$anakId", inclusive = false)
                }
            )
        }

        // ── PEMERIKSAAN KADER ────────────────────────────────────────────────
        composable(
            route     = "pemeriksaan/{anakId}",
            arguments = listOf(navArgument("anakId") { type = NavType.StringType })
        ) { backStack ->
            val anakId  = backStack.arguments?.getString("anakId") ?: ""
            val context = LocalContext.current

            var anakData by remember { mutableStateOf<AnakData?>(null) }
            LaunchedEffect(anakId) {
                val db = DatabaseHelper(context).readableDatabase
                val cursor = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ANAK_NAMA}, ${DatabaseHelper.COL_ANAK_TGL_LAHIR}, " +
                            "${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} " +
                            "FROM ${DatabaseHelper.TABLE_ANAK} WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
                    arrayOf(anakId)
                )
                if (cursor.moveToFirst()) {
                    val nama     = cursor.getString(0) ?: ""
                    val tglLahir = cursor.getString(1) ?: ""
                    val gender   = cursor.getString(2) ?: "-"
                    anakData = AnakData(
                        id           = anakId,
                        nama         = nama,
                        umurBulan    = hitungUmurBulan(tglLahir),
                        jenisKelamin = when (gender.trim().lowercase()) {
                            "laki-laki", "l" -> "L"
                            "perempuan", "p" -> "P"
                            else             -> gender.ifBlank { "-" }
                        }
                    )
                }
                cursor.close()
                db.close()
            }

            PemeriksaanScreen(
                anakId         = anakId,
                kaderId        = formViewModel.loggedInKaderId,
                namaAnak       = anakData?.nama ?: "",
                umurBulan      = anakData?.umurBulan ?: 0,
                jenisKelamin   = anakData?.jenisKelamin ?: "",
                onNavigateBack = { navController.popBackStack() },
                onSimpan       = { bb, tb, analisis ->
                    anakViewModel.simpanHasilPemeriksaan(bb, tb, analisis)
                }
            )
        }

        // ── IMUNISASI ────────────────────────────────────────────────────────
        composable(
            route     = "imunisasi/{anakId}",
            arguments = listOf(navArgument("anakId") { type = NavType.StringType })
        ) { backStack ->
            val anakId    = backStack.arguments?.getString("anakId") ?: ""
            val vaksinMap by anakViewModel.vaksinDiberikan.collectAsState()
            val vaksinAnak = vaksinMap[anakId] ?: emptyMap()
            val context   = LocalContext.current

            var namaAnak  by remember { mutableStateOf("") }
            var namaOrtu  by remember { mutableStateOf("") }
            var tglLahir  by remember { mutableStateOf("") }
            var umurBulan by remember { mutableStateOf(0) }

            LaunchedEffect(anakId) {
                val db = DatabaseHelper(context).readableDatabase
                val cursor = db.rawQuery(
                    """
                    SELECT a.${DatabaseHelper.COL_ANAK_NAMA},
                           a.${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                           o.${DatabaseHelper.COL_ORTU_NAMA}
                    FROM   ${DatabaseHelper.TABLE_ANAK} a
                    LEFT JOIN ${DatabaseHelper.TABLE_ORTU} o
                           ON a.${DatabaseHelper.COL_ANAK_ORTU_ID} = o.${DatabaseHelper.COL_ORTU_ID}
                    WHERE  a.${DatabaseHelper.COL_ANAK_ID} = ?
                    """.trimIndent(),
                    arrayOf(anakId)
                )
                if (cursor.moveToFirst()) {
                    namaAnak  = cursor.getString(0) ?: ""
                    val tgl   = cursor.getString(1) ?: ""
                    namaOrtu  = cursor.getString(2) ?: ""
                    tglLahir  = tgl
                    umurBulan = hitungUmurBulan(tgl)
                }
                cursor.close()
                db.close()
            }

            ImunisasiScreen(
                namaAnak      = namaAnak,
                namaOrtu      = namaOrtu,
                nikAnak       = anakId,
                tglLahirAnak  = tglLahir,
                kaderId       = formViewModel.loggedInKaderId,
                umurBulan     = umurBulan,
                onNavigateBack = { navController.popBackStack() },
                vaksinAwal     = vaksinAnak,
                onSimpanVaksin = { namaVaksin, tanggal ->
                    anakViewModel.simpanVaksin(namaVaksin, tanggal)
                }
            )
        }

        // ── DATA ANAK ────────────────────────────────────────────────────────
        composable("data_anak") {
            DataAnakScreen(
                onAnakClick    = { anak ->
                    safeNavigate {
                        navController.navigate("riwayat/${anak.id}") { launchSingleTop = true }
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

        // ── DAFTAR ANAK BARU ─────────────────────────────────────────────────
        composable("daftar_anak_baru") {
            DaftarAnakBaruScreen(
                viewModel          = formViewModel,
                dariKonfirmasi     = false,
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToHubung = {
                    safeNavigate { navController.navigate("hubung_orang_tua") { launchSingleTop = true } }
                }
            )
        }

        // ── DAFTAR ANAK PERBAIKI ─────────────────────────────────────────────
        composable("daftar_anak_perbaiki") {
            DaftarAnakBaruScreen(
                viewModel          = formViewModel,
                dariKonfirmasi     = true,
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToHubung = {
                    safeNavigate { navController.navigate("hubung_orang_tua") { launchSingleTop = true } }
                }
            )
        }

        // ── HUBUNG ORANG TUA ─────────────────────────────────────────────────
        composable("hubung_orang_tua") {
            HubungOrangTuaScreen(
                viewModel      = formViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = {
                    safeNavigate { navController.navigate("konfirmasi_data") { launchSingleTop = true } }
                }
            )
        }

        // ── KONFIRMASI DATA ───────────────────────────────────────────────────
        composable("konfirmasi_data") {
            KonfirmasiDataScreen(
                viewModel         = formViewModel,
                onNavigateBack    = { navController.popBackStack() },
                onSimpanClicked   = {
                    safeNavigate {
                        navController.navigate("sukses_daftar") { launchSingleTop = true }
                    }
                },
                onPerbaikiClicked = {
                    safeNavigate { navController.navigate("daftar_anak_perbaiki") { launchSingleTop = true } }
                }
            )
        }

        // ── SUKSES DAFTAR ────────────────────────────────────────────────────
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

        // ── RIWAYAT ANAK ─────────────────────────────────────────────────────
        composable(
            route     = "riwayat/{anakId}",
            arguments = listOf(navArgument("anakId") { type = NavType.StringType })
        ) { backStack ->
            val anakId      = backStack.arguments?.getString("anakId") ?: ""
            val analisisMap by anakViewModel.hasilAnalisis.collectAsState()
            val vaksinMap   by anakViewModel.vaksinDiberikan.collectAsState()

            RiwayatScreen(
                anakId                  = anakId,
                hasilAnalisis           = analisisMap[anakId],
                vaksinDiberikan         = vaksinMap[anakId] ?: emptyMap(),
                onNavigateBack          = { navController.popBackStack() },
                onNavigateToPemeriksaan = {
                    safeNavigate {
                        navController.navigate("pemeriksaan/$anakId") { launchSingleTop = true }
                    }
                },
                onNavigateToImunisasi   = {
                    safeNavigate {
                        navController.navigate("imunisasi/$anakId") { launchSingleTop = true }
                    }
                }
            )
        }
    }
}