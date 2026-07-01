package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

// ── Warna ──
private val VakHeaderBlue        = Color(0xFF1964A3)
private val VakBackgroundDark    = Color(0xFF121212)
private val VakSurfaceDark       = Color(0xFF2A2A2A)
private val VakSurfaceDarkBorder = Color(0xFF444444)
private val VakActiveTabGreen    = Color(0xFF14634B)
private val VakTextWhite         = Color(0xFFFFFFFF)
private val VakTextGrey          = Color(0xFF888888)
private val VakBadgeMintBg       = Color(0xFF98E6C8)
private val VakBadgeMintText     = Color(0xFF14634B)
private val VakBadgeRedBg        = Color(0xFF5C2C2C)
private val VakBadgeRedText      = Color(0xFFE55B5B)
private val VakIconGreen         = Color(0xFF1E8F6A)

// ── Data class lokal (hanya untuk UI layer ini) ──
// CATATAN: Jika VaksinRef, RiwayatVaksin, DatabaseHelper sudah ada
// di file lain (misal DatabaseHelper.kt / VaksinRepository.kt),
// HAPUS blok di bawah ini dan pastikan import-nya sudah benar.

private data class VaksinOrtuGroup(
    val usia: String,
    val statusLengkap: Boolean,
    val statusTerlambat: Boolean,
    val items: List<VaksinOrtuItem>
)

private data class VaksinOrtuItem(
    val nama: String,
    val info: String,
    val isLate: Boolean,
    val isDone: Boolean
)

// ═══════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════

