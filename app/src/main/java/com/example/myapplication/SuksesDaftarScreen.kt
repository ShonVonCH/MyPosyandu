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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
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
//  Local design tokens
// ─────────────────────────────────────────────────────────────
private val SuccessCircleBg     = Color(0xFFB8EDD8)   // mint large circle
private val SuccessIconColor    = Color(0xFF1E7A55)   // dark green check inside circle
private val DataCardBg          = Color(0xFF2A2A2A)
private val DataCardBorder      = Color(0xFF444444)
private val InnerBoxBg          = Color(0xFF1A1A1A)   // very dark connected-status box
private val ConnectedIconColor  = Color(0xFF2E9B6E)   // green circle check outline
private val AvatarGreyLight     = Color(0xFFCCCCCC)
private val UpdateCardBg        = Color(0xFFBBCFEF)   // cornflower blue card
private val UpdateNavyDark      = Color(0xFF1A3A6E)   // all text inside blue card
private val UpdateDividerColor  = Color(0xFF1A3A6E).copy(alpha = 0.25f)
private val SubtextGrey         = Color(0xFFAAAAAA)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun SuksesDaftarScreen(
    onNavigateBack  : () -> Unit = {},
    onSelesaiClicked: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header
        SuksesHeader(onNavigateBack = onNavigateBack)

        // 2. Status sukses — ikon + teks
        SuksesStatus()

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Card data anak terhubung
        AnakTerhubungCard(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Card update akun orang tua
        UpdateOrangTuaCard(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Tombol selesai
        SelesaiButton(
            onClick  = onSelesaiClicked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun SuksesHeader(onNavigateBack: () -> Unit) {
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
//  2. STATUS SUKSES
// ════════════════════════════════════════════════════════════

@Composable
fun SuksesStatus() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Large mint circle with outlined check icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SuccessCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.CheckCircle,
                contentDescription = "Sukses",
                tint               = SuccessIconColor,
                modifier           = Modifier.size(52.dp)
            )
        }

        // Success description text
        Text(
            text       = "Budi Prasetyo telah terdaftar dan sudah terhubung ke akun orang tua",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  3. CARD: ANAK TERHUBUNG
// ════════════════════════════════════════════════════════════

@Composable
fun AnakTerhubungCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DataCardBg)
            .border(1.dp, DataCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Profile row: avatar + name + info
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AvatarGreyLight)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = "Budi Prasetyo",
                        color      = TextWhite,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "2 bulan -  Laki-laki",
                        color    = SubtextGrey,
                        fontSize = 13.sp
                    )
                }
            }

            // Connected status inner box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(InnerBoxBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text     = "Terhubung ke akun orang tua",
                            color    = SubtextGrey,
                            fontSize = 12.sp
                        )
                        Text(
                            text       = "Rina Susanti - @ortu_rina",
                            color      = TextWhite,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Green outlined check circle
                    Icon(
                        imageVector        = Icons.Outlined.CheckCircle,
                        contentDescription = "Terhubung",
                        tint               = ConnectedIconColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  4. CARD: UPDATE AKUN ORANG TUA
// ════════════════════════════════════════════════════════════

@Composable
fun UpdateOrangTuaCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UpdateCardBg)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Card title
            Text(
                text       = "Update di akun orang tua",
                color      = UpdateNavyDark,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )

            // Profile row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AvatarGreyLight)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = "Rina Susanti(@ortu_rina)",
                        color      = UpdateNavyDark,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "Kini memiliki 4 anak terdaftar",
                        color    = UpdateNavyDark,
                        fontSize = 13.sp
                    )
                }
            }

            // Divider — navy semi-transparent
            Divider(color = UpdateDividerColor, thickness = 1.dp)

            // Info text
            Text(
                text       = "Budi Prasetyo sudah muncul di halaman anak-anak saat orang tua login berikutnya.",
                color      = UpdateNavyDark,
                fontSize   = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  5. TOMBOL SELESAI
// ════════════════════════════════════════════════════════════

@Composable
fun SelesaiButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            text       = "Selesai",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun SuksesDaftarScreenPreview() {
    SuksesDaftarScreen()
}
