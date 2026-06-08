package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Local token (konsisten dengan HasilScreen.kt)
// ─────────────────────────────────────────────────────────────
private val TabActiveBg   = Color(0xFF1E6B4E)   // hijau gelap — tab "Input" aktif
private val TabActiveText = TextWhite
private val TabIdleBg     = Color(0xFF3A3A3A)   // abu-abu — tab non-aktif
private val TabIdleText   = Color(0xFFAAAAAA)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun PemeriksaanScreen(
    namaAnak       : String  = "Michael Kwok",
    onNavigateBack : () -> Unit = {},
    onNavigateToHasil: () -> Unit = {}
) {
    // ── State ────────────────────────────────────────────────
    var beratBadan    by remember { mutableStateOf("") }
    var tinggiBadan   by remember { mutableStateOf("") }
    var lingkarKepala by remember { mutableStateOf("") }
    var lingkarLengan by remember { mutableStateOf("") }
    var tanggal       by remember { mutableStateOf("26/05/2025") }
    var activeTab     by remember { mutableStateOf(0) }
    // ────────────────────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ①  Header baru — profil anak
        HeaderProfilAnak(
            onNavigateBack = onNavigateBack
        )

        // ②  Tab row baru
        PemeriksaanTabs(
            activeTab     = activeTab,
            onTabSelected = { activeTab = it }
        )

        // ③  Konten berdasarkan tab
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                // ── Tab 0: Input ────────────────────────────────────────
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            AntropometriCard(
                                beratBadan      = beratBadan,
                                onBeratChange   = { beratBadan = it },
                                tinggiBadan     = tinggiBadan,
                                onTinggiChange  = { tinggiBadan = it },
                                lingkarKepala   = lingkarKepala,
                                onKepalaChange  = { lingkarKepala = it },
                                lingkarLengan   = lingkarLengan,
                                onLenganChange  = { lingkarLengan = it },
                                modifier        = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        item {
                            TanggalCard(
                                tanggal         = tanggal,
                                onTanggalChange = { tanggal = it },
                                modifier        = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        item {
                            AnalisisDanSimpanButton(
                                onClick  = onNavigateToHasil,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }

                // ── Tab 1: Hasil ────────────────────────────────────────
                1 -> {
                    // TODO: Tampilkan ringkasan hasil di sini jika diperlukan
                }

                // ── Tab 2: Grafik TB/U ───────────────────────────────────
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        GrafikTBUContent(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // ── Tab 3: Grafik BB/U ───────────────────────────────────
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        GrafikBBUContent(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  HEADER PROFIL ANAK
// ════════════════════════════════════════════════════════════

@Composable
fun HeaderProfilAnak(
    subTitle      : String = "Michael Kwok36 Bulan ~ Laki-Laki",
    halamanJudul  : String = "Pemeriksaan",
    onNavigateBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Tombol kembali — outlined, isi nama anak
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
                Text(
                    text     = "Kembali",
                    color    = TextWhite,
                    fontSize = 13.sp
                )
            }

            // Judul halaman
            Text(
                text       = halamanJudul,
                color      = TextWhite,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // Sub-judul profil anak
            Text(
                text     = subTitle,
                color    = TextWhite.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PEMERIKSAAN TABS
// ════════════════════════════════════════════════════════════

@Composable
fun PemeriksaanTabs(
    activeTab    : Int = 0,           // 0=Input, 1=Hasil, 2=Grafik TB/U, 3=Grafik BB/U
    onTabSelected: (Int) -> Unit = {}
) {
    val tabs = listOf("Input", "Hasil", "Grafik\nTB/U", "Grafik\nBB/U")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) TabActiveBg else TabIdleBg)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    color      = if (isActive) TabActiveText else TabIdleText,
                    fontSize   = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 14.sp,
                    maxLines   = 2
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  CARD: DATA ANTROPOMETRI
// ════════════════════════════════════════════════════════════

@Composable
fun AntropometriCard(
    beratBadan      : String,
    onBeratChange   : (String) -> Unit,
    tinggiBadan     : String,
    onTinggiChange  : (String) -> Unit,
    lingkarKepala   : String,
    onKepalaChange  : (String) -> Unit,
    lingkarLengan   : String,
    onLenganChange  : (String) -> Unit,
    modifier        : Modifier = Modifier
) {
    PemeriksaanCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Card title
            Text(
                text       = "Data Antropometri",
                color      = TextWhite,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Row 1: Berat badan | Tinggi badan
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AntropometriField(
                    label        = "Berat badan (kg)",
                    value        = beratBadan,
                    onValueChange = onBeratChange,
                    modifier     = Modifier.weight(1f)
                )
                AntropometriField(
                    label        = "Tinggi badan (cm)",
                    value        = tinggiBadan,
                    onValueChange = onTinggiChange,
                    modifier     = Modifier.weight(1f)
                )
            }

            // Row 2: Lingkar kepala | Lingkar lengan
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AntropometriField(
                    label        = "Lingkar Kepala (cm)",
                    value        = lingkarKepala,
                    onValueChange = onKepalaChange,
                    modifier     = Modifier.weight(1f)
                )
                AntropometriField(
                    label        = "Lingkar lengan (cm)",
                    value        = lingkarLengan,
                    onValueChange = onLenganChange,
                    modifier     = Modifier.weight(1f)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  CARD: TANGGAL PEMERIKSAAN
// ════════════════════════════════════════════════════════════

@Composable
fun TanggalCard(
    tanggal         : String,
    onTanggalChange : (String) -> Unit,
    modifier        : Modifier = Modifier
) {
    PemeriksaanCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Label
            Text(
                text     = "Tanggal Pemeriksaan",
                color    = TextGrey,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            // Full-width date input
            PemeriksaanInputBox(
                value         = tanggal,
                onValueChange = onTanggalChange,
                placeholder   = "dd/mm/yyyy",
                keyboardType  = KeyboardType.Number,
                modifier      = Modifier.fillMaxWidth()
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  ANALISIS DAN SIMPAN BUTTON
// ════════════════════════════════════════════════════════════

@Composable
fun AnalisisDanSimpanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Assessment, null, tint = TextWhite, modifier = Modifier.size(20.dp))
            Text("Analisis Dan Simpan", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ════════════════════════════════════════════════════════════
//  REUSABLE: CARD CONTAINER
// ════════════════════════════════════════════════════════════

@Composable
fun PemeriksaanCardContainer(
    modifier  : Modifier = Modifier,
    content   : @Composable ColumnScope.() -> Unit
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            content  = content
        )
    }
}

// ════════════════════════════════════════════════════════════
//  REUSABLE: SINGLE ANTROPOMETRI FIELD (label + input)
// ════════════════════════════════════════════════════════════

@Composable
fun AntropometriField(
    label         : String,
    value         : String,
    onValueChange : (String) -> Unit,
    modifier      : Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text     = label,
            color    = TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
        PemeriksaanInputBox(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = "0.0",
            keyboardType  = KeyboardType.Decimal,
            modifier      = Modifier.fillMaxWidth()
        )
    }
}

// ════════════════════════════════════════════════════════════
//  REUSABLE: CUSTOM INPUT BOX
// ════════════════════════════════════════════════════════════

@Composable
fun PemeriksaanInputBox(
    value         : String,
    onValueChange : (String) -> Unit,
    placeholder   : String,
    modifier      : Modifier = Modifier,
    keyboardType  : KeyboardType = KeyboardType.Text
) {
    val inputBoxBg     = Color(0xFF3A3A3A)   // slightly lighter than card bg
    val inputBoxBorder = Color(0xFF555555)   // visible but subtle border

    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        singleLine    = true,
        cursorBrush   = SolidColor(AccentGreen),
        textStyle     = TextStyle(
            color      = TextWhite,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Normal
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(inputBoxBg)
            .border(
                width  = 1.dp,
                color  = inputBoxBorder,
                shape  = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text     = placeholder,
                        color    = Color(0xFF6B6B6B),
                        fontSize = 14.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

// ════════════════════════════════════════════════════════════
//  TAB 2: Grafik TB/U
// ════════════════════════════════════════════════════════════

@Composable
fun GrafikTBUContent(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card utama — area grafik
        GrafikCardContainer(height = 380.dp) {
            Text(
                text       = "Tinggi Badan / Usia (TB/U)",
                color      = TextWhite,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // TODO: Pasang komponen grafik (mis. MPAndroidChart / Vico) di sini
        }

        // Card bawah — area legend / keterangan
        GrafikCardContainer(height = 90.dp) {
            // TODO: Tambahkan legend warna & label di sini
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 3: Grafik BB/U  (placeholder identik, judul berbeda)
// ════════════════════════════════════════════════════════════

@Composable
fun GrafikBBUContent(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GrafikCardContainer(height = 380.dp) {
            Text(
                text       = "Berat Badan / Usia (BB/U)",
                color      = TextWhite,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // TODO: Pasang komponen grafik BB/U di sini
        }

        GrafikCardContainer(height = 90.dp) {
            // TODO: Tambahkan legend warna & label di sini
        }
    }
}

// ════════════════════════════════════════════════════════════
//  REUSABLE: Card container untuk grafik
// ════════════════════════════════════════════════════════════

@Composable
private fun GrafikCardContainer(
    height  : Dp,
    content : @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A2A))
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(content = content)
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212, showSystemUi = true)
@Composable
fun PemeriksaanScreenPreview() {
    PemeriksaanScreen()
}
