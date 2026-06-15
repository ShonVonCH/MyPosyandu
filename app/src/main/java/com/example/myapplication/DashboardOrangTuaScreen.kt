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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
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
import java.text.SimpleDateFormat
import java.util.*

private val DashHeaderBlue        = Color(0xFF1964A3)
private val DashBackgroundDark    = Color(0xFF121212)
private val DashSurfaceDark       = Color(0xFF2A2A2A)
private val DashSurfaceDarkBorder = Color(0xFF444444)
private val DashTextWhite         = Color(0xFFFFFFFF)
private val DashTextGrey          = Color(0xFF888888)
private val DashAvatarMint        = Color(0xFF98E6C8)

@Composable
fun DashboardOrangTuaScreen(
    username              : String         = "",
    onNavigateToDetailAnak: (String) -> Unit = {},
    onNavigateToHome      : () -> Unit       = {},
    onNavigateToTicket    : () -> Unit       = {},
    onNavigateToFood      : () -> Unit       = {},
    onNavigateToProfile   : () -> Unit       = {},
    onNavigateToLogout    : () -> Unit       = {}
) {
    val context = LocalContext.current

    var namaOrtu         by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var anakList         by remember { mutableStateOf<List<AnakData>>(emptyList()) }
    var jadwalBerikutnya by remember { mutableStateOf<JadwalBerikutnya?>(null) }
    var posyanduInfo     by remember { mutableStateOf("") }

    // FIX: gunakan Unit sebagai key supaya hanya jalan sekali saat komposisi pertama.
    // Data di-reload manual lewat LaunchedEffect(username) tapi pastikan
    // list direset di awal agar tidak double.
    LaunchedEffect(username) {
        if (username.isBlank()) return@LaunchedEffect

        // ── RESET state sebelum query baru ──────────────────────────────
        namaOrtu         = ""
        posyanduInfo     = ""
        jadwalBerikutnya = null
        anakList         = emptyList()   // ← kunci fix double data

        val db = DatabaseHelper(context).readableDatabase

        // Ambil data ortu berdasarkan username
        val cursorOrtu = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ORTU_ID}, ${DatabaseHelper.COL_ORTU_NAMA} " +
                    "FROM ${DatabaseHelper.TABLE_ORTU} " +
                    "WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ?",
            arrayOf(username)
        )
        var ortuId = ""
        if (cursorOrtu.moveToFirst()) {
            ortuId   = cursorOrtu.getString(0) ?: ""
            namaOrtu = cursorOrtu.getString(1) ?: ""
        }
        cursorOrtu.close()

        // Ambil posyandu info dari user login
        val cursorUser = db.rawQuery(
            """
            SELECT p.${DatabaseHelper.COL_POSYANDU_NAMA}, p.${DatabaseHelper.COL_POSYANDU_KELURAHAN},
                   p.${DatabaseHelper.COL_POSYANDU_RW}, p.${DatabaseHelper.COL_POSYANDU_ALAMAT}
            FROM ${DatabaseHelper.TABLE_USERS} u
            LEFT JOIN ${DatabaseHelper.TABLE_POSYANDU} p
                   ON u.${DatabaseHelper.COL_USERS_POSYANDU_ID} = p.${DatabaseHelper.COL_POSYANDU_ID}
            LIMIT 1
            """.trimIndent(), null
        )
        if (cursorUser.moveToFirst()) {
            val nama = cursorUser.getString(0) ?: "Posyandu"
            val kel  = cursorUser.getString(1) ?: ""
            val rw   = cursorUser.getString(2) ?: ""
            val alm  = cursorUser.getString(3) ?: ""
            posyanduInfo = buildString {
                append(nama)
                if (kel.isNotBlank()) append(", Kel. $kel")
                if (rw.isNotBlank()) append(", RW-$rw")
                if (alm.isNotBlank()) append(" - $alm")
            }
        }
        cursorUser.close()

        // Ambil jadwal posyandu berikutnya
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursorJadwal = db.rawQuery(
            """
            SELECT ${DatabaseHelper.COL_JADWAL_TANGGAL}, ${DatabaseHelper.COL_JADWAL_JAM_MULAI},
                   ${DatabaseHelper.COL_JADWAL_JAM_SELESAI}, ${DatabaseHelper.COL_JADWAL_LOKASI}
            FROM ${DatabaseHelper.TABLE_JADWAL_POSYANDU}
            WHERE ${DatabaseHelper.COL_JADWAL_TANGGAL} >= ?
              AND LOWER(${DatabaseHelper.COL_JADWAL_STATUS}) = 'terjadwal'
            ORDER BY ${DatabaseHelper.COL_JADWAL_TANGGAL} ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(today)
        )
        if (cursorJadwal.moveToFirst()) {
            val tgl        = cursorJadwal.getString(0) ?: ""
            val jamMulai   = cursorJadwal.getString(1) ?: ""
            val jamSelesai = cursorJadwal.getString(2) ?: ""
            val lokasi     = cursorJadwal.getString(3) ?: ""

            val tglFormatted = try {
                val sdfIn  = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
                val sdfOut = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
                sdfIn.parse(tgl)?.let { sdfOut.format(it) } ?: tgl
            } catch (e: Exception) { tgl }

            jadwalBerikutnya = JadwalBerikutnya(
                lokasi     = lokasi,
                tanggal    = tglFormatted,
                jamMulai   = jamMulai.take(5),
                jamSelesai = jamSelesai.take(5)
            )
        }
        cursorJadwal.close()

        // Ambil anak milik ortu ini — build list baru, bukan append ke lama
        if (ortuId.isNotBlank()) {
            val cursorAnak = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ANAK_ID}, ${DatabaseHelper.COL_ANAK_NAMA}, " +
                        "${DatabaseHelper.COL_ANAK_TGL_LAHIR}, ${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} " +
                        "FROM ${DatabaseHelper.TABLE_ANAK} " +
                        "WHERE ${DatabaseHelper.COL_ANAK_ORTU_ID} = ?",
                arrayOf(ortuId)
            )
            // FIX: buat list baru dari scratch, assign sekali di akhir
            val newList = mutableListOf<AnakData>()
            while (cursorAnak.moveToNext()) {
                val id       = cursorAnak.getString(0) ?: continue
                val nama     = cursorAnak.getString(1) ?: ""
                val tglLahir = cursorAnak.getString(2) ?: ""
                val gender   = cursorAnak.getString(3) ?: "-"
                newList.add(AnakData(
                    id           = id,
                    nama         = nama,
                    umurBulan    = hitungUmurBulan(tglLahir),
                    tanggal      = formatTanggalSingkat(tglLahir),
                    namaOrangTua = namaOrtu,
                    jenisKelamin = when (gender.trim().lowercase()) {
                        "laki-laki", "l" -> "L"
                        "perempuan", "p" -> "P"
                        else             -> gender.ifBlank { "-" }
                    }
                ))
            }
            cursorAnak.close()
            anakList = newList  // ← assign sekali, bukan += / add ke existing state
        }

        db.close()
    }

    // ── Popup Logout ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor  = DashSurfaceDark,
            title = {
                Text("Logout", color = DashTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("Yakin ingin keluar dari akun?", color = DashTextGrey, fontSize = 14.sp)
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
                    Text("Batal", color = DashTextGrey)
                }
            }
        )
    }

    Scaffold(
        backgroundColor = DashBackgroundDark,
        bottomBar = {
            BottomNavBarOrtu(
                onHomeClick    = onNavigateToHome,
                onTicketClick  = onNavigateToTicket,
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
            HeaderDashboardOrtu(
                namaOrangTua    = namaOrtu,
                jumlahAnakTotal = anakList.size,
                posyanduInfo    = posyanduInfo
            )

            BannerJadwalOrtu(jadwal = jadwalBerikutnya)

            Text(
                text       = "Anak saya",
                color      = DashTextWhite,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (anakList.isEmpty()) {
                    Text(
                        text     = "Belum ada anak terdaftar",
                        color    = DashTextGrey,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    anakList.forEach { anak ->
                        CardAnakOrtu(
                            nama     = anak.nama,
                            gender   = anak.jenisKelamin,
                            tglLahir = anak.tanggal,
                            onClick  = { onNavigateToDetailAnak(anak.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ── Bottom Nav ────────────────────────────────────────────────────────────────

@Composable
private fun BottomNavBarOrtu(
    onHomeClick   : () -> Unit,
    onTicketClick : () -> Unit,
    onFoodClick   : () -> Unit,
    onProfileClick: () -> Unit
) {
    data class NavEntry(val icon: ImageVector, val label: String, val isActive: Boolean, val action: () -> Unit)
    val entries = listOf(
        NavEntry(Icons.Outlined.Home,              "Home",    true,  onHomeClick),
        NavEntry(Icons.Outlined.ConfirmationNumber,"Antrian", false, onTicketClick),
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
            val tint = if (entry.isActive) DashHeaderBlue else DashTextWhite.copy(alpha = 0.45f)
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

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderDashboardOrtu(
    namaOrangTua    : String,
    jumlahAnakTotal : Int,
    posyanduInfo    : String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashHeaderBlue)
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text       = "MyPosyandu",
            color      = DashTextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Selamat datang,", color = DashTextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(
                    text       = namaOrangTua.ifBlank { "Orang Tua" },
                    color      = DashTextWhite,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = posyanduInfo.ifBlank { "Posyandu" },
                    color = DashTextWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E86C1))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "$jumlahAnakTotal", color = DashTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "anak", color = DashTextWhite, fontSize = 12.sp)
            }
        }
    }
}

// ── Banner Jadwal ─────────────────────────────────────────────────────────────

@Composable
private fun BannerJadwalOrtu(jadwal: JadwalBerikutnya?) {
    val jadwalText = if (jadwal != null) {
        "${jadwal.tanggal} • ${jadwal.jamMulai}–${jadwal.jamSelesai}"
    } else {
        "Belum ada jadwal terjadwal"
    }
    val lokasiText = jadwal?.lokasi ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DashHeaderBlue)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, "Jadwal", tint = DashTextWhite, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text  = if (jadwal != null) "Jadwal Posyandu berikutnya" else "Jadwal Posyandu",
                    color = DashTextWhite.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Text(
                    text       = jadwalText,
                    color      = DashTextWhite,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (lokasiText.isNotBlank()) {
                    Text(
                        text  = "$lokasiText - Harap datang tepat waktu",
                        color = DashTextWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ── Card Anak ─────────────────────────────────────────────────────────────────

@Composable
private fun CardAnakOrtu(
    nama    : String,
    gender  : String,
    tglLahir: String,
    onClick : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashSurfaceDark)
            .border(1.dp, DashSurfaceDarkBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(DashAvatarMint))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nama,   color = DashTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(gender, color = DashTextGrey,  fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = DashTextGrey)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Tanggal lahir",          color = DashTextGrey,  fontSize = 13.sp)
        Text(tglLahir.ifBlank { "-" }, color = DashTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun DashboardOrangTuaScreenPreview() {
    DashboardOrangTuaScreen(username = "")
}