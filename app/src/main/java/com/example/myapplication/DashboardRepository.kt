package com.example.myapplication

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardRepository(context: Context) {

    private val db = DatabaseHelper(context).readableDatabase

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val today: String
        get() = LocalDate.now().format(formatter)

    private val bulanIni: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    /**
     * Total anak terdaftar dari TABLE_ANAK
     */
    fun getTotalAnak(): Int {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_ANAK}",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * Anak hadir HARI INI
     * Cek dari TABLE_PEMERIKSAAN → tanggal = hari ini
     * UNION dengan TABLE_IMUNISASI → tgl_vaksin = hari ini, status = 1
     */
    fun getAnakHadirHariIni(): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) FROM (
                SELECT ${DatabaseHelper.COL_PMRK_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_PEMERIKSAAN}
                WHERE ${DatabaseHelper.COL_PMRK_TGL} = ?

                UNION

                SELECT ${DatabaseHelper.COL_IMN_NIK_ANAK} AS anak_id
                FROM ${DatabaseHelper.TABLE_IMUNISASI}
                WHERE ${DatabaseHelper.COL_IMN_STATUS} = 1
                  AND ${DatabaseHelper.COL_IMN_TGL_VAKSIN} = ?
            )
            """.trimIndent(),
            arrayOf(today, today)
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * Anak hadir BULAN INI (unik per anak_id)
     */
    fun getAnakHadirBulanIni(): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) FROM (
                SELECT ${DatabaseHelper.COL_PMRK_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_PEMERIKSAAN}
                WHERE strftime('%Y-%m', ${DatabaseHelper.COL_PMRK_TGL}) = ?

                UNION

                SELECT ${DatabaseHelper.COL_IMN_NIK_ANAK} AS anak_id
                FROM ${DatabaseHelper.TABLE_IMUNISASI}
                WHERE ${DatabaseHelper.COL_IMN_STATUS} = 1
                  AND strftime('%Y-%m', ${DatabaseHelper.COL_IMN_TGL_VAKSIN}) = ?
            )
            """.trimIndent(),
            arrayOf(bulanIni, bulanIni)
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
}