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

private val PemHeaderBlue        = Color(0xFF1964A3)
private val PemActiveTabGreen    = Color(0xFF14634B)
private val PemInfoBlueBg        = Color(0xFF85B3E9)
private val PemInfoBlueText      = Color(0xFF1964A3)

@Composable
fun PemeriksaanOrtuScreen(
    anakId               : String   = "",
    onNavigateBack       : () -> Unit = {},
    onNavigateToRingkasan: () -> Unit = {},
    onNavigateToVaksin   : () -> Unit = {}
) {
    val context = LocalContext.current
    val repo    = remember { DetailAnakRepository(context) }

    var namaAnak        by remember { mutableStateOf("") }
    var riwayat         by remember { mutableStateOf<List<RiwayatPemeriksaanItem>>(emptyList()) }
    var usiaGenderLabel by remember { mutableStateOf("") }

    LaunchedEffect(anakId) {
        val db = DatabaseHelper(context).readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ANAK_NAMA} FROM ${DatabaseHelper.TABLE_ANAK} " +
                    "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
            arrayOf(anakId)
        )
        if (cursor.moveToFirst()) namaAnak = cursor.getString(0) ?: ""
        cursor.close()
        db.close()

        riwayat = repo.getRiwayatPemeriksaan(anakId)
        val dataRingkasan = repo.getDataRingkasan(anakId)
        usiaGenderLabel = dataRingkasan?.let {
            val genderLabel = if (it.jenisKelamin.uppercase().let { g -> g == "L" || g == "LAKI-LAKI" })
                "Laki-laki" else "Perempuan"
            "${it.umurBulan} Bulan - $genderLabel"
        } ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        PemHeaderProfile(
            namaAnak      = namaAnak,
            usiaGender    = usiaGenderLabel,
            onBackClicked = onNavigateBack
        )

        PemTabs(
            onNavigateToRingkasan = onNavigateToRingkasan,
            onNavigateToVaksin    = onNavigateToVaksin
        )

        PemeriksaanContent(riwayat = riwayat)

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun PemHeaderProfile(
    namaAnak     : String,
    usiaGender   : String,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PemHeaderBlue)
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
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite, modifier = Modifier.size(18.dp))
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
                    .background(PemHeaderBlue)
            )
            Column {
                Text(namaAnak,   color = TextWhite,                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(usiaGender, color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PemTabs(
    onNavigateToRingkasan: () -> Unit,
    onNavigateToVaksin   : () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("Ringkasan",   false, onNavigateToRingkasan),
            Triple("Pemeriksaan", true,  {}),
            Triple("Vaksin",      false, onNavigateToVaksin)
        ).forEach { (label, isActive, onClick) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) PemActiveTabGreen else SurfaceDark)
                    .clickable(enabled = !isActive, onClick = onClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    color      = if (isActive) TextWhite else TextGrey,
                    fontSize   = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    Divider(color = SurfaceDarkBorder, thickness = 1.dp)
}

// Ganti fungsi PemeriksaanContent dan ItemRiwayat saja, sisanya sama

@Composable
private fun PemeriksaanContent(riwayat: List<RiwayatPemeriksaanItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text       = "Riwayat pemeriksaan",
                color      = TextWhite,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 16.dp)
            )

            if (riwayat.isEmpty()) {
                Text("Belum ada riwayat pemeriksaan.", color = TextGrey, fontSize = 14.sp)
            } else {
                riwayat.forEachIndexed { index, item ->
                    ItemRiwayat(
                        item       = item,
                        isLastItem = index == riwayat.lastIndex
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PemInfoBlueBg)
                .padding(16.dp)
        ) {
            Text(
                text       = "Data hanya dapat diisi oleh kader posyandu. Hadir ke posyandu setiap bulan untuk pemeriksaan rutin.",
                color      = PemInfoBlueText,
                fontSize   = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ItemRiwayat(
    item      : RiwayatPemeriksaanItem,
    isLastItem: Boolean = false
) {
    val hasil = item.hasil

    // Warna baris berdasarkan status overall
    val statusColor = when {
        hasil == null -> TextGrey
        hasil.warnasTBU == StatusWarna.DANGER || hasil.warnasBBU == StatusWarna.DANGER -> Color(0xFFE74C3C)
        hasil.warnasTBU == StatusWarna.WARN   || hasil.warnasBBU == StatusWarna.WARN   -> Color(0xFFF5A623)
        else -> Color(0xFF3DB89C)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Tanggal + status chip
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(item.tanggal, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (hasil != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text       = when {
                            hasil.warnasTBU == StatusWarna.DANGER || hasil.warnasBBU == StatusWarna.DANGER -> "Perlu Penanganan"
                            hasil.warnasTBU == StatusWarna.WARN   || hasil.warnasBBU == StatusWarna.WARN   -> "Perlu Perhatian"
                            else -> "Normal"
                        },
                        color      = statusColor,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Detail antropometri
        Text(
            text  = buildString {
                if (item.tb.isNotBlank()) append("TB: ${item.tb} cm")
                if (item.bb.isNotBlank()) append("  BB: ${item.bb} kg")
                if (item.lk.isNotBlank()) append("  LK: ${item.lk} cm")
                if (item.ll.isNotBlank()) append("  LL: ${item.ll} cm")
            },
            color    = TextWhite.copy(alpha = 0.75f),
            fontSize = 13.sp
        )

        // Z-score row
        if (hasil != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text     = "TB/U: ${hasil.zScoreTBU} (${hasil.statusTBU})",
                    color    = statusColor.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
                Text(
                    text     = "BB/U: ${hasil.zScoreBBU} (${hasil.statusBBU})",
                    color    = statusColor.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        if (!isLastItem) {
            Divider(
                color    = TextWhite.copy(alpha = 0.1f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PemeriksaanOrtuScreenPreview() {
    PemeriksaanOrtuScreen()
}