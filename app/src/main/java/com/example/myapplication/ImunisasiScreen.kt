package com.example.myapplication

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
//  Jadwal vaksin posyandu 0–24 bulan
//  key  = usia dalam bulan (0 = lahir)
//  name = nama vaksin
// ─────────────────────────────────────────────────────────────
data class JadwalVaksin(
    val usiaBulan : Int,
    val namaVaksin: String
)

val jadwalVaksinPosyandu: List<JadwalVaksin> = listOf(
    JadwalVaksin(0,  "Hepatitis B (HB-0)"),
    JadwalVaksin(0,  "Polio 0 (OPV)"),
    JadwalVaksin(1,  "BCG"),
    JadwalVaksin(1,  "Polio 1"),
    JadwalVaksin(2,  "DPT-HB-Hib 1"),
    JadwalVaksin(2,  "Polio 2"),
    JadwalVaksin(2,  "PCV 1"),
    JadwalVaksin(3,  "DPT-HB-Hib 2"),
    JadwalVaksin(3,  "Polio 3"),
    JadwalVaksin(3,  "PCV 2"),
    JadwalVaksin(4,  "DPT-HB-Hib 3"),
    JadwalVaksin(4,  "Polio 4 (IPV)"),
    JadwalVaksin(9,  "Campak-Rubella (MR) 1"),
    JadwalVaksin(9,  "Yellow Fever"),
    JadwalVaksin(12, "PCV 3"),
    JadwalVaksin(18, "DPT-HB-Hib 4 (Booster)"),
    JadwalVaksin(18, "Campak-Rubella (MR) 2"),
    JadwalVaksin(24, "Hepatitis A"),
    JadwalVaksin(24, "Tifoid")
)

