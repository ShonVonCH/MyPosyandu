package com.example.myapplication

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardRepository(context: Context) {

    private val db = DatabaseHelper(context).readableDatabase

    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val today: String
        get() = LocalDate.now().format(formatter)

    private val bulanIni: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"))

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
     * Format tanggal di DB: dd/MM/yyyy
     * Cek dari TABLE_PEMERIKSAAN → tanggal = hari ini (dd/MM/yyyy)
     * UNION DISTINCT dengan TABLE_VAKSIN_RIWAYAT → tanggal_pemberian = hari ini
     * DISTINCT = anak_id yang sama dianggap 1 meski ada di kedua tabel
     */
    fun getAnakHadirHariIni(): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(DISTINCT anak_id) FROM (
                SELECT ${DatabaseHelper.COL_PMRK_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_PEMERIKSAAN}
                WHERE ${DatabaseHelper.COL_PMRK_TGL} = ?

                UNION ALL

                SELECT ${DatabaseHelper.COL_VR_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_VAKSIN_RIWAYAT}
                WHERE ${DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN} = ?
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
     * Format tanggal di DB: dd/MM/yyyy
     * Extract MM/yyyy dengan substr(tanggal, 4, 7) = '06/2026'
     * DISTINCT anak_id yang hadir di bulan ini, baik dari pemeriksaan maupun vaksin
     */
    fun getAnakHadirBulanIni(): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(DISTINCT anak_id) FROM (
                SELECT ${DatabaseHelper.COL_PMRK_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_PEMERIKSAAN}
                WHERE substr(${DatabaseHelper.COL_PMRK_TGL}, 4, 7) = ?

                UNION ALL

                SELECT ${DatabaseHelper.COL_VR_ANAK_ID} AS anak_id
                FROM ${DatabaseHelper.TABLE_VAKSIN_RIWAYAT}
                WHERE substr(${DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN}, 4, 7) = ?
            )
            """.trimIndent(),
            arrayOf(bulanIni, bulanIni)
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * Jumlah jadwal posyandu di BULAN INI
     * Format tanggal di jadwal_posyandu: yyyy-MM-dd (dari API)
     * strftime works untuk yyyy-MM-dd
     */
    fun getJadwalBulanIni(): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) 
            FROM ${DatabaseHelper.TABLE_JADWAL_POSYANDU}
            WHERE strftime('%Y-%m', ${DatabaseHelper.COL_JADWAL_TANGGAL}) = ?
              AND LOWER(${DatabaseHelper.COL_JADWAL_STATUS}) = 'terjadwal'
            """.trimIndent(),
            arrayOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /**
     * Total kehadiran yang diharapkan = jumlah jadwal posyandu di bulan ini × total anak
     * Jika tidak ada jadwal, fallback ke total anak (1×)
     */
    fun getTotalKehadiranTarget(): Int {
        val jadwalCount = getJadwalBulanIni()
        val totalAnak = getTotalAnak()
        return if (jadwalCount > 0) jadwalCount * totalAnak else totalAnak
    }
}