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
private val StatBoxBg      = Color(0xFF1A1A1A)
private val StatusCardBg   = Color(0xFFB8EDD8)
private val StatusTextDark = Color(0xFF1E6B4E)
private val VaksinGreen    = Color(0xFF7ECFB0)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatScreen(
    namaAnak                : String  = "Michael Kwok",
    umurBulan               : Int     = 0,
    jenisKelamin            : String  = "-",   // "L" atau "P"
    beratBadanTerakhir      : String  = "",
    tinggiBadanTerakhir     : String  = "",
    onNavigateBack          : () -> Unit = {},
    onNavigateToPemeriksaan : () -> Unit = {},
    onNavigateToImunisasi   : () -> Unit = {}
) {
    // Tampilkan "–" bila belum pernah diisi
    val tampilBerat  = if (beratBadanTerakhir.isNotBlank())  "$beratBadanTerakhir kg"  else "–"
    val tampilTinggi = if (tinggiBadanTerakhir.isNotBlank()) "$tinggiBadanTerakhir cm" else "–"

    // Label umur & jenis kelamin
    val labelJK = when (jenisKelamin) {
        "L"  -> "Laki-Laki"
        "P"  -> "Perempuan"
        else -> jenisKelamin
    }
    val labelUmur = if (umurBulan > 0) "$umurBulan Bulan" else "–"
    val subLabel  = if (labelUmur != "–" && labelJK.isNotBlank() && labelJK != "-")
        "$labelUmur · $labelJK"
    else if (labelUmur != "–") labelUmur
    else if (labelJK.isNotBlank() && labelJK != "-") labelJK
    else "–"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        RiwayatHeader(
            namaAnak       = namaAnak,
            subLabel       = subLabel,
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiwayatActions(
            onNavigateToPemeriksaan = onNavigateToPemeriksaan,
            onNavigateToImunisasi   = onNavigateToImunisasi,
            modifier                = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiwayatSummary(
            beratBadanDisplay  = tampilBerat,
            tinggiBadanDisplay = tampilTinggi,
            modifier           = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiwayatStatus(namaAnak = namaAnak, modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatHeader(
    namaAnak      : String,
    subLabel      : String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TextWhite, RoundedCornerShape(8.dp))
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
                Text(text = "Kembali", color = TextWhite, fontSize = 13.sp)
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                        text  = subLabel,
                        color = TextGreenLight,
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
        ActionCard(
            label       = "Pemeriksaan",
            iconEmoji   = "🩺",
            bgColor     = MenuMintBg,
            borderColor = MenuMintBorder,
            textColor   = MenuMintText,
            onClick     = onNavigateToPemeriksaan,
            modifier    = Modifier.weight(1f)
        )
        ActionCard(
            label       = "Imunisasi",
            iconEmoji   = "💉",
            bgColor     = MenuOrangeBg,
            borderColor = MenuOrangeBorder,
            textColor   = MenuOrangeText,
            onClick     = onNavigateToImunisasi,
            modifier    = Modifier.weight(1f)
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = iconEmoji, fontSize = 26.sp)
                Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  3. SUMMARY CARD
// ════════════════════════════════════════════════════════════

@Composable
fun RiwayatSummary(
    beratBadanDisplay  : String,
    tinggiBadanDisplay : String,
    modifier           : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Ringkasan terkini", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatBox(
                    value    = beratBadanDisplay,
                    label    = "Berat badan",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatBox(
                    value    = tinggiBadanDisplay,
                    label    = "Tinggi badan",
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatBoxBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(text = "100%", color = VaksinGreen, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Vaksin lengkap", color = TextGrey, fontSize = 12.sp)
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
            Text(text = value, color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = TextGrey, fontSize = 12.sp)
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
            Text(text = "Kondisi Baik", color = StatusTextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212L)
@Composable
fun RiwayatScreenPreview() {
    RiwayatScreen(
        beratBadanTerakhir  = "8.1",
        tinggiBadanTerakhir = "72",
        umurBulan           = 36,
        jenisKelamin        = "L"
    )
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212L)
@Composable
fun RiwayatScreenEmptyPreview() {
    RiwayatScreen()
}