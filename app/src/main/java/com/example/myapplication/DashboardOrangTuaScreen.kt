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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DashHeaderBlue        = Color(0xFF1964A3)
private val DashBackgroundDark    = Color(0xFF121212)
private val DashSurfaceDark       = Color(0xFF2A2A2A)
private val DashSurfaceDarkBorder = Color(0xFF444444)
private val DashTextWhite         = Color(0xFFFFFFFF)
private val DashTextGrey          = Color(0xFF888888)
private val DashAvatarMint        = Color(0xFF98E6C8)

@Composable
fun DashboardOrangTuaScreen(
    username              : String         = "",
    onNavigateToDetailAnak: (String) -> Unit = {},  // param: anakId
    onNavigateToHome      : () -> Unit       = {},
    onNavigateToTicket    : () -> Unit       = {},
    onNavigateToFood      : () -> Unit       = {},
    onNavigateToProfile   : () -> Unit       = {}
) {
    val context = LocalContext.current

    var namaOrtu   by remember { mutableStateOf("") }
    var anakList   by remember { mutableStateOf<List<AnakData>>(emptyList()) }

    LaunchedEffect(username) {
        if (username.isBlank()) return@LaunchedEffect
        val db = DatabaseHelper(context).readableDatabase

        // Ambil data ortu berdasarkan username
        val cursorOrtu = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ORTU_ID}, ${DatabaseHelper.COL_ORTU_NAMA} " +
                    "FROM ${DatabaseHelper.TABLE_ORTU} " +
                    "WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ?",
            arrayOf(username)
        )
        var ortuId = ""
        if (cursorOrtu.moveToFirst()) {
            ortuId   = cursorOrtu.getString(0) ?: ""
            namaOrtu = cursorOrtu.getString(1) ?: ""
        }
        cursorOrtu.close()

        // Ambil anak milik ortu ini
        if (ortuId.isNotBlank()) {
            val cursorAnak = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ANAK_ID}, ${DatabaseHelper.COL_ANAK_NAMA}, " +
                        "${DatabaseHelper.COL_ANAK_TGL_LAHIR}, ${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} " +
                        "FROM ${DatabaseHelper.TABLE_ANAK} " +
                        "WHERE ${DatabaseHelper.COL_ANAK_ORTU_ID} = ?",
                arrayOf(ortuId)
            )
            val list = mutableListOf<AnakData>()
            while (cursorAnak.moveToNext()) {
                val id       = cursorAnak.getString(0) ?: continue
                val nama     = cursorAnak.getString(1) ?: ""
                val tglLahir = cursorAnak.getString(2) ?: ""
                val gender   = cursorAnak.getString(3) ?: "-"
                list.add(AnakData(
                    id           = id,
                    nama         = nama,
                    umurBulan    = hitungUmurBulan(tglLahir),
                    tanggal      = tglLahir,
                    namaOrangTua = namaOrtu,
                    jenisKelamin = when (gender.trim().lowercase()) {
                        "laki-laki", "l" -> "L"
                        "perempuan", "p" -> "P"
                        else             -> gender.ifBlank { "-" }
                    }
                ))
            }
            cursorAnak.close()
            anakList = list
        }
        db.close()
    }

    Scaffold(
        backgroundColor = DashBackgroundDark,
        bottomBar = {
            BottomNavBarOrtu(
                onHomeClick    = onNavigateToHome,
                onTicketClick  = onNavigateToTicket,
                onFoodClick    = onNavigateToFood,
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
            HeaderDashboardOrtu(
                namaOrangTua    = namaOrtu,
                jumlahAnakTotal = anakList.size
            )

            BannerJadwalOrtu()

            Text(
                text       = "Anak saya",
                color      = DashTextWhite,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (anakList.isEmpty()) {
                    Text(
                        text     = "Belum ada anak terdaftar",
                        color    = DashTextGrey,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    anakList.forEach { anak ->
                        CardAnakOrtu(
                            nama      = anak.nama,
                            gender    = anak.jenisKelamin,
                            tglLahir  = anak.tanggal,
                            onClick   = { onNavigateToDetailAnak(anak.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Bottom Nav ────────────────────────────────────────────────────────────────

@Composable
private fun BottomNavBarOrtu(
    onHomeClick   : () -> Unit,
    onTicketClick : () -> Unit,
    onFoodClick   : () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(DashSurfaceDark)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        NavIconOrtu(Icons.Outlined.Home,               isActive = true,  onClick = onHomeClick)
        NavIconOrtu(Icons.Outlined.ConfirmationNumber, isActive = false, onClick = onTicketClick)
        NavIconOrtu(Icons.Outlined.Restaurant,         isActive = false, onClick = onFoodClick)
        NavIconOrtu(Icons.Outlined.Person,             isActive = false, onClick = onProfileClick)
    }
}

@Composable
private fun NavIconOrtu(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    Icon(
        imageVector        = icon,
        contentDescription = null,
        tint               = if (isActive) DashHeaderBlue else DashTextWhite.copy(alpha = 0.5f),
        modifier           = Modifier.size(32.dp).clickable(onClick = onClick)
    )
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderDashboardOrtu(namaOrangTua: String, jumlahAnakTotal: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashHeaderBlue)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text       = "MyPosyandu",
            color      = DashTextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Selamat datang,", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(
                    text       = namaOrangTua.ifBlank { "Orang Tua" },
                    color      = DashTextWhite,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Posyandu Mawar, RW-04", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E86C1))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "$jumlahAnakTotal", color = DashTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "anak", color = DashTextWhite, fontSize = 12.sp)
            }
        }
    }
}

// ── Banner Jadwal ─────────────────────────────────────────────────────────────

@Composable
private fun BannerJadwalOrtu() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DashHeaderBlue)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, "Jadwal", tint = DashTextWhite, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Jadwal Posyandu berikutnya", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(text = "Senin, 2 Juni 2025 -- 08:00", color = DashTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "Balai RW 04 - Harap datang tepat waktu", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

// ── Card Anak ─────────────────────────────────────────────────────────────────

@Composable
private fun CardAnakOrtu(
    nama    : String,
    gender  : String,
    tglLahir: String,
    onClick : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashSurfaceDark)
            .border(1.dp, DashSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(DashAvatarMint))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nama,   color = DashTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(gender, color = DashTextGrey,  fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = DashTextGrey)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Tanggal lahir",          color = DashTextGrey,  fontSize = 13.sp)
        Text(tglLahir.ifBlank { "-" }, color = DashTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun DashboardOrangTuaScreenPreview() {
    DashboardOrangTuaScreen(username = "")
}