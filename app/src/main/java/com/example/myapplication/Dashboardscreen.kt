package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    totalAnak           : Int   = 0,
    anakHadir           : Int   = 0,
    anakHadirBulan      : Int   = 0,
    jadwalBulanIni      : Int   = 0,
    onNavigateToDataAnak: () -> Unit = {},
    onNavigateToLaporan : () -> Unit = {},
    onNavigateToPanggil : () -> Unit = {},  // ← TAMBAH INI
    onNavigateToUser    : () -> Unit = {}
) {
    val context = LocalContext.current

    var kaderInfo        by remember { mutableStateOf<KaderInfo?>(null) }
    var jadwalBerikutnya by remember { mutableStateOf<JadwalBerikutnya?>(null) }

    LaunchedEffect(Unit) {
        val repo         = DashboardKaderRepository(context)
        kaderInfo        = repo.getKaderInfo()
        jadwalBerikutnya = repo.getJadwalBerikutnya()
    }

    val targetHadir = if (jadwalBulanIni > 0) jadwalBulanIni * totalAnak else totalAnak
    val persenHadir = if (targetHadir > 0) anakHadirBulan.toFloat() / targetHadir else 0f
    val persenLabel = if (targetHadir > 0) "${(persenHadir * 100).toInt()}%" else "0%"

    Scaffold(
        backgroundColor = BackgroundDark,
        bottomBar = {
            DashboardBottomBar(
                onHomeClick    = { /* already on home */ },
                onPanggilClick = onNavigateToPanggil,  // ← Sambungkan ke sini
                onLaporanClick = onNavigateToLaporan,
                onUserClick    = onNavigateToUser,
                currentTab     = "home"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                HeaderSection(kaderInfo = kaderInfo)
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                ScheduleCard(
                    jadwal   = jadwalBerikutnya,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                StatsRow(
                    totalAnak = totalAnak,
                    anakHadir = anakHadir,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                ProgressCard(
                    attendancePercent = persenHadir,
                    percentLabel      = persenLabel,
                    anakHadirBulan    = anakHadirBulan,
                    targetHadir       = targetHadir,
                    jadwalBulanIni    = jadwalBulanIni,
                    totalAnak         = totalAnak,
                    modifier          = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                MenuGrid(
                    modifier        = Modifier.padding(horizontal = 16.dp),
                    onDataAnakClick = onNavigateToDataAnak,
                    onLaporanClick  = onNavigateToLaporan
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                ExportDatabaseButton(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  BOTTOM NAVIGATION BAR
// ════════════════════════════════════════════════════════════

@Composable
fun DashboardBottomBar(
    onHomeClick   : () -> Unit = {},
    onPanggilClick: () -> Unit = {},
    onLaporanClick: () -> Unit = {},
    onUserClick   : () -> Unit = {},
    currentTab    : String     = "home"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .border(
                width = 1.dp,
                color = Color(0xFF3A3A3C),
                shape = RoundedCornerShape(0.dp)
            )
            .navigationBarsPadding()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            DashBottomItem(
                icon       = Icons.Outlined.Home,
                label      = "Homepage",
                isSelected = currentTab == "home",
                onClick    = onHomeClick
            )
            DashBottomItem(
                icon       = Icons.Outlined.Notifications,  // ← Icon untuk Panggil
                label      = "Panggil",
                isSelected = currentTab == "panggil",
                onClick    = onPanggilClick
            )
            DashBottomItem(
                icon       = Icons.Outlined.Person,
                label      = "User",
                isSelected = currentTab == "user",
                onClick    = onUserClick
            )
        }
    }
}

@Composable
private fun DashBottomItem(
    icon      : ImageVector,   // ← Ganti dari String (emoji) ke ImageVector
    label     : String,
    isSelected: Boolean,
    onClick   : () -> Unit
) {
    val labelColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFF9E9E9E)

    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(                      // ← Ganti dari Text(emoji) ke Icon()
            imageVector = icon,
            contentDescription = label,
            tint = labelColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text       = label,
            color      = labelColor,
            fontSize   = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER SECTION
// ════════════════════════════════════════════════════════════

@Composable
fun HeaderSection(kaderInfo: KaderInfo? = null) {
    val nama         = kaderInfo?.nama         ?: "-"
    val posyanduNama = kaderInfo?.posyanduNama  ?: "-"
    val kelurahan    = kaderInfo?.kelurahan     ?: ""
    val alamat       = kaderInfo?.alamat        ?: ""

    val alamatLengkap = buildString {
        if (posyanduNama.isNotBlank()) append(posyanduNama)
        if (kelurahan.isNotBlank())    append(", Kel. $kelurahan")
        if (alamat.isNotBlank())       append(", $alamat")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = HeaderGreen)
            .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text       = "MyPosyandu",
                color      = TextWhite,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text       = "Selamat datang,",
                color      = TextGreenLight,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text       = nama,
                color      = TextWhite,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text       = alamatLengkap,
                color      = TextGreenLight,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. SCHEDULE CARD
// ════════════════════════════════════════════════════════════

@Composable
fun ScheduleCard(
    jadwal  : JadwalBerikutnya? = null,
    modifier: Modifier          = Modifier
) {
    val tanggalFormatted = jadwal?.tanggal?.let { raw ->
        try {
            val sdf    = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
            val date   = sdf.parse(raw)
            val outSdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            date?.let { outSdf.format(it) }
        } catch (e: Exception) { raw }
    }

    val jamMulai   = jadwal?.jamMulai?.take(5)   ?: "-"
    val jamSelesai = jadwal?.jamSelesai?.take(5) ?: "-"

    val jadwalText = if (jadwal != null && tanggalFormatted != null)
        "$tanggalFormatted • $jamMulai–$jamSelesai"
    else
        "Belum ada jadwal terjadwal"

    val lokasiText = jadwal?.lokasi ?: ""

    Card(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        backgroundColor = ScheduleCardGreen,
        elevation       = 4.dp
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CircleMint)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text       = if (jadwal != null) "Jadwal Posyandu berikutnya" else "Jadwal Posyandu",
                    color      = TextGreenLight,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = jadwalText,
                    color      = TextWhite,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (lokasiText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text       = lokasiText,
                        color      = TextGreenLight,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  3. STATS ROW
// ════════════════════════════════════════════════════════════

@Composable
fun StatsRow(
    totalAnak: Int      = 0,
    anakHadir: Int      = 0,
    modifier : Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label      = "Total Anak",
            value      = totalAnak.toString(),
            valueColor = TextWhite,
            modifier   = Modifier.weight(1f)
        )
        StatCard(
            label      = "Anak yang hadir",
            value      = anakHadir.toString(),
            valueColor = AccentGreen,
            modifier   = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label     : String,
    value     : String,
    valueColor: Color,
    modifier  : Modifier = Modifier
) {
    Card(
        modifier        = modifier,
        shape           = RoundedCornerShape(16.dp),
        backgroundColor = SurfaceDark,
        elevation       = 0.dp
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = label,
                color      = TextGrey,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text       = value,
                color      = valueColor,
                fontSize   = 40.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 44.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  4. ATTENDANCE PROGRESS CARD
// ════════════════════════════════════════════════════════════

@Composable
fun ProgressCard(
    attendancePercent: Float,
    percentLabel     : String,
    anakHadirBulan   : Int,
    targetHadir      : Int,
    jadwalBulanIni   : Int,
    totalAnak        : Int,
    modifier         : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                color = SurfaceDarkBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Kehadiran bulan ini",
                    color      = TextWhite,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text       = percentLabel,
                    color      = AccentGreenBright,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("$anakHadirBulan anak hadir")
                    if (jadwalBulanIni > 0) {
                        append(" dari $targetHadir target ($jadwalBulanIni jadwal × $totalAnak anak)")
                    } else {
                        append(" dari $targetHadir anak")
                    }
                },
                color    = TextGrey,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress        = attendancePercent,
                modifier        = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color           = AccentGreen,
                backgroundColor = ProgressTrack
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  5. MENU GRID
// ════════════════════════════════════════════════════════════

data class MenuItemData(
    val label      : String,
    val bgColor    : Color,
    val borderColor: Color,
    val iconColor  : Color,
    val textColor  : Color,
    val iconEmoji  : String
)

@Composable
fun MenuGrid(
    modifier       : Modifier  = Modifier,
    onDataAnakClick: () -> Unit = {},
    onLaporanClick : () -> Unit = {}
) {
    val items = listOf(
        MenuItemData("Data Anak", MenuBlueBg, MenuBlueBorder, MenuBlueIcon, MenuBlueText, "📋"),
        MenuItemData("Laporan",   MenuPinkBg, MenuPinkBorder, MenuPinkIcon, MenuPinkText, "📝")
    )
    val clicks = listOf(onDataAnakClick, onLaporanClick)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEachIndexed { index, data ->
                MenuItemButton(
                    data     = data,
                    onClick  = clicks[index],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MenuItemButton(
    data    : MenuItemData,
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier         = modifier
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(data.bgColor)
            .border(
                width = 1.5.dp,
                color = data.borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = data.iconEmoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text       = data.label,
                color      = data.textColor,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(
        totalAnak = 10,
        anakHadir = 7
    )
}