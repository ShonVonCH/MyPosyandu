package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CalendarMonth
import java.util.Calendar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import android.util.Log
import kotlinx.coroutines.launch

private val TabActiveBg      = Color(0xFF1E6B4E)
private val TabActiveText    = TextWhite
private val TabIdleBg        = Color(0xFF3A3A3A)
private val TabIdleText      = Color(0xFFAAAAAA)
private val WHOCardBg        = Color(0xFF2A2A2A)
private val WHOCardBorder    = Color(0xFF3A3A3A)
private val ProgressTrackWHO = Color(0xFF444444)

private val StatusNormalBg      = Color(0xFFB8EDD8)
private val StatusNormalText    = Color(0xFF1E6B4E)
private val StatusWarnBg        = Color(0xFFFFF3CD)
private val StatusWarnText      = Color(0xFF856404)
private val StatusDangerBg      = Color(0xFFFFDADA)
private val StatusDangerText    = Color(0xFF9B1C1C)
private val StatusProgressGreen  = Color(0xFF3DB89C)
private val StatusProgressYellow = Color(0xFFF5A623)
private val StatusProgressRed    = Color(0xFFE74C3C)

private val ChartColorMedian = Color(0xFF3DB89C)
private val ChartColorSD2    = Color(0xFFF5A623)
private val ChartColorSD3    = Color(0xFFE74C3C)
private val ChartColorAnak   = Color(0xFFFF6B6B)
private val ChartGridColor   = Color(0xFF3A3A3A)

data class HasilAnalisis(
    val zScoreTBU : Double,
    val zScoreBBU : Double,
    val statusTBU : String,
    val statusBBU : String,
    val warnasTBU : StatusWarna,
    val warnasBBU : StatusWarna,
    val saranTBU  : String,
    val saranBBU  : String
)

enum class StatusWarna { NORMAL, WARN, DANGER }

fun zScoreToProgress(z: Double): Float = ((z + 4.0) / 8.0).coerceIn(0.0, 1.0).toFloat()

fun statusToProgressColor(warna: StatusWarna) = when (warna) {
    StatusWarna.NORMAL -> StatusProgressGreen
    StatusWarna.WARN   -> StatusProgressYellow
    StatusWarna.DANGER -> StatusProgressRed
}

