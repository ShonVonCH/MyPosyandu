package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
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
                android.util.Log.e("LAPORAN", "User tidak ditemukan di SQLite")
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

            android.util.Log.d("LAPORAN", "Hadir=$totalHadir, Stunting=$totalStunting, VaksinTerlambat=$totalVaksinTerlambat")

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

            android.util.Log.d("LAPORAN", "Insert lokal sukses: laporanId=$laporanId")

            // 7. SYNC KE API — pakai pola EXACT sama seperti ApiService.kt
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
            android.util.Log.e("LAPORAN", "Gagal simpan laporan: ${e.message}", e)
            return@withContext LaporanResult(null, false, "Error: ${e.message}")
        }
    }

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

            android.util.Log.d("LAPORAN_VAKSIN",
                "Anak=$namaAnak, Vaksin=$namaVaksin, Terlambat=$isTerlambat")

            if (isTerlambat && !processedVaksinAnak.contains(anakId)) {
                totalVaksinTerlambat++
                processedVaksinAnak.add(anakId)
            }
        }
        cursor.close()
        return totalVaksinTerlambat
    }

    // ═══════════════════════════════════════════════════════════
    //  SYNC LAPORAN KE API — POLA EXACT SAMA SEPERTI ApiService.kt
    // ═══════════════════════════════════════════════════════════
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

            // ── STEP 1: GET → dapat challenge HTML ───────────────────────────
            val res1  = httpClient.newCall(
                Request.Builder().url(apiUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/json")
                    .build()
            ).execute()
            val body1 = res1.body?.string() ?: ""
            res1.close()
            android.util.Log.d("SYNC_LAPORAN_API", "Step1 body: ${body1.take(100)}")

            if (!body1.trimStart().startsWith("[")) {
                // ── STEP 2: Solve challenge + GET redirect URL ────────────────
                val challenge = parseChallenge(body1, apiUrl)
                if (challenge == null) {
                    android.util.Log.e("SYNC_LAPORAN_API", "Gagal parse challenge saat insert laporan")
                    return@withContext false
                }

                val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
                android.util.Log.d("SYNC_LAPORAN_API", "Cookie __test=$cookieValue")

                setTestCookie("myposyandu.gt.tc", cookieValue)

                // GET ke redirect URL untuk validasi cookie — wajib sebelum POST
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
                android.util.Log.d("SYNC_LAPORAN_API", "Step2 (redirect) body: ${body2.take(100)}")
            }

            // ── STEP 3: POST data laporan ────────────────────────────────────
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

            android.util.Log.d("SYNC_LAPORAN_API",
                "Insert response: code=${postResponse.code}, body=$postBody")

            return@withContext try {
                val json    = JSONObject(postBody)
                val success = json.optBoolean("success", false)
                val exists  = json.optBoolean("exists",  false)
                success || exists
            } catch (e: Exception) {
                postResponse.isSuccessful
            }

        } catch (e: Exception) {
            android.util.Log.e("SYNC_LAPORAN_API", "Gagal insert laporan: ${e.message}", e)
            return@withContext false
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════
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
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val lahir = LocalDate.parse(tglLahir, formatter)
            val referensi = LocalDate.parse(tglReferensi, formatter)
            ChronoUnit.MONTHS.between(lahir, referensi).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val lahir = LocalDate.parse(tglLahir, formatter)
                val referensi = LocalDate.parse(tglReferensi, formatter)
                ChronoUnit.MONTHS.between(lahir, referensi).toInt().coerceAtLeast(0)
            } catch (e2: Exception) { 0 }
        }
    }

    private fun hitungZScoreTBU(tb: Double, umurBulan: Int, jenisKelamin: String): Double {
        val isLaki = jenisKelamin.contains("L", ignoreCase = true) ||
                jenisKelamin.contains("laki", ignoreCase = true)
        val tabel = if (isLaki) tabelTBU_LakiLaki else tabelTBU_Perempuan
        val (median, sd) = interpolasi(umurBulan, tabel)
        return (tb - median) / sd
    }
}