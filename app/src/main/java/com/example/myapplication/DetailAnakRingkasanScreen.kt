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
import androidx.compose.material.icons.filled.ArrowBack
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

private val DetailHeaderBlue       = Color(0xFF1964A3)
private val DetailActiveTabGreen   = Color(0xFF14634B)
private val DetailSoftRedBg        = Color(0xFFE5989B)
private val DetailTextRedDark      = Color(0xFF800000)
private val DetailSoftYellowBg     = Color(0xFFFFF3CD)
private val DetailTextYellowDark   = Color(0xFF856404)
private val DetailInfoScheduleBlue = Color(0xFF7FB3D5)
private val DetailTextScheduleBlue = Color(0xFF1964A3)
private val DetailInfoVaccineGreen = Color(0xFF98E6C8)
private val DetailTextVaccineGreen = Color(0xFF14634B)
private val DetailVaccineMintText  = Color(0xFF42B883)

private data class GrowthStatus(
    val bg       : Color,
    val textColor: Color,
    val title    : String,
    val message  : String
)

@Composable
fun DetailAnakRingkasanScreen(
    anakId                  : String   = "",
    onNavigateBack          : () -> Unit = {},
    onNavigateToPemeriksaan : () -> Unit = {},
    onNavigateToVaksin      : () -> Unit = {}
) {
    val context = LocalContext.current
    val repo    = remember { DetailAnakRepository(context) }

    var namaAnak      by remember { mutableStateOf("") }
    var dataRingkasan by remember { mutableStateOf<DataRingkasanAnak?>(null) }
    var statusVaksin  by remember { mutableStateOf(Pair(0, 0)) }
    var hasilAnalisis by remember { mutableStateOf<HasilAnalisis?>(null) }
    var alamatPosyandu by remember { mutableStateOf("") }

    LaunchedEffect(anakId) {
        val db = DatabaseHelper(context).readableDatabase

        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ANAK_NAMA}, ${DatabaseHelper.COL_ANAK_TGL_LAHIR}, ${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} " +
                    "FROM ${DatabaseHelper.TABLE_ANAK} WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
            arrayOf(anakId)
        )
        if (cursor.moveToFirst()) {
            namaAnak = cursor.getString(0) ?: ""
        }
        cursor.close()
        db.close()

        dataRingkasan = repo.getDataRingkasan(anakId)
        statusVaksin  = repo.getStatusVaksin(anakId)

        dataRingkasan?.let { d ->
            val tb = d.tbTerakhir.toDoubleOrNull() ?: 0.0
            val bb = d.bbTerakhir.toDoubleOrNull() ?: 0.0
            if (tb > 0.0 && bb > 0.0) {
                hasilAnalisis = analisisWHO(
                    tinggiBadan  = tb,
                    beratBadan   = bb,
                    umurBulan    = d.umurBulan,
                    jenisKelamin = d.jenisKelamin
                )
            }
        }

        val vaksinRepo = VaksinRiwayatRepository(context)
        alamatPosyandu = vaksinRepo.getAlamatPosyandu()
    }

    val data = dataRingkasan

    val usiaGenderLabel = data?.let {
        val genderLabel = if (it.jenisKelamin.uppercase().let { g -> g == "L" || g == "LAKI-LAKI" })
            "Laki-laki" else "Perempuan"
        "${it.umurBulan} Bulan - $genderLabel"
    } ?: ""

    val (vaksinSudah, vaksinTotal) = statusVaksin
    val vaksinPersen = if (vaksinTotal > 0) (vaksinSudah * 100 / vaksinTotal) else 0
    val vaksinLabel  = if (vaksinTotal > 0) "$vaksinPersen%" else "Belum ada data"
    val vaksinDesc   = when {
        vaksinTotal == 0               -> "Belum ada data vaksin tercatat."
        vaksinSudah == vaksinTotal     -> "Anak telah menerima seluruh imunisasi yang direkomendasikan sesuai jadwal usianya."
        else -> "Anak telah menerima $vaksinSudah dari $vaksinTotal imunisasi. Masih ada ${vaksinTotal - vaksinSudah} vaksin yang perlu dilengkapi."
    }

    var selectedTab by remember { mutableStateOf("Ringkasan") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        HeaderParentProfile(
            namaAnak      = namaAnak,
            usiaGender    = usiaGenderLabel,
            onBackClicked = onNavigateBack
        )

        RingkasanTabs(
            selectedTab   = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                when (tab) {
                    "Pemeriksaan" -> onNavigateToPemeriksaan()
                    "Vaksin"      -> onNavigateToVaksin()
                    else          -> {}
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (data == null) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Belum ada data pemeriksaan.\nHadir ke posyandu untuk pemeriksaan pertama.",
                    color      = TextGrey,
                    fontSize   = 14.sp,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            RingkasanContent(
                bb           = data.bbTerakhir,
                tb           = data.tbTerakhir,
                vaksinLabel  = vaksinLabel,
                vaksinDesc   = vaksinDesc,
                namaAnak     = namaAnak,
                hasilAnalisis = hasilAnalisis,
                alamatPosyandu = alamatPosyandu
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun HeaderParentProfile(
    namaAnak     : String,
    usiaGender   : String,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DetailHeaderBlue)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text       = "MyPosyandu",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, TextWhite.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                .clickable(onClick = onBackClicked)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite, modifier = Modifier.size(18.dp))
            Text("Semua Anak", color = TextWhite, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, TextWhite, CircleShape)
                    .background(DetailHeaderBlue)
            )
            Column {
                Text(namaAnak,   color = TextWhite,                   fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(usiaGender, color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun RingkasanTabs(
    selectedTab  : String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("Ringkasan", "Pemeriksaan", "Vaksin")

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isActive = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) DetailActiveTabGreen else SurfaceDark)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = tab,
                    color      = if (isActive) TextWhite else TextGrey,
                    fontSize   = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    Divider(color = SurfaceDarkBorder, thickness = 1.dp)
}

@Composable
private fun RingkasanContent(
    bb           : String,
    tb           : String,
    vaksinLabel  : String,
    vaksinDesc   : String,
    namaAnak     : String,
    hasilAnalisis: HasilAnalisis?,
    alamatPosyandu: String
) {
    val growthStatus: GrowthStatus = when {
        hasilAnalisis == null -> GrowthStatus(
            bg        = SurfaceDark,
            textColor = TextGrey,
            title     = "Belum ada data",
            message   = "Belum ada data pemeriksaan yang tercatat."
        )
        hasilAnalisis.warnasTBU == StatusWarna.DANGER -> GrowthStatus(
            bg        = DetailSoftRedBg,
            textColor = DetailTextRedDark,
            title     = "Perlu Perhatian",
            message   = "$namaAnak: ${hasilAnalisis.statusTBU} (Z-score TB/U: ${"%.1f".format(hasilAnalisis.zScoreTBU)}). ${hasilAnalisis.saranTBU}"
        )
        hasilAnalisis.warnasBBU == StatusWarna.DANGER -> GrowthStatus(
            bg        = DetailSoftRedBg,
            textColor = DetailTextRedDark,
            title     = "Perlu Perhatian",
            message   = "$namaAnak: ${hasilAnalisis.statusBBU} (Z-score BB/U: ${"%.1f".format(hasilAnalisis.zScoreBBU)}). ${hasilAnalisis.saranBBU}"
        )
        hasilAnalisis.warnasTBU == StatusWarna.WARN -> GrowthStatus(
            bg        = DetailSoftYellowBg,
            textColor = DetailTextYellowDark,
            title     = "Perlu Dipantau",
            message   = "$namaAnak: ${hasilAnalisis.statusTBU} (Z-score TB/U: ${"%.1f".format(hasilAnalisis.zScoreTBU)}). ${hasilAnalisis.saranTBU}"
        )
        hasilAnalisis.warnasBBU == StatusWarna.WARN -> GrowthStatus(
            bg        = DetailSoftYellowBg,
            textColor = DetailTextYellowDark,
            title     = "Perlu Dipantau",
            message   = "$namaAnak: ${hasilAnalisis.statusBBU} (Z-score BB/U: ${"%.1f".format(hasilAnalisis.zScoreBBU)}). ${hasilAnalisis.saranBBU}"
        )
        else -> GrowthStatus(
            bg        = DetailInfoVaccineGreen,
            textColor = DetailTextVaccineGreen,
            title     = "Tumbuh Normal",
            message   = "$namaAnak tumbuh normal. TB/U: ${hasilAnalisis.statusTBU}, BB/U: ${hasilAnalisis.statusBBU}. Pertahankan pola makan dan gizi seimbang."
        )
    }

    Column(
        modifier            = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                value    = if (bb.isNotBlank()) "$bb kg" else "-",
                label    = "Berat badan",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value    = if (tb.isNotBlank()) "$tb cm" else "-",
                label    = "Tinggi badan",
                modifier = Modifier.weight(1f)
            )
        }

        VaccineStatusCard(value = vaksinLabel, label = "Vaksin lengkap")

        InfoCard(
            title     = growthStatus.title,
            detail    = growthStatus.message,
            bgColor   = growthStatus.bg,
            textColor = growthStatus.textColor
        )

        InfoCard(
            title     = "Posyandu berikutnya",
            detail    = if (alamatPosyandu.isNotBlank()) {
                "Hadir ke posyandu setiap bulan untuk pemeriksaan rutin. Bawa buku KIA dan kartu imunisasi.\n\nLokasi: $alamatPosyandu"
            } else {
                "Hadir ke posyandu setiap bulan untuk pemeriksaan rutin. Bawa buku KIA dan kartu imunisasi."
            },
            bgColor   = DetailInfoScheduleBlue,
            textColor = DetailTextScheduleBlue
        )

        InfoCard(
            title     = "Vaksin",
            detail    = vaksinDesc,
            bgColor   = DetailInfoVaccineGreen,
            textColor = DetailTextVaccineGreen
        )
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Text(value, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextGrey, fontSize = 14.sp)
    }
}

@Composable
private fun VaccineStatusCard(value: String, label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text(value, color = DetailVaccineMintText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextGrey, fontSize = 14.sp)
    }
}

@Composable
private fun InfoCard(title: String, detail: String, bgColor: Color, textColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title,  color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = textColor, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun DetailAnakRingkasanScreenPreview() {
    DetailAnakRingkasanScreen()
}