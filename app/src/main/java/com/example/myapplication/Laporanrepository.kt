package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LaporanRepository(private val context: Context) {

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        .apply { timeZone = TimeZone.getDefault() }

    private val dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dbFmtMonth = DateTimeFormatter.ofPattern("MM/yyyy")

    data class LaporanResult(
        val laporanId: String?,
        val syncSuccess: Boolean,
        val syncMessage: String
    )

    data class ExportResult(
        val success: Boolean,
        val message: String,
        val filePath: String? = null
    )

    // ═══════════════════════════════════════════════════════════
    //  BUAT & SIMPAN LAPORAN KE SQLITE + SYNC API
    // ═══════════════════════════════════════════════════════════
    suspend fun buatDanSimpanLaporan(
        dariCal: Calendar,
        sampaiCal: Calendar,
        cakupan: String
    ): LaporanResult = withContext(Dispatchers.IO) {
        val db = DatabaseHelper(context).writableDatabase

        try {
            // 1. Ambil kaderId & posyanduId
            val cursorUser = db.rawQuery(
                """
                SELECT ${DatabaseHelper.COL_USERS_ID}, ${DatabaseHelper.COL_USERS_POSYANDU_ID}
                FROM ${DatabaseHelper.TABLE_USERS}
                LIMIT 1
                """.trimIndent(), null
            )
            val kaderId: String
            val posyanduId: String
            if (cursorUser.moveToFirst()) {
                kaderId = cursorUser.getString(0) ?: ""
                posyanduId = cursorUser.getString(1) ?: ""
            } else {
                cursorUser.close()
                Log.e("LAPORAN", "User tidak ditemukan di SQLite")
                return@withContext LaporanResult(null, false, "User tidak ditemukan")
            }
            cursorUser.close()

            if (kaderId.isBlank()) {
                return@withContext LaporanResult(null, false, "kaderId kosong")
            }

            // 2. Ambil jadwalId
            val tglDariStr = dbFmt.format(dariCal.time.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            val tglSampaiStr = dbFmt.format(sampaiCal.time.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate())

            val cursorJadwal = db.rawQuery(
                """
                SELECT ${DatabaseHelper.COL_JADWAL_ID}
                FROM ${DatabaseHelper.TABLE_JADWAL_POSYANDU}
                WHERE ${DatabaseHelper.COL_JADWAL_POSYANDU_ID} = ?
                  AND ${DatabaseHelper.COL_JADWAL_TANGGAL} BETWEEN ? AND ?
                  AND LOWER(${DatabaseHelper.COL_JADWAL_STATUS}) = 'terjadwal'
                ORDER BY ${DatabaseHelper.COL_JADWAL_TANGGAL} DESC
                LIMIT 1
                """.trimIndent(),
                arrayOf(posyanduId, tglDariStr, tglSampaiStr)
            )
            val jadwalId = if (cursorJadwal.moveToFirst()) cursorJadwal.getString(0) else null
            cursorJadwal.close()

            // 3. Format periode
            val dariLocalDate = dariCal.time.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val sampaiLocalDate = sampaiCal.time.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

            val bulanDari = dbFmtMonth.format(dariLocalDate)
            val bulanSampai = dbFmtMonth.format(sampaiLocalDate)

            // 4. Filter gender
            val genderFilter = when (cakupan) {
                "Balita Laki-laki" -> "AND a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} IN ('L','laki-laki','Laki-laki')"
                "Balita Perempuan" -> "AND a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} IN ('P','perempuan','Perempuan')"
                else -> ""
            }

            // 5. Hitung metrics
            val totalHadir = hitungTotalHadir(db, bulanDari, bulanSampai)
            val totalStunting = hitungTotalStunting(db, bulanDari, bulanSampai, genderFilter)
            val totalVaksinTerlambat = hitungTotalVaksinTerlambat(db, bulanDari, bulanSampai, genderFilter)

            Log.d("LAPORAN", "Hadir=$totalHadir, Stunting=$totalStunting, VaksinTerlambat=$totalVaksinTerlambat")

            // 6. Insert ke SQLite
            val laporanId = UUID.randomUUID().toString()
            val now = isoFmt.format(Date())

            val ringkasan = JSONObject().apply {
                put("periode_dari", tglDariStr)
                put("periode_sampai", tglSampaiStr)
                put("cakupan", cakupan)
                put("total_hadir", totalHadir)
                put("total_stunting", totalStunting)
                put("total_vaksin_terlambat", totalVaksinTerlambat)
            }.toString()

            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_LAP_ID, laporanId)
                put(DatabaseHelper.COL_LAP_KADER_ID, kaderId)
                put(DatabaseHelper.COL_LAP_TOTAL_HADIR, totalHadir)
                put(DatabaseHelper.COL_LAP_TOTAL_STUNTING, totalStunting)
                put(DatabaseHelper.COL_LAP_TOTAL_VAKSIN_TERLAMBAT, totalVaksinTerlambat)
                put(DatabaseHelper.COL_LAP_RINGKASAN, ringkasan)
                put(DatabaseHelper.COL_LAP_GENERATED_AT, now)
                if (jadwalId != null) put(DatabaseHelper.COL_LAP_JADWAL_ID, jadwalId)
                else putNull(DatabaseHelper.COL_LAP_JADWAL_ID)
            }

            val rowId = db.insertOrThrow(DatabaseHelper.TABLE_LAPORAN, null, cv)
            db.close()

            if (rowId == -1L) {
                return@withContext LaporanResult(null, false, "Gagal insert ke database lokal")
            }

            Log.d("LAPORAN", "Insert lokal sukses: laporanId=$laporanId")

            // 7. SYNC KE API
            val success = insertLaporanToApi(
                laporanId = laporanId,
                kaderId = kaderId,
                jadwalId = jadwalId,
                totalHadir = totalHadir,
                totalStunting = totalStunting,
                totalVaksinTerlambat = totalVaksinTerlambat,
                ringkasan = ringkasan,
                generatedAt = now,
                tglDari = tglDariStr,
                tglSampai = tglSampaiStr
            )

            return@withContext LaporanResult(
                laporanId = laporanId,
                syncSuccess = success,
                syncMessage = if (success) "Sync API berhasil" else "Sync API gagal"
            )

        } catch (e: Exception) {
            Log.e("LAPORAN", "Gagal simpan laporan: ${e.message}", e)
            return@withContext LaporanResult(null, false, "Error: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPORT LAPORAN (PDF, EXCEL, CSV)
    // ═══════════════════════════════════════════════════════════
    suspend fun exportLaporan(
        dariCal: Calendar,
        sampaiCal: Calendar,
        cakupan: String,
        format: String  // "pdf", "excel", "csv"
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val db = DatabaseHelper(context).readableDatabase

            // Ambil data kader
            val cursorUser = db.rawQuery(
                """
                SELECT u.${DatabaseHelper.COL_USERS_NAMA}, p.${DatabaseHelper.COL_POSYANDU_NAMA}
                FROM ${DatabaseHelper.TABLE_USERS} u
                LEFT JOIN ${DatabaseHelper.TABLE_POSYANDU} p 
                    ON u.${DatabaseHelper.COL_USERS_POSYANDU_ID} = p.${DatabaseHelper.COL_POSYANDU_ID}
                LIMIT 1
                """.trimIndent(), null
            )
            val namaKader = if (cursorUser.moveToFirst()) cursorUser.getString(0) ?: "Kader" else "Kader"
            val namaPosyandu = if (cursorUser.moveToFirst()) cursorUser.getString(1) ?: "Posyandu" else "Posyandu"
            cursorUser.close()

            // Format periode
            val dariLocalDate = dariCal.time.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val sampaiLocalDate = sampaiCal.time.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val bulanDari = dbFmtMonth.format(dariLocalDate)
            val bulanSampai = dbFmtMonth.format(sampaiLocalDate)

            // Filter gender
            val genderFilter = when (cakupan) {
                "Balita Laki-laki" -> "AND a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} IN ('L','laki-laki','Laki-laki')"
                "Balita Perempuan" -> "AND a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN} IN ('P','perempuan','Perempuan')"
                else -> ""
            }

            // Ambil data anak dengan pemeriksaan
            val dataAnak = mutableListOf<Map<String, String>>()
            val cursor = db.rawQuery(
                """
                SELECT DISTINCT 
                    a.${DatabaseHelper.COL_ANAK_ID},
                    a.${DatabaseHelper.COL_ANAK_NAMA},
                    a.${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                    a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN},
                    o.${DatabaseHelper.COL_ORTU_NAMA}
                FROM ${DatabaseHelper.TABLE_ANAK} a
                JOIN ${DatabaseHelper.TABLE_ORTU} o ON a.${DatabaseHelper.COL_ANAK_ORTU_ID} = o.${DatabaseHelper.COL_ORTU_ID}
                WHERE EXISTS (
                    SELECT 1 FROM ${DatabaseHelper.TABLE_PEMERIKSAAN} p 
                    WHERE p.${DatabaseHelper.COL_PMRK_ANAK_ID} = a.${DatabaseHelper.COL_ANAK_ID}
                    AND substr(p.${DatabaseHelper.COL_PMRK_TGL}, 4, 7) BETWEEN ? AND ?
                )
                $genderFilter
                ORDER BY a.${DatabaseHelper.COL_ANAK_NAMA}
                """.trimIndent(),
                arrayOf(bulanDari, bulanSampai)
            )

            while (cursor.moveToNext()) {
                val anakId = cursor.getString(0)
                val nama = cursor.getString(1) ?: ""
                val tglLahir = cursor.getString(2) ?: ""
                val gender = cursor.getString(3) ?: ""
                val ortu = cursor.getString(4) ?: ""

                // Ambil pemeriksaan terakhir
                val cursorPmrk = db.rawQuery(
                    """
                    SELECT ${DatabaseHelper.COL_PMRK_TGL}, ${DatabaseHelper.COL_PMRK_BB}, 
                           ${DatabaseHelper.COL_PMRK_TB}, ${DatabaseHelper.COL_PMRK_STATUS_GIZI}
                    FROM ${DatabaseHelper.TABLE_PEMERIKSAAN}
                    WHERE ${DatabaseHelper.COL_PMRK_ANAK_ID} = ?
                    AND substr(${DatabaseHelper.COL_PMRK_TGL}, 4, 7) BETWEEN ? AND ?
                    ORDER BY ${DatabaseHelper.COL_PMRK_TGL} DESC
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(anakId, bulanDari, bulanSampai)
                )

                val pmrkTgl: String
                val pmrkBb: String
                val pmrkTb: String
                val pmrkStatus: String
                if (cursorPmrk.moveToFirst()) {
                    pmrkTgl = cursorPmrk.getString(0) ?: "-"
                    pmrkBb = cursorPmrk.getString(1) ?: "-"
                    pmrkTb = cursorPmrk.getString(2) ?: "-"
                    pmrkStatus = cursorPmrk.getString(3) ?: "-"
                } else {
                    pmrkTgl = "-"
                    pmrkBb = "-"
                    pmrkTb = "-"
                    pmrkStatus = "-"
                }
                cursorPmrk.close()

                // Hitung umur
                val umur = hitungUmurBulan(tglLahir)

                dataAnak.add(mapOf(
                    "no" to (dataAnak.size + 1).toString(),
                    "nama" to nama,
                    "tgl_lahir" to tglLahir,
                    "umur" to "$umur bln",
                    "gender" to gender,
                    "ortu" to ortu,
                    "pmrk_tgl" to pmrkTgl,
                    "bb" to pmrkBb,
                    "tb" to pmrkTb,
                    "status_gizi" to pmrkStatus
                ))
            }
            cursor.close()
            db.close()

            // Generate file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Laporan_${namaPosyandu}_${bulanDari}_${bulanSampai}_$timestamp"

            return@withContext when (format.lowercase()) {
                "csv" -> exportCSV(dataAnak, namaKader, namaPosyandu, bulanDari, bulanSampai, fileName)
                "pdf" -> exportPDF(dataAnak, namaKader, namaPosyandu, bulanDari, bulanSampai, fileName)
                "excel" -> exportExcel(dataAnak, namaKader, namaPosyandu, bulanDari, bulanSampai, fileName)
                else -> ExportResult(false, "Format tidak didukung: $format")
            }

        } catch (t: Throwable) {
            Log.e("LAPORAN_EXPORT", "Export error: ${t.message}", t)
            return@withContext ExportResult(false, "Export gagal: ${t.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPORT PDF - MENGGUNAKAN ANDROID PdfDocument (NATIVE)
    // ═══════════════════════════════════════════════════════════
    private fun exportPDF(
        data: List<Map<String, String>>,
        namaKader: String,
        namaPosyandu: String,
        bulanDari: String,
        bulanSampai: String,
        fileName: String
    ): ExportResult {
        return try {
            // Buat PDF menggunakan Android PdfDocument (native, tidak perlu library)
            val pdfDocument = PdfDocument()

            // Page info: A4 size (595 x 842 points)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

            // Font paint
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            val headerPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8f
            }

            val linePaint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 1f
            }

            val greenPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }

            // Calculate pages needed
            val rowsPerPage = 35
            val totalPages = (data.size + rowsPerPage - 1) / rowsPerPage

            for (pageNum in 0 until totalPages.coerceAtLeast(1)) {
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                var y = 40f

                // Header - Page number
                canvas.drawText("Halaman ${pageNum + 1} dari $totalPages", 550f, 20f, textPaint)

                // Title
                canvas.drawText("LAPORAN POSYANDU", 297f, y, titlePaint)
                y += 25f

                // Info
                canvas.drawText("Posyandu: $namaPosyandu", 50f, y, headerPaint)
                y += 15f
                canvas.drawText("Kader: $namaKader", 50f, y, headerPaint)
                y += 15f
                canvas.drawText("Periode: $bulanDari - $bulanSampai", 50f, y, headerPaint)
                y += 15f
                canvas.drawText("Tanggal Cetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 50f, y, headerPaint)
                y += 20f

                // Table header background
                val headerBgPaint = Paint().apply { color = Color.rgb(46, 125, 50) }
                canvas.drawRect(50f, y, 545f, y + 20f, headerBgPaint)

                // Table headers
                val colX = listOf(55f, 75f, 160f, 230f, 270f, 330f, 400f, 445f, 485f, 530f)
                val headers = listOf("No", "Nama Anak", "Tgl Lahir", "Umur", "Gender", "Ortu", "Tgl Pmrk", "BB", "TB", "Status")

                headers.forEachIndexed { i, h ->
                    canvas.drawText(h, colX[i], y + 14f, Paint().apply {
                        color = Color.WHITE
                        textSize = 8f
                        typeface = Typeface.DEFAULT_BOLD
                    })
                }
                y += 20f

                // Draw rows
                val startIdx = pageNum * rowsPerPage
                val endIdx = minOf(startIdx + rowsPerPage, data.size)

                for (i in startIdx until endIdx) {
                    val row = data[i]

                    // Alternating row background
                    if (i % 2 == 0) {
                        canvas.drawRect(50f, y, 545f, y + 15f, Paint().apply { color = Color.rgb(245, 245, 245) })
                    }

                    // Border line
                    canvas.drawLine(50f, y, 545f, y, linePaint)

                    // Data
                    canvas.drawText(row["no"] ?: "", colX[0], y + 11f, textPaint)
                    canvas.drawText(truncateText(row["nama"] ?: "", 12), colX[1], y + 11f, textPaint)
                    canvas.drawText(row["tgl_lahir"] ?: "", colX[2], y + 11f, textPaint)
                    canvas.drawText(row["umur"] ?: "", colX[3], y + 11f, textPaint)
                    canvas.drawText(row["gender"] ?: "", colX[4], y + 11f, textPaint)
                    canvas.drawText(truncateText(row["ortu"] ?: "", 10), colX[5], y + 11f, textPaint)
                    canvas.drawText(row["pmrk_tgl"] ?: "", colX[6], y + 11f, textPaint)
                    canvas.drawText(row["bb"] ?: "", colX[7], y + 11f, textPaint)
                    canvas.drawText(row["tb"] ?: "", colX[8], y + 11f, textPaint)
                    canvas.drawText(truncateText(row["status_gizi"] ?: "", 8), colX[9], y + 11f, textPaint)

                    y += 15f
                }

                // Bottom border
                canvas.drawLine(50f, y, 545f, y, linePaint)

                // Footer
                y += 20f
                canvas.drawText("Total Anak: ${data.size}", 50f, y, headerPaint)
                y += 15f
                canvas.drawText("Dicetak dari aplikasi MyPosyandu", 50f, y, textPaint)

                pdfDocument.finishPage(page)
            }

            // Save to file
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return ExportResult(false, "Gagal membuat file PDF di folder Downloads")

            val os = context.contentResolver.openOutputStream(uri)
                ?: return ExportResult(false, "Gagal membuka output stream untuk PDF")
            
            os.use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            ExportResult(
                true,
                "PDF berhasil disimpan di Downloads/$fileName.pdf",
                uri.toString()
            )
        } catch (t: Throwable) {
            Log.e("PDF_EXPORT", "PDF error: ${t.message}", t)
            ExportResult(false, "PDF gagal: ${t.message}")
        }
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) text.substring(0, maxLength - 2) + ".." else text
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPORT CSV
    // ═══════════════════════════════════════════════════════════
    private fun exportCSV(
        data: List<Map<String, String>>,
        namaKader: String,
        namaPosyandu: String,
        bulanDari: String,
        bulanSampai: String,
        fileName: String
    ): ExportResult {
        return try {
            val csvBuilder = StringBuilder()

            // BOM untuk Excel UTF-8
            csvBuilder.append('\uFEFF')

            // Header
            csvBuilder.appendLine("LAPORAN POSYANDU")
            csvBuilder.appendLine("Posyandu: $namaPosyandu")
            csvBuilder.appendLine("Kader: $namaKader")
            csvBuilder.appendLine("Periode: $bulanDari - $bulanSampai")
            csvBuilder.appendLine("Tanggal Cetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            csvBuilder.appendLine()

            // Column headers
            csvBuilder.appendLine("No,Nama Anak,Tanggal Lahir,Umur,Jenis Kelamin,Nama Ortu,Tgl Pemeriksaan,BB (kg),TB (cm),Status Gizi")

            // Data
            data.forEach { row ->
                csvBuilder.appendLine(
                    "${row["no"]},${escapeCSV(row["nama"])},${row["tgl_lahir"]},${row["umur"]}," +
                            "${row["gender"]},${escapeCSV(row["ortu"])},${row["pmrk_tgl"]}," +
                            "${row["bb"]},${row["tb"]},${row["status_gizi"]}"
                )
            }

            // Summary
            csvBuilder.appendLine()
            csvBuilder.appendLine("Total Anak: ${data.size}")

            // Save to Downloads
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.csv")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return ExportResult(false, "Gagal membuat file CSV di folder Downloads")

            val os = context.contentResolver.openOutputStream(uri)
                ?: return ExportResult(false, "Gagal membuka output stream untuk CSV")

            os.use { it.write(csvBuilder.toString().toByteArray(Charsets.UTF_8)) }

            ExportResult(true, "CSV berhasil disimpan di Downloads/$fileName.csv", uri.toString())
        } catch (t: Throwable) {
            Log.e("CSV_EXPORT", "CSV error: ${t.message}", t)
            ExportResult(false, "CSV gagal: ${t.message}")
        }
    }

    private fun escapeCSV(value: String?): String {
        val v = value ?: ""
        return if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            "\"${v.replace("\"", "\"\"")}\""
        } else v
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPORT EXCEL (XLSX menggunakan Apache POI)
    // ═══════════════════════════════════════════════════════════
    private fun exportExcel(
        data: List<Map<String, String>>,
        namaKader: String,
        namaPosyandu: String,
        bulanDari: String,
        bulanSampai: String,
        fileName: String
    ): ExportResult {
        return try {
            // Check if Apache POI available
            try {
                Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook")
            } catch (e: Throwable) {
                // Fallback to CSV with .xlsx extension if POI not available
                return exportExcelFallback(data, namaKader, namaPosyandu, bulanDari, bulanSampai, fileName)
            }

            // Use Apache POI for real Excel
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
            val sheet = workbook.createSheet("Laporan Posyandu")

            // Styles
            val titleFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 16.toShort()
                color = org.apache.poi.ss.usermodel.IndexedColors.DARK_GREEN.getIndex()
            }
            val titleStyle = workbook.createCellStyle().apply { setFont(titleFont) }

            val headerFont = workbook.createFont().apply {
                bold = true
                color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex()
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.GREEN.getIndex()
                fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
            }

            val cellStyle = workbook.createCellStyle().apply {
                borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
            }

            // Title rows
            var rowNum = 0
            val titleRow = sheet.createRow(rowNum++)
            val titleCell = titleRow.createCell(0)
            titleCell.setCellValue("LAPORAN POSYANDU")
            titleCell.cellStyle = titleStyle
            sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9))

            val infoRow1 = sheet.createRow(rowNum++)
            infoRow1.createCell(0).setCellValue("Posyandu: $namaPosyandu")
            val infoRow2 = sheet.createRow(rowNum++)
            infoRow2.createCell(0).setCellValue("Kader: $namaKader")
            val infoRow3 = sheet.createRow(rowNum++)
            infoRow3.createCell(0).setCellValue("Periode: $bulanDari - $bulanSampai")
            val infoRow4 = sheet.createRow(rowNum++)
            infoRow4.createCell(0).setCellValue("Tanggal Cetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            rowNum++

            // Header row
            val headers = arrayOf("No", "Nama Anak", "Tgl Lahir", "Umur", "Gender", "Nama Ortu", "Tgl Pemeriksaan", "BB (kg)", "TB (cm)", "Status Gizi")
            val headerRow = sheet.createRow(rowNum++)
            headers.forEachIndexed { i, h ->
                val cell = headerRow.createCell(i)
                cell.setCellValue(h)
                cell.cellStyle = headerStyle
            }

            // Data rows
            data.forEach { row ->
                val dataRow = sheet.createRow(rowNum++)
                dataRow.createCell(0).setCellValue(row["no"]?.toDoubleOrNull() ?: 0.0)
                dataRow.createCell(1).setCellValue(row["nama"] ?: "")
                dataRow.createCell(2).setCellValue(row["tgl_lahir"] ?: "")
                dataRow.createCell(3).setCellValue(row["umur"] ?: "")
                dataRow.createCell(4).setCellValue(row["gender"] ?: "")
                dataRow.createCell(5).setCellValue(row["ortu"] ?: "")
                dataRow.createCell(6).setCellValue(row["pmrk_tgl"] ?: "")
                dataRow.createCell(7).setCellValue(row["bb"]?.toDoubleOrNull() ?: 0.0)
                dataRow.createCell(8).setCellValue(row["tb"]?.toDoubleOrNull() ?: 0.0)
                dataRow.createCell(9).setCellValue(row["status_gizi"] ?: "")

                (0..9).forEach { i ->
                    dataRow.getCell(i)?.cellStyle = cellStyle
                }
            }

            // Summary
            rowNum++
            val summaryRow = sheet.createRow(rowNum++)
            summaryRow.createCell(0).setCellValue("Total Anak:")
            summaryRow.createCell(1).setCellValue(data.size.toDouble())

            // Save
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.xlsx")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return ExportResult(false, "Gagal membuat file Excel di folder Downloads")

            val os = context.contentResolver.openOutputStream(uri)
                ?: return ExportResult(false, "Gagal membuka output stream untuk Excel")

            os.use { workbook.write(it) }
            workbook.close()

            ExportResult(true, "Excel berhasil disimpan di Downloads/$fileName.xlsx", uri.toString())
        } catch (t: Throwable) {
            Log.e("EXCEL_EXPORT", "Excel error: ${t.message}", t)
            ExportResult(false, "Excel gagal: ${t.message}")
        }
    }

    // Fallback Excel kalau Apache POI tidak tersedia
    private fun exportExcelFallback(
        data: List<Map<String, String>>,
        namaKader: String,
        namaPosyandu: String,
        bulanDari: String,
        bulanSampai: String,
        fileName: String
    ): ExportResult {
        return try {
            val csvBuilder = StringBuilder()
            csvBuilder.append('\uFEFF') // BOM

            csvBuilder.appendLine("LAPORAN POSYANDU")
            csvBuilder.appendLine("Posyandu: $namaPosyandu")
            csvBuilder.appendLine("Kader: $namaKader")
            csvBuilder.appendLine("Periode: $bulanDari - $bulanSampai")
            csvBuilder.appendLine()

            // Tab-separated untuk Excel
            csvBuilder.appendLine("No\tNama Anak\tTgl Lahir\tUmur\tGender\tOrtu\tTgl Pemeriksaan\tBB\tTB\tStatus Gizi")
            data.forEach { row ->
                csvBuilder.appendLine(
                    "${row["no"]}\t${row["nama"]}\t${row["tgl_lahir"]}\t${row["umur"]}\t" +
                            "${row["gender"]}\t${row["ortu"]}\t${row["pmrk_tgl"]}\t" +
                            "${row["bb"]}\t${row["tb"]}\t${row["status_gizi"]}"
                )
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.xls")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return ExportResult(false, "Gagal membuat file Excel (fallback) di folder Downloads")

            val os = context.contentResolver.openOutputStream(uri)
                ?: return ExportResult(false, "Gagal membuka output stream untuk Excel (fallback)")

            os.use { it.write(csvBuilder.toString().toByteArray(Charsets.UTF_8)) }

            ExportResult(true, "Excel (fallback) disimpan di Downloads/$fileName.xls", uri.toString())
        } catch (t: Throwable) {
            Log.e("EXCEL_FALLBACK", "Excel fallback error: ${t.message}", t)
            ExportResult(false, "Excel fallback gagal: ${t.message}")
        }
    }

    // ... (existing helper methods: hitungTotalHadir, hitungTotalStunting, etc.) ...

    private fun hitungTotalHadir(db: android.database.sqlite.SQLiteDatabase, bulanDari: String, bulanSampai: String): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(DISTINCT anak_id) FROM (
                SELECT ${DatabaseHelper.COL_PMRK_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_PEMERIKSAAN}
                WHERE substr(${DatabaseHelper.COL_PMRK_TGL}, 4, 7) BETWEEN ? AND ?
                UNION
                SELECT ${DatabaseHelper.COL_VR_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_VAKSIN_RIWAYAT}
                WHERE substr(${DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN}, 4, 7) BETWEEN ? AND ?
            )
            """.trimIndent(),
            arrayOf(bulanDari, bulanSampai, bulanDari, bulanSampai)
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    private fun hitungTotalStunting(
        db: android.database.sqlite.SQLiteDatabase,
        bulanDari: String,
        bulanSampai: String,
        genderFilter: String
    ): Int {
        val cursor = db.rawQuery(
            """
            SELECT p.${DatabaseHelper.COL_PMRK_ANAK_ID},
                   p.${DatabaseHelper.COL_PMRK_TB},
                   p.${DatabaseHelper.COL_PMRK_TGL},
                   a.${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                   a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN}
            FROM ${DatabaseHelper.TABLE_PEMERIKSAAN} p
            JOIN ${DatabaseHelper.TABLE_ANAK} a ON p.${DatabaseHelper.COL_PMRK_ANAK_ID} = a.${DatabaseHelper.COL_ANAK_ID}
            WHERE substr(p.${DatabaseHelper.COL_PMRK_TGL}, 4, 7) BETWEEN ? AND ?
            $genderFilter
            """.trimIndent(),
            arrayOf(bulanDari, bulanSampai)
        )

        var totalStunting = 0
        val processedAnak = mutableSetOf<String>()

        while (cursor.moveToNext()) {
            val anakId = cursor.getString(0) ?: continue
            val tb = cursor.getDouble(1)
            val tglPemeriksaan = cursor.getString(2) ?: ""
            val tglLahir = cursor.getString(3) ?: ""
            val jenisKelamin = cursor.getString(4) ?: ""

            val umurBulan = hitungUmurBulanPadaTanggal(tglLahir, tglPemeriksaan)
            val zScoreTBU = hitungZScoreTBU(tb, umurBulan, jenisKelamin)

            if (zScoreTBU < -2.0 && !processedAnak.contains(anakId)) {
                totalStunting++
                processedAnak.add(anakId)
            }
        }
        cursor.close()
        return totalStunting
    }

    private fun hitungTotalVaksinTerlambat(
        db: android.database.sqlite.SQLiteDatabase,
        bulanDari: String,
        bulanSampai: String,
        genderFilter: String
    ): Int {
        val cursor = db.rawQuery(
            """
            SELECT vr.${DatabaseHelper.COL_VR_ANAK_ID},
                   vr.${DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN},
                   a.${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                   ref.${DatabaseHelper.COL_VAKSIN_REF_BATAS_BULAN},
                   ref.${DatabaseHelper.COL_VAKSIN_REF_NAMA},
                   a.${DatabaseHelper.COL_ANAK_NAMA}
            FROM ${DatabaseHelper.TABLE_VAKSIN_RIWAYAT} vr
            JOIN ${DatabaseHelper.TABLE_ANAK} a ON vr.${DatabaseHelper.COL_VR_ANAK_ID} = a.${DatabaseHelper.COL_ANAK_ID}
            JOIN ${DatabaseHelper.TABLE_VAKSIN_REF} ref ON vr.${DatabaseHelper.COL_VR_VAKSIN_REF_ID} = ref.${DatabaseHelper.COL_VAKSIN_REF_ID}
            WHERE substr(vr.${DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN}, 4, 7) BETWEEN ? AND ?
            $genderFilter
            """.trimIndent(),
            arrayOf(bulanDari, bulanSampai)
        )

        var totalVaksinTerlambat = 0
        val processedVaksinAnak = mutableSetOf<String>()

        while (cursor.moveToNext()) {
            val anakId = cursor.getString(0) ?: continue
            val tglPemberian = cursor.getString(1) ?: ""
            val tglLahir = cursor.getString(2) ?: ""
            val batasBulan = cursor.getInt(3)
            val namaVaksin = cursor.getString(4) ?: ""
            val namaAnak = cursor.getString(5) ?: ""

            val tglBatas = hitungTanggalBatas(tglLahir, batasBulan)
            val tglPemberianParsed = parseTanggal(tglPemberian)
            val tglBatasParsed = parseTanggal(tglBatas)

            val isTerlambat = if (tglPemberianParsed != null && tglBatasParsed != null) {
                tglPemberianParsed.isAfter(tglBatasParsed)
            } else {
                val umurSaatPemberian = hitungUmurBulanPadaTanggal(tglLahir, tglPemberian)
                umurSaatPemberian > batasBulan
            }

            Log.d("LAPORAN_VAKSIN", "Anak=$namaAnak, Vaksin=$namaVaksin, Terlambat=$isTerlambat")

            if (isTerlambat && !processedVaksinAnak.contains(anakId)) {
                totalVaksinTerlambat++
                processedVaksinAnak.add(anakId)
            }
        }
        cursor.close()
        return totalVaksinTerlambat
    }

    private suspend fun insertLaporanToApi(
        laporanId: String,
        kaderId: String,
        jadwalId: String?,
        totalHadir: Int,
        totalStunting: Int,
        totalVaksinTerlambat: Int,
        ringkasan: String,
        generatedAt: String,
        tglDari: String,
        tglSampai: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiUrl    = "https://myposyandu.gt.tc/api_posyandu/laporan.php"
            val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

            val formBody = FormBody.Builder()
                .add("id", laporanId)
                .add("kader_id", kaderId)
                .add("jadwal_id", jadwalId ?: "")
                .add("total_hadir", totalHadir.toString())
                .add("total_stunting", totalStunting.toString())
                .add("total_vaksin_terlambat", totalVaksinTerlambat.toString())
                .add("ringkasan", ringkasan)
                .add("generated_at", generatedAt)
                .add("periode_dari", tglDari)
                .add("periode_sampai", tglSampai)
                .build()

            // STEP 1: GET → dapat challenge HTML
            val res1  = httpClient.newCall(
                Request.Builder().url(apiUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/json")
                    .build()
            ).execute()
            val body1 = res1.body?.string() ?: ""
            res1.close()
            Log.d("SYNC_LAPORAN_API", "Step1 body: ${body1.take(100)}")

            if (!body1.trimStart().startsWith("[")) {
                // STEP 2: Solve challenge + GET redirect URL
                val challenge = parseChallenge(body1, apiUrl)
                if (challenge == null) {
                    Log.e("SYNC_LAPORAN_API", "Gagal parse challenge saat insert laporan")
                    return@withContext false
                }

                val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
                Log.d("SYNC_LAPORAN_API", "Cookie __test=$cookieValue")

                setTestCookie("myposyandu.gt.tc", cookieValue)

                val redirectUrl = if (challenge.redirectUrl.startsWith("http"))
                    challenge.redirectUrl
                else "https://myposyandu.gt.tc${challenge.redirectUrl}"

                val res2 = httpClient.newCall(
                    Request.Builder().url(redirectUrl)
                        .header("User-Agent", userAgent)
                        .header("Accept", "text/html,application/json")
                        .header("Referer", apiUrl)
                        .build()
                ).execute()
                val body2 = res2.body?.string() ?: ""
                res2.close()
                Log.d("SYNC_LAPORAN_API", "Step2 (redirect) body: ${body2.take(100)}")
            }

            // STEP 3: POST data laporan
            val postResponse = httpClient.newCall(
                Request.Builder().url(apiUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .header("Referer", apiUrl)
                    .post(formBody)
                    .build()
            ).execute()
            val postBody = postResponse.body?.string() ?: ""
            postResponse.close()

            Log.d("SYNC_LAPORAN_API", "Insert response: code=${postResponse.code}, body=$postBody")

            return@withContext try {
                val json    = JSONObject(postBody)
                val success = json.optBoolean("success", false)
                val exists  = json.optBoolean("exists",  false)
                success || exists
            } catch (e: Exception) {
                postResponse.isSuccessful
            }

        } catch (e: Exception) {
            Log.e("SYNC_LAPORAN_API", "Gagal insert laporan: ${e.message}", e)
            return@withContext false
        }
    }

    // ... existing helper methods ...
    private fun hitungTanggalBatas(tglLahir: String, batasBulan: Int): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val lahir = LocalDate.parse(tglLahir, formatter)
            val batas = lahir.plusMonths(batasBulan.toLong())
            String.format("%02d/%02d/%04d", batas.dayOfMonth, batas.monthValue, batas.year)
        } catch (e: Exception) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val lahir = LocalDate.parse(tglLahir, formatter)
                val batas = lahir.plusMonths(batasBulan.toLong())
                String.format("%02d/%02d/%04d", batas.dayOfMonth, batas.monthValue, batas.year)
            } catch (e2: Exception) { "" }
        }
    }

    private fun parseTanggal(tgl: String): LocalDate? {
        return try {
            DateTimeFormatter.ofPattern("dd/MM/yyyy").let { LocalDate.parse(tgl, it) }
        } catch (e: Exception) {
            try {
                DateTimeFormatter.ofPattern("yyyy-MM-dd").let { LocalDate.parse(tgl, it) }
            } catch (e2: Exception) { null }
        }
    }

    private fun hitungUmurBulanPadaTanggal(tglLahir: String, tglReferensi: String): Int {
        return try {
            val lahir = parseTanggal(tglLahir) ?: return 0
            val referensi = parseTanggal(tglReferensi) ?: return 0
            ChronoUnit.MONTHS.between(lahir, referensi).toInt().coerceAtLeast(0)
        } catch (e: Exception) { 0 }
    }

    private fun hitungZScoreTBU(tb: Double, umurBulan: Int, jenisKelamin: String): Double {
        val isLaki = jenisKelamin.contains("L", ignoreCase = true) ||
                jenisKelamin.contains("laki", ignoreCase = true)
        val tabel = if (isLaki) tabelTBU_LakiLaki else tabelTBU_Perempuan
        val (median, sd) = interpolasi(umurBulan, tabel)
        return (tb - median) / sd
    }

    private fun hitungUmurBulan(tglLahir: String): Int {
        return try {
            val lahir = parseTanggal(tglLahir) ?: return 0
            ChronoUnit.MONTHS.between(lahir, LocalDate.now()).toInt().coerceAtLeast(0)
        } catch (e: Exception) { 0 }
    }
}