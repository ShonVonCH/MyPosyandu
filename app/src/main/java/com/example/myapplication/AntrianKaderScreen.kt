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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
private val KaderHeaderGreen = Color(0xFF2E9B6E)
private val KaderBackgroundDark = Color(0xFF121212)
private val KaderSurfaceDark = Color(0xFF2A2A2A)
private val KaderSurfaceDarkBorder = Color(0xFF444444)
private val KaderTextWhite = Color(0xFFFFFFFF)
private val KaderTextGrey = Color(0xFF888888)
private val KaderAvatarMint = Color(0xFF98E6C8)
private val KaderNeonGreen = Color(0xFF00C896)
private val KaderButtonGreen = Color(0xFF2E9B6E)
private val KaderButtonRed = Color(0xFFE74C3C)
private val KaderDipanggilBg = Color(0xFF14634B)
private val KaderMenungguBorder = Color(0xFF00C896)

// Data class untuk item antrian di kader
data class AntrianKaderItem(
    val id: String,
    val nomor: Int,
    val namaAnak: String,
    val namaOrtu: String,
    val usia: String,
    val status: Int,
    val waktuAmbil: String
)

@Composable
fun AntrianKaderScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToPanggil: () -> Unit = {},
    onNavigateToLaporan: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var antrianItems by remember { mutableStateOf<List<AntrianKaderItem>>(emptyList()) }
    var nomorDipanggil by remember { mutableStateOf("014") }
    var totalMenunggu by remember { mutableStateOf(3) }
    var isLoading by remember { mutableStateOf(false) }
    var posyanduNama by remember { mutableStateOf("Posyandu Melati") }
    var jadwalInfo by remember { mutableStateOf("Senin 2 Juni 2025 - 08:00 - 11:00") }

    // Fungsi load data - didefinisikan sebagai lambda
    val loadAntrianData = {
        scope.launch {
            isLoading = true
            try {
                val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context)

                if (antrianAktif != null) {
                    val items = AntrianApiService.getAntrianItems(context, antrianAktif.id)

                    val db = DatabaseHelper(context).readableDatabase
                    val mappedItems = items.filter { it.status == 1 }.map { item ->
                        val cursorAnak = db.rawQuery(
                            "SELECT ${DatabaseHelper.COL_ANAK_NAMA} FROM ${DatabaseHelper.TABLE_ANAK} WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
                            arrayOf(item.anakId)
                        )
                        val namaAnak = if (cursorAnak.moveToFirst()) cursorAnak.getString(0) ?: "Anak" else "Anak"
                        cursorAnak.close()

                        val cursorOrtu = db.rawQuery(
                            "SELECT ${DatabaseHelper.COL_ORTU_NAMA} FROM ${DatabaseHelper.TABLE_ORTU} WHERE ${DatabaseHelper.COL_ORTU_ID} = ?",
                            arrayOf(item.ortuId)
                        )
                        val namaOrtu = if (cursorOrtu.moveToFirst()) cursorOrtu.getString(0) ?: "Ortu" else "Ortu"
                        cursorOrtu.close()

                        val cursorUmur = db.rawQuery(
                            "SELECT ${DatabaseHelper.COL_ANAK_TGL_LAHIR} FROM ${DatabaseHelper.TABLE_ANAK} WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
                            arrayOf(item.anakId)
                        )
                        val umur = if (cursorUmur.moveToFirst()) {
                            val tgl = cursorUmur.getString(0) ?: ""
                            hitungUmurKader(tgl)
                        } else ""
                        cursorUmur.close()

                        AntrianKaderItem(
                            id = item.id,
                            nomor = item.nomor,
                            namaAnak = namaAnak,
                            namaOrtu = namaOrtu,
                            usia = umur,
                            status = item.status,
                            waktuAmbil = item.waktuAmbil
                        )
                    }
                    db.close()

                    antrianItems = mappedItems.sortedBy { it.nomor }
                    totalMenunggu = mappedItems.size

                    val dipanggil = items.find { it.status == 0 }?.nomor?.toString()?.padStart(3, '0') ?: "000"
                    nomorDipanggil = dipanggil
                }
            } catch (e: Exception) {
                android.util.Log.e("ANTRIAN_KADER", "Error load data: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Fungsi panggil berikutnya - didefinisikan sebagai lambda
    val panggilBerikutnya = {
        scope.launch {
            try {
                val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context)
                if (antrianAktif != null) {
                    val items = AntrianApiService.getAntrianItems(context, antrianAktif.id)
                    val berikutnya = items.find { it.status == 1 }

                    if (berikutnya != null) {
                        AntrianApiService.panggilAntrian(berikutnya.id)
                        nomorDipanggil = berikutnya.nomor.toString().padStart(3, '0')
                        loadAntrianData()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ANTRIAN_KADER", "Error panggil: ${e.message}", e)
            }
        }
    }

    // Fungsi tidak hadir - didefinisikan sebagai lambda
    val tidakHadir = { itemId: String ->
        scope.launch {
            try {
                val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context)
                if (antrianAktif != null) {
                    AntrianApiService.tidakHadir(itemId, antrianAktif.id)
                    loadAntrianData()
                }
            } catch (e: Exception) {
                android.util.Log.e("ANTRIAN_KADER", "Error tidak hadir: ${e.message}", e)
            }
        }
    }

    // Load data saat screen dibuka
    LaunchedEffect(Unit) {
        loadAntrianData()
    }

    Scaffold(
        backgroundColor = KaderBackgroundDark,
        bottomBar = {
            KaderBottomBar(
                onHomeClick = onNavigateToHome,
                onPanggilClick = onNavigateToPanggil,
                onLaporanClick = onNavigateToLaporan,
                currentTab = "panggil"
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
                posyanduNama = posyanduNama,
                jadwalInfo = jadwalInfo
            )

            CardDipanggil(nomor = nomorDipanggil)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ButtonPanggilBerikutnya(
                    onClick = { panggilBerikutnya() },
                    modifier = Modifier.weight(1f)
                )
                ButtonTidakHadir(
                    onClick = {
                        val item = antrianItems.find { it.nomor.toString().padStart(3, '0') == nomorDipanggil }
                        item?.let { tidakHadir(it.id) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Menunggu Giliran",
                color = KaderTextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = KaderNeonGreen)
                }
            } else if (antrianItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada antrian",
                        color = KaderTextGrey,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    antrianItems.forEach { item ->
                        CardMenunggu(
                            nomor = item.nomor.toString().padStart(3, '0'),
                            namaAnak = item.namaAnak,
                            onClick = {
                                scope.launch {
                                    AntrianApiService.panggilAntrian(item.id)
                                    nomorDipanggil = item.nomor.toString().padStart(3, '0')
                                    loadAntrianData()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                val parts = tanggalLahir.split("/")
                LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            tanggalLahir.matches(Regex("""\d{4}-\d{2}-\d{2}""")) -> {
                LocalDate.parse(tanggalLahir, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            else -> LocalDate.now()
        }
        val bulan = ChronoUnit.MONTHS.between(lahir, LocalDate.now()).toInt().coerceAtLeast(0)
        "$bulan Bulan"
    } catch (e: Exception) {
        ""
    }
}

// ─────────────────────────────────────────────────────────────
//  KOMPONEN: HEADER
// ─────────────────────────────────────────────────────────────
@Composable
private fun KaderHeader(
    posyanduNama: String,
    jadwalInfo: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KaderHeaderGreen)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        Text(
            text = "MyPosyandu",
            color = KaderTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Text(
            text = "Antrian $posyanduNama",
            color = KaderTextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = jadwalInfo,
            color = KaderTextWhite.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  KOMPONEN: CARD DIPANGGIL
// ─────────────────────────────────────────────────────────────
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
        Text(
            text = "DIPANGGIL",
            color = KaderTextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = nomor,
            color = KaderTextWhite,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 56.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  KOMPONEN: TOMBOL PANGGIL BERIKUTNYA
// ─────────────────────────────────────────────────────────────
@Composable
private fun ButtonPanggilBerikutnya(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(KaderButtonGreen)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildString {
                append("Panggil")
                appendLine()
                append("berikutnya")
            },
            color = KaderTextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  KOMPONEN: TOMBOL TIDAK HADIR
// ─────────────────────────────────────────────────────────────
@Composable
private fun ButtonTidakHadir(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = "Tidak Hadir",
            color = KaderButtonRed,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  KOMPONEN: CARD MENUNGGU
// ─────────────────────────────────────────────────────────────
@Composable
private fun CardMenunggu(
    nomor: String,
    namaAnak: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KaderSurfaceDark)
            .border(1.dp, KaderMenungguBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = nomor,
            color = KaderNeonGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(KaderAvatarMint)
        )

        Text(
            text = namaAnak,
            color = KaderTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  KOMPONEN: BOTTOM NAVIGATION BAR
// ─────────────────────────────────────────────────────────────
@Composable
private fun KaderBottomBar(
    onHomeClick: () -> Unit,
    onPanggilClick: () -> Unit,
    onLaporanClick: () -> Unit,
    currentTab: String = "panggil"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KaderSurfaceDark)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KaderBottomItem(
                emoji = "🏠",
                label = "Homepage",
                isActive = currentTab == "home",
                onClick = onHomeClick
            )
            KaderBottomItem(
                emoji = "📣",
                label = "Panggil",
                isActive = currentTab == "panggil",
                onClick = onPanggilClick
            )
            KaderBottomItem(
                emoji = "📝",
                label = "Daftar",
                isActive = currentTab == "daftar",
                onClick = {}
            )
            KaderBottomItem(
                emoji = "📊",
                label = "Laporan",
                isActive = currentTab == "laporan",
                onClick = onLaporanClick
            )
        }
    }
}

@Composable
private fun KaderBottomItem(
    emoji: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) KaderNeonGreen else KaderTextGrey,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AntrianKaderScreenPreview() {
    AntrianKaderScreen()
}