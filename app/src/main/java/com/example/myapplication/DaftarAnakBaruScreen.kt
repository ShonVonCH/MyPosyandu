package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Local design tokens
// ─────────────────────────────────────────────────────────────
private val FormCardBg      = Color(0xFF2A2A2A)
private val FormCardBorder  = Color(0xFF444444)
private val FieldBorder     = Color(0xFF555555)
private val FieldBg         = Color(0xFF333333)
private val LabelColor      = Color(0xFFAAAAAA)
private val PlaceholderColor= Color(0xFF6B6B6B)
private val PageLabelColor  = Color(0xFF666666)

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun DaftarAnakBaruScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToHubung: () -> Unit = {}
) {

    // ── Form state ───────────────────────────────────────────
    var namaLengkap   by remember { mutableStateOf("") }
    var nik           by remember { mutableStateOf("") }
    var tanggalLahir  by remember { mutableStateOf("") }
    var jenisKelamin  by remember { mutableStateOf("") }
    var alamat        by remember { mutableStateOf("") }
    // ────────────────────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Micro page-label above header
        Text(
            text     = "Daftar Anak Baru",
            color    = PageLabelColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )

        // Green header
        DaftarAnakHeader(onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(20.dp))

        // Form card
        FormIdentitas(
            namaLengkap      = namaLengkap,
            onNamaChange     = { namaLengkap = it },
            nik              = nik,
            onNikChange      = { nik = it },
            tanggalLahir     = tanggalLahir,
            onTanggalChange  = { tanggalLahir = it },
            jenisKelamin     = jenisKelamin,
            onJenisChange    = { jenisKelamin = it },
            alamat           = alamat,
            onAlamatChange   = { alamat = it },
            modifier         = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit / link button
        HubungkanButton(
            onClick  = onNavigateToHubung,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun DaftarAnakHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Back button — outlined pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width  = 1.dp,
                        color  = TextWhite,
                        shape  = RoundedCornerShape(8.dp)
                    )
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
                    text       = "Kembali",
                    color      = TextWhite,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Page title
            Text(
                text       = "Daftar Anak Baru",
                color      = TextWhite,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  2. FORM CARD — identitas balita
// ════════════════════════════════════════════════════════════

@Composable
fun FormIdentitas(
    namaLengkap    : String,
    onNamaChange   : (String) -> Unit,
    nik            : String,
    onNikChange    : (String) -> Unit,
    tanggalLahir   : String,
    onTanggalChange: (String) -> Unit,
    jenisKelamin   : String,
    onJenisChange  : (String) -> Unit,
    alamat         : String,
    onAlamatChange : (String) -> Unit,
    modifier       : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FormCardBg)
            .border(
                width  = 1.dp,
                color  = FormCardBorder,
                shape  = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Card heading
            Text(
                text       = "identitas balita",
                color      = TextWhite,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )

            // Field 1: Nama lengkap
            FormField(
                label        = "Nama lengkap",
                value        = namaLengkap,
                onValueChange = onNamaChange,
                placeholder  = "Nama Balita"
            )

            // Field 2: NIK
            FormField(
                label        = "NIK",
                value        = nik,
                onValueChange = onNikChange,
                placeholder  = "NIK Balita",
                keyboardType = KeyboardType.Number
            )

            // Field 3 & 4: Tanggal Lahir (wide) + Jenis Kelamin (narrow)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                // Tanggal Lahir — takes ~62% of width
                FormField(
                    label         = "Tanggal Lahir",
                    value         = tanggalLahir,
                    onValueChange = onTanggalChange,
                    placeholder   = "dd/mm/yyyy",
                    keyboardType  = KeyboardType.Number,
                    trailingIcon  = {
                        Icon(
                            imageVector        = Icons.Default.DateRange,
                            contentDescription = "Pilih tanggal",
                            tint               = LabelColor,
                            modifier           = Modifier.size(18.dp)
                        )
                    },
                    modifier      = Modifier.weight(1.6f)
                )

                // Jenis Kelamin — takes ~38% of width
                FormField(
                    label         = "Jenis Kelamin",
                    value         = jenisKelamin,
                    onValueChange = onJenisChange,
                    placeholder   = "0.0",
                    modifier      = Modifier.weight(1f)
                )
            }

            // Field 5: Alamat
            FormField(
                label        = "Alamat",
                value        = alamat,
                onValueChange = onAlamatChange,
                placeholder  = "Alamat Balita"
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  REUSABLE: SINGLE FORM FIELD  (label + slim OutlinedTextField)
// ════════════════════════════════════════════════════════════

@Composable
fun FormField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    modifier     : Modifier               = Modifier,
    keyboardType : KeyboardType           = KeyboardType.Text,
    trailingIcon : (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text     = label,
            color    = LabelColor,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier
                .fillMaxWidth()
                .height(46.dp),          // slim field height matching mockup
            placeholder = {
                Text(
                    text   = placeholder,
                    color  = PlaceholderColor,
                    fontSize = 13.sp
                )
            },
            trailingIcon  = trailingIcon,
            singleLine    = true,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                color    = TextWhite,
                fontSize = 13.sp
            ),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor            = TextWhite,
                cursorColor          = AccentGreen,
                focusedBorderColor   = AccentGreen,
                unfocusedBorderColor = FieldBorder,
                backgroundColor      = FieldBg,
                placeholderColor     = PlaceholderColor,
                trailingIconColor    = LabelColor,
                focusedLabelColor    = AccentGreen,
                unfocusedLabelColor  = LabelColor
            )
        )
    }
}

// ════════════════════════════════════════════════════════════
//  3. SUBMIT BUTTON
// ════════════════════════════════════════════════════════════

@Composable
fun HubungkanButton(
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark)
            .border(
                width  = 1.dp,
                color  = TextWhite,
                shape  = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Hubungkan ke Akun Orang Tua",
            color      = TextWhite,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun DaftarAnakBaruScreenPreview() {
    DaftarAnakBaruScreen()
}
