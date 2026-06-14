package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailAnakScreen(onNavigateBack: () -> Unit = {}) {
    val backgroundColor = Color(0xFF121212) // Hitam pekat

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. Header Hijau Tosca
        DetailAnakHeader(onBack = onNavigateBack)

        // 2. Tab Section (Barisan kotak di bawah header)
        DetailTabSection()

        // Area Konten (Hitam Kosong)
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun DetailAnakHeader(onBack: () -> Unit) {
    val headerColor = Color(0xFF2E9E7B) // Hijau Tosca Emerald
    val avatarColor = Color(0xFF98E6C8) // Mint Muda

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                .clickable { onBack() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Profil Anak
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Foto/Avatar Lingkaran
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Michael Kwok",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "36 Bulan ~ Laki-Laki",
                    color = Color(0xCCFFFFFF), // Putih transparan
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DetailTabSection() {
    val tabBgColor = Color(0xFF2A2A2A) // Abu-abu gelap
    val activeTabColor = Color(0xFF1E6E55) // Hijau gelap (Aktif)
    val inactiveTabColor = Color(0xFFB0B0B0) // Abu-abu terang (Tidak aktif)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tabBgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kotak 1 (Hijau/Aktif)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .background(activeTabColor, RoundedCornerShape(6.dp))
            )
            // Kotak 2 (Abu/Tidak Aktif)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .background(inactiveTabColor, RoundedCornerShape(6.dp))
            )
            // Kotak 3 (Abu/Tidak Aktif)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .background(inactiveTabColor, RoundedCornerShape(6.dp))
            )
        }
        // Garis Divider Tipis di bawah tab
        Divider(color = Color(0xFF333333), thickness = 1.dp)
    }
}
