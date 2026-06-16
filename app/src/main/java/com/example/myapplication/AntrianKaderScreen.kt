package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────────────────────
//  Warna Lokal
// ─────────────────────────────────────────────────────────────
private val KaderHeaderGreen    = Color(0xFF2E9B6E)
private val KaderBackgroundDark = Color(0xFF121212)
private val KaderSurfaceDark    = Color(0xFF2A2A2A)
private val KaderTextWhite      = Color(0xFFFFFFFF)
private val KaderTextGrey       = Color(0xFF888888)
private val KaderAvatarMint     = Color(0xFF98E6C8)
private val KaderNeonGreen      = Color(0xFF00C896)
private val KaderButtonGreen    = Color(0xFF2E9B6E)
private val KaderButtonRed      = Color(0xFFE74C3C)
private val KaderDipanggilBg    = Color(0xFF14634B)
private val KaderMenungguBorder = Color(0xFF00C896)

// ─────────────────────────────────────────────────────────────
//  Data class tampilan list antrian kader
// ─────────────────────────────────────────────────────────────
data class AntrianKaderItem(
    val id        : String,
    val nomor     : Int,
    val namaAnak  : String,
    val namaOrtu  : String,
    val usia      : String,
    val status    : Int,       // 1=menunggu, 0=dipanggil, 2=tdk hadir
    val waktuAmbil: String,
    val anakId    : String,
    val ortuId    : String
)

