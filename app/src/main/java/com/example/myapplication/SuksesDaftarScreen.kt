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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SuccessCircleBg    = Color(0xFFB8EDD8)
private val SuccessIconColor   = Color(0xFF1E7A55)
private val DataCardBg         = Color(0xFF2A2A2A)
private val DataCardBorder     = Color(0xFF444444)
private val InnerBoxBg         = Color(0xFF1A1A1A)
private val ConnectedIconColor = Color(0xFF2E9B6E)
private val AvatarGreyLight    = Color(0xFFCCCCCC)
private val UpdateCardBg       = Color(0xFFBBCFEF)
private val UpdateNavyDark     = Color(0xFF1A3A6E)
private val UpdateDividerColor = Color(0xFF1A3A6E).copy(alpha = 0.25f)
private val SubtextGrey        = Color(0xFFAAAAAA)

@Composable
fun SuksesDaftarScreen(
    viewModel       : FormDataViewModel = FormDataViewModel(),
    onNavigateBack  : () -> Unit = {},
    onSelesaiClicked: () -> Unit = {}
) {
    val anak = viewModel.lastSavedAnak
    val ortu = viewModel.akunOrangTuaList
        .firstOrNull { it.username == viewModel.lastSavedOrangTua.username }
        ?: OrangTuaAccount(
            nama       = viewModel.lastSavedOrangTua.nama,
            username   = viewModel.lastSavedOrangTua.username,
            jumlahAnak = 1
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        SuksesHeader(onNavigateBack = onNavigateBack)
        SuksesStatus(namaAnak = anak.namaLengkap)

        Spacer(modifier = Modifier.height(20.dp))

        AnakTerhubungCard(
            anak     = anak,
            ortu     = ortu,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        UpdateOrangTuaCard(
            anak     = anak,
            ortu     = ortu,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelesaiDaftarButtons(
            onClick  = onSelesaiClicked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SuksesHeader(onNavigateBack: () -> Unit) {
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
fun SuksesStatus(namaAnak: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SuccessCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.CheckCircle,
                contentDescription = "Sukses",
                tint               = SuccessIconColor,
                modifier           = Modifier.size(52.dp)
            )
        }

        Text(
            text       = "${namaAnak.ifBlank { "-" }} telah terdaftar dan sudah terhubung ke akun orang tua",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun AnakTerhubungCard(
    anak    : FormAnakData,
    ortu    : OrangTuaAccount,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DataCardBg)
            .border(1.dp, DataCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AvatarGreyLight)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = anak.namaLengkap.ifBlank { "-" },
                        color      = TextWhite,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "${anak.tanggalLahir.ifBlank { "-" }}  •  ${anak.jenisKelamin.ifBlank { "-" }}",
                        color    = SubtextGrey,
                        fontSize = 13.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(InnerBoxBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text     = "Terhubung ke akun orang tua",
                            color    = SubtextGrey,
                            fontSize = 12.sp
                        )
                        Text(
                            text       = "${ortu.nama} - ${ortu.username}",
                            color      = TextWhite,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector        = Icons.Outlined.CheckCircle,
                        contentDescription = "Terhubung",
                        tint               = ConnectedIconColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateOrangTuaCard(
    anak    : FormAnakData,
    ortu    : OrangTuaAccount,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UpdateCardBg)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text       = "Update di akun orang tua",
                color      = UpdateNavyDark,
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
                        .background(AvatarGreyLight)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = "${ortu.nama} (${ortu.username})",
                        color      = UpdateNavyDark,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "Kini memiliki ${ortu.jumlahAnak} anak terdaftar",
                        color    = UpdateNavyDark,
                        fontSize = 13.sp
                    )
                }
            }

            Divider(color = UpdateDividerColor, thickness = 1.dp)

            Text(
                text       = "${anak.namaLengkap.ifBlank { "Anak ini" }} sudah muncul di halaman anak-anak saat orang tua login berikutnya.",
                color      = UpdateNavyDark,
                fontSize   = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun SelesaiDaftarButtons(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun SuksesDaftarScreenPreview() {
    SuksesDaftarScreen(viewModel = FormDataViewModel())
}