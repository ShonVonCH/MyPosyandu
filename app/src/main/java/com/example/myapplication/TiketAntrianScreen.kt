package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
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
private val TikHeaderBlue = Color(0xFF1964A3)
private val TikTicketBlue = Color(0xFF1A6EBA) // Biru tiket agak terang sedikit
private val TikBackgroundDark = Color(0xFF121212)
private val TikSurfaceDark = Color(0xFF2A2A2A)
private val TikTextWhite = Color(0xFFFFFFFF)
private val TikMintInfoBg = Color(0xFF98E6C8)
private val TikMintInfoText = Color(0xFF14634B)

@Composable
fun TiketAntrianScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToTicket: () -> Unit = {},
    onNavigateToFood: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    Scaffold(
        backgroundColor = TikBackgroundDark,
        bottomBar = {
            BottomNavBarOrtuTiket(
                onHomeClick = onNavigateToHome,
                onTicketClick = onNavigateToTicket, // Tab tiket menyala
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
            // 1. Header Simple
            HeaderSimple()

            // 2. Area Konten Utama
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kartu Tiket Biru
                CardTiketUtama(
                    nomorAntrian = "022",
                    namaTanggal = "Nabila Rahmah - 2 Juni 2025",
                    dipanggilSekarang = "014",
                    sisaGiliran = "8"
                )

                // Kartu Info Hijau Mint
                CardInfoMint(
                    pesan = "Anda akan dipanggil saat giliran mendekati antrian anda"
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: KARTU TIKET & INFO
// ════════════════════════════════════════════════════════════

@Composable
private fun CardTiketUtama(
    nomorAntrian: String,
    namaTanggal: String,
    dipanggilSekarang: String,
    sisaGiliran: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TikTicketBlue)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NOMOR ANTRIAN ANDA",
            color = TikTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = nomorAntrian,
            color = TikTextWhite,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 80.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = namaTanggal,
            color = TikTextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Divider(
            color = TikTextWhite,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        // Baris Bawah: Dipanggil Sekarang & Sisa Giliran
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom Kiri
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Dipanggil Sekarang", color = TikTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(dipanggilSekarang, color = TikTextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            
            // Garis Pemisah Vertikal
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(TikTextWhite.copy(alpha = 0.5f))
            )
            
            // Kolom Kanan
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Sisa Giliran", color = TikTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(sisaGiliran, color = TikTextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardInfoMint(pesan: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TikMintInfoBg)
            .padding(16.dp)
    ) {
        Text(
            text = pesan,
            color = TikMintInfoText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: HEADER & BOTTOM NAV
// ════════════════════════════════════════════════════════════

@Composable
private fun HeaderSimple() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TikHeaderBlue)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MyPosyandu",
            color = TikTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BottomNavBarOrtuTiket(
    onHomeClick: () -> Unit,
    onTicketClick: () -> Unit,
    onFoodClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TikSurfaceDark)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavIconPrivate(icon = Icons.Outlined.Home, isActive = false, onClick = onHomeClick)
        NavIconPrivate(icon = Icons.Outlined.ConfirmationNumber, isActive = true, onClick = onTicketClick)
        NavIconPrivate(icon = Icons.Outlined.Restaurant, isActive = false, onClick = onFoodClick)
        NavIconPrivate(icon = Icons.Outlined.Person, isActive = false, onClick = onProfileClick)
    }
}

@Composable
private fun NavIconPrivate(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val tintColor = if (isActive) TikHeaderBlue else TikTextWhite.copy(alpha = 0.5f)
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tintColor,
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun TiketAntrianScreenPreview() {
    TiketAntrianScreen()
}
