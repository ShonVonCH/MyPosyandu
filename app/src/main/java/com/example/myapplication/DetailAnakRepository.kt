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

class DetailAnakRepository(context: Context) {

    private val db        = DatabaseHelper(context).readableDatabase
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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

    fun getStatusVaksin(anakId: String): Pair<Int, Int> {
        // TABLE_IMUNISASI masih pakai nama_anak — sesuaikan jika sudah migrasi
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*),
                   SUM(CASE WHEN ${DatabaseHelper.COL_IMN_STATUS} = 1 THEN 1 ELSE 0 END)
            FROM   ${DatabaseHelper.TABLE_IMUNISASI}
            WHERE  ${DatabaseHelper.COL_IMN_NIK_ANAK} = ?
            """.trimIndent(),
            arrayOf(anakId)
        )
        var total = 0; var sudah = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
            sudah = cursor.getInt(1)
        }
        cursor.close()
        return Pair(sudah, total)
    }

    // ── Helpers tanggal ──────────────────────────────────────
    private fun hitungUmurBulanDariString(tglLahirStr: String): Int {
        return try {
            val lahir = LocalDate.parse(tglLahirStr, formatter)
            val now   = LocalDate.now()
            ((now.year - lahir.year) * 12 + (now.monthValue - lahir.monthValue)).coerceAtLeast(0)
        } catch (e: Exception) { 0 }
    }

    private fun hitungUmurBulanAntaraTanggal(tglLahirStr: String, tglPmrkStr: String): Int {
        return try {
            val lahir = LocalDate.parse(tglLahirStr, formatter)
            val pmrk  = LocalDate.parse(tglPmrkStr,  formatter)
            ((pmrk.year - lahir.year) * 12 + (pmrk.monthValue - lahir.monthValue)).coerceAtLeast(0)
        } catch (e: Exception) { 0 }
    }
}