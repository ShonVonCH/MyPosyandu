package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class PemeriksaanRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    data class PemeriksaanRow(
        val id         : String,
        val anakId     : String,
        val kaderId    : String,
        val tgl        : String,
        val bb         : Double,
        val tb         : Double,
        val lk         : Double,
        val ll         : Double,
        val zScoreTbu  : Double,
        val zScoreBbu  : Double,
        val statusGizi : String,
        val catatan    : String
    )

    fun insertPemeriksaan(
        id         : String,
        anakId     : String,
        kaderId    : String,
        tgl        : String,
        bb         : Double,
        tb         : Double,
        lk         : Double,
        ll         : Double,
        zScoreTbu  : Double,
        zScoreBbu  : Double,
        statusGizi : String,
        catatan    : String
    ): Long {
        val db = dbHelper.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_PMRK_ID,          id)
                put(DatabaseHelper.COL_PMRK_ANAK_ID,     anakId)
                put(DatabaseHelper.COL_PMRK_KADER_ID,    kaderId)
                put(DatabaseHelper.COL_PMRK_TGL,         tgl)
                put(DatabaseHelper.COL_PMRK_BB,          bb)
                put(DatabaseHelper.COL_PMRK_TB,          tb)
                put(DatabaseHelper.COL_PMRK_LK,          lk)
                put(DatabaseHelper.COL_PMRK_LL,          ll)
                put(DatabaseHelper.COL_PMRK_Z_SCORE_TBU, zScoreTbu)
                put(DatabaseHelper.COL_PMRK_Z_SCORE_BBU, zScoreBbu)
                put(DatabaseHelper.COL_PMRK_STATUS_GIZI, statusGizi)
                put(DatabaseHelper.COL_PMRK_CATATAN,     catatan)
            }
            db.insert(DatabaseHelper.TABLE_PEMERIKSAAN, null, values)
        } finally {
            db.close()
        }
    }

    fun getPemeriksaanByAnak(anakId: String): List<PemeriksaanRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<PemeriksaanRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_PEMERIKSAAN,
                null,
                "${DatabaseHelper.COL_PMRK_ANAK_ID} = ?",
                arrayOf(anakId),
                null, null,
                "${DatabaseHelper.COL_PMRK_TGL} DESC"
            )
            while (cursor.moveToNext()) result.add(cursor.toRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    fun getPemeriksaanTerakhir(anakId: String): PemeriksaanRow? {
        val db     = dbHelper.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                DatabaseHelper.TABLE_PEMERIKSAAN,
                null,
                "${DatabaseHelper.COL_PMRK_ANAK_ID} = ?",
                arrayOf(anakId),
                null, null,
                "${DatabaseHelper.COL_PMRK_TGL} DESC",
                "1"
            )
            if (cursor.moveToFirst()) cursor.toRow() else null
        } finally {
            cursor?.close()
            db.close()
        }
    }

    fun getAllPemeriksaan(): List<PemeriksaanRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<PemeriksaanRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_PEMERIKSAAN,
                null, null, null, null, null,
                "${DatabaseHelper.COL_PMRK_TGL} DESC"
            )
            while (cursor.moveToNext()) result.add(cursor.toRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    private fun Cursor.toRow() = PemeriksaanRow(
        id         = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_ID))          ?: "",
        anakId     = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_ANAK_ID))     ?: "",
        kaderId    = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_KADER_ID))    ?: "",
        tgl        = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_TGL))         ?: "",
        bb         = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_BB)),
        tb         = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_TB)),
        lk         = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_LK)),
        ll         = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_LL)),
        zScoreTbu  = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_Z_SCORE_TBU)),
        zScoreBbu  = getDouble(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_Z_SCORE_BBU)),
        statusGizi = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_STATUS_GIZI)) ?: "",
        catatan    = getString(getColumnIndexOrThrow(DatabaseHelper.COL_PMRK_CATATAN))     ?: ""
    )
}