@Composable
fun PemeriksaanScreen(
    anakId         : String   = "",
    kaderId        : String   = "",
    namaAnak       : String   = "",
    umurBulan      : Int      = 0,
    jenisKelamin   : String   = "",
    onNavigateBack : () -> Unit = {},
    onSimpan       : (beratBadan: String, tinggiBadan: String, analisis: HasilAnalisis) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val kaderIdEffect = remember { mutableStateOf(kaderId) }
    LaunchedEffect(kaderId) {
        if (kaderId.isBlank()) {
            val db = DatabaseHelper(context).readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_USERS_ID} FROM ${DatabaseHelper.TABLE_USERS} LIMIT 1",
                null
            )
            if (cursor.moveToFirst()) {
                kaderIdEffect.value = cursor.getString(0) ?: ""
            }
            cursor.close()
            db.close()
        } else {
            kaderIdEffect.value = kaderId
        }
    }

    var beratBadan    by remember { mutableStateOf("") }
    var tinggiBadan   by remember { mutableStateOf("") }
    var lingkarKepala by remember { mutableStateOf("") }
    var lingkarLengan by remember { mutableStateOf("") }
    var tanggal       by remember { mutableStateOf("12/06/2026") }
    var activeTab     by remember { mutableStateOf(0) }
    var hasil         by remember { mutableStateOf<HasilAnalisis?>(null) }

    var errorBB by remember { mutableStateOf(false) }
    var errorTB by remember { mutableStateOf(false) }
    var errorLK by remember { mutableStateOf(false) }
    var errorLL by remember { mutableStateOf(false) }
    var errorTgl by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        HeaderProfilAnak(
            subTitle       = "$namaAnak ~ $umurBulan Bulan ~ $jenisKelamin",
            onNavigateBack = onNavigateBack
        )

        PemeriksaanTabs(
            activeTab     = activeTab,
            onTabSelected = { activeTab = it }
        )

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> TabInputContent(
                    beratBadan      = beratBadan,
                    onBeratChange   = { beratBadan = it; errorBB = false },
                    tinggiBadan     = tinggiBadan,
                    onTinggiChange  = { tinggiBadan = it; errorTB = false },
                    lingkarKepala   = lingkarKepala,
                    onKepalaChange  = { lingkarKepala = it; errorLK = false },
                    lingkarLengan   = lingkarLengan,
                    onLenganChange  = { lingkarLengan = it; errorLL = false },
                    tanggal         = tanggal,
                    onTanggalChange = { tanggal = it; errorTgl = false },
                    errorBB         = errorBB,
                    errorTB         = errorTB,
                    errorLK         = errorLK,
                    errorLL         = errorLL,
                    errorTgl        = errorTgl,
                    onAnalisisClick = {
                        var hasError = false
                        if (beratBadan.isBlank()) { errorBB = true; hasError = true }
                        if (tinggiBadan.isBlank()) { errorTB = true; hasError = true }
                        if (lingkarKepala.isBlank()) { errorLK = true; hasError = true }
                        if (lingkarLengan.isBlank()) { errorLL = true; hasError = true }
                        if (tanggal.isBlank()) { errorTgl = true; hasError = true }

                        if (!hasError) {
                            val tb = tinggiBadan.toDoubleOrNull()
                            val bb = beratBadan.toDoubleOrNull()
                            if (tb != null && bb != null) {
                                val analisis = analisisWHO(tb, bb, umurBulan, jenisKelamin)
                                hasil        = analisis
                                activeTab    = 1
                                onSimpan(beratBadan, tinggiBadan, analisis)

                                val repo = PemeriksaanRepository(context)
                                val kaderIdFinal = kaderIdEffect.value

                                val statusGiziDb = when (analisis.statusBBU.lowercase().trim()) {
                                    "gizi buruk"                    -> "gizi_buruk"
                                    "gizi kurang"                   -> "gizi_kurang"
                                    "gizi lebih"                    -> "gizi_lebih"
                                    "obesitas"                      -> "obesitas"
                                    else                            -> "normal"
                                }

                                repo.insertPemeriksaan(
                                    id         = java.util.UUID.randomUUID().toString(),
                                    anakId     = anakId,
                                    kaderId    = kaderIdFinal,
                                    tgl        = tanggal,
                                    bb         = beratBadan.toDoubleOrNull() ?: 0.0,
                                    tb         = tinggiBadan.toDoubleOrNull() ?: 0.0,
                                    lk         = lingkarKepala.toDoubleOrNull() ?: 0.0,
                                    ll         = lingkarLengan.toDoubleOrNull() ?: 0.0,
                                    zScoreTbu  = analisis.zScoreTBU,
                                    zScoreBbu  = analisis.zScoreBBU,
                                    statusGizi = statusGiziDb,
                                    catatan    = ""
                                )

                                scope.launch {
                                    try {
                                        syncPemeriksaanToApi(context)
                                    } catch (e: Exception) {
                                        Log.e("SYNC_PMRK", "Gagal sync: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                )
                1 -> TabHasilContent(
                    namaAnak     = namaAnak,
                    tanggal      = tanggal,
                    hasil        = hasil
                )
                2 -> TabGrafikTBUContent(
                    namaAnak     = namaAnak,
                    umurBulan    = umurBulan,
                    jenisKelamin = jenisKelamin,
                    hasil        = hasil
                )
                3 -> TabGrafikBBUContent(
                    namaAnak     = namaAnak,
                    umurBulan    = umurBulan,
                    jenisKelamin = jenisKelamin,
                    hasil        = hasil
                )
            }
        }
    }
}

@Composable
private fun TabInputContent(
    beratBadan      : String,
    onBeratChange   : (String) -> Unit,
    tinggiBadan     : String,
    onTinggiChange  : (String) -> Unit,
    lingkarKepala   : String,
    onKepalaChange  : (String) -> Unit,
    lingkarLengan   : String,
    onLenganChange  : (String) -> Unit,
    tanggal         : String,
    onTanggalChange : (String) -> Unit,
    errorBB         : Boolean = false,
    errorTB         : Boolean = false,
    errorLK         : Boolean = false,
    errorLL         : Boolean = false,
    errorTgl        : Boolean = false,
    onAnalisisClick : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        AntropometriCard(
            beratBadan     = beratBadan,
            onBeratChange  = onBeratChange,
            tinggiBadan    = tinggiBadan,
            onTinggiChange = onTinggiChange,
            lingkarKepala  = lingkarKepala,
            onKepalaChange = onKepalaChange,
            lingkarLengan  = lingkarLengan,
            onLenganChange = onLenganChange,
            errorBB        = errorBB,
            errorTB        = errorTB,
            errorLK        = errorLK,
            errorLL        = errorLL,
            modifier       = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TanggalCard(
            tanggal         = tanggal,
            onTanggalChange = onTanggalChange,
            isError         = errorTgl,
            modifier        = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnalisisDanSimpanButton(
            onClick  = onAnalisisClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TabHasilContent(
    namaAnak : String,
    tanggal  : String,
    hasil    : HasilAnalisis?
) {
    if (hasil == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector        = Icons.Filled.Assessment,
                    contentDescription = null,
                    tint               = TabIdleText,
                    modifier           = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text      = "Isi data di tab Input\nlalu tekan Analisis Dan Simpan",
                    color     = TabIdleText,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        val overallWarna = if (hasil.warnasTBU == StatusWarna.DANGER || hasil.warnasBBU == StatusWarna.DANGER)
            StatusWarna.DANGER
        else if (hasil.warnasTBU == StatusWarna.WARN || hasil.warnasBBU == StatusWarna.WARN)
            StatusWarna.WARN
        else StatusWarna.NORMAL

        val (summaryBg, summaryText) = when (overallWarna) {
            StatusWarna.NORMAL -> Pair(StatusNormalBg, StatusNormalText)
            StatusWarna.WARN   -> Pair(StatusWarnBg,   StatusWarnText)
            StatusWarna.DANGER -> Pair(StatusDangerBg, StatusDangerText)
        }
        val overallLabel = when (overallWarna) {
            StatusWarna.NORMAL -> "Normal"
            StatusWarna.WARN   -> "Perlu Perhatian"
            StatusWarna.DANGER -> "Perlu Penanganan"
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(summaryBg)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(summaryText.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = if (overallWarna == StatusWarna.NORMAL)
                            Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = overallLabel,
                        tint               = summaryText,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = overallLabel, color = summaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$namaAnak ~ $tanggal", color = summaryText, fontSize = 12.sp)
                    Text(
                        text       = "TB/U: ${hasil.zScoreTBU}   BB/U: ${hasil.zScoreBBU}",
                        color      = summaryText,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WHOCardBg)
                .border(1.dp, WHOCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "Indikator WHO", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                WHOIndicatorBlock(
                    title       = "TB/U - ${hasil.statusTBU}",
                    zScore      = "Z-Score: ${hasil.zScoreTBU}",
                    statusLabel = hasil.statusTBU,
                    progress    = zScoreToProgress(hasil.zScoreTBU),
                    description = hasil.saranTBU,
                    warna       = hasil.warnasTBU
                )

                Divider(color = WHOCardBorder, thickness = 0.8.dp)

                WHOIndicatorBlock(
                    title       = "BB/U - ${hasil.statusBBU}",
                    zScore      = "Z-Score: ${hasil.zScoreBBU}",
                    statusLabel = hasil.statusBBU,
                    progress    = zScoreToProgress(hasil.zScoreBBU),
                    description = hasil.saranBBU,
                    warna       = hasil.warnasBBU
                )

                Divider(color = WHOCardBorder, thickness = 0.8.dp)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Keterangan Skala Z-Score WHO", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "< -3.0  : Sangat Pendek / Gizi Buruk", color = StatusDangerText, fontSize = 11.sp)
                    Text(text = "-3.0 ~ -2.0 : Pendek (Stunting) / Gizi Kurang", color = StatusWarnText, fontSize = 11.sp)
                    Text(text = "-2.0 ~ +2.0 : Normal", color = StatusNormalText, fontSize = 11.sp)
                    Text(text = "> +2.0  : Di Atas Normal / Gizi Lebih", color = StatusWarnText, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun WHOIndicatorBlock(
    title       : String,
    zScore      : String,
    statusLabel : String,
    progress    : Float,
    description : String,
    warna       : StatusWarna
) {
    val (labelBg, labelText) = when (warna) {
        StatusWarna.NORMAL -> Pair(StatusNormalBg,  StatusNormalText)
        StatusWarna.WARN   -> Pair(StatusWarnBg,    StatusWarnText)
        StatusWarna.DANGER -> Pair(StatusDangerBg,  StatusDangerText)
    }
    val progressColor = statusToProgressColor(warna)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title,  color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = zScore, color = TextGrey,  fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(labelBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = statusLabel, color = labelText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress        = progress,
                modifier        = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color           = progressColor,
                backgroundColor = ProgressTrackWHO
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("-4", "-3", "-2", "0", "+2", "+3", "+4").forEach { label ->
                Text(text = label, color = TextGrey, fontSize = 9.sp)
            }
        }

        Text(text = description, color = TextGrey, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun TabGrafikTBUContent(
    namaAnak    : String,
    umurBulan   : Int,
    jenisKelamin: String,
    hasil       : HasilAnalisis?
) {
    val isLaki = jenisKelamin.startsWith("L", ignoreCase = true)
    val tabel  = if (isLaki) tabelTBU_LakiLaki else tabelTBU_Perempuan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GrafikLineCard(
            judul        = "Tinggi Badan / Usia (TB/U)",
            namaAnak     = namaAnak,
            umurBulan    = umurBulan,
            tabel        = tabel,
            nilaiAnak    = hasil?.zScoreTBU,
            satuanLabel  = "cm"
        )

        KeteranganStatusCard(
            judulIndikator = "Stunting (TB/U)",
            status         = hasil?.statusTBU,
            zScore         = hasil?.zScoreTBU,
            warna          = hasil?.warnasTBU,
            saran          = hasil?.saranTBU,
            referensi      = listOf(
                "Sangat Pendek : Z < -3.0",
                "Pendek (Stunting) : -3.0 ≤ Z < -2.0",
                "Normal : -2.0 ≤ Z ≤ +2.0",
                "Tinggi : Z > +2.0"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TabGrafikBBUContent(
    namaAnak    : String,
    umurBulan   : Int,
    jenisKelamin: String,
    hasil       : HasilAnalisis?
) {
    val isLaki = jenisKelamin.startsWith("L", ignoreCase = true)
    val tabel  = if (isLaki) tabelBBU_LakiLaki else tabelBBU_Perempuan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GrafikLineCard(
            judul        = "Berat Badan / Usia (BB/U)",
            namaAnak     = namaAnak,
            umurBulan    = umurBulan,
            tabel        = tabel,
            nilaiAnak    = hasil?.zScoreBBU,
            satuanLabel  = "kg"
        )

        KeteranganStatusCard(
            judulIndikator = "Underweight (BB/U)",
            status         = hasil?.statusBBU,
            zScore         = hasil?.zScoreBBU,
            warna          = hasil?.warnasBBU,
            saran          = hasil?.saranBBU,
            referensi      = listOf(
                "Gizi Buruk : Z < -3.0",
                "Gizi Kurang : -3.0 ≤ Z < -2.0",
                "Gizi Baik : -2.0 ≤ Z ≤ +2.0",
                "Gizi Lebih : +2.0 < Z ≤ +3.0",
                "Obesitas : Z > +3.0"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GrafikLineCard(
    judul       : String,
    namaAnak    : String,
    umurBulan   : Int,
    tabel       : Map<Int, Pair<Double, Double>>,
    nilaiAnak   : Double?,
    satuanLabel : String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WHOCardBg)
            .border(1.dp, WHOCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = judul, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendaLine(color = ChartColorMedian, label = "Median")
                LegendaLine(color = ChartColorSD2,    label = "±2 SD", dashed = true)
                LegendaLine(color = ChartColorSD3,    label = "±3 SD", dashed = true)
                if (nilaiAnak != null) LegendaDot(color = ChartColorAnak, label = namaAnak)
            }

            val keys = tabel.keys.sorted()
            val minAge = keys.first().toFloat()
            val maxAge = keys.last().toFloat()

            val allValues = keys.flatMap { bulan ->
                val (med, sd) = tabel[bulan]!!
                listOf(med - sd * 3, med + sd * 3)
            }
            val minY = (allValues.min() - 1).toFloat()
            val maxY = (allValues.max() + 1).toFloat()

            fun toOffset(age: Float, value: Float, w: Float, h: Float): Offset {
                val x = (age - minAge) / (maxAge - minAge) * w
                val y = h - (value - minY) / (maxY - minY) * h
                return Offset(x, y)
            }

            val chartHeight = 220.dp
            val leftPadding = 36.dp

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(start = leftPadding, bottom = 20.dp)
            ) {
                val w = size.width
                val h = size.height

                val gridStep = if ((maxY - minY) > 50) 20f else if ((maxY - minY) > 20) 10f else 5f
                var gridVal = (minY / gridStep).toInt() * gridStep
                while (gridVal <= maxY) {
                    val gy = h - (gridVal - minY) / (maxY - minY) * h
                    drawLine(
                        color       = ChartGridColor,
                        start       = Offset(0f, gy),
                        end         = Offset(w, gy),
                        strokeWidth = 0.8f
                    )
                    gridVal += gridStep
                }

                keys.forEach { bulan ->
                    val gx = (bulan - minAge) / (maxAge - minAge) * w
                    drawLine(
                        color       = ChartGridColor,
                        start       = Offset(gx, 0f),
                        end         = Offset(gx, h),
                        strokeWidth = 0.8f
                    )
                }

                fun drawLinePath(points: List<Offset>, color: Color, strokeWidth: Float, dashed: Boolean = false) {
                    if (points.size < 2) return
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    if (dashed) {
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val steps = 8
                            for (s in 0 until steps step 2) {
                                val t1 = s.toFloat() / steps
                                val t2 = (s + 1).toFloat() / steps
                                drawLine(
                                    color       = color,
                                    start       = Offset(p1.x + (p2.x - p1.x) * t1, p1.y + (p2.y - p1.y) * t1),
                                    end         = Offset(p1.x + (p2.x - p1.x) * t2, p1.y + (p2.y - p1.y) * t2),
                                    strokeWidth = strokeWidth,
                                    cap         = StrokeCap.Round
                                )
                            }
                        }
                    } else {
                        drawPath(
                            path   = path,
                            color  = color,
                            style  = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                val lineNeg3 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first - tabel[b]!!.second * 3).toFloat(), w, h) }
                val linePos3 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first + tabel[b]!!.second * 3).toFloat(), w, h) }
                drawLinePath(lineNeg3, ChartColorSD3.copy(alpha = 0.7f), 1.5f, dashed = true)
                drawLinePath(linePos3, ChartColorSD3.copy(alpha = 0.7f), 1.5f, dashed = true)

                val lineNeg2 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first - tabel[b]!!.second * 2).toFloat(), w, h) }
                val linePos2 = keys.map { b -> toOffset(b.toFloat(), (tabel[b]!!.first + tabel[b]!!.second * 2).toFloat(), w, h) }
                drawLinePath(lineNeg2, ChartColorSD2.copy(alpha = 0.8f), 1.5f, dashed = true)
                drawLinePath(linePos2, ChartColorSD2.copy(alpha = 0.8f), 1.5f, dashed = true)

                val areaPath = Path().apply {
                    moveTo(lineNeg2[0].x, lineNeg2[0].y)
                    lineNeg2.drop(1).forEach { lineTo(it.x, it.y) }
                    linePos2.reversed().forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(
                    path  = areaPath,
                    color = ChartColorMedian.copy(alpha = 0.08f)
                )

                val lineMedian = keys.map { b -> toOffset(b.toFloat(), tabel[b]!!.first.toFloat(), w, h) }
                drawLinePath(lineMedian, ChartColorMedian, 2.5f, dashed = false)

                if (nilaiAnak != null) {
                    val (medAtAge, sdAtAge) = interpolasi(umurBulan, tabel)
                    val nilaiAbs = medAtAge + nilaiAnak * sdAtAge

                    val xAnak = (umurBulan - minAge) / (maxAge - minAge) * w
                    val yAnak = h - (nilaiAbs.toFloat() - minY) / (maxY - minY) * h

                    for (step in 0 until 20 step 2) {
                        val y1 = h * step / 20f
                        val y2 = h * (step + 1) / 20f
                        drawLine(
                            color       = ChartColorAnak.copy(alpha = 0.5f),
                            start       = Offset(xAnak, y1),
                            end         = Offset(xAnak, y2),
                            strokeWidth = 1.5f,
                            cap         = StrokeCap.Round
                        )
                    }

                    drawCircle(
                        color  = ChartColorAnak.copy(alpha = 0.25f),
                        radius = 12f,
                        center = Offset(xAnak, yAnak)
                    )
                    drawCircle(
                        color  = ChartColorAnak,
                        radius = 6f,
                        center = Offset(xAnak, yAnak)
                    )
                    drawCircle(
                        color  = Color.White,
                        radius = 3f,
                        center = Offset(xAnak, yAnak)
                    )
                }
            }

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = leftPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                keys.forEach { bulan ->
                    Text(
                        text      = if (bulan == 0) "0" else "${bulan}",
                        color     = if (abs(bulan - umurBulan) <= 3) TextWhite else TextGrey,
                        fontSize  = 9.sp,
                        fontWeight = if (abs(bulan - umurBulan) <= 3) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Text(
                text     = "Usia (bulan)",
                color    = TextGrey,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (nilaiAnak != null) {
                val (medAtAge, sdAtAge) = interpolasi(umurBulan, tabel)
                val nilaiAbs = (medAtAge + nilaiAnak * sdAtAge * 10).roundToInt() / 10.0
                Text(
                    text      = "● $namaAnak pada usia $umurBulan bln: $nilaiAbs $satuanLabel (Z = $nilaiAnak)",
                    color     = ChartColorAnak,
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LegendaLine(color: Color, label: String, dashed: Boolean = false) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.width(18.dp).height(8.dp)) {
            if (dashed) {
                for (i in 0..2) {
                    val x1 = size.width * i / 3f
                    val x2 = size.width * (i + 0.6f) / 3f
                    drawLine(color = color, start = Offset(x1, size.height / 2), end = Offset(x2, size.height / 2), strokeWidth = 2f, cap = StrokeCap.Round)
                }
            } else {
                drawLine(color = color, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
        }
        Text(text = label, color = TextGrey, fontSize = 10.sp)
    }
}

@Composable
private fun LegendaDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, color = TextGrey, fontSize = 10.sp)
    }
}

@Composable
private fun KeteranganStatusCard(
    judulIndikator : String,
    status         : String?,
    zScore         : Double?,
    warna          : StatusWarna?,
    saran          : String?,
    referensi      : List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WHOCardBg)
            .border(1.dp, WHOCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Keterangan $judulIndikator", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            if (status != null && zScore != null && warna != null) {
                val (bg, fg) = when (warna) {
                    StatusWarna.NORMAL -> Pair(StatusNormalBg, StatusNormalText)
                    StatusWarna.WARN   -> Pair(StatusWarnBg,   StatusWarnText)
                    StatusWarna.DANGER -> Pair(StatusDangerBg, StatusDangerText)
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(text = "Status anak", color = TextGrey, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(text = status, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Z-Score", color = TextGrey, fontSize = 12.sp)
                    Text(text = zScore.toString(), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = "Belum ada data — lakukan analisis terlebih dahulu", color = TextGrey, fontSize = 12.sp)
            }

            Divider(color = WHOCardBorder, thickness = 0.8.dp)

            Text(text = "Klasifikasi WHO", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            referensi.forEach { line ->
                val isMatch = status != null && line.startsWith(status.split(" ").first(), ignoreCase = true)
                Text(
                    text       = "• $line",
                    color      = if (isMatch) TextWhite else TextGrey,
                    fontSize   = 11.sp,
                    fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (saran != null) {
                Divider(color = WHOCardBorder, thickness = 0.8.dp)
                Text(text = "Rekomendasi", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = saran, color = TextWhite, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun HeaderProfilAnak(
    subTitle      : String = "Michael Kwok ~ 36 Bulan ~ Laki-Laki",
    halamanJudul  : String = "Pemeriksaan",
    onNavigateBack: () -> Unit = {}
) {
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
            Text(text = halamanJudul, color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subTitle, color = TextWhite.copy(alpha = 0.75f), fontSize = 14.sp)
        }
    }
}

@Composable
fun PemeriksaanTabs(
    activeTab    : Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val tabs = listOf("Input", "Hasil", "Grafik\nTB/U", "Grafik\nBB/U")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) TabActiveBg else TabIdleBg)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    color      = if (isActive) TabActiveText else TabIdleText,
                    fontSize   = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 14.sp,
                    maxLines   = 2,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AntropometriCard(
    beratBadan     : String,
    onBeratChange  : (String) -> Unit,
    tinggiBadan    : String,
    onTinggiChange : (String) -> Unit,
    lingkarKepala  : String,
    onKepalaChange : (String) -> Unit,
    lingkarLengan  : String,
    onLenganChange : (String) -> Unit,
    errorBB        : Boolean = false,
    errorTB        : Boolean = false,
    errorLK        : Boolean = false,
    errorLL        : Boolean = false,
    modifier       : Modifier = Modifier
) {
    PemeriksaanCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Data Antropometri", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AntropometriField("Berat badan (kg)",    beratBadan,    onBeratChange,  errorBB, Modifier.weight(1f))
                AntropometriField("Tinggi badan (cm)",   tinggiBadan,   onTinggiChange, errorTB, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AntropometriField("Lingkar Kepala (cm)", lingkarKepala, onKepalaChange, errorLK, Modifier.weight(1f))
                AntropometriField("Lingkar lengan (cm)", lingkarLengan, onLenganChange, errorLL, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TanggalCard(
    tanggal         : String,
    onTanggalChange : (String) -> Unit,
    isError         : Boolean = false,
    modifier        : Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val calendar = remember {
        Calendar.getInstance().also { cal ->
            runCatching {
                val parts = tanggal.split("/")
                if (parts.size == 3) {
                    cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                }
            }
        }
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                onTanggalChange(formatted)
                calendar.set(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    PemeriksaanCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Tanggal Pemeriksaan", color = TextGrey, fontSize = 13.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3A3A3A))
                    .border(1.dp, if (isError) Color.Red else Color(0xFF555555), RoundedCornerShape(8.dp))
                    .clickable { datePickerDialog.show() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = if (tanggal.isNotEmpty()) tanggal else "dd/mm/yyyy",
                        color    = if (tanggal.isNotEmpty()) TextWhite else Color(0xFF6B6B6B),
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector        = Icons.Default.CalendarMonth,
                        contentDescription = "Pilih tanggal",
                        tint               = Color(0xFFAAAAAA),
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
            if (isError) {
                Text("Harus diisi", color = Color.Red, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AnalisisDanSimpanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Assessment, null, tint = TextWhite, modifier = Modifier.size(20.dp))
            Text("Analisis Dan Simpan", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PemeriksaanCardContainer(
    modifier: Modifier = Modifier,
    content : @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun AntropometriField(
    label         : String,
    value         : String,
    onValueChange : (String) -> Unit,
    isError       : Boolean = false,
    modifier      : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = TextGrey, fontSize = 12.sp, maxLines = 1)
        PemeriksaanInputBox(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = "0.0",
            keyboardType  = KeyboardType.Decimal,
            isError       = isError,
            modifier      = Modifier.fillMaxWidth().height(48.dp)
        )
        if (isError) {
            Text("Harus diisi", color = Color.Red, fontSize = 11.sp)
        }
    }
}

@Composable
fun PemeriksaanInputBox(
    value         : String,
    onValueChange : (String) -> Unit,
    placeholder   : String,
    modifier      : Modifier = Modifier,
    keyboardType  : KeyboardType = KeyboardType.Text,
    isError       : Boolean = false
) {
    BasicTextField(
        value           = value,
        onValueChange   = onValueChange,
        singleLine      = true,
        cursorBrush     = SolidColor(AccentGreen),
        textStyle       = TextStyle(color = TextWhite, fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier        = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3A3A3A))
            .border(1.dp, if (isError) Color.Red else Color(0xFF555555), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) Text(placeholder, color = Color(0xFF6B6B6B), fontSize = 14.sp)
                innerTextField()
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, showSystemUi = true)
@Composable
fun PemeriksaanScreenPreview() {
    PemeriksaanScreen()
}