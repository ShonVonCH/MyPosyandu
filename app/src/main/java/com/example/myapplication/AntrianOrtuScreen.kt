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
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
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
//  Warna Lokal
// ─────────────────────────────────────────────────────────────
private val AntHeaderBlue = Color(0xFF1964A3)
private val AntBackgroundDark = Color(0xFF121212)
private val AntSurfaceDark = Color(0xFF2A2A2A)
private val AntSurfaceDarkBorder = Color(0xFF444444)
private val AntTextWhite = Color(0xFFFFFFFF)
private val AntTextGrey = Color(0xFF888888)
private val AntAvatarMint = Color(0xFF98E6C8)
private val AntNeonGreen = Color(0xFF00C896) // Warna hijau terang untuk antrean aktif
private val AntButtonBlue = Color(0xFF1964A3)

@Composable
fun AntrianOrtuScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToTicket: () -> Unit = {},
    onNavigateToFood: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    Scaffold(
        backgroundColor = AntBackgroundDark,
        bottomBar = {
            BottomNavBarAntrian(
                onHomeClick = onNavigateToHome,
                onTicketClick = onNavigateToTicket, // Saat ini sedang di halaman ini
                onFoodClick = onNavigateToFood,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header Biru (Tanpa badge jumlah anak sesuai desain)
            HeaderAntrianOrtu()

            // 2. Judul Section
            Text(
                text = "Anak saya",
                color = AntTextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            // 3. List Antrean Anak
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Antrean Aktif (Menunggu)
                CardAntrianAktif(
                    nama = "Nabila Rahman",
                    usia = "37 Bulan",
                    nomorAntrian = "022"
                )

                // Card 2: Belum Ambil Antrean
                CardAmbilAntrian(
                    nama = "Sean Hermawan",
                    usia = "37 Bulan",
                    onClick = onNavigateToTicket
                )

                // Card 3: Belum Ambil Antrean
                CardAmbilAntrian(
                    nama = "Rizki Pratama",
                    usia = "37 Bulan",
                    onClick = onNavigateToTicket
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: BOTTOM NAVIGATION BAR
// ════════════════════════════════════════════════════════════
@Composable
private fun BottomNavBarAntrian(
    onHomeClick: () -> Unit,
    onTicketClick: () -> Unit,
    onFoodClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AntSurfaceDark)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home (Tidak Aktif)
        NavIconAntrian(icon = Icons.Outlined.Home, isActive = false, onClick = onHomeClick)
        // Ticket (AKTIF - Warna Biru)
        NavIconAntrian(icon = Icons.Outlined.ConfirmationNumber, isActive = true, onClick = onTicketClick)
        // Food/Gizi (Tidak Aktif)
        NavIconAntrian(icon = Icons.Outlined.Restaurant, isActive = false, onClick = onFoodClick)
        // Profile (Tidak Aktif)
        NavIconAntrian(icon = Icons.Outlined.Person, isActive = false, onClick = onProfileClick)
    }
}

@Composable
private fun NavIconAntrian(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val tintColor = if (isActive) AntHeaderBlue else AntTextWhite.copy(alpha = 0.5f)
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
//  KOMPONEN: HEADER
// ════════════════════════════════════════════════════════════
@Composable
private fun HeaderAntrianOrtu() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AntHeaderBlue)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "MyPosyandu",
            color = AntTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
        )

        Column {
            Text("Selamat datang,", color = AntTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            Text("Pak Sean Albert", color = AntTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Posyandu Mawar, RW-04", color = AntTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: KARTU ANTREAN
// ════════════════════════════════════════════════════════════

@Composable
private fun CardAntrianAktif(nama: String, usia: String, nomorAntrian: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AntSurfaceDark)
            .border(2.dp, AntNeonGreen, RoundedCornerShape(12.dp)) // Border hijau terang
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info Kiri
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AntAvatarMint))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(nama, color = AntTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(usia, color = AntTextGrey, fontSize = 14.sp)
                }
            }
            
            // Nomor Antrean Kanan
            Column(horizontalAlignment = Alignment.End) {
                Text(nomorAntrian, color = AntNeonGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Antrian Anda", color = AntNeonGreen.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Status Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AntNeonGreen)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Status : Menunggu",
                color = AntTextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun CardAmbilAntrian(nama: String, usia: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AntSurfaceDark)
            .border(1.dp, AntSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Info Kiri
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AntAvatarMint))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(nama, color = AntTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(usia, color = AntTextGrey, fontSize = 14.sp)
            }
        }
        
        // Tombol Ambil Antrean
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AntButtonBlue)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Ambil antrian", color = AntTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AntrianOrtuScreenPreview() {
    AntrianOrtuScreen()
}
