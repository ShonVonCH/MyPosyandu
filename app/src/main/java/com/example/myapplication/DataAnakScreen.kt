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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════
//  DATA MODEL
// ════════════════════════════════════════════════════════════

data class AnakData(
    val nama       : String,
    val status     : String,
    val tinggiBadan: Int,
    val beratBadan : Int,
    val umurBulan  : Int,
    val tanggal    : String
)

// ════════════════════════════════════════════════════════════
//  SAMPLE DATA
// ════════════════════════════════════════════════════════════

private val dummyAnakList = List(6) {
    AnakData(
        nama        = "Michael Kwok",
        status      = "Gizi Kurang",
        tinggiBadan = 100,
        beratBadan  = 40,
        umurBulan   = 36,
        tanggal     = "Apr 2025"
    )
}

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun DataAnakScreen(
    anakList: List<AnakData> = dummyAnakList,
    onTambahClick: () -> Unit = {},
    onAnakClick: (AnakData) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, anakList) {
        if (searchQuery.isBlank()) anakList
        else anakList.filter { it.nama.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Header (fixed at top, not scrollable) ──────────
        DataAnakHeader(
            jumlahAnak = anakList.size,
            onBack = onNavigateBack
        )

        // ── Scrollable body ────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SearchBarSection(
                    query       = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier    = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(filteredList) { anak ->
                AnakListItem(
                    data    = anak,
                    onClick = { onAnakClick(anak) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Bottom button (always visible) ─────────────────
        TambahAnakButton(
            onClick  = onTambahClick,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun DataAnakHeader(jumlahAnak: Int, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // App bar title with Back Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
                Text(
                    text = "MyPosyandu",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(24.dp)) // To balance the back icon
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
//  2. SEARCH BAR
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
                verticalAlignment = Alignment.CenterVertically,
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

// ════════════════════════════════════════════════════════════
//  3. LIST ITEM
// ════════════════════════════════════════════════════════════

@Composable
fun AnakListItem(
    data   : AnakData,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CircleMint)
            )

            // Name + stats (takes remaining space)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = data.nama,
                    color      = TextWhite,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = data.status,
                    color    = TextGrey,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = "TB: ${data.tinggiBadan}  BB: ${data.beratBadan}  Umur: ${data.umurBulan} Bulan",
                    color    = TextGrey,
                    fontSize = 12.sp
                )
            }

            // Date — pinned to bottom-right via Arrangement
            Text(
                text     = data.tanggal,
                color    = TextGrey,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }

    Divider(
        color     = SurfaceDarkBorder,
        thickness = 0.8.dp
    )
}

// ════════════════════════════════════════════════════════════
//  4. TAMBAH ANAK BUTTON
// ════════════════════════════════════════════════════════════

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
            .border(
                width  = 1.dp,
                color  = TextWhite,
                shape  = RoundedCornerShape(14.dp)
            )
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