@Composable
fun AntrianKaderScreen(
    onNavigateBack     : () -> Unit = {},
    onNavigateToHome   : () -> Unit = {},
    onNavigateToPanggil: () -> Unit = {},
    onNavigateToLaporan: () -> Unit = {},
    onNavigateToLogout : () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var antrianItems     by remember { mutableStateOf<List<AntrianKaderItem>>(emptyList()) }
    var itemDipanggil    by remember { mutableStateOf<AntrianItemApi?>(null) }  // FIX: simpan item yang sedang dipanggil
    var nomorDipanggil   by remember { mutableStateOf("--") }
    var isLoading        by remember { mutableStateOf(false) }
    var posyanduNama     by remember { mutableStateOf("") }
    var posyanduId       by remember { mutableStateOf<String?>(null) }
    var jadwalInfo       by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val scaffoldState    = rememberScaffoldState()

    // ── Logout Dialog ───────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor  = Color(0xFF2A2A2A),
            title = {
                Text("Logout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("Yakin ingin keluar dari akun?", color = Color(0xFF888888), fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    try {
                        val db = DatabaseHelper(context).writableDatabase
                        db.execSQL("PRAGMA foreign_keys = OFF")
                        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_USERS}")
                        db.execSQL("PRAGMA foreign_keys = ON")
                        db.close()
                    } catch (e: Exception) {
                        android.util.Log.e("LOGOUT_ERROR", "Error clearing user table: ${e.message}")
                    }
                    onNavigateToLogout()
                }) {
                    Text("Logout", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = Color(0xFF888888))
                }
            }
        )
    }

    // ── Load posyandu_id kader dari DB lokal (sekali saat init) ────────────
    LaunchedEffect(Unit) {
        try {
            val db = DatabaseHelper(context).readableDatabase
            val cur = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_USERS_POSYANDU_ID}, p.${DatabaseHelper.COL_POSYANDU_NAMA} " +
                        "FROM ${DatabaseHelper.TABLE_USERS} u " +
                        "JOIN ${DatabaseHelper.TABLE_POSYANDU} p " +
                        "  ON u.${DatabaseHelper.COL_USERS_POSYANDU_ID} = p.${DatabaseHelper.COL_POSYANDU_ID} " +
                        "LIMIT 1", null
            )
            if (cur.moveToFirst()) {
                posyanduId   = cur.getString(0)
                posyanduNama = cur.getString(1) ?: ""
            }
            cur.close()
            db.close()
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "Load posyandu error: ${e.message}")
        }
    }

    // ── Load data antrian dari API ──────────────────────────────────────────
    suspend fun loadData() {
        isLoading = true
        try {
            // FIX: gunakan posyanduId agar kader hanya lihat antrian posyanduny sendiri
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context, posyanduId)

            if (antrianAktif == null) {
                antrianItems   = emptyList()
                itemDipanggil  = null
                nomorDipanggil = "--"
                isLoading      = false
                return
            }

            val itemsRaw = AntrianApiService.getAntrianItems(context, antrianAktif.id)
            android.util.Log.d("KADER", "Total items dari API: ${itemsRaw.size}")

            val db = DatabaseHelper(context).readableDatabase

            // FIX: Cari item yang sedang dipanggil (status=0) untuk tombol "Tidak Hadir"
            val currentDipanggil = itemsRaw.find { it.status == 0 }
            itemDipanggil = currentDipanggil

            // Nomor dipanggil: ambil dari header antrian (nomor_saat_ini)
            nomorDipanggil = if (antrianAktif.nomorSaatIni > 0)
                antrianAktif.nomorSaatIni.toString().padStart(3, '0')
            else "--"

            // Filter hanya yang menunggu (status=1), dedup per ortu+anak
            val menunggu = itemsRaw
                .filter { it.status == 1 }
                .groupBy { "${it.ortuId}__${it.anakId}" }
                .mapNotNull { (_, group) -> group.minByOrNull { it.nomor } }
                .sortedBy { it.nomor }

            android.util.Log.d("KADER", "Menunggu setelah dedup: ${menunggu.size}")

            val mappedItems = menunggu.map { item ->
                // Nama & umur anak
                val cursorAnak = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ANAK_NAMA}, ${DatabaseHelper.COL_ANAK_TGL_LAHIR} " +
                            "FROM ${DatabaseHelper.TABLE_ANAK} WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
                    arrayOf(item.anakId)
                )
                val namaAnak: String
                val umur: String
                if (cursorAnak.moveToFirst()) {
                    namaAnak = cursorAnak.getString(0) ?: "Anak"
                    umur     = hitungUmurKader(cursorAnak.getString(1) ?: "")
                } else {
                    namaAnak = "Anak"
                    umur     = ""
                }
                cursorAnak.close()

                // Nama ortu
                val cursorOrtu = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ORTU_NAMA} FROM ${DatabaseHelper.TABLE_ORTU} " +
                            "WHERE ${DatabaseHelper.COL_ORTU_ID} = ?",
                    arrayOf(item.ortuId)
                )
                val namaOrtu = if (cursorOrtu.moveToFirst()) cursorOrtu.getString(0) ?: "" else ""
                cursorOrtu.close()

                AntrianKaderItem(
                    id         = item.id,
                    nomor      = item.nomor,
                    namaAnak   = namaAnak,
                    namaOrtu   = namaOrtu,
                    usia       = umur,
                    status     = item.status,
                    waktuAmbil = item.waktuAmbil,
                    anakId     = item.anakId,
                    ortuId     = item.ortuId
                )
            }

            db.close()
            antrianItems = mappedItems

        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "Error load: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    // ── Panggil berikutnya ──────────────────────────────────────────────────
    suspend fun panggilBerikutnya() {
        try {
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context, posyanduId) ?: return
            val items        = AntrianApiService.getAntrianItems(context, antrianAktif.id)

            // FIX: cari item menunggu dengan nomor terkecil (item berikutnya)
            val berikutnya = items
                .filter { it.status == 1 }
                .groupBy { "${it.ortuId}__${it.anakId}" }
                .mapNotNull { (_, g) -> g.minByOrNull { it.nomor } }
                .minByOrNull { it.nomor }

            if (berikutnya != null) {
                val ok = AntrianApiService.panggilAntrian(context, berikutnya)
                if (ok) {
                    val nomorBaru = berikutnya.nomor.toString().padStart(3, '0')
                    nomorDipanggil = nomorBaru
                    itemDipanggil  = berikutnya.copy(status = 0)
                    scaffoldState.snackbarHostState.showSnackbar("Berhasil memanggil nomor $nomorBaru")
                    kotlinx.coroutines.delay(800)
                    loadData()
                } else {
                    scaffoldState.snackbarHostState.showSnackbar("Gagal memanggil antrian. Periksa koneksi.")
                }
            } else {
                scaffoldState.snackbarHostState.showSnackbar("Tidak ada antrian yang menunggu")
            }
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "Error panggil: ${e.message}", e)
        }
    }

    // ── Tidak hadir ─────────────────────────────────────────────────────────
    // FIX: gunakan itemDipanggil yang disimpan di state, bukan cari ulang
    //      berdasarkan nomorDipanggil yang mungkin belum match di list
    suspend fun tidakHadir() {
        try {
            val target = itemDipanggil
            if (target == null) {
                // Fallback: cari dari lokal berdasarkan nomorDipanggil
                val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context, posyanduId) ?: run {
                    scaffoldState.snackbarHostState.showSnackbar("Tidak ada antrian aktif")
                    return
                }
                val items    = AntrianApiService.getAntrianItems(context, antrianAktif.id)
                val nomorInt = nomorDipanggil.toIntOrNull() ?: run {
                    scaffoldState.snackbarHostState.showSnackbar("Belum ada yang dipanggil")
                    return
                }
                // Cari item berdasarkan nomor (status bisa 0 atau 1, belum tentu terupdate)
                val itemByNomor = items.find { it.nomor == nomorInt }
                if (itemByNomor != null) {
                    val ok = AntrianApiService.tidakHadir(context, itemByNomor)
                    if (ok) {
                        itemDipanggil = null
                        scaffoldState.snackbarHostState.showSnackbar("Nomor $nomorDipanggil ditandai tidak hadir")
                        kotlinx.coroutines.delay(500)
                        loadData()
                    }
                } else {
                    scaffoldState.snackbarHostState.showSnackbar("Item antrian tidak ditemukan")
                }
                return
            }

            val ok = AntrianApiService.tidakHadir(context, target)
            if (ok) {
                itemDipanggil = null
                scaffoldState.snackbarHostState.showSnackbar("Nomor $nomorDipanggil ditandai tidak hadir")
                kotlinx.coroutines.delay(500)
                loadData()
            } else {
                scaffoldState.snackbarHostState.showSnackbar("Gagal tandai tidak hadir")
            }
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "Error tidak hadir: ${e.message}", e)
        }
    }

    // FIX: tunggu posyanduId selesai diload sebelum loadData()
    LaunchedEffect(posyanduId) {
        if (posyanduId != null || posyanduNama.isNotEmpty()) {
            loadData()
        }
    }

    // Fallback: jika posyanduId tidak ada dalam 2 detik, tetap load
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        if (!isLoading && antrianItems.isEmpty()) {
            loadData()
        }
    }

    Scaffold(
        scaffoldState   = scaffoldState,
        backgroundColor = KaderBackgroundDark,
        bottomBar = {
            KaderBottomBar(
                onHomeClick    = onNavigateToHome,
                onPanggilClick = { /* already here */ },
                onLogoutClick  = { showLogoutDialog = true },
                currentTab     = "panggil"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            KaderHeader(
                posyanduNama = posyanduNama.ifBlank { "Posyandu" },
                jadwalInfo   = jadwalInfo.ifBlank { getCurrentDate() }
            )

            CardDipanggil(nomor = nomorDipanggil)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier              = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ButtonPanggilBerikutnya(
                    onClick  = { scope.launch { panggilBerikutnya() } },
                    modifier = Modifier.weight(1f)
                )
                // FIX: tombol "Tidak Hadir" hanya aktif jika ada item yang sedang dipanggil
                ButtonTidakHadir(
                    onClick  = { scope.launch { tidakHadir() } },
                    enabled  = itemDipanggil != null || nomorDipanggil != "--",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "Menunggu Giliran",
                    color      = KaderTextWhite,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!isLoading && antrianItems.isNotEmpty()) {
                    Text(
                        text     = "${antrianItems.size} orang",
                        color    = KaderNeonGreen,
                        fontSize = 13.sp
                    )
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = KaderNeonGreen)
                            Spacer(Modifier.height(8.dp))
                            Text("Memuat antrian...", color = KaderTextGrey, fontSize = 13.sp)
                        }
                    }
                }

                antrianItems.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text      = "Tidak ada antrian yang menunggu",
                                color     = KaderTextGrey,
                                fontSize  = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "Ortu perlu ambil antrian terlebih dahulu",
                                color     = KaderTextGrey.copy(alpha = 0.6f),
                                fontSize  = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        antrianItems.forEach { item ->
                            CardMenunggu(
                                nomor    = item.nomor.toString().padStart(3, '0'),
                                namaAnak = item.namaAnak,
                                namaOrtu = item.namaOrtu,
                                usia     = item.usia,
                                onClick  = {
                                    // Klik kartu = panggil item tersebut secara spesifik
                                    scope.launch {
                                        try {
                                            val aktif = AntrianApiService.getAntrianAktifHariIni(context, posyanduId)
                                            if (aktif != null) {
                                                val fullItems = AntrianApiService.getAntrianItems(context, aktif.id)
                                                val target = fullItems.find { it.id == item.id }
                                                if (target != null) {
                                                    val ok = AntrianApiService.panggilAntrian(context, target)
                                                    if (ok) {
                                                        val nomorBaru = item.nomor.toString().padStart(3, '0')
                                                        nomorDipanggil = nomorBaru
                                                        itemDipanggil  = target.copy(status = 0)
                                                        scaffoldState.snackbarHostState.showSnackbar("Memanggil nomor $nomorBaru")
                                                        kotlinx.coroutines.delay(800)
                                                        loadData()
                                                    } else {
                                                        scaffoldState.snackbarHostState.showSnackbar("Gagal memanggil. Coba lagi.")
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ANTRIAN_KADER", "Error panggil item: ${e.message}", e)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Tombol refresh manual
            TextButton(
                onClick  = { scope.launch { loadData() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("↻  Perbarui Antrian", color = KaderTextGrey, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  HELPER: Hitung Umur
// ─────────────────────────────────────────────────────────────
private fun hitungUmurKader(tanggalLahir: String): String {
    return try {
        val lahir = when {
            tanggalLahir.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                val p = tanggalLahir.split("/")
                LocalDate.of(p[2].toInt(), p[1].toInt(), p[0].toInt())
            }
            tanggalLahir.matches(Regex("""\d{4}-\d{2}-\d{2}""")) ->
                LocalDate.parse(tanggalLahir, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            else -> LocalDate.now()
        }
        val bulan = ChronoUnit.MONTHS.between(lahir, LocalDate.now()).toInt().coerceAtLeast(0)
        "$bulan Bulan"
    } catch (_: Exception) { "" }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN
// ════════════════════════════════════════════════════════════

@Composable
private fun KaderHeader(posyanduNama: String, jadwalInfo: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KaderHeaderGreen)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        Text(
            text       = "MyPosyandu",
            color      = KaderTextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        Text(
            text       = "Antrian $posyanduNama",
            color      = KaderTextWhite,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text     = jadwalInfo,
            color    = KaderTextWhite.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CardDipanggil(nomor: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(KaderDipanggilBg)
            .padding(16.dp)
    ) {
        Text("DIPANGGIL", color = KaderTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            text       = nomor,
            color      = KaderTextWhite,
            fontSize   = 56.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 56.sp
        )
    }
}

@Composable
private fun ButtonPanggilBerikutnya(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KaderButtonGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Panggil\nberikutnya",
            color      = KaderTextWhite,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ButtonTidakHadir(
    onClick : () -> Unit,
    enabled : Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Transparent)
            .border(
                2.dp,
                if (enabled) KaderButtonRed else KaderButtonRed.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Tidak Hadir",
            color      = if (enabled) KaderButtonRed else KaderButtonRed.copy(alpha = 0.3f),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
    }
}

@Composable
private fun CardMenunggu(
    nomor    : String,
    namaAnak : String,
    namaOrtu : String,
    usia     : String,
    onClick  : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KaderSurfaceDark)
            .border(1.dp, KaderMenungguBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = nomor,
            color      = KaderNeonGreen,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.width(48.dp)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(KaderAvatarMint)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = namaAnak,
                color      = KaderTextWhite,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (usia.isNotBlank() || namaOrtu.isNotBlank()) {
                Text(
                    text     = listOf(usia, namaOrtu).filter { it.isNotBlank() }.joinToString(" · "),
                    color    = KaderTextGrey,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun KaderBottomBar(
    onHomeClick   : () -> Unit,
    onPanggilClick: () -> Unit,
    onLogoutClick : () -> Unit = {},
    currentTab    : String = "panggil"
) {
    data class NavEntry(val icon: ImageVector, val label: String, val tab: String, val action: () -> Unit)
    val entries = listOf(
        NavEntry(Icons.Outlined.Home,               "Home",    "home",    onHomeClick),
        NavEntry(Icons.Outlined.ConfirmationNumber, "Antrian", "panggil", onPanggilClick),
        NavEntry(Icons.Outlined.PowerSettingsNew,   "Logout",  "logout",  onLogoutClick)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .navigationBarsPadding()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            entries.forEach { entry ->
                val isActive = currentTab == entry.tab
                val tint = if (isActive) KaderNeonGreen else Color.White.copy(alpha = 0.45f)

                Column(
                    modifier = Modifier
                        .clickable(onClick = entry.action)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = entry.icon,
                        contentDescription = entry.label,
                        tint               = tint,
                        modifier           = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text       = entry.label,
                        color      = tint,
                        fontSize   = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AntrianKaderScreenPreview() {
    AntrianKaderScreen()
}