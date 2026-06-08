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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
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
private val HeaderBlueBg        = Color(0xFF1565C0)   // header utama biru
private val BadgeBlueDark       = Color(0xFF0D47A1)   // kotak badge "4 anak"
private val ScheduleBannerBg    = Color(0xFF1976D2)   // banner jadwal biru terang
private val ScheduleSubText     = Color(0xFFBBCFEF)   // teks transparan dalam banner
private val CardBg              = Color(0xFF2A2A2A)
private val CardBorder          = Color(0xFF3A3A3A)
private val NewBadgeBg          = Color(0xFFD32F2F)   // merah badge "Baru"
private val NewInfoBoxBg        = Color(0xFFBBCFEF)   // biru pastel info box
private val NewInfoBoxText      = Color(0xFF1A3A6E)   // navy teks info box
private val SubtextGrey         = Color(0xFFAAAAAA)
private val LastCheckLabelColor = Color(0xFF888888)

// ─────────────────────────────────────────────────────────────
//  Data model
// ─────────────────────────────────────────────────────────────

data class AnakOrangTua(
    val nama          : String,
    val usiaBulan     : Int,
    val jenisKelamin  : String,
    val isNew         : Boolean = false,
    val terakhirTanggal: String? = null,   // null jika isNew
    val terakhirTB    : String? = null
)

private val dummyAnakList = listOf(
    AnakOrangTua("Budi Prasetyo",  2,  "Laki-laki",  isNew = true),
    AnakOrangTua("Nabila Rahmah",  14, "Perempuan",  terakhirTanggal = "25 Apr 2025", terakhirTB = "65 cm"),
    AnakOrangTua("Rizki Pratama",  37, "Laki-laki",  terakhirTanggal = "26 Apr 2025", terakhirTB = "93 cm")
)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun DashboardOrangTuaScreen(
    onNavigateToDetailAnak: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header branding + greeting
        OrangTuaHeader(jumlahAnak = dummyAnakList.size)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Banner jadwal
        JadwalBanner(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Section title
        Text(
            text       = "Anak saya",
            color      = TextWhite,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Child list
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            dummyAnakList.forEach { anak ->
                ChildCard(
                    data    = anak,
                    onClick = { onNavigateToDetailAnak(anak.nama) }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun OrangTuaHeader(jumlahAnak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBlueBg)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
    ) {
        Column {
            // App title — centered
            Text(
                text       = "MyPosyandu",
                color      = TextWhite,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Greeting row + badge
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Greeting text column
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text     = "Selamat datang,",
                        color    = TextWhite,
                        fontSize = 13.sp
                    )
                    Text(
                        text       = "Pak Sean Albert",
                        color      = TextWhite,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "Posyandu Mawar, RW-04",
                        color    = TextWhite.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }

                // Badge: jumlah anak
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BadgeBlueDark)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "$jumlahAnak",
                            color      = TextWhite,
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 34.sp
                        )
                        Text(
                            text     = "anak",
                            color    = TextWhite,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. JADWAL BANNER
// ════════════════════════════════════════════════════════════

@Composable
fun JadwalBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScheduleBannerBg)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.CalendarMonth,
                contentDescription = "Jadwal",
                tint               = TextWhite,
                modifier           = Modifier.size(26.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text     = "Jadwal Posyandu berikutnya",
                    color    = ScheduleSubText,
                    fontSize = 12.sp
                )
                Text(
                    text       = "Senin, 2 Juni 2025 -- 08:00",
                    color      = TextWhite,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = "Balai RW 04 - Harap datang tepat waktu",
                    color    = ScheduleSubText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  3. CHILD CARD  (reusable, handles "Baru" vs normal state)
// ════════════════════════════════════════════════════════════

@Composable
fun ChildCard(
    data   : AnakOrangTua,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Main profile row ──────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar mint circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CircleMint)
                )

                // Name + age/gender
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text       = data.nama,
                            color      = TextWhite,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Badge "Baru" — hanya tampil jika isNew
                        if (data.isNew) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NewBadgeBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text       = "Baru",
                                    color      = TextWhite,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text     = "${data.usiaBulan} bulan - ${data.jenisKelamin}",
                        color    = SubtextGrey,
                        fontSize = 14.sp
                    )
                }

                // Chevron right
                Icon(
                    imageVector        = Icons.Filled.ChevronRight,
                    contentDescription = "Detail",
                    tint               = SubtextGrey,
                    modifier           = Modifier.size(22.dp)
                )
            }

            // ── Conditional content ──────────────────────────
            if (data.isNew) {
                // Info box biru — anak baru belum diperiksa
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NewInfoBoxBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text     = "Baru didaftarkan kader. Belum ada pemeriksaan.",
                        color    = NewInfoBoxText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            } else {
                // Terakhir diperiksa info — anak normal
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text     = "Terakhir diperiksa",
                        color    = LastCheckLabelColor,
                        fontSize = 13.sp
                    )
                    Text(
                        text       = "${data.terakhirTanggal} - TB ${data.terakhirTB}",
                        color      = TextWhite,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun DashboardOrangTuaScreenPreview() {
    DashboardOrangTuaScreen()
}
