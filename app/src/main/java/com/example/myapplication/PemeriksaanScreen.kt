package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────
//  Design tokens
// ─────────────────────────────────────────────────────────────
private val TabActiveBg      = Color(0xFF1E6B4E)
private val TabActiveText    = TextWhite
private val TabIdleBg        = Color(0xFF3A3A3A)
private val TabIdleText      = Color(0xFFAAAAAA)
private val WHOCardBg        = Color(0xFF2A2A2A)
private val WHOCardBorder    = Color(0xFF3A3A3A)
private val ProgressTrackWHO = Color(0xFF444444)

// Status colors
private val StatusNormalBg      = Color(0xFFB8EDD8)
private val StatusNormalText    = Color(0xFF1E6B4E)
private val StatusWarnBg        = Color(0xFFFFF3CD)
private val StatusWarnText      = Color(0xFF856404)
private val StatusDangerBg      = Color(0xFFFFDADA)
private val StatusDangerText    = Color(0xFF9B1C1C)
private val StatusProgressGreen  = Color(0xFF3DB89C)
private val StatusProgressYellow = Color(0xFFF5A623)
private val StatusProgressRed    = Color(0xFFE74C3C)

// Chart colors
private val ChartColorMedian = Color(0xFF3DB89C)
private val ChartColorSD2    = Color(0xFFF5A623)
private val ChartColorSD3    = Color(0xFFE74C3C)
private val ChartColorAnak   = Color(0xFFFF6B6B)
private val ChartGridColor   = Color(0xFF3A3A3A)

// ════════════════════════════════════════════════════════════
//  Z-SCORE WHO CALCULATION
// ════════════════════════════════════════════════════════════

data class HasilAnalisis(
    val zScoreTBU : Double,
    val zScoreBBU : Double,
    val statusTBU : String,
    val statusBBU : String,
    val warnasTBU : StatusWarna,
    val warnasBBU : StatusWarna,
    val saranTBU  : String,
    val saranBBU  : String
)

enum class StatusWarna { NORMAL, WARN, DANGER }

private val tabelTBU_LakiLaki = mapOf(
    0  to Pair(49.9, 1.9),  6  to Pair(67.6, 2.5),
    12 to Pair(75.7, 2.7),  18 to Pair(82.3, 2.9),
    24 to Pair(87.8, 3.1),  30 to Pair(92.7, 3.3),
    36 to Pair(96.1, 3.5),  42 to Pair(99.9, 3.6),
    48 to Pair(103.3, 3.7), 54 to Pair(106.4, 3.8),
    60 to Pair(110.0, 4.0)
)

private val tabelTBU_Perempuan = mapOf(
    0  to Pair(49.1, 1.9),  6  to Pair(65.7, 2.5),
    12 to Pair(74.0, 2.7),  18 to Pair(80.7, 2.9),
    24 to Pair(86.4, 3.1),  30 to Pair(91.2, 3.3),
    36 to Pair(95.1, 3.5),  42 to Pair(98.7, 3.6),
    48 to Pair(102.7, 3.7), 54 to Pair(105.9, 3.8),
    60 to Pair(109.4, 4.0)
)

private val tabelBBU_LakiLaki = mapOf(
    0  to Pair(3.3, 0.45),  6  to Pair(7.9, 0.90),
    12 to Pair(9.6, 1.05),  18 to Pair(11.1, 1.18),
    24 to Pair(12.2, 1.30), 30 to Pair(13.3, 1.42),
    36 to Pair(14.3, 1.53), 42 to Pair(15.3, 1.64),
    48 to Pair(16.3, 1.75), 54 to Pair(17.3, 1.87),
    60 to Pair(18.3, 2.00)
)

private val tabelBBU_Perempuan = mapOf(
    0  to Pair(3.2, 0.43),  6  to Pair(7.3, 0.85),
    12 to Pair(8.9, 1.00),  18 to Pair(10.2, 1.12),
    24 to Pair(11.5, 1.24), 30 to Pair(12.7, 1.37),
    36 to Pair(13.9, 1.49), 42 to Pair(15.0, 1.61),
    48 to Pair(16.1, 1.73), 54 to Pair(17.2, 1.86),
    60 to Pair(18.2, 1.98)
)

