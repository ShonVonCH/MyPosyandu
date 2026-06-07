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
import androidx.compose.material.icons.filled.CheckCircle
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
//  Local design tokens
// ─────────────────────────────────────────────────────────────
private val SummaryCardBg     = Color(0xFFB8EDD8)   // mint green
private val SummaryTextDark   = Color(0xFF1E6B4E)   // dark green text
private val WHOCardBg         = Color(0xFF2A2A2A)
private val WHOCardBorder     = Color(0xFF3A3A3A)
private val NormalLabelBg     = Color(0xFFEEEEEE)   // near-white pill
private val NormalLabelText   = Color(0xFF1A1A1A)
private val ProgressGreen     = Color(0xFF3DB89C)
private val ProgressTrackWHO  = Color(0xFF444444)
private val TabActiveBg       = Color(0xFF1E6B4E)   // dark green active tab
private val TabActiveText     = TextWhite
private val TabIdleBg         = Color(0xFF3A3A3A)
private val TabIdleText       = Color(0xFFAAAAAA)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun HasilScreen(onNavigateBack: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header + tab navigation
        HasilHeader(onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(16.dp))

        // Card ringkasan status normal
        HasilSummaryCard(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card indikator WHO
        IndikatorWHOCard(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER  (green section + tab row)
// ════════════════════════════════════════════════════════════

@Composable
fun HasilHeader(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Back button + name
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
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
            Text(text = "Michael Kwok", color = TextWhite, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text       = "Pemeriksaan",
                color      = TextWhite,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = "Michael Kwok36 Bulan ~ Laki-Laki",
                color    = TextGreenLight,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab row — "Hasil" is the active tab
        HasilTabRow(activeTab = 1)   // 0=Input, 1=Hasil, 2=Grafik TB/U, 3=Grafik BB/U

        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────
//  Tab Row
// ─────────────────────────────────────────────────────────────

@Composable
private fun HasilTabRow(activeTab: Int) {
    val tabs = listOf("Input", "Hasil", "Grafik\nTB/U", "Grafik\nBB/U")

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) TabActiveBg else TabIdleBg)
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    color      = if (isActive) TabActiveText else TabIdleText,
                    fontSize   = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 13.sp,
                    maxLines   = 2
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. SUMMARY CARD  (mint green, status normal)
// ════════════════════════════════════════════════════════════

@Composable
fun HasilSummaryCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SummaryCardBg)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkmark icon — circular
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SummaryTextDark.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Filled.CheckCircle,
                    contentDescription = "Normal",
                    tint               = SummaryTextDark,
                    modifier           = Modifier.size(28.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "Normal",
                    color      = SummaryTextDark,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = "Michael Kwok ~ 3 Apr 2026",
                    color    = SummaryTextDark,
                    fontSize = 12.sp
                )
                Text(
                    text     = "TB/U: -1.45          BB/U: -1.65",
                    color    = SummaryTextDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  3. INDIKATOR WHO CARD
// ════════════════════════════════════════════════════════════

@Composable
fun IndikatorWHOCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WHOCardBg)
            .border(1.dp, WHOCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Text(
                text       = "Indikator WHO",
                color      = TextWhite,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // TB/U block
            WHOIndicatorBlock(
                title      = "TB/U - Stunting",
                zScore     = "Z: -1.45",
                statusLabel = "Normal",
                progress   = 0.65f,
                description = "Tinggi badan normal."
            )

            // Divider between blocks
            Divider(color = WHOCardBorder, thickness = 0.8.dp)

            // BB/U block
            WHOIndicatorBlock(
                title      = "BB/U - Underweight",
                zScore     = "Z: -1.65",
                statusLabel = "Normal",
                progress   = 0.60f,
                description = "Berat badan normal."
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Reusable: single WHO indicator block
// ─────────────────────────────────────────────────────────────

@Composable
private fun WHOIndicatorBlock(
    title      : String,
    zScore     : String,
    statusLabel: String,
    progress   : Float,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // Title row + Normal pill
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = title,
                    color      = TextWhite,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text     = zScore,
                    color    = TextGrey,
                    fontSize = 12.sp
                )
            }

            // "Normal" pill label
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NormalLabelBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = statusLabel,
                    color      = NormalLabelText,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Progress bar
        LinearProgressIndicator(
            progress        = progress,
            modifier        = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color           = ProgressGreen,
            backgroundColor = ProgressTrackWHO
        )

        // Description
        Text(
            text     = description,
            color    = TextGrey,
            fontSize = 12.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun HasilScreenPreview() {
    HasilScreen()
}
