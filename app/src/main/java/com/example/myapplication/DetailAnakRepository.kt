package com.example.myapplication

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DataRingkasanAnak(
    val bbTerakhir  : String,
    val tbTerakhir  : String,
    val lkTerakhir  : String,
    val llTerakhir  : String,
    val tglPmrk     : String,
    val umurBulan   : Int,
    val jenisKelamin: String
)

data class RiwayatPemeriksaanItem(
    val tanggal  : String,
    val bb       : String,
    val tb       : String,
    val lk       : String,
    val ll       : String,
    val hasil    : HasilAnalisis?
)

class DetailAnakRepository(private val context: Context) {

    private val db        = DatabaseHelper(context).readableDatabase

    // Support both formats: yyyy-MM-dd (ISO, from API sync) and dd/MM/yyyy (legacy)
    private fun parseTanggal(tgl: String): LocalDate {
        return try {
            when {
                tgl.contains("-") && tgl.substring(0, 4).toIntOrNull() != null -> {
                    // Format yyyy-MM-dd (ISO 8601, from API sync)
                    LocalDate.parse(tgl, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }
                tgl.contains("/") -> {
                    // Format dd/MM/yyyy (legacy local format)
                    val parts = tgl.split("/")
                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                }
                else -> LocalDate.now()
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailAnakRepo", "Gagal parse tanggal: '$tgl'", e)
            LocalDate.now()
        }
    }

    fun getDataRingkasan(anakId: String): DataRingkasanAnak? {
        // Ambil pemeriksaan terakhir
        val cursorPmrk = db.rawQuery(
            """
            SELECT ${DatabaseHelper.COL_PMRK_BB},
                   ${DatabaseHelper.COL_PMRK_TB},
                   ${DatabaseHelper.COL_PMRK_LK},
                   ${DatabaseHelper.COL_PMRK_LL},
                   ${DatabaseHelper.COL_PMRK_TGL}
            FROM   ${DatabaseHelper.TABLE_PEMERIKSAAN}
            WHERE  ${DatabaseHelper.COL_PMRK_ANAK_ID} = ?
            ORDER BY ${DatabaseHelper.COL_PMRK_TGL} DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(anakId)
        )
        var bb = ""; var tb = ""; var lk = ""; var ll = ""; var tgl = ""
        if (cursorPmrk.moveToFirst()) {
            bb  = cursorPmrk.getDouble(0).takeIf { it != 0.0 }?.toString() ?: ""
            tb  = cursorPmrk.getDouble(1).takeIf { it != 0.0 }?.toString() ?: ""
            lk  = cursorPmrk.getDouble(2).takeIf { it != 0.0 }?.toString() ?: ""
            ll  = cursorPmrk.getDouble(3).takeIf { it != 0.0 }?.toString() ?: ""
            tgl = cursorPmrk.getString(4) ?: ""
        }
        cursorPmrk.close()

        // Ambil data anak
        val cursorAnak = db.rawQuery(
            """
            SELECT ${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                   ${DatabaseHelper.COL_ANAK_JENIS_KELAMIN}
            FROM   ${DatabaseHelper.TABLE_ANAK}
            WHERE  ${DatabaseHelper.COL_ANAK_ID} = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(anakId)
        )
        var umurBulan = 0; var gender = ""
        if (cursorAnak.moveToFirst()) {
            umurBulan = hitungUmurBulanDariString(cursorAnak.getString(0) ?: "")
            gender    = cursorAnak.getString(1) ?: ""
        }
        cursorAnak.close()

        if (bb.isBlank() && tb.isBlank()) return null

        return DataRingkasanAnak(
            bbTerakhir   = bb,
            tbTerakhir   = tb,
            lkTerakhir   = lk,
            llTerakhir   = ll,
            tglPmrk      = tgl,
            umurBulan    = umurBulan,
            jenisKelamin = gender
        )
    }

    fun getRiwayatPemeriksaan(anakId: String): List<RiwayatPemeriksaanItem> {
        // Ambil tanggal lahir & gender
        val cursorAnak = db.rawQuery(
            """
            SELECT ${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                   ${DatabaseHelper.COL_ANAK_JENIS_KELAMIN}
            FROM   ${DatabaseHelper.TABLE_ANAK}
            WHERE  ${DatabaseHelper.COL_ANAK_ID} = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(anakId)
        )
        var tglLahir = ""; var gender = ""
        if (cursorAnak.moveToFirst()) {
            tglLahir = cursorAnak.getString(0) ?: ""
            gender   = cursorAnak.getString(1) ?: ""
        }
        cursorAnak.close()

        val cursorPmrk = db.rawQuery(
            """
            SELECT ${DatabaseHelper.COL_PMRK_TGL},
                   ${DatabaseHelper.COL_PMRK_BB},
                   ${DatabaseHelper.COL_PMRK_TB},
                   ${DatabaseHelper.COL_PMRK_LK},
                   ${DatabaseHelper.COL_PMRK_LL}
            FROM   ${DatabaseHelper.TABLE_PEMERIKSAAN}
            WHERE  ${DatabaseHelper.COL_PMRK_ANAK_ID} = ?
            ORDER BY ${DatabaseHelper.COL_PMRK_TGL} DESC
            """.trimIndent(),
            arrayOf(anakId)
        )

        val list = mutableListOf<RiwayatPemeriksaanItem>()
        while (cursorPmrk.moveToNext()) {
            val tglPmrk = cursorPmrk.getString(0) ?: ""
            val bb      = cursorPmrk.getDouble(1).takeIf { it != 0.0 }?.toString() ?: ""
            val tb      = cursorPmrk.getDouble(2).takeIf { it != 0.0 }?.toString() ?: ""
            val lk      = cursorPmrk.getDouble(3).takeIf { it != 0.0 }?.toString() ?: ""
            val ll      = cursorPmrk.getDouble(4).takeIf { it != 0.0 }?.toString() ?: ""

            val umurSaatPmrk = hitungUmurBulanAntaraTanggal(tglLahir, tglPmrk)

            val hasil = if (bb.isNotBlank() && tb.isNotBlank()) {
                analisisWHO(
                    tinggiBadan  = tb.toDoubleOrNull() ?: 0.0,
                    beratBadan   = bb.toDoubleOrNull() ?: 0.0,
                    umurBulan    = umurSaatPmrk,
                    jenisKelamin = gender
                )
            } else null

            list.add(RiwayatPemeriksaanItem(
                tanggal = tglPmrk,
                bb      = bb,
                tb      = tb,
                lk      = lk,
                ll      = ll,
                hasil   = hasil
            ))
        }
        cursorPmrk.close()
        return list
    }

    // ── FIX: getStatusVaksin pakai vaksin_riwayat & vaksin_referensi ──
    fun getStatusVaksin(anakId: String): Pair<Int, Int> {
        val repo = VaksinRiwayatRepository(context)

        // Hitung umur anak untuk tahu berapa vaksin yang seharusnya sudah diberikan
        val cursorAnak = db.rawQuery(
            """
            SELECT ${DatabaseHelper.COL_ANAK_TGL_LAHIR}
            FROM   ${DatabaseHelper.TABLE_ANAK}
            WHERE  ${DatabaseHelper.COL_ANAK_ID} = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(anakId)
        )
        var umurBulan = 0
        if (cursorAnak.moveToFirst()) {
            umurBulan = hitungUmurBulanDariString(cursorAnak.getString(0) ?: "")
        }
        cursorAnak.close()

        // Total vaksin yang seharusnya sudah diberikan (usia_bulan <= umur anak)
        val semuaVaksin = repo.getVaksinSudahWaktunya(umurBulan)
        val total = semuaVaksin.size

        // Vaksin yang sudah diberikan ke anak ini
        val sudahDiberikan = repo.getVaksinSudahDiberikan(anakId)
        val sudah = semuaVaksin.count { sudahDiberikan.contains(it.id) }

        return Pair(sudah, total)
    }

    // ── Helpers tanggal ──────────────────────────────────────
    private fun hitungUmurBulanDariString(tglLahirStr: String): Int {
        return try {
            val lahir = parseTanggal(tglLahirStr)
            val now   = LocalDate.now()
            ((now.year - lahir.year) * 12 + (now.monthValue - lahir.monthValue)).coerceAtLeast(0)
        } catch (e: Exception) {
            android.util.Log.e("DetailAnakRepo", "Gagal hitung umur: '$tglLahirStr'", e)
            0
        }
    }

    private fun hitungUmurBulanAntaraTanggal(tglLahirStr: String, tglPmrkStr: String): Int {
        return try {
            val lahir = parseTanggal(tglLahirStr)
            val pmrk  = parseTanggal(tglPmrkStr)
            ((pmrk.year - lahir.year) * 12 + (pmrk.monthValue - lahir.monthValue)).coerceAtLeast(0)
        } catch (e: Exception) {
            android.util.Log.e("DetailAnakRepo", "Gagal hitung umur: '$tglLahirStr' vs '$tglPmrkStr'", e)
            0
        }
    }
}