private fun interpolasi(umur: Int, tabel: Map<Int, Pair<Double, Double>>): Pair<Double, Double> {
    val keys = tabel.keys.sorted()
    val lower = keys.lastOrNull { it <= umur } ?: keys.first()
    val upper = keys.firstOrNull { it >= umur } ?: keys.last()
    if (lower == upper) return tabel[lower]!!
    val (medLow, sdLow) = tabel[lower]!!
    val (medUp,  sdUp)  = tabel[upper]!!
    val ratio = (umur - lower).toDouble() / (upper - lower)
    return Pair(
        medLow + ratio * (medUp - medLow),
        sdLow  + ratio * (sdUp  - sdLow)
    )
}

fun hitungZScore(nilai: Double, median: Double, sd: Double): Double =
    (nilai - median) / sd

fun analisisWHO(
    tinggiBadan : Double,
    beratBadan  : Double,
    umurBulan   : Int,
    jenisKelamin: String
): HasilAnalisis {
    val isLaki = jenisKelamin.contains("Laki", ignoreCase = true)

    val (medTB, sdTB) = interpolasi(umurBulan, if (isLaki) tabelTBU_LakiLaki else tabelTBU_Perempuan)
    val (medBB, sdBB) = interpolasi(umurBulan, if (isLaki) tabelBBU_LakiLaki else tabelBBU_Perempuan)

    val zTBU = hitungZScore(tinggiBadan, medTB, sdTB)
    val zBBU = hitungZScore(beratBadan,  medBB, sdBB)

    val (statusTBU, warnaTBU, saranTBU) = when {
        zTBU < -3.0 -> Triple(
            "Sangat Pendek",
            StatusWarna.DANGER,
            "Anak mengalami stunting berat. Segera rujuk ke tenaga kesehatan dan tingkatkan asupan gizi."
        )
        zTBU < -2.0 -> Triple(
            "Pendek (Stunting)",
            StatusWarna.WARN,
            "Anak berisiko stunting. Pantau pertumbuhan rutin dan perbaiki pola makan bergizi seimbang."
        )
        zTBU > 3.0  -> Triple(
            "Sangat Tinggi",
            StatusWarna.WARN,
            "Tinggi badan di atas rata-rata. Pantau kondisi kesehatan secara berkala."
        )
        else -> Triple(
            "Normal",
            StatusWarna.NORMAL,
            "Tinggi badan sesuai usia. Pertahankan pola makan dan stimulasi tumbuh kembang."
        )
    }

    // Obesitas check must come before Gizi Lebih (more specific)
    val (statusBBU, warnaBBU, saranBBU) = when {
        zBBU < -3.0 -> Triple(
            "Gizi Buruk",
            StatusWarna.DANGER,
            "Anak mengalami gizi buruk. Segera rujuk ke puskesmas untuk tata laksana gizi buruk."
        )
        zBBU < -2.0 -> Triple(
            "Gizi Kurang",
            StatusWarna.WARN,
            "Berat badan di bawah normal. Tingkatkan asupan kalori dan protein, pantau setiap bulan."
        )
        zBBU > 3.0  -> Triple(
            "Obesitas",
            StatusWarna.DANGER,
            "Anak mengalami obesitas. Konsultasikan ke dokter untuk penanganan lebih lanjut."
        )
        zBBU > 2.0  -> Triple(
            "Gizi Lebih",
            StatusWarna.WARN,
            "Berat badan di atas normal. Perhatikan pola makan dan aktivitas fisik anak."
        )
        else -> Triple(
            "Gizi Baik",
            StatusWarna.NORMAL,
            "Berat badan sesuai usia. Pertahankan pola makan bergizi dan aktivitas fisik."
        )
    }

    return HasilAnalisis(
        zScoreTBU  = (zTBU * 100).roundToInt() / 100.0,
        zScoreBBU  = (zBBU * 100).roundToInt() / 100.0,
        statusTBU  = statusTBU,
        statusBBU  = statusBBU,
        warnasTBU  = warnaTBU,
        warnasBBU  = warnaBBU,
        saranTBU   = saranTBU,
        saranBBU   = saranBBU
    )
}

fun zScoreToProgress(z: Double): Float = ((z + 4.0) / 8.0).coerceIn(0.0, 1.0).toFloat()

