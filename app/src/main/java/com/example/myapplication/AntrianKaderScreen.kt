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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
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
    val id       : String,
    val nomor    : Int,
    val namaAnak : String,
    val namaOrtu : String,
    val usia     : String,
    val status   : Int,       // 1=menunggu, 0=dipanggil, 2=tdk hadir
    val waktuAmbil: String,
    val anakId   : String,    // untuk deduplikasi
    val ortuId   : String     // untuk deduplikasi
)

@Composable
fun AntrianKaderScreen(
    onNavigateBack     : () -> Unit = {},
    onNavigateToHome   : () -> Unit = {},
    onNavigateToPanggil: () -> Unit = {},
    onNavigateToLaporan: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var antrianItems   by remember { mutableStateOf<List<AntrianKaderItem>>(emptyList()) }
    var nomorDipanggil by remember { mutableStateOf("--") }
    var isLoading      by remember { mutableStateOf(false) }
    var posyanduNama   by remember { mutableStateOf("") }
    var jadwalInfo     by remember { mutableStateOf("") }

    // ── Load data antrian dari API ──────────────────────────────────────
    suspend fun loadData() {
        isLoading = true
        try {
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context)

            if (antrianAktif == null) {
                antrianItems   = emptyList()
                nomorDipanggil = "--"
                isLoading      = false
                return
            }

            val items = AntrianApiService.getAntrianItems(context, antrianAktif.id)
            android.util.Log.d("KADER", "Total items dari API: ${items.size}")

            val db = DatabaseHelper(context).readableDatabase

            // Ambil info posyandu & jadwal dari DB
            val cursorKader = db.rawQuery(
                "SELECT p.${DatabaseHelper.COL_POSYANDU_NAMA} " +
                        "FROM ${DatabaseHelper.TABLE_USERS} u " +
                        "JOIN ${DatabaseHelper.TABLE_POSYANDU} p " +
                        "  ON u.${DatabaseHelper.COL_USERS_POSYANDU_ID} = p.${DatabaseHelper.COL_POSYANDU_ID} " +
                        "LIMIT 1", null
            )
            if (cursorKader.moveToFirst()) posyanduNama = cursorKader.getString(0) ?: ""
            cursorKader.close()

            // ── Filter: status 1 = menunggu ──────────────────────────────
            // Deduplikasi: kalau ortu_id + anak_id sama, ambil satu (nomor terkecil)
            val menunggu = items
                .filter { it.status == 1 }
                .groupBy { "${it.ortuId}__${it.anakId}" }
                .mapNotNull { (_, group) -> group.minByOrNull { it.nomor } }
                .sortedBy { it.nomor }

            android.util.Log.d("KADER", "Menunggu setelah dedup: ${menunggu.size}")

            val mappedItems = menunggu.map { item ->
                // Nama anak
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
                    id        = item.id,
                    nomor     = item.nomor,
                    namaAnak  = namaAnak,
                    namaOrtu  = namaOrtu,
                    usia      = umur,
                    status    = item.status,
                    waktuAmbil= item.waktuAmbil,
                    anakId    = item.anakId,
                    ortuId    = item.ortuId
                )
            }

            db.close()

            antrianItems   = mappedItems
            nomorDipanggil = if (antrianAktif.nomorSaatIni > 0)
                antrianAktif.nomorSaatIni.toString().padStart(3, '0')
            else "--"

        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "Error load: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    // ── Panggil berikutnya ──────────────────────────────────────────────
    suspend fun panggilBerikutnya() {
        try {
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context) ?: return
            val items        = AntrianApiService.getAntrianItems(context, antrianAktif.id)

            // Cari nomor terkecil yang masih menunggu (status 1), dedup ortu+anak
            val berikutnya = items
                .filter { it.status == 1 }
                .groupBy { "${it.ortuId}__${it.anakId}" }
                .mapNotNull { (_, g) -> g.minByOrNull { it.nomor } }
                .minByOrNull { it.nomor }

            if (berikutnya != null) {
                AntrianApiService.panggilAntrian(berikutnya.id)
                nomorDipanggil = berikutnya.nomor.toString().padStart(3, '0')
                loadData()
            }
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "Error panggil: ${e.message}", e)
        }
    }

    // ── Tidak hadir ─────────────────────────────────────────────────────
    suspend fun tidakHadir() {
        try {
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context) ?: return
            val items        = AntrianApiService.getAntrianItems(context, antrianAktif.id)
            val nomorInt     = nomorDipanggil.toIntOrNull() ?: return

            // Status 0 = sedang dipanggil
            val itemDipanggil = items.find { it.status == 0 && it.nomor == nomorInt }
            if (itemDipanggil != null) {
                AntrianApiService.tidakHadir(itemDipanggil.id, antrianAktif.id)
            }
            loadData()
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "Error tidak hadir: ${e.message}", e)
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        backgroundColor = KaderBackgroundDark,
        bottomBar = {
            KaderBottomBar(
                onHomeClick    = onNavigateToHome,
                onPanggilClick = onNavigateToPanggil,
                onLaporanClick = onNavigateToLaporan,
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
                ButtonTidakHadir(
                    onClick  = { scope.launch { tidakHadir() } },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier            = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment   = Alignment.CenterVertically,
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
                        modifier        = Modifier.fillMaxWidth().padding(32.dp),
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
                        modifier        = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = "Tidak ada antrian yang menunggu",
                            color     = KaderTextGrey,
                            fontSize  = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    Column(
                        modifier              = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        antrianItems.forEach { item ->
                            CardMenunggu(
                                nomor    = item.nomor.toString().padStart(3, '0'),
                                namaAnak = item.namaAnak,
                                namaOrtu = item.namaOrtu,
                                usia     = item.usia,
                                onClick  = {
                                    scope.launch {
                                        try {
                                            val aktif = AntrianApiService.getAntrianAktifHariIni(context)
                                            if (aktif != null) {
                                                AntrianApiService.panggilAntrian(item.id)
                                                nomorDipanggil = item.nomor.toString().padStart(3, '0')
                                                loadData()
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
            text      = "MyPosyandu",
            color     = KaderTextWhite,
            fontSize  = 16.sp,
            fontWeight= FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        Text(
            text       = "Antrian $posyanduNama",
            color      = KaderTextWhite,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text    = jadwalInfo,
            color   = KaderTextWhite.copy(alpha = 0.8f),
            fontSize= 14.sp
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
            .clip(RoundedCornerShape(10.dp))
            .background(KaderButtonGreen)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
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
private fun ButtonTidakHadir(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Transparent)
            .border(2.dp, KaderButtonRed, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Tidak Hadir",
            color      = KaderButtonRed,
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
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Nomor antrian
        Text(
            text       = nomor,
            color      = KaderNeonGreen,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.width(48.dp)
        )

        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(KaderAvatarMint)
        )

        // Info anak + ortu
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = namaAnak,
                color      = KaderTextWhite,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (usia.isNotBlank() || namaOrtu.isNotBlank()) {
                Text(
                    text    = listOf(usia, namaOrtu).filter { it.isNotBlank() }.joinToString(" · "),
                    color   = KaderTextGrey,
                    fontSize= 12.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  BOTTOM NAV — bentuk sama seperti ortu (Icon + label)
//  isi: Homepage, Panggil, Laporan
// ─────────────────────────────────────────────────────────────
@Composable
private fun KaderBottomBar(
    onHomeClick   : () -> Unit,
    onPanggilClick: () -> Unit,
    onLaporanClick: () -> Unit,
    currentTab    : String = "panggil"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .border(width = 1.dp, color = Color(0xFF3A3A3C), shape = RoundedCornerShape(0.dp))
            .navigationBarsPadding()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            KaderBottomItem(
                icon       = Icons.Outlined.Home,
                label      = "Homepage",
                isSelected = currentTab == "home",
                onClick    = onHomeClick
            )
            KaderBottomItem(
                icon       = Icons.Outlined.Notifications,
                label      = "Panggil",
                isSelected = currentTab == "panggil",
                onClick    = onPanggilClick
            )
            KaderBottomItem(
                icon       = Icons.Outlined.Person,
                label      = "Laporan",
                isSelected = currentTab == "laporan",
                onClick    = onLaporanClick
            )
        }
    }
}

@Composable
private fun KaderBottomItem(
    icon      : ImageVector,
    label     : String,
    isSelected: Boolean,
    onClick   : () -> Unit
) {
    val tint = if (isSelected) KaderNeonGreen else KaderTextGrey
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = tint,
            modifier           = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text       = label,
            color      = tint,
            fontSize   = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AntrianKaderScreenPreview() {
    AntrianKaderScreen()
}