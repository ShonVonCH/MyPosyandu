package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KaderBlue        = Color(0xFF1964A3)
private val KaderBackground  = Color(0xFF121212)
private val KaderSurface     = Color(0xFF2A2A2A)
private val KaderBorder      = Color(0xFF444444)
private val KaderTextWhite   = Color(0xFFFFFFFF)
private val KaderTextGrey    = Color(0xFF888888)
private val KaderGreen       = Color(0xFF00C896)
private val KaderAmber       = Color(0xFFFFC947)
private val KaderRed         = Color(0xFFE74C3C)

data class AntrianKaderRow(
    val item     : AntrianItemApi,
    val namaAnak : String
)

@Composable
fun AntrianKaderScreen(
    kaderId            : String,
    onNavigateBack     : () -> Unit = {},
    onNavigateToHome   : () -> Unit = {},
    onNavigateToPanggil: () -> Unit = {},
    onNavigateToLogout : () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var posyanduId           by remember { mutableStateOf<String?>(null) }
    var nomorSedangDilayani   by remember { mutableStateOf(0) }
    var daftarMenunggu        by remember { mutableStateOf<List<AntrianKaderRow>>(emptyList()) }
    var isLoading             by remember { mutableStateOf(false) }
    var isProcessing          by remember { mutableStateOf(false) }
    var statusMessage         by remember { mutableStateOf("") }
    var errorMessage          by remember { mutableStateOf("") }
    var showLogoutDialog      by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor  = KaderSurface,
            title = {
                Text("Logout", color = KaderTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("Yakin ingin keluar dari akun?", color = KaderTextGrey, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    try {
                        val db = DatabaseHelper(context).writableDatabase
                        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_USERS}")
                        db.close()
                    } catch (e: Exception) {
                        android.util.Log.e("ANTRIAN_KADER", "Logout error: ${e.message}")
                    }
                    onNavigateToLogout()
                }) {
                    Text("Logout", color = KaderRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = KaderTextGrey)
                }
            }
        )
    }

    // BARU: cari posyandu_id milik kader yang login, dari tabel users.
    // Sama seperti pola getPosyanduIdForUser() di AntrianApiService,
    // tapi khusus buat kader (login lewat tabel users, bukan ortu).
    fun cariPosyanduIdKader(): String? {
        return try {
            val db = DatabaseHelper(context).readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_USERS_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_USERS} " +
                        "WHERE ${DatabaseHelper.COL_USERS_ID} = ?",
                arrayOf(kaderId)
            )
            val id = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            db.close()
            id
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "cariPosyanduIdKader error: ${e.message}", e)
            null
        }
    }

    fun ambilNamaAnak(anakId: String): String {
        return try {
            val db = DatabaseHelper(context).readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ANAK_NAMA} FROM ${DatabaseHelper.TABLE_ANAK} " +
                        "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
                arrayOf(anakId)
            )
            val nama = if (cursor.moveToFirst()) cursor.getString(0) ?: "-" else "-"
            cursor.close()
            db.close()
            nama
        } catch (e: Exception) {
            "-"
        }
    }

    suspend fun loadData() {
        if (kaderId.isBlank()) {
            errorMessage = "Error: data kader tidak tersedia. Silakan login ulang."
            return
        }
        isLoading    = true
        errorMessage = ""
        try {
            val posId = posyanduId ?: cariPosyanduIdKader().also { posyanduId = it }
            if (posId == null) {
                errorMessage = "Posyandu untuk akun kader ini tidak ditemukan."
                return
            }

            val aktif = AntrianApiService.getAntrianAktifHariIni(context, posId)
            nomorSedangDilayani = aktif?.nomorSaatIni ?: 0

            val items    = if (aktif != null) AntrianApiService.getAntrianItems(context, aktif.id) else emptyList()
            val menunggu = getAntrianMenungguUrut(items)

            daftarMenunggu = menunggu.map { AntrianKaderRow(it, ambilNamaAnak(it.anakId)) }
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_KADER", "loadData error: ${e.message}", e)
            errorMessage = "Gagal memuat antrian: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(kaderId) { loadData() }

    // Auto-refresh tiap 8 detik biar daftar tunggu selalu terbaru.
    LaunchedEffect(kaderId) {
        while (true) {
            delay(8_000L)
            if (!isProcessing) loadData()
        }
    }

    fun panggilBerikutnya() {
        val next = daftarMenunggu.firstOrNull()?.item ?: run {
            statusMessage = "Antrian sudah habis, tidak ada yang menunggu"
            return
        }
        scope.launch {
            isProcessing  = true
            statusMessage = ""
            val ok = AntrianApiService.panggilAntrian(context, next)
            if (ok) {
                // FIX: update angka "Sedang Dilayani" LANGSUNG di sini, jangan
                // nunggu loadData() selesai roundtrip ke server — ini yang
                // bikin dulu keliatan "diam"/nggak berubah walau panggilan
                // sebenarnya sukses.
                nomorSedangDilayani = next.nomor
                statusMessage = "Memanggil nomor ${formatNomorAntrian(next.nomor)}"
            } else {
                statusMessage = "Gagal memanggil nomor ${formatNomorAntrian(next.nomor)}, coba lagi"
            }
            loadData()
            isProcessing = false
        }
    }

    fun panggilNomorTertentu(row: AntrianKaderRow) {
        scope.launch {
            isProcessing  = true
            statusMessage = ""
            val ok = AntrianApiService.panggilAntrian(context, row.item)
            if (ok) {
                nomorSedangDilayani = row.item.nomor
                statusMessage = "Memanggil nomor ${formatNomorAntrian(row.item.nomor)}"
            } else {
                statusMessage = "Gagal memanggil nomor ${formatNomorAntrian(row.item.nomor)}"
            }
            loadData()
            isProcessing = false
        }
    }

    fun tandaiTidakHadir(row: AntrianKaderRow) {
        scope.launch {
            isProcessing  = true
            statusMessage = ""
            val ok = AntrianApiService.tidakHadir(context, row.item)
            statusMessage = if (ok) "Nomor ${formatNomorAntrian(row.item.nomor)} ditandai tidak hadir"
            else "Gagal menandai tidak hadir"
            loadData()
            isProcessing = false
        }
    }

    Scaffold(
        backgroundColor = KaderBackground,
        bottomBar = {
            BottomNavBarKader(
                onHomeClick    = onNavigateToHome,
                onPanggilClick = onNavigateToPanggil,
                onLogoutClick  = { showLogoutDialog = true }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaderBlue)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = Color.White,
                    modifier           = Modifier.size(24.dp).clickable { onNavigateBack() }
                )
                Spacer(Modifier.width(16.dp))
                Text("Loket Panggil Antrian", color = KaderTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (errorMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3A1A1A))
                        .padding(12.dp)
                ) {
                    Text(errorMessage, color = Color(0xFFDD6F6F), fontSize = 13.sp)
                }
            }

            // Papan nomor sedang dilayani — besar, jadi fokus utama loket
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D0D0D))
                    .border(1.dp, KaderBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SEDANG DILAYANI", color = KaderTextGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = if (nomorSedangDilayani > 0) formatNomorAntrian(nomorSedangDilayani) else "—",
                    color      = KaderGreen,
                    fontSize   = 56.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick  = { panggilBerikutnya() },
                    enabled  = !isProcessing && daftarMenunggu.isNotEmpty(),
                    colors   = ButtonDefaults.buttonColors(backgroundColor = KaderBlue),
                    modifier = Modifier.height(48.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = KaderTextWhite, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (daftarMenunggu.isNotEmpty())
                                "Panggil Nomor ${formatNomorAntrian(daftarMenunggu.first().item.nomor)}"
                            else "Tidak Ada Antrian",
                            color = KaderTextWhite, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text     = statusMessage,
                    color    = KaderAmber,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Text(
                text       = "Menunggu (${daftarMenunggu.size})",
                color      = KaderTextWhite,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            when {
                isLoading && daftarMenunggu.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KaderGreen)
                    }
                }
                daftarMenunggu.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada yang menunggu", color = KaderTextGrey, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier            = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(daftarMenunggu, key = { it.item.id }) { row ->
                            BarisAntrianKader(
                                row        = row,
                                isPertama  = row == daftarMenunggu.first(),
                                isDisabled = isProcessing,
                                onPanggil  = { panggilNomorTertentu(row) },
                                onSkip     = { tandaiTidakHadir(row) }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisAntrianKader(
    row       : AntrianKaderRow,
    isPertama : Boolean,
    isDisabled: Boolean,
    onPanggil : () -> Unit,
    onSkip    : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KaderSurface)
            .border(1.dp, if (isPertama) KaderGreen else KaderBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                Text(formatNomorAntrian(row.item.nomor), color = KaderTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(row.namaAnak, color = KaderTextWhite, fontSize = 15.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick  = onSkip,
                enabled  = !isDisabled,
                colors   = ButtonDefaults.buttonColors(backgroundColor = KaderRed),
                modifier = Modifier.height(36.dp),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text("Tidak Hadir", color = KaderTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick  = onPanggil,
                enabled  = !isDisabled,
                colors   = ButtonDefaults.buttonColors(backgroundColor = if (isPertama) KaderGreen else KaderBlue),
                modifier = Modifier.height(36.dp),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text("Panggil", color = KaderTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BottomNavBarKader(
    onHomeClick   : () -> Unit,
    onPanggilClick: () -> Unit,
    onLogoutClick : () -> Unit
) {
    data class NavEntry(val icon: ImageVector, val label: String, val isActive: Boolean, val action: () -> Unit)
    val entries = listOf(
        NavEntry(Icons.Outlined.Home,               "Home",    false, onHomeClick),
        NavEntry(Icons.Outlined.ConfirmationNumber, "Antrian", true,  onPanggilClick),
        NavEntry(Icons.Outlined.PowerSettingsNew,   "Logout",  false, onLogoutClick)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            entries.forEach { entry ->
                val tint = if (entry.isActive) HeaderGreen else Color.White.copy(alpha = 0.45f)
                Column(
                    modifier = Modifier
                        .clickable(onClick = entry.action)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(entry.icon, contentDescription = entry.label, tint = tint, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        entry.label,
                        color      = tint,
                        fontSize   = 10.sp,
                        fontWeight = if (entry.isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AntrianKaderScreenPreview() {
    AntrianKaderScreen(kaderId = "kader-1")
}