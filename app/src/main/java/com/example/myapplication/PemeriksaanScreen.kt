package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun PemeriksaanScreen(onNavigateBack: () -> Unit = {}) {

    // ── State Management ─────────────────────────────────────
    var beratBadan    by remember { mutableStateOf("") }
    var tinggiBadan   by remember { mutableStateOf("") }
    var lingkarKepala by remember { mutableStateOf("") }
    var lingkarLengan by remember { mutableStateOf("") }
    var tanggal       by remember { mutableStateOf("26/05/2025") }
    // ─────────────────────────────────────────────────────────

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Header hijau atas
        item {
            PemeriksaanHeader(onBackClick = onNavigateBack)
        }

        // 2. Card Data Antropometri
        item {
            Spacer(modifier = Modifier.height(32.dp))
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

        // 3. Card Tanggal Pemeriksaan
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TanggalCard(
                tanggal       = tanggal,
                onTanggalChange = { tanggal = it },
                modifier      = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER SECTION
// ════════════════════════════════════════════════════════════

@Composable
fun PemeriksaanHeader(onBackClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = HeaderGreen,
                shape = RoundedCornerShape(
                    topStart    = 0.dp,
                    topEnd      = 0.dp,
                    bottomStart = 0.dp,
                    bottomEnd   = 0.dp
                )
            )
            .padding(
                start  = 20.dp,
                end    = 20.dp,
                top    = 48.dp,       // safe area + breathing room
                bottom = 28.dp
            )
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBackClick() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
                Text(
                    text       = "Pemeriksaan",
                    color      = TextWhite,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = "Catat data kesehatan balita",
                color      = TextGreenLight,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 48.dp) // Offset to align with title text next to back button
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. CARD: DATA ANTROPOMETRI
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
//  3. CARD: TANGGAL PEMERIKSAAN
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
//  — border abu-abu, background dark, placeholder abu-abu,
//    teks input putih, sudut membulat
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
                // Placeholder text — shown only when value is empty
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
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212, showSystemUi = true)
@Composable
fun PemeriksaanScreenPreview() {
    PemeriksaanScreen()
}
