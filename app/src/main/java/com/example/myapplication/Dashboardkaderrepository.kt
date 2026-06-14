package com.example.myapplication

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

data class KaderInfo(
    val nama       : String,
    val kelurahan  : String,
    val rw         : String,
    val posyanduNama: String,
    val alamat     : String  // <-- TAMBAH field alamat
)

data class JadwalBerikutnya(
    val lokasi    : String,
    val tanggal   : String,
    val jamMulai  : String,
    val jamSelesai: String
)

class DashboardKaderRepository(context: Context) {

    private val db = DatabaseHelper(context).readableDatabase

    // Ambil info kader dari tabel users + posyandu
    fun getKaderInfo(): KaderInfo? {
        val userCursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_USERS_NAMA}, ${DatabaseHelper.COL_USERS_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_USERS} LIMIT 1",
            null
        )

        if (!userCursor.moveToFirst()) {
            userCursor.close()
            return null
        }

        val nama       = userCursor.getString(0) ?: ""
        val posyanduId = userCursor.getString(1) ?: ""
        userCursor.close()

        val posyanduCursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_POSYANDU_NAMA}, ${DatabaseHelper.COL_POSYANDU_KELURAHAN}, ${DatabaseHelper.COL_POSYANDU_RW}, ${DatabaseHelper.COL_POSYANDU_ALAMAT} FROM ${DatabaseHelper.TABLE_POSYANDU} WHERE ${DatabaseHelper.COL_POSYANDU_ID} = ?",
            arrayOf(posyanduId)
        )

        val posyanduNama : String
        val kelurahan    : String
        val rw           : String
        val alamat       : String

        if (posyanduCursor.moveToFirst()) {
            posyanduNama = posyanduCursor.getString(0) ?: ""
            kelurahan    = posyanduCursor.getString(1) ?: ""
            rw           = posyanduCursor.getString(2) ?: ""
            alamat       = posyanduCursor.getString(3) ?: ""
        } else {
            posyanduNama = ""
            kelurahan    = ""
            rw           = ""
            alamat       = ""
        }
        posyanduCursor.close()

        return KaderInfo(
            nama        = nama,
            kelurahan   = kelurahan,
            rw          = rw,
            posyanduNama = posyanduNama,
            alamat      = alamat
        )
    }

    // Ambil jadwal posyandu berikutnya (status Terjadwal, tanggal terdekat dari hari ini)
    fun getJadwalBerikutnya(): JadwalBerikutnya? {
        // Ambil posyandu_id dari user yang login
        val userCursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_USERS_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_USERS} LIMIT 1",
            null
        )

        if (!userCursor.moveToFirst()) {
            userCursor.close()
            return null
        }

        val posyanduId = userCursor.getString(0) ?: ""
        userCursor.close()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val jadwalCursor = db.rawQuery(
            """
            SELECT 
                ${DatabaseHelper.COL_JADWAL_LOKASI},
                ${DatabaseHelper.COL_JADWAL_TANGGAL},
                ${DatabaseHelper.COL_JADWAL_JAM_MULAI},
                ${DatabaseHelper.COL_JADWAL_JAM_SELESAI}
            FROM ${DatabaseHelper.TABLE_JADWAL_POSYANDU}
            WHERE ${DatabaseHelper.COL_JADWAL_POSYANDU_ID} = ?
              AND LOWER(${DatabaseHelper.COL_JADWAL_STATUS}) = 'terjadwal'
              AND ${DatabaseHelper.COL_JADWAL_TANGGAL} >= ?
            ORDER BY ${DatabaseHelper.COL_JADWAL_TANGGAL} ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(posyanduId, today)
        )

        if (!jadwalCursor.moveToFirst()) {
            jadwalCursor.close()
            return null
        }

        val result = JadwalBerikutnya(
            lokasi     = jadwalCursor.getString(0) ?: "",
            tanggal    = jadwalCursor.getString(1) ?: "",
            jamMulai   = jadwalCursor.getString(2) ?: "",
            jamSelesai = jadwalCursor.getString(3) ?: ""
        )
        jadwalCursor.close()

        return result
    }
}