// ─────────────────────────────────────────────────────────────
//  State vaksin yang sudah diberikan: Map<namaVaksin, tanggal>
// ─────────────────────────────────────────────────────────────
// Diletakkan di ViewModel-level di produksi; di sini pakai
// remember di screen supaya data survive selama sesi imunisasi.

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiScreen(
    namaAnak      : String = "Michael Kwok",
    umurBulan     : Int    = 36,
    onNavigateBack: () -> Unit = {},
    // Map vaksin awal dari ViewModel + callback simpan
    vaksinAwal    : Map<String, String> = emptyMap(),
    onSimpanVaksin: (namaVaksin: String, tanggal: String) -> Unit = { _, _ -> }
) {
    // Inisialisasi dari data ViewModel agar data bertahan antar sesi
    val vaksinDiberikan = remember(vaksinAwal) {
        mutableStateMapOf<String, String>().also { it.putAll(vaksinAwal) }
    }

    var activeTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ImunisasiHeader(namaAnak = namaAnak, umurBulan = umurBulan, onNavigateBack = onNavigateBack)
        ImunisasiTabs(activeTab = activeTab, onTabSelected = { activeTab = it })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            when (activeTab) {
                0 -> TabStatus(umurBulan = umurBulan, vaksinDiberikan = vaksinDiberikan)
                1 -> TabAkanDatang(umurBulan = umurBulan)
                2 -> TabCatat(
                    umurBulan       = umurBulan,
                    vaksinDiberikan = vaksinDiberikan,
                    onSimpan        = { namaVaksin, tanggal ->
                        vaksinDiberikan[namaVaksin] = tanggal
                        onSimpanVaksin(namaVaksin, tanggal)   // ← persist ke ViewModel
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB 0 — STATUS  (vaksin sesuai usia anak)
// ════════════════════════════════════════════════════════════

@Composable
fun TabStatus(
    umurBulan      : Int,
    vaksinDiberikan: Map<String, String>
) {
    // Kelompokkan jadwal berdasarkan usia, hanya sampai umur anak sekarang
    val usiaTampil = jadwalVaksinPosyandu
        .filter { it.usiaBulan <= umurBulan }
        .groupBy { it.usiaBulan }
        .toSortedMap()

    if (usiaTampil.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada jadwal vaksin untuk usia ini.", color = TextGrey, fontSize = 13.sp)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        usiaTampil.forEach { (usiaBulan, daftarVaksin) ->
            // Tentukan status kartu
            val semuaDiberikan = daftarVaksin.all { vaksinDiberikan.containsKey(it.namaVaksin) }
            val adaYgBelum     = daftarVaksin.any  { !vaksinDiberikan.containsKey(it.namaVaksin) }
            val iniUsiaSaatIni = usiaBulan == umurBulan

            // Badge: Lengkap / Pending (bulan aktif belum semua) / Terlambat (bulan lalu belum semua)
            val statusKartu = when {
                semuaDiberikan                     -> "Lengkap"
                iniUsiaSaatIni && adaYgBelum       -> "Pending"
                !iniUsiaSaatIni && adaYgBelum      -> "Terlambat"
                else                               -> "Lengkap"
            }

            val labelUsia = if (usiaBulan == 0) "Lahir" else "$usiaBulan Bulan"

            ImunisasiCard(
                usia   = labelUsia,
                status = statusKartu,
                vaccines = daftarVaksin.map { jadwal ->
                    val tanggal    = vaksinDiberikan[jadwal.namaVaksin]
                    val terlambat  = tanggal == null && usiaBulan < umurBulan
                    val pending    = tanggal == null && usiaBulan == umurBulan
                    VaccineEntry(
                        name    = jadwal.namaVaksin,
                        info    = when {
                            tanggal != null -> "Diberikan: $tanggal"
                            terlambat       -> "Belum diberikan (terlambat)"
                            else            -> "Belum diberikan"
                        },
                        state   = when {
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
fun TabAkanDatang(umurBulan: Int) {
    val mendatang = jadwalVaksinPosyandu
        .filter { it.usiaBulan > umurBulan }
        .groupBy { it.usiaBulan }
        .toSortedMap()

    if (mendatang.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Semua jadwal vaksin sudah terlewati.", color = TextGrey, fontSize = 13.sp)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        mendatang.forEach { (usiaBulan, daftarVaksin) ->
            val labelUsia = if (usiaBulan == 0) "Lahir" else "$usiaBulan Bulan"
            ImunisasiCard(
                usia   = labelUsia,
                status = "Akan Datang",
                vaccines = daftarVaksin.map { jadwal ->
                    VaccineEntry(
                        name  = jadwal.namaVaksin,
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
    umurBulan      : Int,
    vaksinDiberikan: Map<String, String>,
    onSimpan       : (namaVaksin: String, tanggal: String) -> Unit
) {
    val context  = LocalContext.current
    val calendar = Calendar.getInstance()

    // Hanya vaksin yang sudah waktunya (usia <= umur anak) DAN belum diberikan
    val semuaVaksin = jadwalVaksinPosyandu
        .filter { it.usiaBulan <= umurBulan && !vaksinDiberikan.containsKey(it.namaVaksin) }
        .sortedWith(compareBy({ it.usiaBulan }, { it.namaVaksin }))

    var selectedVaksin  by remember { mutableStateOf<JadwalVaksin?>(null) }
    var tanggalDipilih  by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val d = day.toString().padStart(2, '0')
                val m = (month + 1).toString().padStart(2, '0')
                tanggalDipilih = "$d/$m/$year"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    if (semuaVaksin.isEmpty()) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)).background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Semua vaksin sudah diberikan! 🎉", color = VaccineInfoGreen, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
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

            // ── Pilih vaksin — custom expandable list ─────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Pilih Vaksin", color = LabelColor, fontSize = 12.sp)

                // Trigger row
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
                            text  = selectedVaksin?.let {
                                "${it.namaVaksin}  (${if (it.usiaBulan == 0) "Lahir" else "${it.usiaBulan} bln"})"
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

                // List yang muncul tepat di bawah trigger, scrollable, batas bawah navigation bar
                if (dropdownExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)          // max ~5-6 item, sisanya scroll
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(Color(0xFF222222))
                            .border(1.dp, AccentGreen, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()     // jangan ketutupan home bar
                        ) {
                            items(semuaVaksin) { jadwal ->
                                val sudahDiberikan = vaksinDiberikan.containsKey(jadwal.namaVaksin)
                                val labelUsia      = if (jadwal.usiaBulan == 0) "Lahir" else "${jadwal.usiaBulan} bln"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedVaksin   = jadwal
                                            dropdownExpanded = false
                                        }
                                        .background(
                                            if (selectedVaksin == jadwal) Color(0xFF1E3A2A)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = jadwal.namaVaksin,
                                            color      = if (sudahDiberikan) TextGrey else TextWhite,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text     = "Usia: $labelUsia",
                                            color    = TextGrey,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (sudahDiberikan) {
                                        Icon(
                                            imageVector        = Icons.Default.Check,
                                            contentDescription = "Sudah diberikan",
                                            tint               = IconCheckBg,
                                            modifier           = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Pilih Tanggal ──────────────────────────────
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

            // ── Tombol Simpan ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selectedVaksin != null && tanggalDipilih.isNotEmpty())
                            HeaderGreen else Color(0xFF444444)
                    )
                    .clickable(
                        enabled = selectedVaksin != null && tanggalDipilih.isNotEmpty()
                    ) {
                        onSimpan(selectedVaksin!!.namaVaksin, tanggalDipilih)
                        selectedVaksin  = null
                        tanggalDipilih  = ""
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
                Icon(
                    imageVector        = Icons.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = TextWhite,
                    modifier           = Modifier.size(16.dp)
                )
                Text(text = "Kembali", color = TextWhite, fontSize = 13.sp)
            }

            Text(
                text       = "Imunisasi",
                color      = TextWhite,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )

            val labelUmur = "$umurBulan Bulan"
            Text(
                text     = "$namaAnak ~ $labelUmur",
                color    = TextWhite.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TAB ROW
// ════════════════════════════════════════════════════════════

@Composable
fun ImunisasiTabs(
    activeTab    : Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
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
//  IMUNISASI CARD  (reusable)
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
                Text(
                    text       = usia,
                    color      = TextWhite,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = status)
            }

            Column {
                vaccines.forEachIndexed { index, vaccine ->
                    VaccineItem(entry = vaccine)
                    if (index < vaccines.lastIndex) {
                        Divider(
                            color     = DividerColor,
                            thickness = 0.8.dp,
                            modifier  = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Status Badge
// ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: String) {
    val (bg, text) = when (status.lowercase()) {
        "terlambat"   -> BadgeTerlambatBg   to BadgeTerlambatText
        "pending"     -> BadgePendingBg     to BadgePendingText
        "akan datang" -> BadgePendingBg     to BadgePendingText
        else          -> BadgeLengkapBg     to BadgeLengkapText   // Lengkap
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

// ─────────────────────────────────────────────────────────────
//  Vaccine Item Row
// ─────────────────────────────────────────────────────────────

@Composable
fun VaccineItem(entry: VaccineEntry) {
    val (iconBg, icon, infoColor) = when (entry.state) {
        VaccineState.DONE    -> Triple(IconCheckBg,   Icons.Filled.Check, VaccineInfoGreen)
        VaccineState.LATE    -> Triple(IconLateBg,    Icons.Filled.Close, VaccineInfoRed)
        VaccineState.PENDING -> Triple(IconPendingBg, Icons.Filled.Close, VaccineInfoYellow)
    }
    // Untuk PENDING pakai ikon jam/titik, kita pakai tanda "–" lewat text override
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (entry.state == VaccineState.PENDING) {
                Text("?", color = Color(0xFF1A1A1A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = TextWhite,
                    modifier           = Modifier.size(18.dp)
                )
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
    ImunisasiScreen(namaAnak = "Michael Kwok", umurBulan = 3)
}