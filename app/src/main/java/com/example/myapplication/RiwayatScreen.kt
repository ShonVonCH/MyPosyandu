package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StatBoxBg      = Color(0xFF1A1A1A)
private val VaksinGreen    = Color(0xFF7ECFB0)

private val KondisiBaikBg    = Color(0xFFB8EDD8)
private val KondisiBaikText  = Color(0xFF1E6B4E)
private val KondisiWarnBg    = Color(0xFFFFF3CD)
private val KondisiWarnText  = Color(0xFF856404)
private val KondisiBurukBg   = Color(0xFFFFDADA)
private val KondisiBurukText = Color(0xFF9B1C1C)

@Composable
fun RiwayatScreen(
    anakId                  : String         = "",
    hasilAnalisis           : HasilAnalisis? = null,
    vaksinDiberikan         : Map<String, String> = emptyMap(),
    onNavigateBack          : () -> Unit     = {},
    onNavigateToPemeriksaan : () -> Unit     = {},
    onNavigateToImunisasi   : () -> Unit     = {}
) {
    val context = LocalContext.current

    var namaAnak     by remember { mutableStateOf("") }
    var umurBulan    by remember { mutableStateOf(0) }
    var jenisKelamin by remember { mutableStateOf("-") }
    var dbBB         by remember { mutableStateOf("") }
    var dbTB         by remember { mutableStateOf("") }

    // Vaksin dari DB
    var totalVaksin  by remember { mutableStateOf(0) }
    var sudahVaksin  by remember { mutableStateOf(0) }

    LaunchedEffect(anakId) {
        if (anakId.isBlank()) return@LaunchedEffect

        val db = DatabaseHelper(context).readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ANAK_NAMA}, ${DatabaseHelper.COL_ANAK_TGL_LAHIR}, " +
                    "${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} FROM ${DatabaseHelper.TABLE_ANAK} " +
                    "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
            arrayOf(anakId)
        )
        if (cursor.moveToFirst()) {
            namaAnak     = cursor.getString(0) ?: ""
            val tgl      = cursor.getString(1) ?: ""
            val gender   = cursor.getString(2) ?: "-"
            umurBulan    = hitungUmurBulan(tgl)
            jenisKelamin = when (gender.trim().lowercase()) {
                "laki-laki", "l" -> "L"
                "perempuan", "p" -> "P"
                else             -> gender.ifBlank { "-" }
            }
        }
        cursor.close()
        db.close()

        val pemeriksaanRepo = PemeriksaanRepository(context)
        val row  = pemeriksaanRepo.getPemeriksaanTerakhir(anakId)
        dbBB = row?.bb?.let { if (it > 0) it.toString() else "" } ?: ""
        dbTB = row?.tb?.let { if (it > 0) it.toString() else "" } ?: ""

        // Hitung vaksin dari DB
        val vaksinRepo   = VaksinRiwayatRepository(context)
        val seharusnya   = vaksinRepo.getVaksinSudahWaktunya(umurBulan)
        val sudahDiSet   = vaksinRepo.getVaksinSudahDiberikan(anakId)
        totalVaksin = seharusnya.size
        sudahVaksin = seharusnya.count { sudahDiSet.contains(it.id) }
    }

    val tampilBerat  = if (dbBB.isNotBlank()) "$dbBB kg" else "–"
    val tampilTinggi = if (dbTB.isNotBlank()) "$dbTB cm" else "–"

    val labelJK = when (jenisKelamin) {
        "L"  -> "Laki-Laki"
        "P"  -> "Perempuan"
        else -> jenisKelamin
    }
    val labelUmur = if (umurBulan > 0) "$umurBulan Bulan" else "–"
    val subLabel  = when {
        labelUmur != "–" && labelJK.isNotBlank() && labelJK != "-" -> "$labelUmur · $labelJK"
        labelUmur != "–"                                            -> labelUmur
        labelJK.isNotBlank() && labelJK != "-"                      -> labelJK
        else                                                         -> "–"
    }

    val persenVaksin  = if (totalVaksin > 0) (sudahVaksin * 100) / totalVaksin else 0
    val vaksinLengkap = totalVaksin > 0 && sudahVaksin == totalVaksin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        RiwayatHeader(
            namaAnak       = namaAnak,
            subLabel       = subLabel,
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiwayatActions(
            onNavigateToPemeriksaan = onNavigateToPemeriksaan,
            onNavigateToImunisasi   = onNavigateToImunisasi,
            modifier                = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiwayatSummary(
            beratBadanDisplay  = tampilBerat,
            tinggiBadanDisplay = tampilTinggi,
            sudahVaksin        = sudahVaksin,
            totalVaksin        = totalVaksin,
            persenVaksin       = persenVaksin,
            modifier           = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiwayatStatus(
            namaAnak      = namaAnak,
            hasilAnalisis = hasilAnalisis,
            vaksinLengkap = vaksinLengkap,
            totalVaksin   = totalVaksin,
            modifier      = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatHeader(
    namaAnak      : String,
    subLabel      : String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(CircleMint))
                Column {
                    Text(text = namaAnak, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subLabel, color = TextGreenLight, fontSize = 13.sp)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  ACTION BUTTONS
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatActions(
    onNavigateToPemeriksaan: () -> Unit,
    onNavigateToImunisasi  : () -> Unit,
    modifier               : Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard("Pemeriksaan", "🩺", MenuMintBg, MenuMintBorder, MenuMintText, onNavigateToPemeriksaan, Modifier.weight(1f))
        ActionCard("Imunisasi",   "💉", MenuOrangeBg, MenuOrangeBorder, MenuOrangeText, onNavigateToImunisasi, Modifier.weight(1f))
    }
}

@Composable
private fun ActionCard(
    label: String, iconEmoji: String,
    bgColor: Color, borderColor: Color, textColor: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = iconEmoji, fontSize = 26.sp)
                Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  SUMMARY CARD
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatSummary(
    beratBadanDisplay : String,
    tinggiBadanDisplay: String,
    sudahVaksin       : Int,
    totalVaksin       : Int,
    persenVaksin      : Int,
    modifier          : Modifier = Modifier
) {
    val vaksinColor = when {
        totalVaksin == 0           -> TextGrey
        sudahVaksin == totalVaksin -> VaksinGreen
        persenVaksin >= 50         -> Color(0xFFF5A623)
        else                       -> Color(0xFFE74C3C)
    }
    val vaksinLabel = when {
        totalVaksin == 0           -> "Belum ada jadwal"
        sudahVaksin == totalVaksin -> "Vaksin lengkap"
        else                       -> "$sudahVaksin / $totalVaksin vaksin diberikan"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Ringkasan terkini", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStatBox(value = beratBadanDisplay,  label = "Berat badan",  modifier = Modifier.weight(1f))
                SummaryStatBox(value = tinggiBadanDisplay, label = "Tinggi badan", modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatBoxBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text       = if (totalVaksin > 0) "$persenVaksin%" else "–",
                        color      = vaksinColor,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = vaksinLabel, color = TextGrey, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StatBoxBg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(text = value, color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = TextGrey, fontSize = 12.sp)
        }
    }
}

// ════════════════════════════════════════════════════════════
//  STATUS CARD
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatStatus(
    namaAnak     : String,
    hasilAnalisis: HasilAnalisis?,
    vaksinLengkap: Boolean,
    totalVaksin  : Int,
    modifier     : Modifier = Modifier
) {
    if (hasilAnalisis == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2A2A2A))
                .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Belum ada pemeriksaan", color = TextGrey, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text     = "Lakukan pemeriksaan pertama untuk melihat kondisi $namaAnak.",
                    color    = TextGrey,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    val overallWarna = when {
        hasilAnalisis.warnasTBU == StatusWarna.DANGER || hasilAnalisis.warnasBBU == StatusWarna.DANGER -> StatusWarna.DANGER
        hasilAnalisis.warnasTBU == StatusWarna.WARN   || hasilAnalisis.warnasBBU == StatusWarna.WARN   -> StatusWarna.WARN
        else -> StatusWarna.NORMAL
    }

    val adaMasalahVaksin = totalVaksin > 0 && !vaksinLengkap

    val finalWarna = when {
        overallWarna == StatusWarna.DANGER                   -> StatusWarna.DANGER
        overallWarna == StatusWarna.WARN || adaMasalahVaksin -> StatusWarna.WARN
        else                                                 -> StatusWarna.NORMAL
    }

    val (cardBg, cardText) = when (finalWarna) {
        StatusWarna.NORMAL -> KondisiBaikBg  to KondisiBaikText
        StatusWarna.WARN   -> KondisiWarnBg  to KondisiWarnText
        StatusWarna.DANGER -> KondisiBurukBg to KondisiBurukText
    }

    val judulKondisi = when (finalWarna) {
        StatusWarna.NORMAL -> "Kondisi Baik"
        StatusWarna.WARN   -> "Perlu Perhatian"
        StatusWarna.DANGER -> "Perlu Penanganan Segera"
    }

    val deskripsiParts = mutableListOf<String>()
    if (overallWarna == StatusWarna.NORMAL) {
        deskripsiParts += "Pertumbuhan $namaAnak normal (TB/U: ${hasilAnalisis.statusTBU}, BB/U: ${hasilAnalisis.statusBBU})."
    } else {
        if (hasilAnalisis.warnasTBU != StatusWarna.NORMAL)
            deskripsiParts += "Tinggi badan: ${hasilAnalisis.statusTBU} (Z = ${hasilAnalisis.zScoreTBU})."
        if (hasilAnalisis.warnasBBU != StatusWarna.NORMAL)
            deskripsiParts += "Berat badan: ${hasilAnalisis.statusBBU} (Z = ${hasilAnalisis.zScoreBBU})."
    }
    when {
        totalVaksin == 0  -> deskripsiParts += "Belum ada jadwal vaksin untuk usia ini."
        vaksinLengkap     -> deskripsiParts += "Vaksin sesuai usia sudah lengkap."
        else              -> deskripsiParts += "Ada vaksin yang belum diberikan — cek tab Imunisasi."
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = judulKondisi, color = cardText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            deskripsiParts.forEach { kalimat ->
                Text(text = kalimat, color = cardText, fontSize = 13.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun RiwayatScreenPreview() {
    RiwayatScreen(anakId = "")
}