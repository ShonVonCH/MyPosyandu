package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────
// Model
// ─────────────────────────────────────────────

data class AnakData(
    val nama         : String,
    val status       : String,
    val tinggiBadan  : Int,      // nilai awal dari data dummy; bisa di-override oleh AnakViewModel
    val beratBadan   : Int,      // sama ↑
    val umurBulan    : Int,
    val tanggal      : String,
    val namaOrangTua : String = "-",
    val jenisKelamin : String = "-"   // "L" atau "P"
)

/**
 * Hitung umur dalam bulan dari tanggal lahir (format "dd/MM/yyyy") ke hari ini.
 * Kembalikan 0 kalau format tidak dikenali.
 */
fun hitungUmurBulan(tanggalLahir: String): Int {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val lahir     = LocalDate.parse(tanggalLahir, formatter)
        val sekarang  = LocalDate.now()
        ChronoUnit.MONTHS.between(lahir, sekarang).toInt().coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
}

/**
 * Format tanggal "dd/MM/yyyy" → "MMM yyyy", misal "16/01/2026" → "Jan 2026".
 * Kembalikan string asli kalau format tidak dikenali.
 */
fun formatTanggalSingkat(tanggalLahir: String): String {
    return try {
        val input  = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val output = DateTimeFormatter.ofPattern("MMM yyyy")
        LocalDate.parse(tanggalLahir, input).format(output)
    } catch (e: Exception) {
        tanggalLahir
    }
}

// ─────────────────────────────────────────────
// Dummy data — P/L hardcode, umur hardcode
// ─────────────────────────────────────────────

