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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Local Colors (Revisi Tema Orang Tua)
// ─────────────────────────────────────────────────────────────
private val DetailHeaderBlue       = Color(0xFF1964A3) // Biru khusus Ortu
private val DetailActiveTabGreen   = Color(0xFF14634B) // Hijau gelap untuk tab aktif
private val DetailSoftRedBg        = Color(0xFFE5989B) // Merah pudar
private val DetailTextRedDark      = Color(0xFF800000)
private val DetailInfoScheduleBlue = Color(0xFF7FB3D5) // Biru muda jadwal
private val DetailTextScheduleBlue = Color(0xFF1964A3)
private val DetailInfoVaccineGreen = Color(0xFF98E6C8) // Hijau muda vaksin
private val DetailTextVaccineGreen = Color(0xFF14634B)
private val DetailVaccineMintText  = Color(0xFF42B883)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun DetailAnakRingkasanScreen(
    namaAnak: String = "Nabila Rahmah",
    usiaGender: String = "14 Bulan - Perempuan",
    onNavigateBack: () -> Unit = {},
    onNavigateToPemeriksaan: () -> Unit = {},
    onNavigateToVaksin: () -> Unit = {}
) {

    var selectedTab by remember { mutableStateOf("Ringkasan") }

    LaunchedEffect(selectedTab) {
        if (selectedTab == "Pemeriksaan") {
            onNavigateToPemeriksaan()
            selectedTab = "Ringkasan" // Reset agar saat balik ke sini tetap di tab Ringkasan
        } else if (selectedTab == "Vaksin") {
            onNavigateToVaksin()
            selectedTab = "Ringkasan"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 1. HEADER BIRU ORTU ──────────────────────────────
        HeaderParentProfile(
            namaAnak = namaAnak,
            usiaGender = usiaGender,
            onBackClicked = onNavigateBack
        )

        // ── 2. TAB NAVIGASI ──────────────────────────────────
        RingkasanTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 3. AREA KONTEN (Tergantung Tab) ──────────────────
        when (selectedTab) {
            "Ringkasan" -> RingkasanContent()
            "Pemeriksaan" -> Box(modifier = Modifier.padding(16.dp)) { Text("Konten Pemeriksaan...", color = TextGrey) }
            "Vaksin"      -> Box(modifier = Modifier.padding(16.dp)) { Text("Konten Vaksin...", color = TextGrey) }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. COMPONENT: HEADER PROFIL (Warna Biru)
// ════════════════════════════════════════════════════════════

@Composable
private fun HeaderParentProfile(
    namaAnak: String,
    usiaGender: String,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DetailHeaderBlue)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        // Judul MyPosyandu Paling Atas
        Text(
            text = "MyPosyandu",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Tombol Back
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(width = 1.dp, color = TextWhite.copy(alpha = 0.8f), shape = RoundedCornerShape(6.dp))
                .clickable(onClick = onBackClicked)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite, modifier = Modifier.size(18.dp))
            Text(text = "Semua Anak", color = TextWhite, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Baris Bawah: Profil Anak
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar (Lingkaran dengan border putih)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, TextWhite, CircleShape)
                    .background(DetailHeaderBlue)
            )
            // Info
            Column {
                Text(text = namaAnak, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = usiaGender, color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. COMPONENT: TAB NAVIGASI
// ════════════════════════════════════════════════════════════

@Composable
private fun RingkasanTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("Ringkasan", "Pemeriksaan", "Vaksin")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isActive = (tab == selectedTab)
            val bgColor = if (isActive) DetailActiveTabGreen else SurfaceDark
            val textColor = if (isActive) TextWhite else TextGrey

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = tab, color = textColor, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    
    // Garis pembatas bawah tab
    Divider(color = SurfaceDarkBorder, thickness = 1.dp)
}

// ════════════════════════════════════════════════════════════
//  3. CONTENT AREA: TAB "RINGKASAN"
// ════════════════════════════════════════════════════════════

@Composable
private fun RingkasanContent() {
    Column(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. Baris Metrik (BB & TB)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(value = "6.1 kg", label = "Berat badan", modifier = Modifier.weight(1f))
            MetricCard(value = "65 cm", label = "Tinggi badan", modifier = Modifier.weight(1f))
        }

        // B. Card Vaksin
        VaccineStatusCard(value = "100%", label = "Vaksin lengkap")

        // C. Warning Card
        WarningCard(
            title = "Perlu Perhatian", 
            message = "Tinggi badan Nabila di bawah standar. Terdeteksi stunting berat. Segera konsultasikan ke kader atau tenaga kesehatan."
        )

        // D. Info Card: Jadwal
        InfoCard(
            title = "Posyandu berikutnya", 
            detail = "Senin, 2 Juni 2025 pukul 08:00. Bawa buku KIA dan kartu imunisasi.", 
            bgColor = DetailInfoScheduleBlue,
            textColor = DetailTextScheduleBlue
        )

        // E. Info Card: Vaksin
        InfoCard(
            title = "Vaksin", 
            detail = "Anak telah menerima seluruh imunisasi yang direkomendasikan sesuai jadwal usianya. Tidak terdapat vaksin yang masih perlu dilengkapi saat ini.", 
            bgColor = DetailInfoVaccineGreen,
            textColor = DetailTextVaccineGreen
        )
    }
}

// ── Helpers untuk Ringkasan Content ──

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Text(text = value, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextGrey, fontSize = 14.sp)
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
        Text(text = value, color = DetailVaccineMintText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextGrey, fontSize = 14.sp)
    }
}

@Composable
private fun WarningCard(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DetailSoftRedBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, color = DetailTextRedDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = message, color = DetailTextRedDark, fontSize = 13.sp)
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
        Text(text = title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = detail, color = textColor, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun DetailAnakRingkasanScreenPreview() {
    DetailAnakRingkasanScreen()
}
