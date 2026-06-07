package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ─────────────────────────────────────────────────────────────
//  Extra colours local to this screen
// ─────────────────────────────────────────────────────────────
private val StatBoxBg       = Color(0xFF1A1A1A)   // very dark box inside summary card
private val StatusCardBg    = Color(0xFFB8EDD8)   // mint green status card
private val StatusTextDark  = Color(0xFF1E6B4E)   // dark green text on status card
private val VaksinGreen     = Color(0xFF7ECFB0)   // "100%" mint colour

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatScreen(
    namaAnak              : String  = "Michael Kwok",
    beratBadan            : String  = "8.1",
    tinggiBadan           : String  = "72",
    onNavigateBack        : () -> Unit = {},
    onNavigateToPemeriksaan: () -> Unit = {},
    onNavigateToImunisasi : () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header hijau atas
        RiwayatHeader(
            namaAnak       = namaAnak,
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Action buttons row
        RiwayatActions(
            onNavigateToPemeriksaan = onNavigateToPemeriksaan,
            onNavigateToImunisasi   = onNavigateToImunisasi,
            modifier                = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Summary card
        RiwayatSummary(
            beratBadan  = beratBadan,
            tinggiBadan = tinggiBadan,
            modifier    = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Status card
        RiwayatStatus(
            namaAnak = namaAnak,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatHeader(
    namaAnak      : String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Back button — outlined box
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width  = 1.dp,
                        color  = TextWhite,
                        shape  = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onNavigateBack)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector        = Icons.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = TextWhite,
                    modifier           = Modifier.size(16.dp)
                )
                Text(
                    text       = "Kembali",
                    color      = TextWhite,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Profile row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CircleMint)
                )

                Column {
                    Text(
                        text       = namaAnak,
                        color      = TextWhite,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text     = "36 Bulan ~ Laki-Laki",
                        color    = TextGreenLight,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. ACTION BUTTONS ROW
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatActions(
    onNavigateToPemeriksaan: () -> Unit,
    onNavigateToImunisasi  : () -> Unit,
    modifier               : Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card Pemeriksaan
        ActionCard(
            label    = "Pemeriksaan",
            iconEmoji = "🩺",
            bgColor  = MenuMintBg,
            borderColor = MenuMintBorder,
            textColor   = MenuMintText,
            onClick  = onNavigateToPemeriksaan,
            modifier = Modifier.weight(1f)
        )
        // Card Imunisasi
        ActionCard(
            label    = "Imunisasi",
            iconEmoji = "💉",
            bgColor  = MenuOrangeBg,
            borderColor = MenuOrangeBorder,
            textColor   = MenuOrangeText,
            onClick  = onNavigateToImunisasi,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionCard(
    label      : String,
    iconEmoji  : String,
    bgColor    : Color,
    borderColor: Color,
    textColor  : Color,
    onClick    : () -> Unit,
    modifier   : Modifier = Modifier
) {
    // Outer card – dark grey with subtle border
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(
                width  = 1.dp,
                color  = SurfaceDarkBorder,
                shape  = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner coloured button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(
                    width  = 1.5.dp,
                    color  = borderColor,
                    shape  = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── ICON SLOT ──────────────────────────────
                // Replace emoji with:
                // Icon(painter = painterResource(R.drawable.ic_...),
                //      contentDescription = label, tint = textColor,
                //      modifier = Modifier.size(28.dp))
                Text(text = iconEmoji, fontSize = 26.sp)
                // ── END ICON SLOT ──────────────────────────
                Text(
                    text       = label,
                    color      = textColor,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  3. SUMMARY CARD
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatSummary(
    beratBadan  : String,
    tinggiBadan : String,
    modifier    : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(
                width  = 1.dp,
                color  = SurfaceDarkBorder,
                shape  = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text(
                text       = "Ringkasan terkini",
                color      = TextWhite,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Berat & Tinggi row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatBox(
                    value    = "$beratBadan kg",
                    label    = "Berat badan",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatBox(
                    value    = "$tinggiBadan cm",
                    label    = "Tinggi badan",
                    modifier = Modifier.weight(1f)
                )
            }

            // Vaksin full-width box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatBoxBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text       = "100%",
                        color      = VaksinGreen,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text     = "Vaksin lengkap",
                        color    = TextGrey,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    value   : String,
    label   : String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StatBoxBg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text       = value,
                color      = TextWhite,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text     = label,
                color    = TextGrey,
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  4. STATUS CARD
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatStatus(
    namaAnak: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StatusCardBg)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text       = "Kondisi Baik",
                color      = StatusTextDark,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = "Pertumbuhan dan vaksin $namaAnak dalam kondisi baik.",
                color    = StatusTextDark,
                fontSize = 13.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun RiwayatScreenPreview() {
    RiwayatScreen()
}
