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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
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
private val CardBg              = Color(0xFF2A2A2A)
private val CardBorder          = Color(0xFF444444)
private val DividerColor        = Color(0xFF3A3A3A)

// Badge colours
private val BadgeLengkapBg      = Color(0xFF3DD68C)
private val BadgeLengkapText    = Color(0xFF0D4A2A)
private val BadgeTerlambatBg    = Color(0xFFC62828)
private val BadgeTerlambatText  = Color(0xFFFFFFFF)

// Vaccine icon colours
private val IconCheckBg         = Color(0xFF1E7A55)   // dark green circle
private val IconLateBg          = Color(0xFFC62828)   // red circle

// Vaccine text colours
private val VaccineInfoGreen    = Color(0xFF7ECFB0)   // muted mint for "10 Jul 2021 - Posyandu"
private val VaccineInfoRed      = Color(0xFFEF5350)   // red for "Terlambat - ..."

// Tab colours
private val TabActiveBg         = Color(0xFF1E6E55)
private val TabActiveText       = TextWhite
private val TabIdleBg           = Color(0xFFEEEEEE)
private val TabIdleText         = Color(0xFF1A1A1A)

// ─────────────────────────────────────────────────────────────
//  Data model
// ─────────────────────────────────────────────────────────────

data class VaccineData(
    val name  : String,
    val info  : String,
    val isLate: Boolean = false
)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiScreen(onNavigateBack: () -> Unit = {}) {

    var activeTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header hijau
        ImunisasiHeader(onNavigateBack = onNavigateBack)

        // Tab row
        ImunisasiTabs(
            activeTab     = activeTab,
            onTabSelected = { activeTab = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Dummy data cards ──────────────────────────────────

        // 1. Lahir — Lengkap — 1 vaksin
        ImunisasiCard(
            usia     = "Lahir",
            status   = "Lengkap",
            vaccines = listOf(
                VaccineData("Hepatitis B (HB-0)", "10 Jul 2021 - Posyandu")
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 1 Bulan — Lengkap — 2 vaksin
        ImunisasiCard(
            usia     = "1 Bulan",
            status   = "Lengkap",
            vaccines = listOf(
                VaccineData("BCG",     "10 Jul 2021 - Posyandu"),
                VaccineData("Polio 1", "10 Jul 2021 - Posyandu")
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. 2 Bulan — Terlambat — 3 vaksin (1 terlambat)
        ImunisasiCard(
            usia     = "2 Bulan",
            status   = "Terlambat",
            vaccines = listOf(
                VaccineData("DPT-HB-Hib 1", "10 Jul 2021 - Posyandu"),
                VaccineData("Polio 2",       "10 Jul 2021 - Posyandu"),
                VaccineData("IPV 1",         "Terlambat - 15 Mar 2023", isLate = true)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Back button
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

            // Title
            Text(
                text       = "Imunisasi",
                color      = TextWhite,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Subtitle
            Text(
                text     = "Michael Kwok ~ 36 Bulan",
                color    = TextWhite.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. TAB ROW
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiTabs(
    activeTab    : Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val tabs = listOf("Status", "Akan Datang", "Catat")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) TabActiveBg else TabIdleBg)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    color      = if (isActive) TabActiveText else TabIdleText,
                    fontSize   = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  3. IMUNISASI CARD  (reusable per usia)
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiCard(
    usia    : String,
    status  : String,
    vaccines: List<VaccineData>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Card header: usia label + status badge
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = usia,
                    color      = TextWhite,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = status)
            }

            // Vaccine list with dividers
            Column {
                vaccines.forEachIndexed { index, vaccine ->
                    VaccineItem(
                        name   = vaccine.name,
                        info   = vaccine.info,
                        isLate = vaccine.isLate
                    )
                    if (index < vaccines.lastIndex) {
                        Divider(
                            color     = DividerColor,
                            thickness = 0.8.dp,
                            modifier  = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Status Badge  ("Lengkap" / "Terlambat")
// ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: String) {
    val isLate    = status.equals("Terlambat", ignoreCase = true)
    val bgColor   = if (isLate) BadgeTerlambatBg   else BadgeLengkapBg
    val textColor = if (isLate) BadgeTerlambatText  else BadgeLengkapText

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text       = status,
            color      = textColor,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  VaccineItem  (single row inside a card)
// ─────────────────────────────────────────────────────────────

@Composable
fun VaccineItem(
    name  : String,
    info  : String,
    isLate: Boolean = false
) {
    val iconBg    = if (isLate) IconLateBg    else IconCheckBg
    val infoColor = if (isLate) VaccineInfoRed else VaccineInfoGreen

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Circular icon: check (green) or X (red)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (isLate) Icons.Filled.Close else Icons.Filled.Check,
                contentDescription = if (isLate) "Terlambat" else "Selesai",
                tint               = TextWhite,
                modifier           = Modifier.size(18.dp)
            )
        }

        // Name + info
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = name,
                color      = TextWhite,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = info,
                color    = infoColor,
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun ImunisasiScreenPreview() {
    ImunisasiScreen()
}