fun statusToProgressColor(warna: StatusWarna) = when (warna) {
    StatusWarna.NORMAL -> StatusProgressGreen
    StatusWarna.WARN   -> StatusProgressYellow
    StatusWarna.DANGER -> StatusProgressRed
}

// ════════════════════════════════════════════════════════════
//  SCREEN
// ════════════════════════════════════════════════════════════

@Composable
fun PemeriksaanScreen(
    namaAnak         : String = "Michael Kwok",
    umurBulan        : Int    = 36,
    jenisKelamin     : String = "Laki-laki",
    onNavigateBack   : () -> Unit = {},
    onNavigateToHasil: () -> Unit = {},
    /** Dipanggil saat analisis berhasil — kirim (beratBadan, tinggiBadan) ke parent */
    onSimpan         : (beratBadan: String, tinggiBadan: String) -> Unit = { _, _ -> }
) {
    var beratBadan    by remember { mutableStateOf("") }
    var tinggiBadan   by remember { mutableStateOf("") }
    var lingkarKepala by remember { mutableStateOf("") }
    var lingkarLengan by remember { mutableStateOf("") }
    var tanggal       by remember { mutableStateOf("26/05/2025") }
    var activeTab     by remember { mutableStateOf(0) }
    var hasil         by remember { mutableStateOf<HasilAnalisis?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        HeaderProfilAnak(
            subTitle       = "$namaAnak ~ $umurBulan Bulan ~ $jenisKelamin",
            onNavigateBack = onNavigateBack
        )

        PemeriksaanTabs(
            activeTab     = activeTab,
            onTabSelected = { activeTab = it }
        )

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> TabInputContent(
                    beratBadan      = beratBadan,
                    onBeratChange   = { beratBadan = it },
                    tinggiBadan     = tinggiBadan,
                    onTinggiChange  = { tinggiBadan = it },
                    lingkarKepala   = lingkarKepala,
                    onKepalaChange  = { lingkarKepala = it },
                    lingkarLengan   = lingkarLengan,
                    onLenganChange  = { lingkarLengan = it },
                    tanggal         = tanggal,
                    onTanggalChange = { tanggal = it },
                    onAnalisisClick = {
                        val tb = tinggiBadan.toDoubleOrNull()
                        val bb = beratBadan.toDoubleOrNull()
                        if (tb != null && bb != null) {
                            hasil     = analisisWHO(tb, bb, umurBulan, jenisKelamin)
                            activeTab = 1
                            // Simpan ke parent agar RiwayatScreen ikut terupdate
                            onSimpan(beratBadan, tinggiBadan)
                        }
                    }
                )
                1 -> TabHasilContent(
                    namaAnak     = namaAnak,
                    tanggal      = tanggal,
                    hasil        = hasil
                )
                2 -> TabGrafikTBUContent(
                    namaAnak     = namaAnak,
                    umurBulan    = umurBulan,
                    jenisKelamin = jenisKelamin,
                    hasil        = hasil
                )
                3 -> TabGrafikBBUContent(
                    namaAnak     = namaAnak,
                    umurBulan    = umurBulan,
                    jenisKelamin = jenisKelamin,
                    hasil        = hasil
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 0 — INPUT
// ════════════════════════════════════════════════════════════

@Composable
private fun TabInputContent(
    beratBadan      : String,
    onBeratChange   : (String) -> Unit,
    tinggiBadan     : String,
    onTinggiChange  : (String) -> Unit,
    lingkarKepala   : String,
    onKepalaChange  : (String) -> Unit,
    lingkarLengan   : String,
    onLenganChange  : (String) -> Unit,
    tanggal         : String,
    onTanggalChange : (String) -> Unit,
    onAnalisisClick : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        AntropometriCard(
            beratBadan     = beratBadan,
            onBeratChange  = onBeratChange,
            tinggiBadan    = tinggiBadan,
            onTinggiChange = onTinggiChange,
            lingkarKepala  = lingkarKepala,
            onKepalaChange = onKepalaChange,
            lingkarLengan  = lingkarLengan,
            onLenganChange = onLenganChange,
            modifier       = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TanggalCard(
            tanggal         = tanggal,
            onTanggalChange = onTanggalChange,
            modifier        = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnalisisDanSimpanButton(
            onClick  = onAnalisisClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 1 — HASIL
// ════════════════════════════════════════════════════════════

@Composable
private fun TabHasilContent(
    namaAnak : String,
    tanggal  : String,
    hasil    : HasilAnalisis?
) {
    if (hasil == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector        = Icons.Filled.Assessment,
                    contentDescription = null,
                    tint               = TabIdleText,
                    modifier           = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text      = "Isi data di tab Input\nlalu tekan Analisis Dan Simpan",
                    color     = TabIdleText,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Summary card ──────────────────────────────────────
        val overallWarna = if (hasil.warnasTBU == StatusWarna.DANGER || hasil.warnasBBU == StatusWarna.DANGER)
            StatusWarna.DANGER
        else if (hasil.warnasTBU == StatusWarna.WARN || hasil.warnasBBU == StatusWarna.WARN)
            StatusWarna.WARN
        else StatusWarna.NORMAL

        val (summaryBg, summaryText) = when (overallWarna) {
            StatusWarna.NORMAL -> Pair(StatusNormalBg, StatusNormalText)
            StatusWarna.WARN   -> Pair(StatusWarnBg,   StatusWarnText)
            StatusWarna.DANGER -> Pair(StatusDangerBg, StatusDangerText)
        }
        val overallLabel = when (overallWarna) {
            StatusWarna.NORMAL -> "Normal"
            StatusWarna.WARN   -> "Perlu Perhatian"
            StatusWarna.DANGER -> "Perlu Penanganan"
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(summaryBg)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(summaryText.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = if (overallWarna == StatusWarna.NORMAL)
                            Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = overallLabel,
                        tint               = summaryText,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = overallLabel, color = summaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$namaAnak ~ $tanggal", color = summaryText, fontSize = 12.sp)
                    Text(
                        text       = "TB/U: ${hasil.zScoreTBU}   BB/U: ${hasil.zScoreBBU}",
                        color      = summaryText,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Indikator WHO card ────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WHOCardBg)
                .border(1.dp, WHOCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "Indikator WHO", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                // ▸ Label dinamis: "TB/U - <status>" misal "TB/U - Normal"
                WHOIndicatorBlock(
                    title       = "TB/U - ${hasil.statusTBU}",
                    zScore      = "Z-Score: ${hasil.zScoreTBU}",
                    statusLabel = hasil.statusTBU,
                    progress    = zScoreToProgress(hasil.zScoreTBU),
                    description = hasil.saranTBU,
                    warna       = hasil.warnasTBU
                )

                Divider(color = WHOCardBorder, thickness = 0.8.dp)

                // ▸ Label dinamis: "BB/U - <status>" misal "BB/U - Gizi Baik"
                WHOIndicatorBlock(
                    title       = "BB/U - ${hasil.statusBBU}",
                    zScore      = "Z-Score: ${hasil.zScoreBBU}",
                    statusLabel = hasil.statusBBU,
                    progress    = zScoreToProgress(hasil.zScoreBBU),
                    description = hasil.saranBBU,
                    warna       = hasil.warnasBBU
                )

                Divider(color = WHOCardBorder, thickness = 0.8.dp)

                // Keterangan skala Z-Score
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Keterangan Skala Z-Score WHO", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "< -3.0  : Sangat Pendek / Gizi Buruk", color = StatusDangerText, fontSize = 11.sp)
                    Text(text = "-3.0 ~ -2.0 : Pendek (Stunting) / Gizi Kurang", color = StatusWarnText, fontSize = 11.sp)
                    Text(text = "-2.0 ~ +2.0 : Normal", color = StatusNormalText, fontSize = 11.sp)
                    Text(text = "> +2.0  : Di Atas Normal / Gizi Lebih", color = StatusWarnText, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun WHOIndicatorBlock(
    title       : String,
    zScore      : String,
    statusLabel : String,
    progress    : Float,
    description : String,
    warna       : StatusWarna
) {
    val (labelBg, labelText) = when (warna) {
        StatusWarna.NORMAL -> Pair(StatusNormalBg,  StatusNormalText)
        StatusWarna.WARN   -> Pair(StatusWarnBg,    StatusWarnText)
        StatusWarna.DANGER -> Pair(StatusDangerBg,  StatusDangerText)
    }
    val progressColor = statusToProgressColor(warna)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title,  color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = zScore, color = TextGrey,  fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(labelBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = statusLabel, color = labelText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress        = progress,
                modifier        = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color           = progressColor,
                backgroundColor = ProgressTrackWHO
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("-4", "-3", "-2", "0", "+2", "+3", "+4").forEach { label ->
                Text(text = label, color = TextGrey, fontSize = 9.sp)
            }
        }

        Text(text = description, color = TextGrey, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 2 — GRAFIK TB/U
// ════════════════════════════════════════════════════════════

@Composable
private fun TabGrafikTBUContent(
    namaAnak    : String,
    umurBulan   : Int,
    jenisKelamin: String,
    hasil       : HasilAnalisis?
) {
    val isLaki = jenisKelamin.contains("Laki", ignoreCase = true)
    val tabel  = if (isLaki) tabelTBU_LakiLaki else tabelTBU_Perempuan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GrafikLineCard(
            judul        = "Tinggi Badan / Usia (TB/U)",
            namaAnak     = namaAnak,
            umurBulan    = umurBulan,
            tabel        = tabel,
            nilaiAnak    = hasil?.zScoreTBU,
            satuanLabel  = "cm"
        )

        KeteranganStatusCard(
            judulIndikator = "Stunting (TB/U)",
            status         = hasil?.statusTBU,
            zScore         = hasil?.zScoreTBU,
            warna          = hasil?.warnasTBU,
            saran          = hasil?.saranTBU,
            referensi      = listOf(
                "Sangat Pendek : Z < -3.0",
                "Pendek (Stunting) : -3.0 ≤ Z < -2.0",
                "Normal : -2.0 ≤ Z ≤ +2.0",
                "Tinggi : Z > +2.0"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 3 — GRAFIK BB/U
// ════════════════════════════════════════════════════════════

@Composable
private fun TabGrafikBBUContent(
    namaAnak    : String,
    umurBulan   : Int,
    jenisKelamin: String,
    hasil       : HasilAnalisis?
) {
    val isLaki = jenisKelamin.contains("Laki", ignoreCase = true)
    val tabel  = if (isLaki) tabelBBU_LakiLaki else tabelBBU_Perempuan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GrafikLineCard(
            judul        = "Berat Badan / Usia (BB/U)",
            namaAnak     = namaAnak,
            umurBulan    = umurBulan,
            tabel        = tabel,
            nilaiAnak    = hasil?.zScoreBBU,
            satuanLabel  = "kg"
        )

        KeteranganStatusCard(
            judulIndikator = "Underweight (BB/U)",
            status         = hasil?.statusBBU,
            zScore         = hasil?.zScoreBBU,
            warna          = hasil?.warnasBBU,
            saran          = hasil?.saranBBU,
            referensi      = listOf(
                "Gizi Buruk : Z < -3.0",
                "Gizi Kurang : -3.0 ≤ Z < -2.0",
                "Gizi Baik : -2.0 ≤ Z ≤ +2.0",
                "Gizi Lebih : +2.0 < Z ≤ +3.0",
                "Obesitas : Z > +3.0"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  LINE CHART — Grafik Garis WHO
// ════════════════════════════════════════════════════════════

/**
 * Menggambar grafik garis WHO dengan:
 * - Garis median (hijau)
 * - Garis +2 SD dan -2 SD (kuning, putus-putus)
 * - Garis +3 SD dan -3 SD (merah, putus-putus)
 * - Titik + garis posisi anak saat ini (merah solid)
 *
 * Semua nilai diplot sebagai nilai absolut (cm/kg) pada sumbu Y,
 * dengan sumbu X adalah usia (bulan).
 */
@Composable
private fun GrafikLineCard(
    judul       : String,
    namaAnak    : String,
    umurBulan   : Int,
    tabel       : Map<Int, Pair<Double, Double>>,
    nilaiAnak   : Double?,   // z-score anak
    satuanLabel : String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WHOCardBg)
            .border(1.dp, WHOCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = judul, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            // Legenda
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendaLine(color = ChartColorMedian, label = "Median")
                LegendaLine(color = ChartColorSD2,    label = "±2 SD", dashed = true)
                LegendaLine(color = ChartColorSD3,    label = "±3 SD", dashed = true)
                if (nilaiAnak != null) LegendaDot(color = ChartColorAnak, label = namaAnak)
            }

            // Siapkan data series
            val keys = tabel.keys.sorted()
            val minAge = keys.first().toFloat()
            val maxAge = keys.last().toFloat()

            // Kalkulasi semua nilai untuk min/max Y
            val allValues = keys.flatMap { bulan ->
                val (med, sd) = tabel[bulan]!!
                listOf(med - sd * 3, med + sd * 3)
            }
            val minY = (allValues.min() - 1).toFloat()
            val maxY = (allValues.max() + 1).toFloat()

            // Helper: konversi (age, value) -> Offset dalam Canvas
            fun toOffset(age: Float, value: Float, w: Float, h: Float): Offset {
                val x = (age - minAge) / (maxAge - minAge) * w
                val y = h - (value - minY) / (maxY - minY) * h
                return Offset(x, y)
            }

            val chartHeight = 220.dp
            val leftPadding = 36.dp  // ruang label Y axis

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(start = leftPadding, bottom = 20.dp)
            ) {
                val w = size.width
                val h = size.height

                // ── Grid horizontal (setiap nilai bulat) ────────
                val gridStep = if ((maxY - minY) > 50) 20f else if ((maxY - minY) > 20) 10f else 5f
                var gridVal = (minY / gridStep).toInt() * gridStep
                while (gridVal <= maxY) {
                    val gy = h - (gridVal - minY) / (maxY - minY) * h
                    drawLine(
                        color       = ChartGridColor,
                        start       = Offset(0f, gy),
                        end         = Offset(w, gy),
                        strokeWidth = 0.8f
                    )
                    gridVal += gridStep
                }

                // ── Grid vertikal (per interval umur) ───────────
                keys.forEach { bulan ->
                    val gx = (bulan - minAge) / (maxAge - minAge) * w
                    drawLine(
                        color       = ChartGridColor,
                        start       = Offset(gx, 0f),
                        end         = Offset(gx, h),
                        strokeWidth = 0.8f
                    )
                }

                // Helper: gambar path dari list of Offset
                fun drawLinePath(points: List<Offset>, color: Color, strokeWidth: Float, dashed: Boolean = false) {
                    if (points.size < 2) return
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    if (dashed) {
                        // Simulasi dashed: gambar segmen pendek bergantian
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val steps = 8
                            for (s in 0 until steps step 2) {
                                val t1 = s.toFloat() / steps
                                val t2 = (s + 1).toFloat() / steps
                                drawLine(
                                    color       = color,
                                    start       = Offset(p1.x + (p2.x - p1.x) * t1, p1.y + (p2.y - p1.y) * t1),
                                    end         = Offset(p1.x + (p2.x - p1.x) * t2, p1.y + (p2.y - p1.y) * t2),
                                    strokeWidth = strokeWidth,
                                    cap         = StrokeCap.Round
                                )
                            }
                        }
                    } else {
                        drawPath(
                            path   = path,
                            color  = color,
                            style  = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                // ── Garis ±3 SD (merah, putus-putus) ────────────
                val lineNeg3 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first - tabel[b]!!.second * 3).toFloat(), w, h) }
                val linePos3 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first + tabel[b]!!.second * 3).toFloat(), w, h) }
                drawLinePath(lineNeg3, ChartColorSD3.copy(alpha = 0.7f), 1.5f, dashed = true)
                drawLinePath(linePos3, ChartColorSD3.copy(alpha = 0.7f), 1.5f, dashed = true)

                // ── Garis ±2 SD (kuning, putus-putus) ───────────
                val lineNeg2 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first - tabel[b]!!.second * 2).toFloat(), w, h) }
                val linePos2 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first + tabel[b]!!.second * 2).toFloat(), w, h) }
                drawLinePath(lineNeg2, ChartColorSD2.copy(alpha = 0.8f), 1.5f, dashed = true)
                drawLinePath(linePos2, ChartColorSD2.copy(alpha = 0.8f), 1.5f, dashed = true)

                // ── Area fill antara -2 SD dan +2 SD (hijau muda) ─
                val areaPath = Path().apply {
                    moveTo(lineNeg2[0].x, lineNeg2[0].y)
                    lineNeg2.drop(1).forEach { lineTo(it.x, it.y) }
                    linePos2.reversed().forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(
                    path  = areaPath,
                    color = ChartColorMedian.copy(alpha = 0.08f)
                )

                // ── Garis median (hijau solid) ───────────────────
                val lineMedian = keys.map { b -> toOffset(b.toFloat(), tabel[b]!!.first.toFloat(), w, h) }
                drawLinePath(lineMedian, ChartColorMedian, 2.5f, dashed = false)

                // ── Titik + garis vertikal posisi anak ───────────
                if (nilaiAnak != null) {
                    val (medAtAge, sdAtAge) = interpolasi(umurBulan, tabel)
                    val nilaiAbs = medAtAge + nilaiAnak * sdAtAge

                    val xAnak = (umurBulan - minAge) / (maxAge - minAge) * w
                    val yAnak = h - (nilaiAbs.toFloat() - minY) / (maxY - minY) * h

                    // Garis vertikal putus-putus pada usia anak
                    for (step in 0 until 20 step 2) {
                        val y1 = h * step / 20f
                        val y2 = h * (step + 1) / 20f
                        drawLine(
                            color       = ChartColorAnak.copy(alpha = 0.5f),
                            start       = Offset(xAnak, y1),
                            end         = Offset(xAnak, y2),
                            strokeWidth = 1.5f,
                            cap         = StrokeCap.Round
                        )
                    }

                    // Titik anak (lingkaran berisi + outline)
                    drawCircle(
                        color  = ChartColorAnak.copy(alpha = 0.25f),
                        radius = 12f,
                        center = Offset(xAnak, yAnak)
                    )
                    drawCircle(
                        color  = ChartColorAnak,
                        radius = 6f,
                        center = Offset(xAnak, yAnak)
                    )
                    drawCircle(
                        color  = Color.White,
                        radius = 3f,
                        center = Offset(xAnak, yAnak)
                    )
                }
            }

            // ── Label sumbu X (usia bulan) di bawah canvas ──────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = leftPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                keys.forEach { bulan ->
                    Text(
                        text      = if (bulan == 0) "0" else "${bulan}",
                        color     = if (abs(bulan - umurBulan) <= 3) TextWhite else TextGrey,
                        fontSize  = 9.sp,
                        fontWeight = if (abs(bulan - umurBulan) <= 3) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Text(
                text     = "Usia (bulan)",
                color    = TextGrey,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (nilaiAnak != null) {
                val (medAtAge, sdAtAge) = interpolasi(umurBulan, tabel)
                val nilaiAbs = (medAtAge + nilaiAnak * sdAtAge * 10).roundToInt() / 10.0
                Text(
                    text      = "● $namaAnak pada usia $umurBulan bln: $nilaiAbs $satuanLabel (Z = $nilaiAnak)",
                    color     = ChartColorAnak,
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LegendaLine(color: Color, label: String, dashed: Boolean = false) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.width(18.dp).height(8.dp)) {
            if (dashed) {
                for (i in 0..2) {
                    val x1 = size.width * i / 3f
                    val x2 = size.width * (i + 0.6f) / 3f
                    drawLine(color = color, start = Offset(x1, size.height / 2), end = Offset(x2, size.height / 2), strokeWidth = 2f, cap = StrokeCap.Round)
                }
            } else {
                drawLine(color = color, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
        }
        Text(text = label, color = TextGrey, fontSize = 10.sp)
    }
}

@Composable
private fun LegendaDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, color = TextGrey, fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────
//  Keterangan status card (bawah grafik)
// ─────────────────────────────────────────────────────────────

@Composable
private fun KeteranganStatusCard(
    judulIndikator : String,
    status         : String?,
    zScore         : Double?,
    warna          : StatusWarna?,
    saran          : String?,
    referensi      : List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WHOCardBg)
            .border(1.dp, WHOCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Keterangan $judulIndikator", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            if (status != null && zScore != null && warna != null) {
                val (bg, fg) = when (warna) {
                    StatusWarna.NORMAL -> Pair(StatusNormalBg, StatusNormalText)
                    StatusWarna.WARN   -> Pair(StatusWarnBg,   StatusWarnText)
                    StatusWarna.DANGER -> Pair(StatusDangerBg, StatusDangerText)
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(text = "Status anak", color = TextGrey, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(text = status, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Z-Score", color = TextGrey, fontSize = 12.sp)
                    Text(text = zScore.toString(), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = "Belum ada data — lakukan analisis terlebih dahulu", color = TextGrey, fontSize = 12.sp)
            }

            Divider(color = WHOCardBorder, thickness = 0.8.dp)

            Text(text = "Klasifikasi WHO", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            referensi.forEach { line ->
                val isMatch = status != null && line.startsWith(status.split(" ").first(), ignoreCase = true)
                Text(
                    text       = "• $line",
                    color      = if (isMatch) TextWhite else TextGrey,
                    fontSize   = 11.sp,
                    fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (saran != null) {
                Divider(color = WHOCardBorder, thickness = 0.8.dp)
                Text(text = "Rekomendasi", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = saran, color = TextWhite, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  SHARED COMPOSABLES
// ════════════════════════════════════════════════════════════

@Composable
fun HeaderProfilAnak(
    subTitle      : String = "Michael Kwok ~ 36 Bulan ~ Laki-Laki",
    halamanJudul  : String = "Pemeriksaan",
    onNavigateBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TextWhite, RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateBack)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, "Kembali", tint = TextWhite, modifier = Modifier.size(16.dp))
                Text(text = "Kembali", color = TextWhite, fontSize = 13.sp)
            }
            Text(text = halamanJudul, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = subTitle, color = TextWhite.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}

@Composable
fun PemeriksaanTabs(
    activeTab    : Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val tabs = listOf("Input", "Hasil", "Grafik\nTB/U", "Grafik\nBB/U")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) TabActiveBg else TabIdleBg)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    color      = if (isActive) TabActiveText else TabIdleText,
                    fontSize   = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 14.sp,
                    maxLines   = 2,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AntropometriCard(
    beratBadan     : String,
    onBeratChange  : (String) -> Unit,
    tinggiBadan    : String,
    onTinggiChange : (String) -> Unit,
    lingkarKepala  : String,
    onKepalaChange : (String) -> Unit,
    lingkarLengan  : String,
    onLenganChange : (String) -> Unit,
    modifier       : Modifier = Modifier
) {
    PemeriksaanCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Data Antropometri", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AntropometriField("Berat badan (kg)",    beratBadan,    onBeratChange,  Modifier.weight(1f))
                AntropometriField("Tinggi badan (cm)",   tinggiBadan,   onTinggiChange, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AntropometriField("Lingkar Kepala (cm)", lingkarKepala, onKepalaChange, Modifier.weight(1f))
                AntropometriField("Lingkar lengan (cm)", lingkarLengan, onLenganChange, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TanggalCard(
    tanggal         : String,
    onTanggalChange : (String) -> Unit,
    modifier        : Modifier = Modifier
) {
    PemeriksaanCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Tanggal Pemeriksaan", color = TextGrey, fontSize = 13.sp)
            PemeriksaanInputBox(
                value         = tanggal,
                onValueChange = onTanggalChange,
                placeholder   = "dd/mm/yyyy",
                keyboardType  = KeyboardType.Number,
                modifier      = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}

@Composable
fun AnalisisDanSimpanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Assessment, null, tint = TextWhite, modifier = Modifier.size(20.dp))
            Text("Analisis Dan Simpan", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PemeriksaanCardContainer(
    modifier: Modifier = Modifier,
    content : @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun AntropometriField(
    label         : String,
    value         : String,
    onValueChange : (String) -> Unit,
    modifier      : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = TextGrey, fontSize = 12.sp, maxLines = 1)
        PemeriksaanInputBox(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = "0.0",
            keyboardType  = KeyboardType.Decimal,
            modifier      = Modifier.fillMaxWidth().height(48.dp)
        )
    }
}

@Composable
fun PemeriksaanInputBox(
    value         : String,
    onValueChange : (String) -> Unit,
    placeholder   : String,
    modifier      : Modifier = Modifier,
    keyboardType  : KeyboardType = KeyboardType.Text
) {
    BasicTextField(
        value           = value,
        onValueChange   = onValueChange,
        singleLine      = true,
        cursorBrush     = SolidColor(AccentGreen),
        textStyle       = TextStyle(color = TextWhite, fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier        = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3A3A3A))
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) Text(placeholder, color = Color(0xFF6B6B6B), fontSize = 14.sp)
                innerTextField()
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, showSystemUi = true)
@Composable
fun PemeriksaanScreenPreview() {
    PemeriksaanScreen()
}