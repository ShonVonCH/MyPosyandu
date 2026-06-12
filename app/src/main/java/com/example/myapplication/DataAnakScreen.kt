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
import androidx.compose.ui.platform.LocalContext
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
    val id           : String,
    val nama         : String,
    val status       : String  = "-",
    val tinggiBadan  : Double  = 0.0,
    val beratBadan   : Double  = 0.0,
    val umurBulan    : Int     = 0,
    val tanggal      : String  = "-",   // tanggal lahir (format singkat)
    val namaOrangTua : String  = "-",
    val jenisKelamin : String  = "-"
)

fun hitungUmurBulan(tanggalLahir: String): Int {
    return try {
        val lahir = when {
            // format dd/MM/yyyy
            tanggalLahir.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                val parts = tanggalLahir.split("/")
                LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            // format yyyy-MM-dd
            else -> {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                LocalDate.parse(tanggalLahir, formatter)
            }
        }
        ChronoUnit.MONTHS.between(lahir, LocalDate.now()).toInt().coerceAtLeast(0)
    } catch (e: Exception) { 0 }
}

fun formatTanggalSingkat(tanggalLahir: String): String {
    return try {
        val lahir = when {
            tanggalLahir.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                val parts = tanggalLahir.split("/")
                LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            else -> LocalDate.parse(tanggalLahir, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        lahir.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    } catch (e: Exception) { tanggalLahir }
}

// ════════════════════════════════════════════════════════════
//  SCREEN
// ════════════════════════════════════════════════════════════

@Composable
fun DataAnakScreen(
    onTambahClick : () -> Unit         = {},
    onAnakClick   : (AnakData) -> Unit = {},
    onNavigateBack: () -> Unit         = {}
) {
    val context = LocalContext.current

    var allAnakList by remember { mutableStateOf<List<AnakData>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // Load semua anak dari SQLite + pemeriksaan terakhir
    LaunchedEffect(Unit) {
        val db   = DatabaseHelper(context).readableDatabase
        val repo = PemeriksaanRepository(context)

        // JOIN anak + ortu untuk dapat nama ortu
        val cursor = db.rawQuery(
            """
            SELECT a.${DatabaseHelper.COL_ANAK_ID},
                   a.${DatabaseHelper.COL_ANAK_NAMA},
                   a.${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                   a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN},
                   o.${DatabaseHelper.COL_ORTU_NAMA}
            FROM   ${DatabaseHelper.TABLE_ANAK} a
            LEFT JOIN ${DatabaseHelper.TABLE_ORTU} o
                   ON a.${DatabaseHelper.COL_ANAK_ORTU_ID} = o.${DatabaseHelper.COL_ORTU_ID}
            ORDER BY a.${DatabaseHelper.COL_ANAK_NAMA} ASC
            """.trimIndent(),
            null
        )

        val list = mutableListOf<AnakData>()
        while (cursor.moveToNext()) {
            val anakId   = cursor.getString(0) ?: continue
            val nama     = cursor.getString(1) ?: ""
            val tglLahir = cursor.getString(2) ?: ""
            val gender   = cursor.getString(3) ?: "-"
            val namaOrtu = cursor.getString(4) ?: "-"

            val pmrk = repo.getPemeriksaanTerakhir(anakId)

            list.add(AnakData(
                id           = anakId,
                nama         = nama,
                tinggiBadan  = pmrk?.tb ?: 0.0,
                beratBadan   = pmrk?.bb ?: 0.0,
                umurBulan    = hitungUmurBulan(tglLahir),
                tanggal      = formatTanggalSingkat(tglLahir),
                namaOrangTua = namaOrtu,
                jenisKelamin = when (gender.trim().lowercase()) {
                    "laki-laki", "l" -> "L"
                    "perempuan", "p" -> "P"
                    else             -> gender.ifBlank { "-" }
                }
            ))
        }
        cursor.close()
        db.close()
        allAnakList = list
    }

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
                            text       = "Belum ada anak terdaftar.\nTekan \"Tambah Anak Baru\" untuk mulai.",
                            color      = TextGrey,
                            fontSize   = 14.sp,
                            textAlign  = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                items(filteredList) { anak ->
                    AnakListItem(
                        data    = anak,
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
            Text(text = "Data Anak", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "$jumlahAnak Anak Terdaftar", color = TextGreenLight, fontSize = 14.sp)
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
                val bbStr = if (data.beratBadan > 0)  formatAngka(data.beratBadan) + " kg"  else "–"
                val tbStr = if (data.tinggiBadan > 0) formatAngka(data.tinggiBadan) + " cm" else "–"
                Text(
                    text  = "TB: $tbStr  BB: $bbStr  Umur: ${data.umurBulan} bln",
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

private fun formatAngka(nilai: Double): String {
    return if (nilai == kotlin.math.floor(nilai)) nilai.toInt().toString()
    else "%.1f".format(nilai)
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

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun DataAnakScreenPreview() {
    DataAnakScreen()
}