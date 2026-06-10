package com.example.myapplication

import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val FormCardBg       = Color(0xFF2A2A2A)
private val FormCardBorder   = Color(0xFF444444)
private val FieldBorder      = Color(0xFF555555)
private val FieldBg          = Color(0xFF333333)
private val LabelColor       = Color(0xFFAAAAAA)
private val PlaceholderColor = Color(0xFF6B6B6B)

@Composable
fun DaftarAnakBaruScreen(
    viewModel         : FormDataViewModel,
    dariKonfirmasi    : Boolean    = false,
    onNavigateBack    : () -> Unit = {},
    onNavigateToHubung: () -> Unit = {}
) {
    var namaLengkap  by remember { mutableStateOf(viewModel.formAnak.namaLengkap) }
    var nik          by remember { mutableStateOf(viewModel.formAnak.nik) }
    var tanggalLahir by remember { mutableStateOf(viewModel.formAnak.tanggalLahir) }
    var jenisKelamin by remember { mutableStateOf(viewModel.formAnak.jenisKelamin) }
    var alamat       by remember { mutableStateOf(viewModel.formAnak.alamat) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        DaftarAnakHeader(
            dariKonfirmasi = dariKonfirmasi,
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(20.dp))

        FormIdentitas(
            namaLengkap     = namaLengkap,
            onNamaChange    = { namaLengkap = it },
            nik             = nik,
            onNikChange     = { nik = it },
            tanggalLahir    = tanggalLahir,
            onTanggalChange = { tanggalLahir = it },
            jenisKelamin    = jenisKelamin,
            onJenisChange   = { jenisKelamin = it },
            alamat          = alamat,
            onAlamatChange  = { alamat = it },
            modifier        = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (dariKonfirmasi) {
            SelesaiDaftarButtons(
                onClick = {
                    viewModel.formAnak = FormAnakData(
                        namaLengkap  = namaLengkap,
                        nik          = nik,
                        tanggalLahir = tanggalLahir,
                        jenisKelamin = jenisKelamin,
                        alamat       = alamat
                    )
                    onNavigateBack()
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            HubungkanButton(
                onClick = {
                    viewModel.formAnak = FormAnakData(
                        namaLengkap  = namaLengkap,
                        nik          = nik,
                        tanggalLahir = tanggalLahir,
                        jenisKelamin = jenisKelamin,
                        alamat       = alamat
                    )
                    onNavigateToHubung()
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────

@Composable
fun DaftarAnakHeader(
    dariKonfirmasi: Boolean = false,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    text       = "Kembali",
                    color      = TextWhite,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Text(
                text       = if (dariKonfirmasi) "Perbaiki Data Anak" else "Daftar Anak Baru",
                color      = TextWhite,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────
// Form Identitas
// ─────────────────────────────────────────────

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
    val context  = LocalContext.current
    val calendar = Calendar.getInstance()

    // DatePickerDialog — muncul saat kolom tanggal diklik
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                // Format hasil jadi "dd/MM/yyyy"
                val d = dayOfMonth.toString().padStart(2, '0')
                val m = (month + 1).toString().padStart(2, '0')
                onTanggalChange("$d/$m/$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FormCardBg)
            .border(1.dp, FormCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text       = "Identitas Balita",
                color      = TextWhite,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )

            FormField(
                label         = "Nama Lengkap",
                value         = namaLengkap,
                onValueChange = onNamaChange,
                placeholder   = "Nama Balita"
            )

            FormField(
                label         = "NIK",
                value         = nik,
                onValueChange = onNikChange,
                placeholder   = "NIK Balita",
                keyboardType  = KeyboardType.Number
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                // Kolom tanggal lahir — tidak bisa diketik, klik buka DatePicker
                Column(
                    modifier            = Modifier.weight(1.6f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(text = "Tanggal Lahir", color = LabelColor, fontSize = 12.sp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FieldBg)
                            .border(1.dp, FieldBorder, RoundedCornerShape(8.dp))
                            .clickable { datePickerDialog.show() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = tanggalLahir.ifEmpty { "dd/mm/yyyy" },
                                color    = if (tanggalLahir.isEmpty()) PlaceholderColor else TextWhite,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector        = Icons.Default.DateRange,
                                contentDescription = "Pilih tanggal",
                                tint               = LabelColor,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                FormField(
                    label         = "Jenis Kelamin",
                    value         = jenisKelamin,
                    onValueChange = onJenisChange,
                    placeholder   = "L / P",
                    modifier      = Modifier.weight(1f)
                )
            }

            FormField(
                label         = "Alamat",
                value         = alamat,
                onValueChange = onAlamatChange,
                placeholder   = "Alamat Balita"
            )
        }
    }
}

// ─────────────────────────────────────────────
// Form Field Generic
// ─────────────────────────────────────────────

@Composable
fun FormField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    modifier     : Modifier                  = Modifier,
    keyboardType : KeyboardType              = KeyboardType.Text,
    trailingIcon : (@Composable () -> Unit)? = null
) {
    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = label, color = LabelColor, fontSize = 12.sp)

        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = {
                Text(text = placeholder, color = PlaceholderColor, fontSize = 13.sp)
            },
            trailingIcon         = trailingIcon,
            singleLine           = true,
            visualTransformation = VisualTransformation.None,
            keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                color      = TextWhite,
                fontSize   = 13.sp,
                lineHeight = 16.sp
            ),
            shape  = RoundedCornerShape(8.dp),
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

// ─────────────────────────────────────────────
// Tombol
// ─────────────────────────────────────────────

@Composable
fun HubungkanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark)
            .border(1.dp, TextWhite, RoundedCornerShape(12.dp))
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

@Composable
fun SelesaiDaftarButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HeaderGreen)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Selesai",
            color      = TextWhite,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun DaftarAnakBaruScreenPreview() {
    DaftarAnakBaruScreen(viewModel = FormDataViewModel())
}