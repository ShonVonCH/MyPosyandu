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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Local Colors
// ─────────────────────────────────────────────────────────────
private val VakHeaderBlue       = Color(0xFF1964A3)
private val VakBackgroundDark   = Color(0xFF121212)
private val VakSurfaceDark      = Color(0xFF2A2A2A)
private val VakSurfaceDarkBorder = Color(0xFF444444)
private val VakActiveTabGreen   = Color(0xFF14634B)
private val VakTextWhite        = Color(0xFFFFFFFF)
private val VakTextGrey         = Color(0xFF888888)

// Badge Colors
private val VakBadgeMintBg      = Color(0xFF98E6C8)
private val VakBadgeMintText    = Color(0xFF14634B)
private val VakBadgeRedBg       = Color(0xFF5C2C2C) // Merah gelap untuk background badge terlambat
private val VakBadgeRedText     = Color(0xFFE55B5B) // Merah terang untuk teks terlambat
private val VakIconGreen        = Color(0xFF1E8F6A)

@Composable
fun VaksinOrtuScreen(
    anakId                  : String   = "",
    onNavigateBack          : () -> Unit = {},
    onNavigateToRingkasan   : () -> Unit = {},
    onNavigateToPemeriksaan : () -> Unit = {}
) {
    val context = LocalContext.current

    var namaAnak   by remember { mutableStateOf("") }
    var usiaGender by remember { mutableStateOf("") }

    LaunchedEffect(anakId) {
        val db = DatabaseHelper(context).readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ANAK_NAMA}, ${DatabaseHelper.COL_ANAK_TGL_LAHIR}, " +
                    "${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} FROM ${DatabaseHelper.TABLE_ANAK} " +
                    "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
            arrayOf(anakId)
        )
        if (cursor.moveToFirst()) {
            namaAnak   = cursor.getString(0) ?: ""
            val tgl    = cursor.getString(1) ?: ""
            val gender = cursor.getString(2) ?: ""
            val umur   = hitungUmurBulan(tgl)
            val gLabel = if (gender.lowercase().let { it == "l" || it == "laki-laki" }) "Laki-laki" else "Perempuan"
            usiaGender = "$umur Bulan - $gLabel"
        }
        cursor.close()
        db.close()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VakBackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 1. HEADER BIRU ──────────────────────────────────
        HeaderParentProfile(
            namaAnak = namaAnak,
            usiaGender = usiaGender,
            onBackClicked = onNavigateBack
        )

        // ── 2. TAB NAVIGASI (Vaksin Aktif) ──────────────────
        TabsVaksin(
            onNavigateToRingkasan = onNavigateToRingkasan,
            onNavigateToPemeriksaan = onNavigateToPemeriksaan
        )

        // ── 3. KONTEN VAKSIN ────────────────────────────────
        VaksinContentArea()
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun HeaderParentProfile(
    namaAnak: String,
    usiaGender: String,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VakHeaderBlue)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "MyPosyandu",
            color = VakTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, VakTextWhite.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                .clickable(onClick = onBackClicked)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VakTextWhite, modifier = Modifier.size(18.dp))
            Text("Semua Anak", color = VakTextWhite, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, VakTextWhite, CircleShape)
                    .background(VakHeaderBlue)
            )
            Column {
                Text(namaAnak, color = VakTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(usiaGender, color = VakTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TabsVaksin(
    onNavigateToRingkasan: () -> Unit,
    onNavigateToPemeriksaan: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tab Ringkasan (Tidak Aktif -> Navigasi)
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

        // Tab Pemeriksaan (Tidak Aktif -> Navigasi)
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

        // Tab Vaksin (AKTIF -> Tetap di halaman ini)
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

@Composable
private fun VaksinContentArea() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Card Kelengkapan Vaksin
        CardKelengkapan()

        // 2. Daftar Vaksin per Bulan
        VaksinBulanCard(
            usia = "Lahir",
            statusLengkap = true,
            items = listOf(
                VaksinItemData("Hepatitis B (HB-0)", "10 Jul 2021 - Posyandu", isLate = false)
            )
        )

        VaksinBulanCard(
            usia = "1 Bulan",
            statusLengkap = true,
            items = listOf(
                VaksinItemData("BCG", "10 Jul 2021 - Posyandu", isLate = false),
                VaksinItemData("Polio 1", "10 Jul 2021 - Posyandu", isLate = false)
            )
        )

        VaksinBulanCard(
            usia = "2 Bulan",
            statusLengkap = false,
            items = listOf(
                VaksinItemData("DPT-HB-Hib 1", "10 Jul 2021 - Posyandu", isLate = false),
                VaksinItemData("Polio 2", "10 Jul 2021 - Posyandu", isLate = false),
                VaksinItemData("IPV 1", "Terlambat - 15 Mar 2023", isLate = true)
            )
        )
    }
}

@Composable
private fun CardKelengkapan() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VakSurfaceDark)
            .border(1.dp, VakSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kelengkapan vaksin", color = VakTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(VakBadgeMintBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("100% Lengkap", color = VakBadgeMintText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LinearProgressIndicator(
            progress = 1f, // 100%
            color = VakIconGreen,
            backgroundColor = VakSurfaceDarkBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("15 dari 15 vaksin diberikan", color = VakTextWhite.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

// ── Model Data Dummy & Helper UI Vaksin ──

data class VaksinItemData(val nama: String, val info: String, val isLate: Boolean)

@Composable
private fun VaksinBulanCard(usia: String, statusLengkap: Boolean, items: List<VaksinItemData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VakSurfaceDark)
            .border(1.dp, VakSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Header Card (Usia & Badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(usia, color = VakTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (statusLengkap) VakBadgeMintBg else VakBadgeRedBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (statusLengkap) "Lengkap" else "Terlambat",
                    color = if (statusLengkap) VakBadgeMintText else VakBadgeRedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daftar Item Vaksin
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (item.isLate) Icons.Default.Cancel else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (item.isLate) VakBadgeRedText else VakIconGreen,
                    modifier = Modifier.size(24.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(item.nama, color = VakTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(item.info, color = if (item.isLate) VakBadgeRedText else VakIconGreen, fontSize = 13.sp)
                }
            }
            
            // Tambahkan garis bawah jika bukan item terakhir
            if (index < items.size - 1) {
                Divider(color = VakSurfaceDarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VaksinOrtuScreenPreview() {
    VaksinOrtuScreen()
}
