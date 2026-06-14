package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────
//  Warna Lokal
// ─────────────────────────────────────────────────────────────
private val TikHeaderBlue     = Color(0xFF1964A3)
private val TikTicketBlue     = Color(0xFF1A6EBA)
private val TikBackgroundDark = Color(0xFF121212)
private val TikSurfaceDark    = Color(0xFF2A2A2A)
private val TikTextWhite      = Color(0xFFFFFFFF)
private val TikTextGrey       = Color(0xFF888888)
private val TikMintInfoBg     = Color(0xFF98E6C8)
private val TikMintInfoText   = Color(0xFF14634B)
private val TikErrorBg        = Color(0xFF3A1A1A)
private val TikErrorText      = Color(0xFFDD6F6F)

// ─────────────────────────────────────────────────────────────
//  State holder data tiket
// ─────────────────────────────────────────────────────────────
private data class TiketState(
    val nomorAntrian  : String  = "---",
    val namaAnak      : String  = "",
    val tanggal       : String  = "",
    val nomorDipanggil: String  = "--",
    val sisaGiliran   : String  = "--",
    val sudahAmbil    : Boolean = false
)

@Composable
fun TiketAntrianScreen(
    username: String = "",
    onNavigateToHome   : () -> Unit = {},
    onNavigateToTicket : () -> Unit = {},
    onNavigateToFood   : () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToLogout : () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isLoading    by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var tiket        by remember { mutableStateOf(TiketState()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor = TikSurfaceDark,
            title = {
                Text("Logout", color = TikTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("Yakin ingin keluar dari akun?", color = TikTextGrey, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    val db = DatabaseHelper(context).writableDatabase
                    db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_USERS}")
                    db.close()
                    onNavigateToLogout()
                }) {
                    Text("Logout", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = TikTextGrey)
                }
            }
        )
    }

    // ── Fungsi load — bisa dipanggil untuk refresh ──
    suspend fun loadData() {
        if (username.isBlank()) {
            errorMessage = "Username tidak tersedia. Silakan login ulang."
            isLoading = false
            return
        }

        isLoading    = true
        errorMessage = ""

        try {
            val db = DatabaseHelper(context).readableDatabase

            // 1. Cari ortu_id dari username
            val cursorOrtu = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ORTU_ID} FROM ${DatabaseHelper.TABLE_ORTU} " +
                        "WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ? LIMIT 1",
                arrayOf(username)
            )
            var ortuId = if (cursorOrtu.moveToFirst()) cursorOrtu.getString(0) ?: "" else ""
            cursorOrtu.close()

            // Fallback: cari dari tabel users jika tidak ada di ortu
            if (ortuId.isBlank()) {
                val cursorUsers = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_USERS_ID} FROM ${DatabaseHelper.TABLE_USERS} " +
                            "WHERE ${DatabaseHelper.COL_USERS_USERNAME} = ? LIMIT 1",
                    arrayOf(username)
                )
                ortuId = if (cursorUsers.moveToFirst()) cursorUsers.getString(0) ?: "" else ""
                cursorUsers.close()
            }

            if (ortuId.isBlank()) {
                db.close()
                errorMessage = "Data pengguna tidak ditemukan. Coba sinkronisasi ulang."
                isLoading = false
                return
            }

            android.util.Log.d("TIKET", "ortuId=$ortuId, username=$username")

            // 2. Ambil antrian aktif hari ini dari API (dengan fallback ke lokal)
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context)
            android.util.Log.d("TIKET", "antrianAktif=${antrianAktif?.id}")

            if (antrianAktif == null) {
                // Tidak ada antrian aktif hari ini — cek apakah ortu sudah punya tiket lokal
                val today = getCurrentDate()
                val cursorLokal = db.rawQuery(
                    "SELECT ai.${DatabaseHelper.COL_ANTITEM_NOMOR}, ai.${DatabaseHelper.COL_ANTITEM_ANAK_ID}, " +
                            "       ai.${DatabaseHelper.COL_ANTITEM_STATUS} " +
                            "FROM ${DatabaseHelper.TABLE_ANTRIAN_ITEM} ai " +
                            "JOIN ${DatabaseHelper.TABLE_ANTRIAN} a ON ai.${DatabaseHelper.COL_ANTITEM_ANTRIAN_ID} = a.${DatabaseHelper.COL_ANT_ID} " +
                            "WHERE a.${DatabaseHelper.COL_ANT_TANGGAL} = ? " +
                            "  AND ai.${DatabaseHelper.COL_ANTITEM_ORTU_ID} = ? " +
                            "  AND ai.${DatabaseHelper.COL_ANTITEM_STATUS} = 1 " +
                            "LIMIT 1",
                    arrayOf(today, ortuId)
                )
                if (cursorLokal.moveToFirst()) {
                    val nomor   = cursorLokal.getInt(0)
                    val anakId  = cursorLokal.getString(1) ?: ""
                    cursorLokal.close()

                    val cursorAnak = db.rawQuery(
                        "SELECT ${DatabaseHelper.COL_ANAK_NAMA} FROM ${DatabaseHelper.TABLE_ANAK} " +
                                "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?", arrayOf(anakId)
                    )
                    val namaAnak = if (cursorAnak.moveToFirst()) cursorAnak.getString(0) ?: "" else ""
                    cursorAnak.close()

                    tiket = TiketState(
                        nomorAntrian   = nomor.toString().padStart(3, '0'),
                        namaAnak       = namaAnak,
                        tanggal        = formatTanggalTiket(today),
                        nomorDipanggil = "--",
                        sisaGiliran    = "--",
                        sudahAmbil     = true
                    )
                } else {
                    cursorLokal.close()
                    errorMessage = "Tidak ada antrian aktif hari ini."
                }
                db.close()
                isLoading = false
                return
            }

            // 3. Ambil semua item antrian dari server
            val items = AntrianApiService.getAntrianItems(context, antrianAktif.id)
            android.util.Log.d("TIKET", "Total items: ${items.size}")

            // 4. Cari item milik ortu ini (status 1 = menunggu)
            val itemSaya = items.find { it.ortuId == ortuId && it.status == 1 }
            android.util.Log.d("TIKET", "itemSaya: nomor=${itemSaya?.nomor}, anakId=${itemSaya?.anakId}")

            // 5. Nomor yang sedang dipanggil = nomorSaatIni dari header antrian
            //    Kalau 0 berarti belum ada yang dipanggil → tampil "--"
            val nomorSaatIni   = antrianAktif.nomorSaatIni
            val nomorDipanggil = if (nomorSaatIni > 0)
                nomorSaatIni.toString().padStart(3, '0')
            else "--"

            if (itemSaya != null) {
                // Ambil nama anak
                val cursorAnak = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ANAK_NAMA} FROM ${DatabaseHelper.TABLE_ANAK} " +
                            "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
                    arrayOf(itemSaya.anakId)
                )
                val namaAnak = if (cursorAnak.moveToFirst()) cursorAnak.getString(0) ?: "Anak" else "Anak"
                cursorAnak.close()

                // Sisa giliran = nomor_saya - nomorSaatIni - 1
                // Contoh: saya nomor 7, dipanggil sekarang 1 → sisa = 7 - 1 - 1 = 5
                // Kalau belum ada yang dipanggil (nomorSaatIni=0) → sisa = nomor_saya - 1
                val sisa = if (nomorSaatIni > 0)
                    maxOf(0, itemSaya.nomor - nomorSaatIni - 1)
                else
                    maxOf(0, itemSaya.nomor - 1)

                tiket = TiketState(
                    nomorAntrian   = itemSaya.nomor.toString().padStart(3, '0'),
                    namaAnak       = namaAnak,
                    tanggal        = formatTanggalTiket(getCurrentDate()),
                    nomorDipanggil = nomorDipanggil,
                    sisaGiliran    = sisa.toString(),
                    sudahAmbil     = true
                )
            } else {
                // Ortu belum ambil antrian
                tiket = TiketState(
                    nomorAntrian   = "---",
                    namaAnak       = "",
                    tanggal        = formatTanggalTiket(getCurrentDate()),
                    nomorDipanggil = nomorDipanggil,
                    sisaGiliran    = "--",
                    sudahAmbil     = false
                )
            }

            db.close()
        } catch (e: Exception) {
            android.util.Log.e("TIKET_ANTRIAN", "Error: ${e.message}", e)
            errorMessage = "Gagal memuat data antrian: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Load pertama kali
    LaunchedEffect(username) {
        loadData()
    }

    // Auto-refresh setiap 30 detik
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (username.isNotBlank()) {
                try { loadData() } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        backgroundColor = TikBackgroundDark,
        bottomBar = {
            BottomNavBarOrtuTiket(
                onHomeClick    = onNavigateToHome,
                onTicketClick  = {}, // Sudah di halaman ini
                onFoodClick    = onNavigateToFood,
                onProfileClick = { showLogoutDialog = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSimple()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = TikHeaderBlue)
                                Spacer(Modifier.height(12.dp))
                                Text("Memuat data antrian...", color = TikTextGrey, fontSize = 14.sp)
                            }
                        }
                    }

                    errorMessage.isNotEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TikErrorBg)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                color = TikErrorText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Tombol Refresh
                        Button(
                            onClick = { scope.launch { loadData() } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = TikHeaderBlue)
                        ) {
                            Text("Coba Lagi", color = TikTextWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        // Kartu Tiket Biru
                        CardTiketUtama(
                            nomorAntrian       = tiket.nomorAntrian,
                            namaAnak           = if (tiket.namaAnak.isNotBlank()) tiket.namaAnak else "Belum ambil antrian",
                            tanggal            = tiket.tanggal,
                            dipanggilSekarang  = tiket.nomorDipanggil,
                            sisaGiliran        = tiket.sisaGiliran
                        )

                        // Kartu Info Hijau Mint
                        val pesan = when {
                            !tiket.sudahAmbil ->
                                "Anda belum mengambil antrian. Silakan ambil antrian di menu sebelumnya."
                            tiket.nomorDipanggil == "--" ->
                                "Belum ada antrian yang dipanggil. Harap menunggu."
                            tiket.sudahAmbil && tiket.nomorDipanggil == tiket.nomorAntrian ->
                                "Giliran Anda! Silakan menuju loket pemeriksaan."
                            else ->
                                "Nomor ${tiket.nomorDipanggil} sedang dipanggil. Anda akan dipanggil saat giliran mendekati nomor Anda."
                        }
                        CardInfoMint(pesan = pesan)

                        // Tombol Refresh Manual
                        TextButton(
                            onClick = { scope.launch { loadData() } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("↻  Perbarui Status", color = TikTextGrey, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Helper: format tanggal "2026-06-14" → "Minggu, 14 Juni 2026"
// ─────────────────────────────────────────────────────────────
private fun formatTanggalTiket(raw: String): String {
    return try {
        val sdfIn  = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
        val sdfOut = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        sdfOut.format(sdfIn.parse(raw)!!)
    } catch (_: Exception) { raw }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: KARTU TIKET & INFO
// ════════════════════════════════════════════════════════════

@Composable
private fun CardTiketUtama(
    nomorAntrian     : String,
    namaAnak         : String,
    tanggal          : String,
    dipanggilSekarang: String,
    sisaGiliran      : String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TikTicketBlue)
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = "NOMOR ANTRIAN ANDA",
            color      = TikTextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text       = nomorAntrian,
            color      = TikTextWhite,
            fontSize   = 80.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 80.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text       = namaAnak,
            color      = TikTextWhite,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text     = tanggal,
            color    = TikTextWhite.copy(alpha = 0.8f),
            fontSize = 13.sp
        )

        Divider(
            color     = TikTextWhite.copy(alpha = 0.4f),
            thickness = 1.dp,
            modifier  = Modifier.padding(vertical = 16.dp)
        )

        Row(
            modifier                = Modifier.fillMaxWidth(),
            horizontalArrangement   = Arrangement.SpaceEvenly,
            verticalAlignment       = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Dipanggil Sekarang", color = TikTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(dipanggilSekarang, color = TikTextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(TikTextWhite.copy(alpha = 0.5f))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Sisa Giliran", color = TikTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(sisaGiliran, color = TikTextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardInfoMint(pesan: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TikMintInfoBg)
            .padding(16.dp)
    ) {
        Text(
            text       = pesan,
            color      = TikMintInfoText,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: HEADER & BOTTOM NAV
// ════════════════════════════════════════════════════════════

@Composable
private fun HeaderSimple() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TikHeaderBlue)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "MyPosyandu",
            color      = TikTextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BottomNavBarOrtuTiket(
    onHomeClick   : () -> Unit,
    onTicketClick : () -> Unit,
    onFoodClick   : () -> Unit,
    onProfileClick: () -> Unit
) {
    data class NavEntry(val icon: ImageVector, val label: String, val isActive: Boolean, val action: () -> Unit)
    val entries = listOf(
        NavEntry(Icons.Outlined.Home,              "Home",    false, onHomeClick),
        NavEntry(Icons.Outlined.ConfirmationNumber,"Antrian", true,  onTicketClick),
        NavEntry(Icons.Outlined.Restaurant,        "Menu",    false, onFoodClick),
        NavEntry(Icons.Outlined.PowerSettingsNew,  "Logout",  false, onProfileClick)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        entries.forEach { entry ->
            val tint = if (entry.isActive) TikHeaderBlue else Color.White.copy(alpha = 0.45f)
            Column(
                modifier = Modifier.clickable(onClick = entry.action).padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(entry.icon, contentDescription = entry.label, tint = tint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(2.dp))
                Text(entry.label, color = tint, fontSize = 10.sp,
                    fontWeight = if (entry.isActive) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TiketAntrianScreenPreview() {
    TiketAntrianScreen(username = "test_user")
}