private val dummyAnakList = listOf(
    AnakData(nama = "Michael Kwok",  status = "Gizi Kurang", tinggiBadan = 85, beratBadan = 11, umurBulan = 36, tanggal = "Apr 2025", namaOrangTua = "Bapak Kwok",    jenisKelamin = "L"),
    AnakData(nama = "Siti Rahayu",   status = "Normal",      tinggiBadan = 78, beratBadan = 9,  umurBulan = 24, tanggal = "Mar 2025", namaOrangTua = "Ibu Rahayu",    jenisKelamin = "P"),
    AnakData(nama = "Budi Santoso",  status = "Stunting",    tinggiBadan = 70, beratBadan = 8,  umurBulan = 30, tanggal = "Feb 2025", namaOrangTua = "Bapak Santoso", jenisKelamin = "L"),
    AnakData(nama = "Dewi Permata",  status = "Normal",      tinggiBadan = 90, beratBadan = 13, umurBulan = 42, tanggal = "Apr 2025", namaOrangTua = "Ibu Permata",   jenisKelamin = "P"),
    AnakData(nama = "Rizky Pratama", status = "Gizi Buruk",  tinggiBadan = 65, beratBadan = 7,  umurBulan = 18, tanggal = "Jan 2025", namaOrangTua = "Bapak Pratama", jenisKelamin = "L"),
    AnakData(nama = "Aulia Safitri", status = "Normal",      tinggiBadan = 95, beratBadan = 14, umurBulan = 48, tanggal = "Mar 2025", namaOrangTua = "Ibu Safitri",   jenisKelamin = "P")
)

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun DataAnakScreen(
    anakList      : List<AnakData>     = dummyAnakList,
    viewModel     : FormDataViewModel? = null,
    anakViewModel : AnakViewModel?     = null,   // ← untuk baca TB/BB terkini
    onTambahClick : () -> Unit         = {},
    onAnakClick   : (AnakData) -> Unit = {},
    onNavigateBack: () -> Unit         = {}
) {
    // Gabungkan dummy list + anak baru dari viewModel
    val allAnakList = remember(viewModel?.registeredAnakList?.size) {
        val tambahan = viewModel?.registeredAnakList?.map { entry ->
            val umur = hitungUmurBulan(entry.formAnak.tanggalLahir)
            AnakData(
                nama         = entry.formAnak.namaLengkap,
                status       = "Baru Terdaftar",
                tinggiBadan  = 0,
                beratBadan   = 0,
                umurBulan    = umur,
                tanggal      = formatTanggalSingkat(entry.formAnak.tanggalLahir),
                namaOrangTua = entry.namaOrangTua.ifBlank { "-" },
                jenisKelamin = when (entry.formAnak.jenisKelamin.trim().lowercase()) {
                    "laki-laki", "l" -> "L"
                    "perempuan", "p" -> "P"
                    else             -> entry.formAnak.jenisKelamin.ifBlank { "-" }
                }
            )
        } ?: emptyList()
        anakList + tambahan
    }

    // Observasi map hasil pemeriksaan dari AnakViewModel agar list auto-refresh
    val hasilPemeriksaan by (anakViewModel?.hasilPemeriksaan
        ?: kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
            ).collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, allAnakList) {
        if (searchQuery.isBlank()) allAnakList
        else allAnakList.filter { it.nama.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        DataAnakHeader(
            jumlahAnak = allAnakList.size,
            onBack     = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SearchBarSection(
                    query         = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier      = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(filteredList) { anak ->
                // Override TB/BB dari hasil pemeriksaan terbaru (kalau ada)
                val bbTerkini = hasilPemeriksaan[anak.nama]?.first
                val tbTerkini = hasilPemeriksaan[anak.nama]?.second
                val tampilBB  = if (!bbTerkini.isNullOrBlank()) bbTerkini.toDoubleOrNull()?.toInt() ?: anak.beratBadan else anak.beratBadan
                val tampilTB  = if (!tbTerkini.isNullOrBlank()) tbTerkini.toDoubleOrNull()?.toInt() ?: anak.tinggiBadan else anak.tinggiBadan

                AnakListItem(
                    data    = anak.copy(beratBadan = tampilBB, tinggiBadan = tampilTB),
                    onClick = { onAnakClick(anak) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        TambahAnakButton(
            onClick  = onTambahClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }
}

// ─────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────

@Composable
fun DataAnakHeader(jumlahAnak: Int, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint               = TextWhite,
                    modifier           = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
                Text(
                    text       = "MyPosyandu",
                    color      = TextWhite,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text       = "Data Anak",
                color      = TextWhite,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text       = "$jumlahAnak Anak Terdaftar",
                color      = TextGreenLight,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ─────────────────────────────────────────────
// Search Bar
// ─────────────────────────────────────────────

@Composable
fun SearchBarSection(
    query        : String,
    onQueryChange: (String) -> Unit,
    modifier     : Modifier = Modifier
) {
    BasicTextField(
        value         = query,
        onValueChange = onQueryChange,
        singleLine    = true,
        cursorBrush   = SolidColor(AccentGreen),
        textStyle     = TextStyle(
            color    = TextWhite,
            fontSize = 14.sp
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector        = Icons.Filled.Search,
                    contentDescription = "Cari",
                    tint               = TextGrey,
                    modifier           = Modifier.size(20.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text     = "Cari nama balita..",
                            color    = TextGrey,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

// ─────────────────────────────────────────────
// List Item
// ─────────────────────────────────────────────

@Composable
fun AnakListItem(
    data   : AnakData,
    onClick: () -> Unit = {}
) {
    val (badgeBg, badgeTextColor) = when (data.jenisKelamin) {
        "L"  -> Color(0xFF1A3A5C) to Color(0xFF7EC8F0)
        "P"  -> Color(0xFF4A1A3C) to Color(0xFFF07EC8)
        else -> Color(0xFF2A2A2A) to TextGrey
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CircleMint)
            )

            Column(modifier = Modifier.weight(1f)) {

                // Nama + badge gender
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = data.nama,
                        color      = TextWhite,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (data.jenisKelamin != "-") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text       = data.jenisKelamin,
                                color      = badgeTextColor,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(text = data.status, color = TextGrey, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = "TB: ${if (data.tinggiBadan > 0) "${data.tinggiBadan} cm" else "–"}  " +
                            "BB: ${if (data.beratBadan  > 0) "${data.beratBadan} kg"  else "–"}  " +
                            "Umur: ${data.umurBulan} bln",
                    color    = TextGrey,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Ortu: ${data.namaOrangTua}", color = TextGrey, fontSize = 12.sp)
            }

            Text(
                text     = data.tanggal,
                color    = TextGrey,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }

    Divider(color = SurfaceDarkBorder, thickness = 0.8.dp)
}

// ─────────────────────────────────────────────
// Tombol Tambah
// ─────────────────────────────────────────────

@Composable
fun TambahAnakButton(
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundDark)
            .border(1.dp, TextWhite, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Person,
                contentDescription = "Tambah Anak",
                tint               = TextWhite,
                modifier           = Modifier.size(20.dp)
            )
            Text(
                text       = "Tambah Anak Baru",
                color      = TextWhite,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun DataAnakScreenPreview() {
    DataAnakScreen()
}