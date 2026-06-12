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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KonfCardBg        = Color(0xFF2A2A2A)
private val KonfCardBorder    = Color(0xFF444444)
private val KonfRowDivider    = Color(0xFFFFFFFF).copy(alpha = 0.10f)
private val KonfAvatarGrey    = Color(0xFFAAAAAA)
private val KonfInfoBoxBg     = Color(0xFFC5DCF0)
private val KonfInfoBoxText   = Color(0xFF1A4A6E)
private val KonfInfoBoxBold   = Color(0xFF0D3457)
private val KonfSubtext       = Color(0xFFAAAAAA)
private val KonfCardTitleGrey = Color(0xFF888888)

@Composable
fun KonfirmasiDataScreen(
    viewModel        : FormDataViewModel = FormDataViewModel(),
    onNavigateBack   : () -> Unit = {},
    onSimpanClicked  : () -> Unit = {},
    onPerbaikiClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val anak    = viewModel.formAnak
    val ortu    = viewModel.formOrangTua

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
            ortu     = ortu,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        SimpanButton(
            onClick = {
                val ortuRepo = OrtuRepository(context)
                val db       = DatabaseHelper(context).writableDatabase

                // 1. Simpan akun ortu HANYA jika belum ada (akun baru)
                //    Kalau pilih "akun existing", skip — tidak perlu insert lagi
                if (!ortuRepo.isUsernameExists(ortu.username)) {
                    ortuRepo.insertOrtu(
                        nama     = ortu.nama,
                        username = ortu.username,
                        password = ortu.password
                    )
                }

                // 2. Ambil ortu_id berdasarkan username
                val cursorOrtu = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ORTU_ID} FROM ${DatabaseHelper.TABLE_ORTU} " +
                            "WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ? LIMIT 1",
                    arrayOf(ortu.username)
                )
                val ortuId = if (cursorOrtu.moveToFirst()) cursorOrtu.getString(0) ?: "" else ""
                cursorOrtu.close()

                // 3. Insert anak ke tabel anak
                if (ortuId.isNotBlank() && anak.namaLengkap.isNotBlank()) {
                    val anakId = anak.nik.ifBlank { java.util.UUID.randomUUID().toString() }
                    val posyanduCursor = db.rawQuery(
                        "SELECT ${DatabaseHelper.COL_USERS_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_USERS} LIMIT 1",
                        null
                    )
                    val posyanduId = if (posyanduCursor.moveToFirst()) posyanduCursor.getString(0) ?: "" else ""
                    posyanduCursor.close()

                    val anakValues = android.content.ContentValues().apply {
                        put(DatabaseHelper.COL_ANAK_ID,            anakId)
                        put(DatabaseHelper.COL_ANAK_NAMA,          anak.namaLengkap)
                        put(DatabaseHelper.COL_ANAK_TGL_LAHIR,     anak.tanggalLahir)
                        put(DatabaseHelper.COL_ANAK_JENIS_KELAMIN, anak.jenisKelamin)
                        put(DatabaseHelper.COL_ANAK_ORTU_ID,       ortuId)
                        put(DatabaseHelper.COL_ANAK_POSYANDU_ID,   posyanduId)
                        put(DatabaseHelper.COL_ANAK_CREATED_AT,    System.currentTimeMillis().toString())
                    }
                    db.insertWithOnConflict(
                        DatabaseHelper.TABLE_ANAK,
                        null,
                        anakValues,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                    )
                    android.util.Log.d("KonfirmasiData",
                        "Anak '${anak.namaLengkap}' disimpan id=$anakId ortuId=$ortuId")
                }
                db.close()

                // 4. Update in-memory state
                viewModel.simpanAnak()
                onSimpanClicked()
            },
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
                Icon(Icons.Filled.ArrowBack, "Kembali", tint = TextWhite,
                    modifier = Modifier.size(16.dp))
                Text("Kembali", color = TextWhite, fontSize = 14.sp)
            }
            Text("Konfirmasi Data", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DataBalitaCard(anak: FormAnakData, modifier: Modifier = Modifier) {
    KonfirmasiCardContainer(modifier = modifier) {
        Column {
            Text("Data Balita", color = TextWhite, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
            DataRowItem("Nama",          anak.namaLengkap.ifBlank { "-" })
            DataRowItem("NIK",           anak.nik.ifBlank { "-" })
            DataRowItem("Tanggal Lahir", anak.tanggalLahir.ifBlank { "-" })
            DataRowItem("Jenis Kelamin", anak.jenisKelamin.ifBlank { "-" })
            DataRowItem("Alamat",        anak.alamat.ifBlank { "-" })
        }
    }
}

@Composable
fun DataRowItem(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(label, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = KonfRowDivider, thickness = 1.dp)
    }
}

@Composable
fun AkunOrangTuaCard(ortu: FormOrangTuaData, modifier: Modifier = Modifier) {
    KonfirmasiCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Akun Orang Tua", color = KonfCardTitleGrey,
                fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(KonfAvatarGrey))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ortu.nama.ifBlank { "-" }, color = TextWhite,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(ortu.username.ifBlank { "-" }, color = KonfSubtext, fontSize = 13.sp)
                }
            }
            OrangTuaInfoBox(nama = ortu.nama, username = ortu.username)
        }
    }
}

@Composable
private fun OrangTuaInfoBox(nama: String, username: String) {
    val infoText = buildAnnotatedString {
        withStyle(SpanStyle(color = KonfInfoBoxText, fontSize = 13.sp)) {
            append("Anak ini akan ditambahkan ke akun ")
        }
        withStyle(SpanStyle(color = KonfInfoBoxBold, fontSize = 13.sp, fontWeight = FontWeight.Bold)) {
            append(username.ifBlank { "-" })
        }
        withStyle(SpanStyle(color = KonfInfoBoxText, fontSize = 13.sp)) {
            append(" dan langsung terlihat saat orang tua login. ")
            append(nama.ifBlank { "Orang tua" })
            append(" sudah terdaftar.")
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(KonfInfoBoxBg).padding(12.dp)
    ) { Text(text = infoText, lineHeight = 20.sp) }
}

@Composable
fun SimpanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(HeaderGreen).clickable(onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Simpan & Daftarkan", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PerbaikiButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark).border(1.dp, TextWhite, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Perbaiki data", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KonfirmasiCardContainer(
    modifier: Modifier = Modifier,
    content : @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(KonfCardBg).border(1.dp, KonfCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) { Column(content = content) }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun KonfirmasiDataScreenPreview() { KonfirmasiDataScreen() }