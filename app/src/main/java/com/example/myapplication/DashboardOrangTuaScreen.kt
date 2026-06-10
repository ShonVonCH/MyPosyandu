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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Warna Lokal (Tema Biru Orang Tua)
// ─────────────────────────────────────────────────────────────
private val DashHeaderBlue = Color(0xFF1964A3)
private val DashLightBlueBg = Color(0xFF85B3E9)
private val DashBackgroundDark = Color(0xFF121212)
private val DashSurfaceDark = Color(0xFF2A2A2A)
private val DashSurfaceDarkBorder = Color(0xFF444444)
private val DashTextWhite = Color(0xFFFFFFFF)
private val DashTextGrey = Color(0xFF888888)
private val DashBadgeRed = Color(0xFFD32F2F)
private val DashAvatarMint = Color(0xFF98E6C8)

@Composable
fun DashboardOrangTuaScreen(
    onNavigateToDetailAnak: (String) -> Unit = {},
    // Parameter navigasi untuk Bottom Bar
    onNavigateToHome: () -> Unit = {},
    onNavigateToTicket: () -> Unit = {},
    onNavigateToFood: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    // Scaffold digunakan agar BottomBar selalu menempel di bawah
    Scaffold(
        backgroundColor = DashBackgroundDark,
        bottomBar = {
            BottomNavBarOrtu(
                onHomeClick = onNavigateToHome,
                onTicketClick = onNavigateToTicket,
                onFoodClick = onNavigateToFood,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        // Konten utama yang bisa di-scroll diletakkan di sini
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Penting: agar konten tidak tertutup BottomBar
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header Biru
            HeaderDashboardOrtu()

            // 2. Banner Jadwal
            BannerJadwalOrtu()

            // 3. Section Anak Saya
            Text(
                text = "Anak saya",
                color = DashTextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // List Anak
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Anak 1 (Baru)
                CardAnakBaru(
                    nama = "Budi Prasetyo",
                    usiaGender = "2 bulan - Laki-laki",
                    onClick = { onNavigateToDetailAnak("Budi Prasetyo") }
                )

                // Anak 2 (Lama)
                CardAnakLama(
                    nama = "Nabila Rahmah",
                    usiaGender = "14 bulan - Perempuan",
                    tanggalPeriksa = "25 Apr 2025 - TB 65 cm",
                    onClick = { onNavigateToDetailAnak("Nabila Rahmah") }
                )

                // Anak 3 (Lama)
                CardAnakLama(
                    nama = "Rizki Pratama",
                    usiaGender = "37 bulan - Laki-laki",
                    tanggalPeriksa = "26 Apr 2025 - TB 93 cm",
                    onClick = { onNavigateToDetailAnak("Rizki Pratama") }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: BOTTOM NAVIGATION BAR
// ════════════════════════════════════════════════════════════
@Composable
private fun BottomNavBarOrtu(
    onHomeClick: () -> Unit,
    onTicketClick: () -> Unit,
    onFoodClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashSurfaceDark)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home (Aktif - Warna Biru)
        NavIcon(icon = Icons.Outlined.Home, isActive = true, onClick = onHomeClick)
        // Ticket
        NavIcon(icon = Icons.Outlined.ConfirmationNumber, isActive = false, onClick = onTicketClick)
        // Food/Gizi
        NavIcon(icon = Icons.Outlined.Restaurant, isActive = false, onClick = onFoodClick)
        // Profile
        NavIcon(icon = Icons.Outlined.Person, isActive = false, onClick = onProfileClick)
    }
}

@Composable
private fun NavIcon(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val tintColor = if (isActive) DashHeaderBlue else DashTextWhite.copy(alpha = 0.5f)
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tintColor,
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
    )
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: HEADER & BANNER
// ════════════════════════════════════════════════════════════
@Composable
private fun HeaderDashboardOrtu() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashHeaderBlue)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "MyPosyandu",
            color = DashTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Selamat datang,", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                Text("Pak Sean Albert", color = DashTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Posyandu Mawar, RW-04", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            
            // Badge 4 Anak
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E86C1))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("4", color = DashTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("anak", color = DashTextWhite, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BannerJadwalOrtu() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DashHeaderBlue)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Jadwal", tint = DashTextWhite, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Jadwal Posyandu berikutnya", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("Senin, 2 Juni 2025 -- 08:00", color = DashTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Balai RW 04 - Harap datang tepat waktu", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: KARTU ANAK
// ════════════════════════════════════════════════════════════
@Composable
private fun CardAnakBaru(nama: String, usiaGender: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashSurfaceDark)
            .border(1.dp, DashSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(DashAvatarMint))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nama, color = DashTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(DashBadgeRed).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Baru", color = DashTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(usiaGender, color = DashTextGrey, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DashTextGrey)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(DashLightBlueBg).padding(12.dp)) {
            Text("Baru didaftarkan kader. Belum ada pemeriksaan.", color = DashHeaderBlue, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CardAnakLama(nama: String, usiaGender: String, tanggalPeriksa: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashSurfaceDark)
            .border(1.dp, DashSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(DashAvatarMint))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nama, color = DashTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(usiaGender, color = DashTextGrey, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DashTextGrey)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Terakhir diperiksa", color = DashTextGrey, fontSize = 13.sp)
        Text(tanggalPeriksa, color = DashTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardOrangTuaScreenPreview() {
    DashboardOrangTuaScreen()
}
