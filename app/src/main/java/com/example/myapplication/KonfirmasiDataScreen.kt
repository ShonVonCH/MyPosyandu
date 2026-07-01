package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val scope = rememberCoroutineScope()
    val anak    = viewModel.formAnak
    val ortu    = viewModel.formOrangTua

    // State untuk status sync
    var syncStatus by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

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
                val dbHelper  = DatabaseHelper(context)
                val db        = dbHelper.writableDatabase

                // Format created_at: yyyy-MM-dd HH:mm:ss
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val createdAt = sdf.format(Date())

                // 1. Simpan akun ortu ke SQLite lokal HANYA jika belum ada,
                //    pakai db instance yang SAMA agar tidak ada race condition
                val cursorCek = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ORTU_ID} FROM ${DatabaseHelper.TABLE_ORTU} " +
                            "WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ? LIMIT 1",
                    arrayOf(ortu.username)
                )
                val existingOrtuId = if (cursorCek.moveToFirst()) cursorCek.getString(0) ?: "" else ""
                cursorCek.close()

                val ortuId: String
                if (existingOrtuId.isNotBlank()) {
                    // Sudah ada, pakai ID yang existing
                    ortuId = existingOrtuId
                    android.util.Log.d("KonfirmasiData", "Ortu sudah ada, pakai id=$ortuId")
                } else {
                    // Belum ada, generate UUID dan insert langsung via db yang sama
                    val newOrtuId = java.util.UUID.randomUUID().toString()
                    val posyanduCursorOrtu = db.rawQuery(
                        "SELECT ${DatabaseHelper.COL_USERS_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_USERS} LIMIT 1",
                        null
                    )
                    val posyanduIdOrtu = if (posyanduCursorOrtu.moveToFirst()) posyanduCursorOrtu.getString(0) ?: "" else ""
                    posyanduCursorOrtu.close()

                    val ortuValues = android.content.ContentValues().apply {
                        put(DatabaseHelper.COL_ORTU_ID,         newOrtuId)
                        put(DatabaseHelper.COL_ORTU_NAMA,        ortu.nama)
                        put(DatabaseHelper.COL_ORTU_USERNAME,    ortu.username)
                        put(DatabaseHelper.COL_ORTU_PASSWORD,    ortu.password)
                        put(DatabaseHelper.COL_ORTU_ROLE,        "ortu")
                        put(DatabaseHelper.COL_ORTU_POSYANDU_ID, posyanduIdOrtu)
                        put(DatabaseHelper.COL_ORTU_CREATED_AT,  createdAt)
                    }
                    db.insertWithOnConflict(
                        DatabaseHelper.TABLE_ORTU, null, ortuValues,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
                    )
                    ortuId = newOrtuId
                    android.util.Log.d("KonfirmasiData", "Ortu baru diinsert id=$ortuId")
                }

                // 3. Insert anak ke tabel anak
                if (ortuId.isNotBlank() && anak.namaLengkap.isNotBlank()) {
                    // ID anak selalu UUID — NIK disimpan terpisah, bukan sebagai primary key
                    val anakId = anak.nik
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
                        put(DatabaseHelper.COL_ANAK_CREATED_AT,    createdAt)
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

                // 5. SYNC KE API — Insert ortu dan anak yang belum ada di database API
                isSyncing = true
                syncStatus = "Mengsinkronkan ke server..."

                scope.launch {
                    try {
                        // Sync ortu dulu
                        val resultOrtu = syncOrtuToApi(context)
                        android.util.Log.d("SYNC_ORTU", resultOrtu.message)

                        // Lalu sync anak
                        val resultAnak = syncAnakToApi(context)
                        android.util.Log.d("SYNC_ANAK", resultAnak.message)

                        syncStatus = resultOrtu.message + " | " + resultAnak.message
                    } catch (e: Exception) {
                        syncStatus = "Sync gagal: ${e.message}"
                        android.util.Log.e("SYNC", "Error: ${e.message}", e)
                    } finally {
                        isSyncing = false
                        // Navigate setelah sync selesai
                        onSimpanClicked()
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Tampilkan status sync
        if (isSyncing || syncStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val isSuccess = !syncStatus.contains("gagal", ignoreCase = true) &&
                    !syncStatus.contains("Mengsinkronkan")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isSyncing -> Color(0xFF1A3A2A)
                            isSuccess -> Color(0xFF1A3A2A)
                            else -> Color(0xFF3A1A1A)
                        }
                    )
                    .padding(12.dp)
            ) {
                if (isSyncing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF6FDDAA),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = syncStatus,
                            color = Color(0xFF6FDDAA),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = syncStatus,
                        color = if (isSuccess) Color(0xFF6FDDAA) else Color(0xFFDD6F6F),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

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
                    contentDescription = "Kembali",
                    tint               = TextWhite,
                    modifier           = Modifier
                        .size(24.dp)
                        .clickable { onNavigateBack() }
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
            Text(text = "Konfirmasi Data", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
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