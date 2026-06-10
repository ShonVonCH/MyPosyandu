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
import androidx.compose.runtime.Composable
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
//  Local Colors (Konsisten dengan DetailAnakRingkasanScreen)
// ─────────────────────────────────────────────────────────────
private val PemHeaderBlue       = Color(0xFF1964A3)
private val PemBackgroundDark   = Color(0xFF121212)
private val PemSurfaceDark      = Color(0xFF2A2A2A)
private val PemSurfaceDarkBorder = Color(0xFF444444)
private val PemActiveTabGreen   = Color(0xFF14634B)
private val PemTextWhite        = Color(0xFFFFFFFF)
private val PemTextGrey         = Color(0xFF888888)
private val PemInfoBlueBg       = Color(0xFF85B3E9)
private val PemInfoBlueText     = Color(0xFF1964A3)

@Composable
fun PemeriksaanOrtuScreen(
    namaAnak: String = "Nabila Rahmah",
    usiaGender: String = "14 Bulan - Perempuan",
    onNavigateBack: () -> Unit = {},
    onNavigateToRingkasan: () -> Unit = {},
    onNavigateToVaksin: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PemBackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 1. HEADER BIRU ──────────────────────────────────
        HeaderParentProfile(
            namaAnak = namaAnak,
            usiaGender = usiaGender,
            onBackClicked = onNavigateBack
        )

        // ── 2. TAB NAVIGASI (Pemeriksaan Aktif) ──────────────
        TabsPemeriksaan(
            onNavigateToRingkasan = onNavigateToRingkasan,
            onNavigateToVaksin = onNavigateToVaksin
        )

        // ── 3. KONTEN RIWAYAT PEMERIKSAAN ────────────────────
        PemeriksaanContentArea()

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
            .background(PemHeaderBlue)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "MyPosyandu",
            color = PemTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, PemTextWhite.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                .clickable(onClick = onBackClicked)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = PemTextWhite, modifier = Modifier.size(18.dp))
            Text("Semua Anak", color = PemTextWhite, fontSize = 14.sp)
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
                    .border(2.dp, PemTextWhite, CircleShape)
                    .background(PemHeaderBlue)
            )
            Column {
                Text(namaAnak, color = PemTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(usiaGender, color = PemTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TabsPemeriksaan(
    onNavigateToRingkasan: () -> Unit,
    onNavigateToVaksin: () -> Unit
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
                .background(PemSurfaceDark)
                .clickable { onNavigateToRingkasan() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Ringkasan", color = PemTextGrey, fontSize = 13.sp)
        }

        // Tab Pemeriksaan (AKTIF -> Tetap di halaman ini)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(PemActiveTabGreen)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Pemeriksaan", color = PemTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Tab Vaksin (Tidak Aktif -> Navigasi)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(PemSurfaceDark)
                .clickable { onNavigateToVaksin() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Vaksin", color = PemTextGrey, fontSize = 13.sp)
        }
    }
    Divider(color = PemSurfaceDarkBorder, thickness = 1.dp)
}

@Composable
private fun PemeriksaanContentArea() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card Riwayat
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PemSurfaceDark)
                .border(1.dp, PemSurfaceDarkBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Riwayat pemeriksaan",
                color = PemTextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ItemRiwayat("25 Apr 2025", "TB: 100cm - BB: 40kg -\nLK: 43cm", "Z: -3.2")
            ItemRiwayat("23 Mar 2025", "TB: 100cm - BB: 40kg -\nLK: 43cm", "Z: -3.2")
            ItemRiwayat("25 Apr 2025", "TB: 100cm - BB: 40kg -\nLK: 43cm", "Z: -3.2", isLastItem = true)
        }

        // Card Info Biru
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PemInfoBlueBg)
                .padding(16.dp)
        ) {
            Text(
                text = "Data hanya dapat di isi oleh kader posyandu. Hadir ke posyandu setiap bulan untuk pemeriksaan rutin",
                color = PemInfoBlueText,
                fontSize = 14.sp,
                textAlign = TextAlign.Left,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ItemRiwayat(tanggal: String, detailKiri: String, detailKanan: String, isLastItem: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(tanggal, color = PemTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(detailKiri, color = PemTextWhite.copy(alpha = 0.7f), fontSize = 14.sp, lineHeight = 20.sp)
            Text(detailKanan, color = PemTextWhite.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.End)
        }
        if (!isLastItem) {
            Divider(color = PemTextWhite.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PemeriksaanOrtuScreenPreview() {
    PemeriksaanOrtuScreen()
}