@Composable
fun VaksinOrtuScreen(
    anakId                  : String   = "",
    onNavigateBack          : () -> Unit = {},
    onNavigateToRingkasan   : () -> Unit = {},
    onNavigateToPemeriksaan : () -> Unit = {}
) {
    val context = LocalContext.current
    val repo    = remember { VaksinRiwayatRepository(context) }

    var namaAnak    by remember { mutableStateOf("") }
    var usiaGender  by remember { mutableStateOf("") }
    var vaksinList  by remember { mutableStateOf<List<VaksinOrtuGroup>>(emptyList()) }
    var totalVaksin by remember { mutableStateOf(0) }
    var sudahVaksin by remember { mutableStateOf(0) }

    LaunchedEffect(anakId) {
        val db = DatabaseHelper(context).readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ANAK_NAMA}, ${DatabaseHelper.COL_ANAK_TGL_LAHIR}, " +
                    "${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} FROM ${DatabaseHelper.TABLE_ANAK} " +
                    "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
            arrayOf(anakId)
        )
        var umurBulan = 0
        if (cursor.moveToFirst()) {
            namaAnak  = cursor.getString(0) ?: ""
            val tgl   = cursor.getString(1) ?: ""
            val gender = cursor.getString(2) ?: ""
            umurBulan = hitungUmurBulan(tgl)
            val gLabel = if (gender.lowercase().let { it == "l" || it == "laki-laki" })
                "Laki-laki" else "Perempuan"
            usiaGender = "$umurBulan Bulan - $gLabel"
        }
        cursor.close()
        db.close()

        val semuaVaksin    = repo.getVaksinSudahWaktunya(umurBulan)
        val sudahDiberikan = repo.getVaksinSudahDiberikan(anakId)
        val riwayat        = repo.getRiwayatByAnak(anakId)

        totalVaksin = semuaVaksin.size
        sudahVaksin = semuaVaksin.count { sudahDiberikan.contains(it.id) }

        val grouped = semuaVaksin.groupBy { it.usiaBulan }.toSortedMap()

        vaksinList = grouped.map { (usia, daftarVaksin) ->
            val labelUsia = if (usia == 0) "Lahir" else "$usia Bulan"
            val items = daftarVaksin.map { ref ->
                val riwayatItem = riwayat.find { it.vaksinRefId == ref.id }
                val sudah     = riwayatItem != null
                val terlambat = !sudah && umurBulan > ref.batasBulan
                VaksinOrtuItem(
                    nama  = ref.nama,
                    info  = when {
                        sudah     -> "Diberikan: ${riwayatItem?.tanggalPemberian ?: ""}"
                        terlambat -> "Terlambat - batas ${ref.batasBulan} bulan"
                        else      -> "Belum diberikan"
                    },
                    isLate = terlambat,
                    isDone = sudah
                )
            }
            VaksinOrtuGroup(
                usia            = labelUsia,
                statusLengkap   = items.all { it.isDone },
                statusTerlambat = items.any { it.isLate } && !items.all { it.isDone },
                items           = items
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VakBackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        VaksinOrtuHeader(
            namaAnak      = namaAnak,
            usiaGender    = usiaGender,
            onBackClicked = onNavigateBack
        )
        VaksinOrtuTabs(
            onNavigateToRingkasan   = onNavigateToRingkasan,
            onNavigateToPemeriksaan = onNavigateToPemeriksaan
        )
        VaksinOrtuContent(
            totalVaksin = totalVaksin,
            sudahVaksin = sudahVaksin,
            vaksinList  = vaksinList
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VaksinOrtuHeader(
    namaAnak: String,
    usiaGender: String,
    onBackClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VakHeaderBlue)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = Color.White,
                    modifier           = Modifier
                        .size(24.dp)
                        .clickable { onBackClicked() }
                )
                Text(
                    text       = "MyPosyandu",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .background(VakHeaderBlue)
                )
                Column {
                    Text(namaAnak,   color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(usiaGender, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TABS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VaksinOrtuTabs(
    onNavigateToRingkasan: () -> Unit,
    onNavigateToPemeriksaan: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(VakSurfaceDark)
                .clickable { onNavigateToRingkasan() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Ringkasan", color = VakTextGrey, fontSize = 13.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(VakSurfaceDark)
                .clickable { onNavigateToPemeriksaan() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Pemeriksaan", color = VakTextGrey, fontSize = 13.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(VakActiveTabGreen)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Vaksin", color = VakTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
    Divider(color = VakSurfaceDarkBorder, thickness = 1.dp)
}

// ═══════════════════════════════════════════════════════════════
// CONTENT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VaksinOrtuContent(
    totalVaksin: Int,
    sudahVaksin: Int,
    vaksinList: List<VaksinOrtuGroup>
) {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp)
    ) {
        VaksinOrtuKelengkapanCard(totalVaksin = totalVaksin, sudahVaksin = sudahVaksin)

        if (vaksinList.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada data vaksin", color = VakTextGrey, fontSize = 14.sp)
            }
        } else {
            vaksinList.forEach { group ->
                VaksinOrtuBulanCard(
                    usia            = group.usia,
                    statusLengkap   = group.statusLengkap,
                    statusTerlambat = group.statusTerlambat,
                    items           = group.items
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// KELENGKAPAN CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VaksinOrtuKelengkapanCard(totalVaksin: Int, sudahVaksin: Int) {
    val persen  = if (totalVaksin > 0) (sudahVaksin * 100 / totalVaksin) else 0
    val lengkap = totalVaksin > 0 && sudahVaksin == totalVaksin

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VakSurfaceDark)
            .border(1.dp, VakSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "Kelengkapan vaksin",
                color      = VakTextWhite,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (lengkap) VakBadgeMintBg else VakBadgeRedBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = if (lengkap) "$persen% Lengkap" else "$persen%",
                    color      = if (lengkap) VakBadgeMintText else VakBadgeRedText,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress         = if (totalVaksin > 0) sudahVaksin.toFloat() / totalVaksin else 0f,
            color            = if (lengkap) VakIconGreen else VakBadgeRedText,
            backgroundColor  = VakSurfaceDarkBorder,
            modifier         = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "$sudahVaksin dari $totalVaksin vaksin diberikan",
            color    = VakTextWhite.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// BULAN CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VaksinOrtuBulanCard(
    usia: String,
    statusLengkap: Boolean,
    statusTerlambat: Boolean,
    items: List<VaksinOrtuItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VakSurfaceDark)
            .border(1.dp, VakSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(usia, color = VakTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (statusLengkap) VakBadgeMintBg else VakBadgeRedBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when {
                        statusLengkap   -> "Lengkap"
                        statusTerlambat -> "Terlambat"
                        else            -> "Belum Lengkap"
                    },
                    color = if (statusLengkap) VakBadgeMintText else VakBadgeRedText,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        items.forEachIndexed { index, item ->
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (item.isDone) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = when {
                        item.isDone -> VakIconGreen
                        item.isLate -> VakBadgeRedText
                        else        -> VakTextGrey
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(item.nama, color = VakTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        item.info,
                        color = when {
                            item.isDone -> VakIconGreen
                            item.isLate -> VakBadgeRedText
                            else        -> VakTextGrey
                        },
                        fontSize = 13.sp
                    )
                }
            }

            if (index < items.size - 1) {
                Divider(
                    color     = VakSurfaceDarkBorder,
                    thickness = 1.dp,
                    modifier  = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
fun VaksinOrtuScreenPreview() {
    VaksinOrtuScreen()
}