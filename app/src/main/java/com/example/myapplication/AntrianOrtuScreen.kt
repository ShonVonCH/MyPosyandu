package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Restaurant
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val AntHeaderBlue        = Color(0xFF1964A3)
private val AntBackgroundDark    = Color(0xFF121212)
private val AntSurfaceDark       = Color(0xFF2A2A2A)
private val AntSurfaceDarkBorder = Color(0xFF444444)
private val AntTextWhite         = Color(0xFFFFFFFF)
private val AntTextGrey          = Color(0xFF888888)
private val AntAvatarMint        = Color(0xFF98E6C8)
private val AntNeonGreen         = Color(0xFF00C896)
private val AntNeonAmber         = Color(0xFFFFC947)
private val AntButtonBlue        = Color(0xFF1964A3)

data class AnakAntrianData(
    val id                : String,
    val nama              : String,
    val umurBulan         : Int,
    val nomorAntrian      : String? = null,
    val sudahAmbilAntrian : Boolean = false,
    // BARU: berapa orang yang masih menunggu di depan nomor anak ini
    val orangDiDepan      : Int? = null
)

@Composable
fun AntrianOrtuScreen(
    userId             : String    = "",
    onNavigateBack     : () -> Unit = {},
    onNavigateToHome   : () -> Unit = {},
    onNavigateToTicket : () -> Unit = {},
    onNavigateToFood   : () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToLogout : () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var namaOrtu         by remember { mutableStateOf("") }
    var anakList         by remember { mutableStateOf<List<AnakAntrianData>>(emptyList()) }
    var posyanduInfo     by remember { mutableStateOf("") }
    var isLoading        by remember { mutableStateOf(false) }
    var statusMessage    by remember { mutableStateOf("") }
    var errorMessage     by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // BARU: nomor yang sedang dilayani sekarang (dipakai di header, gaya
    // "papan display" rumah sakit) — 0 kalau belum ada yang dipanggil.
    var nomorSedangDilayani by remember { mutableStateOf(0) }

    // ── Fungsi load data ────────────────────────────────────────────────────
    suspend fun loadData() {
        if (userId.isBlank()) {
            errorMessage = "Error: User ID tidak tersedia. Silakan login ulang."
            return
        }

        isLoading    = true
        errorMessage = ""

        try {
            val db = DatabaseHelper(context).readableDatabase

            // Nama ortu
            if (namaOrtu.isBlank()) {
                val cursorOrtu = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ORTU_NAMA}, ${DatabaseHelper.COL_ORTU_POSYANDU_ID} " +
                            "FROM ${DatabaseHelper.TABLE_ORTU} " +
                            "WHERE ${DatabaseHelper.COL_ORTU_ID} = ?",
                    arrayOf(userId)
                )
                if (cursorOrtu.moveToFirst()) {
                    namaOrtu = cursorOrtu.getString(0) ?: ""
                } else {
                    val cursorUsers = db.rawQuery(
                        "SELECT ${DatabaseHelper.COL_USERS_NAMA} " +
                                "FROM ${DatabaseHelper.TABLE_USERS} " +
                                "WHERE ${DatabaseHelper.COL_USERS_ID} = ?",
                        arrayOf(userId)
                    )
                    if (cursorUsers.moveToFirst()) namaOrtu = cursorUsers.getString(0) ?: ""
                    cursorUsers.close()
                }
                cursorOrtu.close()
            }

            // Posyandu info (load sekali saja)
            if (posyanduInfo.isBlank()) {
                val cursorPosyandu = db.rawQuery(
                    "SELECT p.${DatabaseHelper.COL_POSYANDU_NAMA}, p.${DatabaseHelper.COL_POSYANDU_KELURAHAN}, " +
                            "p.${DatabaseHelper.COL_POSYANDU_RW}, p.${DatabaseHelper.COL_POSYANDU_ALAMAT} " +
                            "FROM ${DatabaseHelper.TABLE_POSYANDU} p " +
                            "JOIN ${DatabaseHelper.TABLE_ORTU} o ON p.${DatabaseHelper.COL_POSYANDU_ID} = o.${DatabaseHelper.COL_ORTU_POSYANDU_ID} " +
                            "WHERE o.${DatabaseHelper.COL_ORTU_ID} = ? LIMIT 1",
                    arrayOf(userId)
                )
                if (cursorPosyandu.moveToFirst()) {
                    val namaPos = cursorPosyandu.getString(0) ?: "Posyandu"
                    val kel     = cursorPosyandu.getString(1) ?: ""
                    val rw      = cursorPosyandu.getString(2) ?: ""
                    val alm     = cursorPosyandu.getString(3) ?: ""
                    posyanduInfo = buildString {
                        append(namaPos)
                        if (kel.isNotBlank()) append(", Kel. $kel")
                        if (rw.isNotBlank()) append(", RW-$rw")
                        if (alm.isNotBlank()) append(" - $alm")
                    }
                }
                cursorPosyandu.close()
            }

            // Ambil posyandu_id ortu untuk filter antrian
            var posyanduId: String? = null
            val cursorPosId = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ORTU_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_ORTU} " +
                        "WHERE ${DatabaseHelper.COL_ORTU_ID} = ?",
                arrayOf(userId)
            )
            if (cursorPosId.moveToFirst()) posyanduId = cursorPosId.getString(0)
            cursorPosId.close()

            // Antrian aktif dari API (filter per posyandu)
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context, posyanduId)
            val antrianItemsHariIni = if (antrianAktif != null)
                AntrianApiService.getAntrianItems(context, antrianAktif.id)
            else emptyList()

            // BARU: update nomor yang sedang dilayani buat header
            nomorSedangDilayani = antrianAktif?.nomorSaatIni ?: 0

            // Map anak_id → item milik userId yang masih menunggu (status=1)
            val itemMenungguByAnak = antrianItemsHariIni
                .filter { it.ortuId == userId && it.status == 1 }
                .associateBy { it.anakId }

            // Fallback lokal: cek antrian_item dari local DB
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            fun getNomorLokal(anakId: String): Int? = try {
                val cur = db.rawQuery(
                    "SELECT ai.${DatabaseHelper.COL_ANTITEM_NOMOR}, ai.${DatabaseHelper.COL_ANTITEM_STATUS} " +
                            "FROM ${DatabaseHelper.TABLE_ANTRIAN_ITEM} ai " +
                            "JOIN ${DatabaseHelper.TABLE_ANTRIAN} a " +
                            "  ON ai.${DatabaseHelper.COL_ANTITEM_ANTRIAN_ID} = a.${DatabaseHelper.COL_ANT_ID} " +
                            "WHERE a.${DatabaseHelper.COL_ANT_TANGGAL} = ? " +
                            "  AND ai.${DatabaseHelper.COL_ANTITEM_ANAK_ID} = ? " +
                            "  AND ai.${DatabaseHelper.COL_ANTITEM_ORTU_ID} = ? " +
                            "  AND ai.${DatabaseHelper.COL_ANTITEM_STATUS} = 1 " +
                            "LIMIT 1",
                    arrayOf(today, anakId, userId)
                )
                val nomor = if (cur.moveToFirst()) cur.getInt(0) else null
                cur.close()
                nomor
            } catch (e: Exception) { null }

            // Query daftar anak
            val cursorAnak = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ANAK_ID}, ${DatabaseHelper.COL_ANAK_NAMA}, " +
                        "${DatabaseHelper.COL_ANAK_TGL_LAHIR} " +
                        "FROM ${DatabaseHelper.TABLE_ANAK} " +
                        "WHERE ${DatabaseHelper.COL_ANAK_ORTU_ID} = ? " +
                        "ORDER BY ${DatabaseHelper.COL_ANAK_NAMA} ASC",
                arrayOf(userId)
            )

            val newList = mutableListOf<AnakAntrianData>()
            while (cursorAnak.moveToNext()) {
                val id       = cursorAnak.getString(0) ?: continue
                val nama     = cursorAnak.getString(1) ?: ""
                val tglLahir = cursorAnak.getString(2) ?: ""
                val umur     = hitungUmurBulanAntrian(tglLahir)

                val itemApi = itemMenungguByAnak[id]
                val nomor   = itemApi?.nomor ?: getNomorLokal(id)

                val sudahAmbil = nomor != null
                val nomorStr   = nomor?.let { formatNomorAntrian(it) }

                // BARU: hitung berapa orang yang masih menunggu di depan nomor ini
                val orangDiDepan = nomor?.let { hitungOrangDiDepan(antrianItemsHariIni, it) }

                newList.add(AnakAntrianData(
                    id                = id,
                    nama              = nama,
                    umurBulan         = umur,
                    nomorAntrian      = nomorStr,
                    sudahAmbilAntrian = sudahAmbil,
                    orangDiDepan      = orangDiDepan
                ))
            }
            cursorAnak.close()
            db.close()

            anakList = newList

        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_ORTU", "Error loading data: ${e.message}", e)
            errorMessage = "Error memuat data: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // ── Load awal ───────────────────────────────────────────────────────────
    LaunchedEffect(userId) {
        loadData()
    }

    // ── Polling otomatis buat sync nomor sedang dilayani + status ──────────
    // Interval dipercepat jadi 8 detik biar terasa "hidup" kayak papan
    // display rumah sakit, dan tetap jalan kalau ada anak yang sedang
    // menunggu, supaya baterai/network nggak boros pas nggak ada antrian.
    LaunchedEffect(userId) {
        while (true) {
            delay(8_000L)
            if (anakList.any { it.sudahAmbilAntrian }) {
                try {
                    loadData()
                } catch (e: Exception) {
                    android.util.Log.e("ANTRIAN_ORTU", "Polling error: ${e.message}")
                }
            }
        }
    }

    // ── Logout Dialog ───────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor  = AntSurfaceDark,
            title = {
                Text("Logout", color = AntTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("Yakin ingin keluar dari akun?", color = AntTextGrey, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    try {
                        val db = DatabaseHelper(context).writableDatabase
                        db.execSQL("PRAGMA foreign_keys = OFF")
                        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_USERS}")
                        db.execSQL("PRAGMA foreign_keys = ON")
                        db.close()
                    } catch (e: Exception) {
                        android.util.Log.e("LOGOUT_ERROR", "Error logout: ${e.message}")
                    }
                    onNavigateToLogout()
                }) {
                    Text("Logout", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = AntTextGrey)
                }
            }
        )
    }

    // ── Ambil antrian ───────────────────────────────────────────────────────
    fun ambilAntrian(anakId: String, anakNama: String) {
        if (userId.isBlank()) {
            statusMessage = "Error: User ID tidak tersedia"
            return
        }
        scope.launch {
            isLoading     = true
            statusMessage = ""
            try {
                val result = AntrianApiService.ambilAntrian(context, anakId, userId)
                if (result.success) {
                    statusMessage = "Berhasil ambil antrian nomor ${result.nomorAntrian} untuk $anakNama"
                    loadData()
                } else {
                    isLoading     = false
                    statusMessage = "Gagal: ${result.message}"
                }
            } catch (e: Exception) {
                isLoading     = false
                statusMessage = "Gagal: ${e.message}"
                android.util.Log.e("ANTRIAN_ORTU", "Error ambil antrian: ${e.message}", e)
            }
        }
    }

    Scaffold(
        backgroundColor = AntBackgroundDark,
        bottomBar = {
            BottomNavBarAntrian(
                onHomeClick    = onNavigateToHome,
                onTicketClick  = {},
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
            HeaderAntrianOrtu(
                namaOrtu     = namaOrtu,
                posyanduInfo = posyanduInfo,
                onBack       = onNavigateBack
            )

            // BARU: papan "sedang dilayani" gaya rumah sakit, tampil kalau
            // ada anak yang sedang antre hari ini.
            if (anakList.any { it.sudahAmbilAntrian }) {
                PapanSedangDilayani(nomor = nomorSedangDilayani)
            }

            Text(
                text       = "Anak saya",
                color      = AntTextWhite,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            )

            if (errorMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3A1A1A))
                        .padding(12.dp)
                ) {
                    Text(text = errorMessage, color = Color(0xFFDD6F6F), fontSize = 13.sp)
                }
            }

            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    isLoading && anakList.isEmpty() -> {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AntNeonGreen)
                        }
                    }
                    anakList.isEmpty() -> {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text     = "Belum ada anak terdaftar",
                                color    = AntTextGrey,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        anakList.forEach { anak ->
                            if (anak.sudahAmbilAntrian && anak.nomorAntrian != null) {
                                CardAntrianAktif(
                                    nama         = anak.nama,
                                    usia         = "${anak.umurBulan} Bulan",
                                    nomorAntrian = anak.nomorAntrian,
                                    orangDiDepan = anak.orangDiDepan ?: 0,
                                    onClick      = onNavigateToTicket
                                )
                            } else {
                                CardAmbilAntrian(
                                    nama      = anak.nama,
                                    usia      = "${anak.umurBulan} Bulan",
                                    onClick   = { ambilAntrian(anak.id, anak.nama) },
                                    isLoading = isLoading
                                )
                            }
                        }
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (statusMessage.startsWith("Berhasil")) Color(0xFF1A3A2A)
                            else Color(0xFF3A1A1A)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text     = statusMessage,
                        color    = if (statusMessage.startsWith("Berhasil")) Color(0xFF6FDDAA)
                        else Color(0xFFDD6F6F),
                        fontSize = 13.sp
                    )
                }
            }

            TextButton(
                onClick  = { scope.launch { loadData() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = AntTextGrey,
                        modifier    = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("↻  Perbarui Status", color = AntTextGrey, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun hitungUmurBulanAntrian(tanggalLahir: String): Int {
    return try {
        val lahir = when {
            tanggalLahir.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                val parts = tanggalLahir.split("/")
                LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            tanggalLahir.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ->
                LocalDate.parse(tanggalLahir, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            else -> LocalDate.now()
        }
        ChronoUnit.MONTHS.between(lahir, LocalDate.now()).toInt().coerceAtLeast(0)
    } catch (e: Exception) {
        android.util.Log.e("AntrianOrtu", "Gagal hitung umur: $tanggalLahir", e)
        0
    }
}

@Composable
private fun PapanSedangDilayani(nomor: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, AntSurfaceDarkBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = "SEDANG DILAYANI",
            color      = AntTextGrey,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = if (nomor > 0) formatNomorAntrian(nomor) else "—",
            color      = AntNeonGreen,
            fontSize   = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BottomNavBarAntrian(
    onHomeClick   : () -> Unit,
    onTicketClick : () -> Unit,
    onFoodClick   : () -> Unit,
    onProfileClick: () -> Unit
) {
    data class NavEntry(val icon: ImageVector, val label: String, val isActive: Boolean, val action: () -> Unit)
    val entries = listOf(
        NavEntry(Icons.Outlined.Home,               "Home",    false, onHomeClick),
        NavEntry(Icons.Outlined.ConfirmationNumber, "Antrian", true,  onTicketClick),
        NavEntry(Icons.Outlined.Restaurant,         "Menu",    false, onFoodClick),
        NavEntry(Icons.Outlined.PowerSettingsNew,   "Logout",  false, onProfileClick)
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
            val tint = if (entry.isActive) AntHeaderBlue else Color.White.copy(alpha = 0.45f)
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

@Composable
private fun HeaderAntrianOrtu(namaOrtu: String = "", posyanduInfo: String = "", onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AntHeaderBlue)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = Color.White,
                    modifier           = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
                Text(
                    text       = "MyPosyandu",
                    color      = AntTextWhite,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f),
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column {
                Text("Selamat datang,", color = AntTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(namaOrtu.ifBlank { "Orang Tua" }, color = AntTextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(posyanduInfo.ifBlank { "Posyandu" }, color = AntTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CardAntrianAktif(
    nama        : String,
    usia        : String,
    nomorAntrian: String,
    orangDiDepan: Int = 0,
    onClick     : () -> Unit = {}
) {
    val isSegera = orangDiDepan <= 0
    val aksenColor = if (isSegera) AntNeonAmber else AntNeonGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AntSurfaceDark)
            .border(2.dp, aksenColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AntAvatarMint))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(nama, color = AntTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(usia, color = AntTextGrey, fontSize = 14.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(nomorAntrian, color = aksenColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Antrian Anda", color = aksenColor.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(aksenColor)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // BARU: pesan dinamis sesuai posisi antrian, bukan cuma "Menunggu"
            Text(
                text = if (isSegera)
                    "Segera giliran Anda — mohon bersiap"
                else
                    "Menunggu — masih ada $orangDiDepan orang di depan Anda",
                color      = Color(0xFF12261F),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CardAmbilAntrian(
    nama     : String,
    usia     : String,
    onClick  : () -> Unit,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AntSurfaceDark)
            .border(1.dp, AntSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AntAvatarMint))
            Column {
                Text(nama, color = AntTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(usia, color = AntTextGrey, fontSize = 14.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isLoading) Color(0xFF444444) else AntButtonBlue)
                .clickable(enabled = !isLoading, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color       = AntTextWhite,
                    modifier    = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text       = "Ambil antrian",
                    color      = AntTextWhite,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AntrianOrtuScreenPreview() {
    AntrianOrtuScreen(userId = "user-123")
}