package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Local design tokens
// ─────────────────────────────────────────────────────────────
private val CardBg              = Color(0xFF2A2A2A)
private val CardBorder          = Color(0xFF444444)
private val RowDividerColor     = Color(0xFFFFFFFF).copy(alpha = 0.10f)
private val AvatarGrey          = Color(0xFFAAAAAA)
private val InfoBoxBg           = Color(0xFFC5DCF0)   // biru muda pastel
private val InfoBoxText         = Color(0xFF1A4A6E)   // biru tua
private val InfoBoxBold         = Color(0xFF0D3457)   // lebih gelap untuk @username
private val SubtextColor        = Color(0xFFAAAAAA)
private val CardTitleGrey       = Color(0xFF888888)   // "Akun Orang Tua" label

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun KonfirmasiDataScreen(
    onNavigateBack   : () -> Unit = {},
    onSimpanClicked  : () -> Unit = {},
    onPerbaikiClicked: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        KonfirmasiHeader(onNavigateBack = onNavigateBack)

        // Info teks
        Text(
            text       = "Pastikan data berikut sudah bener sebelum menyimpan.",
            color      = TextWhite,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // Card 1: Data Balita
        DataBalitaCard(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Card 2: Akun Orang Tua
        AkunOrangTuaCard(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // Tombol aksi
        SimpanButton(
            onClick  = onSimpanClicked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        PerbaikiButton(
            onClick  = onPerbaikiClicked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun KonfirmasiHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Back button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TextWhite, RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateBack)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = TextWhite,
                    modifier           = Modifier.size(16.dp)
                )
                Text(text = "Kembali", color = TextWhite, fontSize = 14.sp)
            }

            // Title
            Text(
                text       = "Konfirmasi Data",
                color      = TextWhite,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. CARD: DATA BALITA
// ════════════════════════════════════════════════════════════

@Composable
fun DataBalitaCard(modifier: Modifier = Modifier) {
    KonfirmasiCardContainer(modifier = modifier) {
        Column {
            Text(
                text       = "Data Balita",
                color      = TextWhite,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 10.dp)
            )

            DataRowItem(label = "Nama",           value = "Budi Prasetyo")
            DataRowItem(label = "Tanggal Lahir",  value = "12 Mar 2025")
            DataRowItem(label = "Usia saat ini",  value = "2 Bulan")
            DataRowItem(label = "Jenis kelamin",  value = "Laki-laki")
            DataRowItem(label = "Nama ibu",       value = "Rina Susanti")
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Reusable: DataRowItem
// ─────────────────────────────────────────────────────────────

@Composable
fun DataRowItem(
    label : String,
    value : String
) {
    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = label,
                color      = TextWhite,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text       = value,
                color      = TextWhite,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Divider di bawah setiap baris (termasuk baris terakhir sesuai desain)
        Divider(color = RowDividerColor, thickness = 1.dp)
    }
}

// ════════════════════════════════════════════════════════════
//  3. CARD: AKUN ORANG TUA
// ════════════════════════════════════════════════════════════

@Composable
fun AkunOrangTuaCard(modifier: Modifier = Modifier) {
    KonfirmasiCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Card title
            Text(
                text       = "Akun Orang Tua",
                color      = CardTitleGrey,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )

            // Profile row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AvatarGrey)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = "Rina Susanti",
                        color      = TextWhite,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "@ortu_rina -  3 anak terdaftar",
                        color    = SubtextColor,
                        fontSize = 13.sp
                    )
                }
            }

            // Info box biru
            OrangTuaInfoBox()
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Info box dengan AnnotatedString untuk bold @ortu_rina
// ─────────────────────────────────────────────────────────────

@Composable
private fun OrangTuaInfoBox() {
    val infoText = buildAnnotatedString {
        withStyle(SpanStyle(color = InfoBoxText, fontSize = 13.sp)) {
            append("Anak ini akan ditambahkan ke akun ")
        }
        withStyle(SpanStyle(
            color      = InfoBoxBold,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold
        )) {
            append("@ortu_rina")
        }
        withStyle(SpanStyle(color = InfoBoxText, fontSize = 13.sp)) {
            append(" dan langsung terlihat saat orang tua login. Rina Susanti sudah punya 3 anak terdaftar")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InfoBoxBg)
            .padding(12.dp)
    ) {
        Text(
            text       = infoText,
            lineHeight = 20.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  4. TOMBOL AKSI
// ════════════════════════════════════════════════════════════

@Composable
fun SimpanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HeaderGreen)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Simpan & Daftarkan",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PerbaikiButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark)
            .border(1.dp, TextWhite, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Perbaiki data",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════
//  REUSABLE: CARD CONTAINER
// ════════════════════════════════════════════════════════════

@Composable
private fun KonfirmasiCardContainer(
    modifier: Modifier = Modifier,
    content : @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun KonfirmasiDataScreenPreview() {
    KonfirmasiDataScreen()
}
