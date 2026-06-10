package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg          = Color(0xFF2A2A2A)
private val CardBorder      = Color(0xFF444444)
private val RowDividerColor = Color(0xFFFFFFFF).copy(alpha = 0.10f)
private val AvatarGrey      = Color(0xFFAAAAAA)
private val InfoBoxBg       = Color(0xFFC5DCF0)
private val InfoBoxText     = Color(0xFF1A4A6E)
private val InfoBoxBold     = Color(0xFF0D3457)
private val SubtextColor    = Color(0xFFAAAAAA)
private val CardTitleGrey   = Color(0xFF888888)

@Composable
fun KonfirmasiDataScreen(
    viewModel        : FormDataViewModel = FormDataViewModel(),
    onNavigateBack   : () -> Unit = {},
    onSimpanClicked  : () -> Unit = {},
    onPerbaikiClicked: () -> Unit = {}
) {
    val anak = viewModel.formAnak

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        KonfirmasiHeader(onNavigateBack = onNavigateBack)

        Text(
            text       = "Pastikan data berikut sudah benar sebelum menyimpan.",
            color      = TextWhite,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        DataBalitaCard(
            anak     = anak,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        AkunOrangTuaCard(
            ortu     = viewModel.formOrangTua,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        SimpanButton(
            onClick  = onSimpanClicked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        PerbaikiButton(
            onClick  = onPerbaikiClicked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun KonfirmasiHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TextWhite, RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateBack)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = TextWhite,
                    modifier           = Modifier.size(16.dp)
                )
                Text(text = "Kembali", color = TextWhite, fontSize = 14.sp)
            }

            Text(
                text       = "Konfirmasi Data",
                color      = TextWhite,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DataBalitaCard(
    anak    : FormAnakData,
    modifier: Modifier = Modifier
) {
    KonfirmasiCardContainer(modifier = modifier) {
        Column {
            Text(
                text       = "Data Balita",
                color      = TextWhite,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 10.dp)
            )

            DataRowItem(label = "Nama",          value = anak.namaLengkap.ifBlank { "-" })
            DataRowItem(label = "NIK",           value = anak.nik.ifBlank { "-" })
            DataRowItem(label = "Tanggal Lahir", value = anak.tanggalLahir.ifBlank { "-" })
            DataRowItem(label = "Jenis Kelamin", value = anak.jenisKelamin.ifBlank { "-" })
            DataRowItem(label = "Alamat",        value = anak.alamat.ifBlank { "-" })
        }
    }
}

@Composable
fun DataRowItem(label: String, value: String) {
    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(text = label, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = RowDividerColor, thickness = 1.dp)
    }
}

@Composable
fun AkunOrangTuaCard(
    ortu    : FormOrangTuaData,
    modifier: Modifier = Modifier
) {
    KonfirmasiCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text       = "Akun Orang Tua",
                color      = CardTitleGrey,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AvatarGrey)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = ortu.nama.ifBlank { "-" },
                        color      = TextWhite,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = ortu.username.ifBlank { "-" },
                        color = SubtextColor,
                        fontSize = 13.sp
                    )
                }
            }

            OrangTuaInfoBox(nama = ortu.nama, username = ortu.username)
        }
    }
}

@Composable
private fun OrangTuaInfoBox(nama: String, username: String) {
    val infoText = buildAnnotatedString {
        withStyle(SpanStyle(color = InfoBoxText, fontSize = 13.sp)) {
            append("Anak ini akan ditambahkan ke akun ")
        }
        withStyle(SpanStyle(color = InfoBoxBold, fontSize = 13.sp, fontWeight = FontWeight.Bold)) {
            append(username.ifBlank { "-" })
        }
        withStyle(SpanStyle(color = InfoBoxText, fontSize = 13.sp)) {
            append(" dan langsung terlihat saat orang tua login. ")
            append(nama.ifBlank { "Orang tua" })
            append(" sudah terdaftar.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InfoBoxBg)
            .padding(12.dp)
    ) {
        Text(text = infoText, lineHeight = 20.sp)
    }
}

@Composable
fun SimpanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            text       = "Simpan & Daftarkan",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PerbaikiButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            text       = "Perbaiki data",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KonfirmasiCardContainer(
    modifier: Modifier = Modifier,
    content : @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun KonfirmasiDataScreenPreview() {
    KonfirmasiDataScreen(viewModel = FormDataViewModel())
}