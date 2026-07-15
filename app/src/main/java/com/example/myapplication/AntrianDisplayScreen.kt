package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════════════════════════════
//  LAYAR DISPLAY / TV — dipasang di ruang tunggu, read-only, auto-refresh.
//  Tidak ada tombol sama sekali, cukup ditaruh di tablet/TV yang
//  menyala terus di ruang tunggu posyandu.
// ═════════════════════════════════════════════════════════════════════

private val DisplayBg     = Color(0xFF0A0A0A)
private val DisplayGreen  = Color(0xFF00C896)
private val DisplayGrey   = Color(0xFF9A9A9A)
private val DisplayCard   = Color(0xFF1C1C1E)
private val DisplayBlue   = Color(0xFF1964A3)

@Composable
fun AntrianDisplayScreen(
    posyanduId  : String,
    namaPosyandu: String = "Posyandu"
) {
    val context = LocalContext.current

    var nomorSedangDilayani by remember { mutableStateOf(0) }
    var antrianBerikutnya   by remember { mutableStateOf<List<Int>>(emptyList()) }
    var totalMenunggu       by remember { mutableStateOf(0) }
    var isLoading           by remember { mutableStateOf(true) }

    suspend fun refresh() {
        try {
            val aktif = AntrianApiService.getAntrianAktifHariIni(context, posyanduId)
            nomorSedangDilayani = aktif?.nomorSaatIni ?: 0

            val items = if (aktif != null) AntrianApiService.getAntrianItems(context, aktif.id) else emptyList()
            val menunggu = getAntrianMenungguUrut(items)

            totalMenunggu     = menunggu.size
            antrianBerikutnya = menunggu.take(5).map { it.nomor }
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_DISPLAY", "refresh error: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    // Display TV nggak butuh interaksi apa pun — cukup auto refresh tiap
    // 5 detik supaya nomor selalu up-to-date buat semua orang yang lihat.
    LaunchedEffect(posyanduId) {
        while (true) {
            refresh()
            delay(5_000L)
        }
    }

    Column(
        modifier            = Modifier.fillMaxSize().background(DisplayBg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = namaPosyandu,
            color      = DisplayGrey,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.weight(1f))

        if (isLoading) {
            CircularProgressIndicator(color = DisplayGreen)
        } else {
            Text(
                text       = "NOMOR SEDANG DILAYANI",
                color      = DisplayGrey,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text       = if (nomorSedangDilayani > 0) formatNomorAntrian(nomorSedangDilayani) else "—",
                color      = DisplayGreen,
                fontSize   = 160.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(48.dp))

            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DisplayCard)
                        .padding(20.dp)
                ) {
                    Text("BERIKUTNYA", color = DisplayGrey, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (antrianBerikutnya.isEmpty()) {
                        Text("Tidak ada antrian", color = DisplayGrey, fontSize = 16.sp)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            antrianBerikutnya.forEach { nomor ->
                                Text(
                                    text       = formatNomorAntrian(nomor),
                                    color      = DisplayBlue,
                                    fontSize   = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DisplayCard)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOTAL MENUNGGU", color = DisplayGrey, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("$totalMenunggu", color = DisplayGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Mohon perhatikan nomor Anda — layar ini diperbarui otomatis",
            color    = DisplayGrey,
            fontSize = 13.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AntrianDisplayScreenPreview() {
    AntrianDisplayScreen(posyanduId = "posyandu-1", namaPosyandu = "Posyandu Melati RW 05")
}