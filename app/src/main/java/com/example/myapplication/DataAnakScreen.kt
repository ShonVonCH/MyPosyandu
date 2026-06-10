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
//  Model
// ─────────────────────────────────────────────

data class AnakData(
    val nama         : String,
    val status       : String,
    val tinggiBadan  : Int,
    val beratBadan   : Int,
    val umurBulan    : Int,
    val tanggal      : String,
    val namaOrangTua : String = "-",
    val jenisKelamin : String = "-"
)

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

fun formatTanggalSingkat(tanggalLahir: String): String {
    return try {
        val input  = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val output = DateTimeFormatter.ofPattern("MMM yyyy")
        LocalDate.parse(tanggalLahir, input).format(output)
    } catch (e: Exception) {
        tanggalLahir
    }
}

// ════════════════════════════════════════════════════════════
//  SCREEN
// ════════════════════════════════════════════════════════════

@Composable
fun DataAnakScreen(
    viewModel     : FormDataViewModel? = null,
    anakViewModel : AnakViewModel?     = null,
    onTambahClick : () -> Unit         = {},
    onAnakClick   : (AnakData) -> Unit = {},
    onNavigateBack: () -> Unit         = {}
) {
    // Semua anak dari ViewModel (tidak ada dummy)
    val allAnakList = remember(viewModel?.registeredAnakList?.size) {
        viewModel?.registeredAnakList?.map { entry ->
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
    }

    // Auto-refresh saat hasil pemeriksaan berubah
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

            if (allAnakList.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = "Belum ada anak terdaftar.\nTekan \"Tambah Anak Baru\" untuk mulai.",
                            color     = TextGrey,
                            fontSize  = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                items(filteredList) { anak ->
                    val bbTerkini = hasilPemeriksaan[anak.nama]?.first
                    val tbTerkini = hasilPemeriksaan[anak.nama]?.second
                    val tampilBB  = bbTerkini?.toDoubleOrNull()?.toInt() ?: anak.beratBadan
                    val tampilTB  = tbTerkini?.toDoubleOrNull()?.toInt() ?: anak.tinggiBadan

                    AnakListItem(
                        data    = anak.copy(beratBadan = tampilBB, tinggiBadan = tampilTB),
                        onClick = { onAnakClick(anak) }
                    )
                }
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

// ════════════════════════════════════════════════════════════
//  HEADER
// ════════════════════════════════════════════════════════════

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

// ════════════════════════════════════════════════════════════
//  SEARCH BAR
// ════════════════════════════════════════════════════════════

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
        textStyle     = TextStyle(color = TextWhite, fontSize = 14.sp),
        modifier      = modifier
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
                        Text(text = "Cari nama balita..", color = TextGrey, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            }
        }
    )
}

// ════════════════════════════════════════════════════════════
//  LIST ITEM
// ════════════════════════════════════════════════════════════

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
                    text  = "TB: ${if (data.tinggiBadan > 0) "${data.tinggiBadan} cm" else "–"}  " +
                            "BB: ${if (data.beratBadan  > 0) "${data.beratBadan} kg"  else "–"}  " +
                            "Umur: ${data.umurBulan} bln",
                    color = TextGrey, fontSize = 12.sp
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

// ════════════════════════════════════════════════════════════
//  TOMBOL TAMBAH
// ════════════════════════════════════════════════════════════

@Composable
fun TambahAnakButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun DataAnakScreenPreview() {
    DataAnakScreen()
}