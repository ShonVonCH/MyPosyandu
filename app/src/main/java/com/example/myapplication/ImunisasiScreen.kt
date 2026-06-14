package com.example.myapplication

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import java.util.Calendar
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Design tokens
// ─────────────────────────────────────────────────────────────
private val CardBg             = Color(0xFF2A2A2A)
private val CardBorder         = Color(0xFF444444)
private val DividerColor       = Color(0xFF3A3A3A)
private val BadgeLengkapBg     = Color(0xFF3DD68C)
private val BadgeLengkapText   = Color(0xFF0D4A2A)
private val BadgeTerlambatBg   = Color(0xFFC62828)
private val BadgeTerlambatText = Color(0xFFFFFFFF)
private val BadgePendingBg     = Color(0xFFF9A825)
private val BadgePendingText   = Color(0xFF1A1A1A)
private val IconCheckBg        = Color(0xFF1E7A55)
private val IconLateBg         = Color(0xFFC62828)
private val IconPendingBg      = Color(0xFFF9A825)
private val VaccineInfoGreen   = Color(0xFF7ECFB0)
private val VaccineInfoRed     = Color(0xFFEF5350)
private val VaccineInfoYellow  = Color(0xFFF9A825)
private val TabActiveBg        = Color(0xFF1E6E55)
private val TabActiveText      = TextWhite
private val TabIdleBg          = Color(0xFFEEEEEE)
private val TabIdleText        = Color(0xFF1A1A1A)
private val FieldBg            = Color(0xFF3A3A3A)
private val FieldBorder        = Color(0xFF555555)
private val LabelColor         = Color(0xFFAAAAAA)

// ─────────────────────────────────────────────────────────────
//  Helper: hitung tanggal batas dari tgl lahir + batas bulan
// ─────────────────────────────────────────────────────────────
private fun hitungTanggalBatas(tglLahir: String, batasBulan: Int): String {
    // tglLahir format: yyyy-MM-dd atau dd/MM/yyyy
    return try {
        val parts = if (tglLahir.contains("-")) tglLahir.split("-")
        else tglLahir.split("/").let { listOf(it[2], it[1], it[0]) }
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        cal.add(Calendar.MONTH, batasBulan)
        "%02d/%02d/%04d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
    } catch (e: Exception) { "" }
}

