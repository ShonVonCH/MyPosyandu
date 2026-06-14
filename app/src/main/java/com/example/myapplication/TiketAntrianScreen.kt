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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Warna Lokal
// ─────────────────────────────────────────────────────────────
private val TikHeaderBlue = Color(0xFF1964A3)
private val TikTicketBlue = Color(0xFF1A6EBA)
private val TikBackgroundDark = Color(0xFF121212)
private val TikSurfaceDark = Color(0xFF2A2A2A)
private val TikTextWhite = Color(0xFFFFFFFF)
private val TikTextGrey = Color(0xFF888888)
private val TikMintInfoBg = Color(0xFF98E6C8)
private val TikMintInfoText = Color(0xFF14634B)

@Composable
fun TiketAntrianScreen(
    username: String = "",
    onNavigateToHome: () -> Unit = {},
    onNavigateToTicket: () -> Unit = {},
    onNavigateToFood: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ TAMBAH: State untuk data dinamis
    var isLoading by remember { mutableStateOf(true) }
    var nomorAntrian by remember { mutableStateOf("---") }
    var namaAnak by remember { mutableStateOf("") }
    var tanggalAntrian by remember { mutableStateOf("") }
    var nomorDipanggil by remember { mutableStateOf("---") }
    var sisaGiliran by remember { mutableStateOf("--") }
    var errorMessage by remember { mutableStateOf("") }

    // Load data antrian dari API
    LaunchedEffect(username) {
        if (username.isBlank()) {
            isLoading = false
            errorMessage = "Username tidak tersedia"
            return@LaunchedEffect
        }

        try {
            // Ambil antrian aktif hari ini
            val antrianAktif = AntrianApiService.getAntrianAktifHariIni(context)

            if (antrianAktif != null) {
                // Ambil semua item antrian
                val items = AntrianApiService.getAntrianItems(context, antrianAktif.id)

                // Cari item milik user ini (berdasarkan ortu_id)
                val db = DatabaseHelper(context).readableDatabase
                val cursorOrtu = db.rawQuery(
                    "SELECT ${DatabaseHelper.COL_ORTU_ID} FROM ${DatabaseHelper.TABLE_ORTU} " +
                            "WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ?",
                    arrayOf(username)
                )
                val ortuId = if (cursorOrtu.moveToFirst()) cursorOrtu.getString(0) ?: "" else ""
                cursorOrtu.close()

                // Cari item antrian untuk ortu ini
                val itemSaya = items.find { it.ortuId == ortuId && it.status == 1 }

                if (itemSaya != null) {
                    // Ambil nama anak
                    val cursorAnak = db.rawQuery(
                        "SELECT ${DatabaseHelper.COL_ANAK_NAMA} FROM ${DatabaseHelper.TABLE_ANAK} " +
                                "WHERE ${DatabaseHelper.COL_ANAK_ID} = ?",
                        arrayOf(itemSaya.anakId)
                    )
                    val namaAnakDb = if (cursorAnak.moveToFirst()) cursorAnak.getString(0) ?: "Anak" else "Anak"
                    cursorAnak.close()

                    nomorAntrian = itemSaya.nomor.toString().padStart(3, '0')
                    namaAnak = namaAnakDb
                    tanggalAntrian = antrianAktif.id.take(10) // atau format tanggal dari antrian

                    // Hitung sisa giliran
                    val nomorSaatIni = antrianAktif.nomorSaatIni
                    val sisa = items.count { it.status == 1 && it.nomor > nomorSaatIni && it.nomor < itemSaya.nomor }
                    sisaGiliran = sisa.toString()
                }

                // Nomor yang sedang dipanggil
                val itemDipanggil = items.find { it.status == 0 }
                nomorDipanggil = itemDipanggil?.nomor?.toString()?.padStart(3, '0') ?: "000"

                db.close()
            } else {
                errorMessage = "Tidak ada antrian aktif hari ini"
            }
        } catch (e: Exception) {
            android.util.Log.e("TIKET_ANTRIAN", "Error: ${e.message}", e)
            errorMessage = "Gagal memuat data: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Auto-refresh setiap 30 detik
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30000) // 30 detik
            // Refresh data...
        }
    }

    Scaffold(
        backgroundColor = TikBackgroundDark,
        bottomBar = {
            BottomNavBarOrtuTiket(
                onHomeClick = onNavigateToHome,
                onTicketClick = onNavigateToTicket,
                onFoodClick = onNavigateToFood,
                onProfileClick = onNavigateToProfile
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
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TikHeaderBlue)
                    }
                } else if (errorMessage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3A1A1A))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDD6F6F),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Kartu Tiket Biru
                    CardTiketUtama(
                        nomorAntrian = nomorAntrian,
                        namaTanggal = if (namaAnak.isNotBlank()) "$namaAnak - $tanggalAntrian" else "Belum ada antrian",
                        dipanggilSekarang = nomorDipanggil,
                        sisaGiliran = sisaGiliran
                    )

                    // Kartu Info Hijau Mint
                    CardInfoMint(
                        pesan = when {
                            nomorAntrian == "---" -> "Anda belum mengambil antrian. Silakan ambil antrian di menu sebelumnya."
                            nomorDipanggil == nomorAntrian -> "Giliran Anda! Silakan menuju loket pemeriksaan."
                            else -> "Anda akan dipanggil saat giliran mendekati antrian Anda. Nomor ${nomorDipanggil} sedang dipanggil."
                        }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  KOMPONEN: KARTU TIKET & INFO
// ════════════════════════════════════════════════════════════

@Composable
private fun CardTiketUtama(
    nomorAntrian: String,
    namaTanggal: String,
    dipanggilSekarang: String,
    sisaGiliran: String
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
            text = "NOMOR ANTRIAN ANDA",
            color = TikTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = nomorAntrian,
            color = TikTextWhite,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 80.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = namaTanggal,
            color = TikTextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Divider(
            color = TikTextWhite,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Baris Bawah: Dipanggil Sekarang & Sisa Giliran
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom Kiri
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Dipanggil Sekarang", color = TikTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(dipanggilSekarang, color = TikTextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            // Garis Pemisah Vertikal
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(TikTextWhite.copy(alpha = 0.5f))
            )

            // Kolom Kanan
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Sisa Giliran", color = TikTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
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
            text = pesan,
            color = TikMintInfoText,
            fontSize = 16.sp,
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
            text = "MyPosyandu",
            color = TikTextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BottomNavBarOrtuTiket(
    onHomeClick: () -> Unit,
    onTicketClick: () -> Unit,
    onFoodClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TikSurfaceDark)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavIconPrivate(icon = Icons.Outlined.Home, isActive = false, onClick = onHomeClick)
        NavIconPrivate(icon = Icons.Outlined.ConfirmationNumber, isActive = true, onClick = onTicketClick)
        NavIconPrivate(icon = Icons.Outlined.Restaurant, isActive = false, onClick = onFoodClick)
        NavIconPrivate(icon = Icons.Outlined.Person, isActive = false, onClick = onProfileClick)
    }
}

@Composable
private fun NavIconPrivate(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val tintColor = if (isActive) TikHeaderBlue else TikTextWhite.copy(alpha = 0.5f)
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tintColor,
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun TiketAntrianScreenPreview() {
    TiketAntrianScreen(username = "test_user")
}