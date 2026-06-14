package com.example.myapplication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ── Warna lokal ───────────────────────────────────────────────────────────────
private val LaporanBg           = Color(0xFF1C1C1E)
private val LaporanCard         = Color(0xFF2C2C2E)
private val LaporanBorder       = Color(0xFF3A3A3C)
private val LaporanGreen        = Color(0xFF2E7D32)
private val LaporanGreenBright  = Color(0xFF4CAF50)
private val LaporanRed          = Color(0xFFD32F2F)
private val LaporanWhite        = Color(0xFFFFFFFF)
private val LaporanGrey         = Color(0xFF9E9E9E)
private val LaporanHeaderGreen  = Color(0xFF1B5E20)
private val LaporanBottomBarBg  = Color(0xFF1C1C1E)
private val LaporanBottomBorder = Color(0xFF3A3A3C)

private val cakupanOptions = listOf("Semua Balita", "Balita Laki-laki", "Balita Perempuan")

// ── Custom Month-Year Picker Dialog ──────────────────────────────────────────
@Composable
fun MonthYearPickerDialog(
    initialYear  : Int,
    initialMonth : Int,
    onDismiss    : () -> Unit,
    onConfirm    : (year: Int, month: Int) -> Unit
) {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    var selectedYear  by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(LaporanCard)
                .border(1.dp, LaporanBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text       = "Pilih Bulan & Tahun",
                    color      = LaporanWhite,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LaporanBg)
                            .clickable { selectedYear-- }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("‹", color = LaporanWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold) }

                    Text("$selectedYear", color = LaporanWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LaporanBg)
                            .clickable { selectedYear++ }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("›", color = LaporanWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }

                Spacer(Modifier.height(16.dp))

                val rows = months.chunked(3)
                rows.forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, monthLabel ->
                            val monthIndex = rowIndex * 3 + colIndex
                            val isSelected = monthIndex == selectedMonth

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) LaporanGreenBright else LaporanBg)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) LaporanGreenBright else LaporanBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedMonth = monthIndex }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = monthLabel,
                                    color      = LaporanWhite,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign  = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (rowIndex < rows.size - 1) Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor    = LaporanGrey,
                            backgroundColor = Color.Transparent
                        )
                    ) { Text("Batal", fontSize = 14.sp) }

                    Button(
                        onClick  = { onConfirm(selectedYear, selectedMonth) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(backgroundColor = LaporanGreenBright)
                    ) { Text("Pilih", color = LaporanWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LaporanScreen(
    onNavigateBack     : () -> Unit = {},
    onNavigateToHome   : () -> Unit = {},
    onNavigateToPanggil: () -> Unit = {},
    onNavigateToUser   : () -> Unit = {}
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val displayFmt     = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))

    var rawDari by remember {
        mutableStateOf(Calendar.getInstance().apply { set(2025, Calendar.JANUARY, 1) })
    }
    var rawSampai by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 1)
            add(Calendar.MONTH, 3)
        })
    }
    var tanggalDari   by remember { mutableStateOf(displayFmt.format(rawDari.time)) }
    var tanggalSampai by remember { mutableStateOf(displayFmt.format(rawSampai.time)) }

    var showPickerFor by remember { mutableStateOf<String?>(null) }

    var cakupan       by remember { mutableStateOf("Semua Balita") }
    var expandCakupan by remember { mutableStateOf(false) }
    var isLoading     by remember { mutableStateOf(false) }

    // Status sync untuk ditampilkan
    var syncStatus    by remember { mutableStateOf("") }
    var isSuccess     by remember { mutableStateOf(false) }

    if (showPickerFor != null) {
        val currentCal = if (showPickerFor == "dari") rawDari else rawSampai
        MonthYearPickerDialog(
            initialYear  = currentCal.get(Calendar.YEAR),
            initialMonth = currentCal.get(Calendar.MONTH),
            onDismiss    = { showPickerFor = null },
            onConfirm    = { year, month ->
                val picked = Calendar.getInstance().apply { set(year, month, 1) }

                if (showPickerFor == "dari") {
                    rawDari = picked
                    tanggalDari = displayFmt.format(picked.time)

                    if (!rawSampai.after(picked)) {
                        rawSampai = Calendar.getInstance().apply {
                            set(year, month, 1)
                            add(Calendar.MONTH, 3)
                        }
                        tanggalSampai = displayFmt.format(rawSampai.time)
                    }
                } else {
                    rawSampai = picked
                    tanggalSampai = displayFmt.format(picked.time)
                }
                showPickerFor = null
            }
        )
    }

    // ── Handler kirim ─────────────────────────────────────────────────────────
    fun kirimLaporan() {
        if (rawDari.after(rawSampai)) {
            Toast.makeText(context, "Tanggal 'Dari' tidak boleh setelah 'Sampai'.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        syncStatus = ""
        coroutineScope.launch {
            val repo = LaporanRepository(context)
            val result = withContext(Dispatchers.IO) {
                repo.buatDanSimpanLaporan(
                    dariCal   = rawDari,
                    sampaiCal = rawSampai,
                    cakupan   = cakupan
                )
            }
            isLoading = false

            syncStatus = result.syncMessage
            isSuccess = result.syncSuccess

            if (result.laporanId != null) {
                Toast.makeText(context, "✅ Laporan disimpan! ${result.syncMessage}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "❌ Gagal: ${result.syncMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        backgroundColor = LaporanBg,
        bottomBar = {
            LaporanBottomBar(
                onHomeClick    = onNavigateToHome,
                onPanggilClick = onNavigateToPanggil,
                onUserClick    = onNavigateToUser,
                currentTab     = "laporan"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LaporanHeaderGreen)
                    .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 20.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onNavigateBack() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowBack, "Kembali", tint = LaporanWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kembali", color = LaporanWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Export Laporan", color = LaporanWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Unduh dan kirim data laporan", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            // Konten
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kartu Pilih Periode
                item {
                    LaporanCardContainer(title = "Pilih Periode") {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dari", color = LaporanGrey, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                DatePickerField(value = tanggalDari) { showPickerFor = "dari" }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sampai", color = LaporanGrey, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                DatePickerField(value = tanggalSampai) { showPickerFor = "sampai" }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text("Cakupan data", color = LaporanGrey, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))

                        Box {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, LaporanBorder, RoundedCornerShape(8.dp))
                                    .background(LaporanBg)
                                    .clickable { expandCakupan = !expandCakupan }
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(cakupan, color = LaporanWhite, fontSize = 14.sp)
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = LaporanGrey)
                                }
                            }
                            DropdownMenu(
                                expanded         = expandCakupan,
                                onDismissRequest = { expandCakupan = false },
                                modifier         = Modifier.fillMaxWidth(0.85f).background(LaporanCard)
                            ) {
                                cakupanOptions.forEach { option ->
                                    DropdownMenuItem(onClick = { cakupan = option; expandCakupan = false }) {
                                        Text(option, color = LaporanWhite, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Status sync
                if (syncStatus.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSuccess) Color(0xFF1A3A2A) else Color(0xFF3A1A1A))
                                .padding(16.dp)
                        ) {
                            Text(
                                text      = syncStatus,
                                color     = if (isSuccess) Color(0xFF6FDDAA) else Color(0xFFDD6F6F),
                                fontSize  = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Kartu Format Ekspor
                item {
                    LaporanCardContainer(title = "Format Ekspor") {
                        Button(
                            onClick  = { /* TODO: export PDF */ },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(backgroundColor = LaporanRed)
                        ) {
                            Text("Laporan PDF lengkap", color = LaporanWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick  = { /* TODO: export Excel */ },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(backgroundColor = LaporanGreen)
                        ) {
                            Text("Data Excel (.xlsx)", color = LaporanWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick  = { /* TODO: export CSV */ },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(10.dp),
                            border   = ButtonDefaults.outlinedBorder.copy(width = 1.5.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor    = LaporanWhite,
                                backgroundColor = Color.Transparent
                            )
                        ) {
                            Text("Data CSV", color = LaporanWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick  = { kirimLaporan() },
                            enabled  = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                backgroundColor         = LaporanGreenBright,
                                disabledBackgroundColor = LaporanGreenBright.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color       = LaporanWhite,
                                    modifier    = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Menyimpan & Sync…", color = LaporanWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("📤  Kirim Laporan", color = LaporanWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Kartu container ───────────────────────────────────────────────────────────
@Composable
private fun LaporanCardContainer(
    title  : String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LaporanCard)
            .border(1.dp, LaporanBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, color = LaporanWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

// ── Field tanggal ─────────────────────────────────────────────────────────────
@Composable
private fun DatePickerField(value: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, LaporanBorder, RoundedCornerShape(8.dp))
            .background(LaporanBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(value, color = LaporanWhite, fontSize = 13.sp)
            Icon(Icons.Default.CalendarToday, "Pilih tanggal", tint = LaporanGrey, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────
@Composable
fun LaporanBottomBar(
    onHomeClick   : () -> Unit = {},
    onPanggilClick: () -> Unit = {},
    onUserClick   : () -> Unit = {},
    currentTab    : String     = "laporan"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LaporanBottomBarBg)
            .border(1.dp, LaporanBottomBorder, RoundedCornerShape(0.dp))
            .navigationBarsPadding()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            LaporanBottomItem(
                icon       = Icons.Outlined.Home,
                label      = "Homepage",
                isSelected = currentTab == "home",
                onClick    = onHomeClick
            )
            LaporanBottomItem(
                icon       = Icons.Outlined.Notifications,
                label      = "Panggil",
                isSelected = currentTab == "panggil",
                onClick    = onPanggilClick
            )
            LaporanBottomItem(
                icon       = Icons.Outlined.Person,
                label      = "User",
                isSelected = currentTab == "user",
                onClick    = onUserClick
            )
        }
    }
}

@Composable
private fun LaporanBottomItem(
    icon      : ImageVector,   // ← Ganti dari String
    label     : String,
    isSelected: Boolean,
    onClick   : () -> Unit
) {
    val labelColor = if (isSelected) LaporanGreenBright else LaporanGrey

    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(                      // ← Ganti dari Text(emoji)
            imageVector = icon,
            contentDescription = label,
            tint = labelColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text       = label,
            color      = labelColor,
            fontSize   = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}