private fun isTerlambat(tglLahir: String, batasBulan: Int): Boolean {
    return try {
        val parts = if (tglLahir.contains("-")) tglLahir.split("-")
        else tglLahir.split("/").let { listOf(it[2], it[1], it[0]) }
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        cal.add(Calendar.MONTH, batasBulan)
        Calendar.getInstance().after(cal)
    } catch (e: Exception) { false }
}

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiScreen(
    namaAnak      : String              = "",
    namaOrtu      : String              = "",
    nikAnak       : String              = "",   // = anakId
    tglLahirAnak  : String              = "",   // untuk hitung batas terlambat
    umurBulan     : Int                 = 0,
    kaderId       : String              = "",
    onNavigateBack: () -> Unit          = {},
    vaksinAwal    : Map<String, String> = emptyMap(),
    onSimpanVaksin: (namaVaksin: String, tanggal: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val repo    = remember { VaksinRiwayatRepository(context) }

    // vaksinRefId -> tanggalPemberian (dari DB vaksin_riwayat)
    val vaksinDiberikan = remember { mutableStateMapOf<String, String>() }

    // Load alamat posyandu sekali saat screen dibuka (untuk DB, tidak ditampilkan di UI)
    var alamatPosyandu by remember { mutableStateOf("Posyandu") }

    // Load dari DB saat screen dibuka
    LaunchedEffect(nikAnak) {
        if (nikAnak.isBlank()) return@LaunchedEffect

        // Ambil alamat posyandu untuk disimpan ke DB (tidak ditampilkan di UI)
        alamatPosyandu = repo.getAlamatPosyandu()

        val riwayat = repo.getRiwayatByAnak(nikAnak)
        riwayat.forEach { row ->
            vaksinDiberikan[row.vaksinRefId] = row.tanggalPemberian
        }
    }

    var activeTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ImunisasiHeader(
            namaAnak       = namaAnak,
            namaOrtu       = namaOrtu,
            umurBulan      = umurBulan,
            onNavigateBack = onNavigateBack
        )
        ImunisasiTabs(activeTab = activeTab, onTabSelected = { activeTab = it })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            when (activeTab) {
                0 -> TabStatus(
                    anakId          = nikAnak,
                    umurBulan       = umurBulan,
                    tglLahirAnak    = tglLahirAnak,
                    vaksinDiberikan = vaksinDiberikan,
                    repo            = repo
                )
                1 -> TabAkanDatang(umurBulan = umurBulan, repo = repo)
                2 -> TabCatat(
                    anakId          = nikAnak,
                    kaderId         = kaderId,
                    umurBulan       = umurBulan,
                    vaksinDiberikan = vaksinDiberikan,
                    repo            = repo,
                    alamatPosyandu  = alamatPosyandu,
                    onSimpan        = { refId, tanggal ->
                        vaksinDiberikan[refId] = tanggal
                        onSimpanVaksin(refId, tanggal)
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 0 — STATUS
// ════════════════════════════════════════════════════════════

@Composable
fun TabStatus(
    anakId         : String,
    umurBulan      : Int,
    tglLahirAnak   : String,
    vaksinDiberikan: Map<String, String>,
    repo           : VaksinRiwayatRepository
) {
    val semuaVaksin = remember(umurBulan) { repo.getVaksinSudahWaktunya(umurBulan) }

    if (semuaVaksin.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada jadwal vaksin untuk usia ini.", color = TextGrey, fontSize = 13.sp)
        }
        return
    }

    // Group by usia_bulan
    val grouped = semuaVaksin.groupBy { it.usiaBulan }.toSortedMap()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        grouped.forEach { (usiaBulan, daftarVaksin) ->
            val semuaDone  = daftarVaksin.all { vaksinDiberikan.containsKey(it.id) }
            val adaBelum   = daftarVaksin.any { !vaksinDiberikan.containsKey(it.id) }
            val adaTelat   = adaBelum && daftarVaksin.any {
                !vaksinDiberikan.containsKey(it.id) && tglLahirAnak.isNotBlank() && isTerlambat(tglLahirAnak, it.batasBulan)
            }

            val statusKartu = when {
                semuaDone  -> "Lengkap"
                adaTelat   -> "Terlambat"
                adaBelum && usiaBulan == umurBulan -> "Pending"
                adaBelum   -> "Terlambat"
                else       -> "Lengkap"
            }

            val labelUsia = if (usiaBulan == 0) "Lahir" else "$usiaBulan Bulan"

            ImunisasiCard(
                usia   = labelUsia,
                status = statusKartu,
                vaccines = daftarVaksin.map { ref ->
                    val tanggal   = vaksinDiberikan[ref.id]
                    val terlambat = tanggal == null && tglLahirAnak.isNotBlank() && isTerlambat(tglLahirAnak, ref.batasBulan)
                    val tglBatas  = if (tglLahirAnak.isNotBlank()) hitungTanggalBatas(tglLahirAnak, ref.batasBulan) else ""

                    VaccineEntry(
                        name  = ref.nama,
                        info  = when {
                            tanggal != null -> "Diberikan: $tanggal"
                            terlambat       -> "Terlambat sejak $tglBatas"
                            else            -> "Belum diberikan"
                        },
                        state = when {
                            tanggal != null -> VaccineState.DONE
                            terlambat       -> VaccineState.LATE
                            else            -> VaccineState.PENDING
                        }
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 1 — AKAN DATANG
// ════════════════════════════════════════════════════════════

@Composable
fun TabAkanDatang(umurBulan: Int, repo: VaksinRiwayatRepository) {
    val mendatang = remember(umurBulan) { repo.getVaksinAkanDatang(umurBulan) }

    if (mendatang.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Semua jadwal vaksin sudah terlewati.", color = TextGrey, fontSize = 13.sp)
        }
        return
    }

    val grouped = mendatang.groupBy { it.usiaBulan }.toSortedMap()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        grouped.forEach { (usiaBulan, daftarVaksin) ->
            val labelUsia = if (usiaBulan == 0) "Lahir" else "$usiaBulan Bulan"
            ImunisasiCard(
                usia   = labelUsia,
                status = "Akan Datang",
                vaccines = daftarVaksin.map { ref ->
                    VaccineEntry(
                        name  = ref.nama,
                        info  = "Jadwal: usia $usiaBulan bulan",
                        state = VaccineState.PENDING
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 2 — CATAT VAKSIN
// ════════════════════════════════════════════════════════════

@Composable
fun TabCatat(
    anakId         : String,
    kaderId        : String,
    umurBulan      : Int,
    vaksinDiberikan: Map<String, String>,
    repo           : VaksinRiwayatRepository,
    alamatPosyandu : String,
    onSimpan       : (vaksinRefId: String, tanggal: String) -> Unit
) {
    val context  = LocalContext.current
    val calendar = Calendar.getInstance()

    // Hanya tampil vaksin yang usia_bulan <= umurBulan dan belum diberikan
    val belumDiberikan = remember(umurBulan, vaksinDiberikan.size) {
        repo.getVaksinSudahWaktunya(umurBulan)
            .filter { !vaksinDiberikan.containsKey(it.id) }
    }

    var selectedVaksin   by remember { mutableStateOf<VaksinRiwayatRepository.VaksinRefRow?>(null) }
    var tanggalDipilih   by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                tanggalDipilih = "%02d/%02d/%04d".format(day, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    if (belumDiberikan.isEmpty()) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Semua vaksin sudah diberikan! 🎉",
                color      = VaccineInfoGreen,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text       = "Catat vaksin yang diberikan",
                color      = TextWhite,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // ── Pilih vaksin ──────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Pilih Vaksin", color = LabelColor, fontSize = 12.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FieldBg)
                        .border(
                            1.dp,
                            if (dropdownExpanded) AccentGreen else FieldBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { dropdownExpanded = !dropdownExpanded }
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedVaksin?.let {
                                "${it.nama}  (${if (it.usiaBulan == 0) "Lahir" else "${it.usiaBulan} bln"})"
                            } ?: "Pilih vaksin...",
                            color    = if (selectedVaksin != null) TextWhite else Color(0xFF6B6B6B),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector        = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint               = if (dropdownExpanded) AccentGreen else LabelColor,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }

                if (dropdownExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(Color(0xFF222222))
                            .border(1.dp, AccentGreen, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                        ) {
                            items(belumDiberikan) { ref ->
                                val labelUsia = if (ref.usiaBulan == 0) "Lahir" else "${ref.usiaBulan} bln"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedVaksin   = ref
                                            dropdownExpanded = false
                                        }
                                        .background(
                                            if (selectedVaksin?.id == ref.id) Color(0xFF1E3A2A)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = ref.nama,
                                            color      = TextWhite,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text     = "Usia: $labelUsia",
                                            color    = TextGrey,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Pilih Tanggal ─────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Tanggal Diberikan", color = LabelColor, fontSize = 12.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FieldBg)
                        .border(1.dp, FieldBorder, RoundedCornerShape(8.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text     = tanggalDipilih.ifEmpty { "Pilih tanggal..." },
                        color    = if (tanggalDipilih.isEmpty()) Color(0xFF6B6B6B) else TextWhite,
                        fontSize = 13.sp
                    )
                }
            }

            // ── Tombol Simpan ─────────────────────────────────
            // Lokasi (alamat posyandu) tidak ditampilkan di UI, tapi tetap dikirim ke DB
            val bisaSimpan = selectedVaksin != null && tanggalDipilih.isNotEmpty()
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (bisaSimpan) HeaderGreen else Color(0xFF444444))
                    .clickable(enabled = bisaSimpan) {
                        val ref = selectedVaksin!!

                        // 1. Simpan ke SQLite lokal
                        repo.insertRiwayat(
                            anakId           = anakId,
                            vaksinRefId      = ref.id,
                            kaderId          = kaderId,
                            tanggalPemberian = tanggalDipilih,
                            lokasi           = alamatPosyandu
                        )
                        onSimpan(ref.id, tanggalDipilih)
                        selectedVaksin = null
                        tanggalDipilih = ""

                        // 2. SYNC KE API (background)
                        scope.launch {
                            try {
                                val resultSync = syncVaksinRiwayatToApi(context)
                                android.util.Log.d("SYNC_VAKSIN", resultSync.message)
                            } catch (e: Exception) {
                                android.util.Log.e("SYNC_VAKSIN", "Gagal sync: ${e.message}", e)
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Simpan",
                    color      = TextWhite,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  HEADER
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiHeader(
    namaAnak      : String,
    namaOrtu      : String  = "",
    umurBulan     : Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TextWhite, RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateBack)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, "Kembali", tint = TextWhite, modifier = Modifier.size(16.dp))
                Text(text = "Kembali", color = TextWhite, fontSize = 13.sp)
            }
            Text(text = "Imunisasi", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "$namaAnak ~ $umurBulan Bulan", color = TextWhite.copy(alpha = 0.75f), fontSize = 13.sp)
            if (namaOrtu.isNotBlank()) {
                Text(text = "Ortu: $namaOrtu", color = TextWhite.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB ROW
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiTabs(activeTab: Int = 0, onTabSelected: (Int) -> Unit = {}) {
    val tabs = listOf("Status", "Akan Datang", "Catat")
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) TabActiveBg else TabIdleBg)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    color      = if (isActive) TabActiveText else TabIdleText,
                    fontSize   = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  IMUNISASI CARD
// ════════════════════════════════════════════════════════════

enum class VaccineState { DONE, LATE, PENDING }

data class VaccineEntry(
    val name  : String,
    val info  : String,
    val state : VaccineState
)

@Composable
fun ImunisasiCard(
    usia    : String,
    status  : String,
    vaccines: List<VaccineEntry>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(text = usia, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                StatusBadge(status = status)
            }
            Column {
                vaccines.forEachIndexed { index, vaccine ->
                    VaccineItem(entry = vaccine)
                    if (index < vaccines.lastIndex) {
                        Divider(color = DividerColor, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, text) = when (status.lowercase()) {
        "terlambat"   -> BadgeTerlambatBg to BadgeTerlambatText
        "pending"     -> BadgePendingBg   to BadgePendingText
        "akan datang" -> BadgePendingBg   to BadgePendingText
        else          -> BadgeLengkapBg   to BadgeLengkapText
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = status, color = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VaccineItem(entry: VaccineEntry) {
    val (iconBg, infoColor) = when (entry.state) {
        VaccineState.DONE    -> IconCheckBg   to VaccineInfoGreen
        VaccineState.LATE    -> IconLateBg    to VaccineInfoRed
        VaccineState.PENDING -> IconPendingBg to VaccineInfoYellow
    }
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            when (entry.state) {
                VaccineState.DONE    -> Icon(Icons.Filled.Check,  null, tint = TextWhite, modifier = Modifier.size(18.dp))
                VaccineState.LATE    -> Icon(Icons.Filled.Close,  null, tint = TextWhite, modifier = Modifier.size(18.dp))
                VaccineState.PENDING -> Text("?", color = Color(0xFF1A1A1A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = entry.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = entry.info, color = infoColor, fontSize = 12.sp)
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun ImunisasiScreenPreview() {
    ImunisasiScreen(namaAnak = "Budi Santoso", umurBulan = 3)
}