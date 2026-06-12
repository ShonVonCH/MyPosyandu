package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class ImunisasiRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    data class ImunisasiRow(
        val id         : Long,
        val namaAnak   : String,
        val nikAnak    : String,
        val namaOrtu   : String,
        val namaVaksin : String,
        val tglVaksin  : String,
        val status     : Int    // 0 = belum, 1 = sudah
    )

    /** Insert atau update vaksin — jika namaAnak+namaVaksin sudah ada, update status & tanggal */
    fun upsertImunisasi(
        namaAnak  : String,
        nikAnak   : String,
        namaOrtu  : String,
        namaVaksin: String,
        tglVaksin : String,
        status    : Int = 1
    ) {
        val db = dbHelper.writableDatabase
        try {
            // Cek apakah sudah ada
            val cursor = db.query(
                DatabaseHelper.TABLE_IMUNISASI,
                arrayOf(DatabaseHelper.COL_IMN_ID),
                "${DatabaseHelper.COL_IMN_NAMA_ANAK} = ? AND ${DatabaseHelper.COL_IMN_NAMA_VAKSIN} = ?",
                arrayOf(namaAnak, namaVaksin),
                null, null, null, "1"
            )
            val exists = cursor.moveToFirst()
            val existingId = if (exists) cursor.getLong(0) else -1L
            cursor.close()

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_IMN_NAMA_ANAK,   namaAnak)
                put(DatabaseHelper.COL_IMN_NIK_ANAK,    nikAnak)
                put(DatabaseHelper.COL_IMN_NAMA_ORTU,   namaOrtu)
                put(DatabaseHelper.COL_IMN_NAMA_VAKSIN, namaVaksin)
                put(DatabaseHelper.COL_IMN_TGL_VAKSIN,  tglVaksin)
                put(DatabaseHelper.COL_IMN_STATUS,      status)
            }

            if (exists) {
                db.update(
                    DatabaseHelper.TABLE_IMUNISASI,
                    values,
                    "${DatabaseHelper.COL_IMN_ID} = ?",
                    arrayOf(existingId.toString())
                )
            } else {
                db.insert(DatabaseHelper.TABLE_IMUNISASI, null, values)
            }
        } finally {
            db.close()
        }
    }

    fun getImunisasiByAnak(namaAnak: String): List<ImunisasiRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<ImunisasiRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_IMUNISASI,
                null,
                "${DatabaseHelper.COL_IMN_NAMA_ANAK} = ?",
                arrayOf(namaAnak),
                null, null,
                "${DatabaseHelper.COL_IMN_ID} DESC"
            )
            while (cursor.moveToNext()) result.add(cursor.toRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    private fun Cursor.toRow() = ImunisasiRow(
        id         = getLong  (getColumnIndexOrThrow(DatabaseHelper.COL_IMN_ID)),
        namaAnak   = getString(getColumnIndexOrThrow(DatabaseHelper.COL_IMN_NAMA_ANAK))   ?: "",
        nikAnak    = getString(getColumnIndexOrThrow(DatabaseHelper.COL_IMN_NIK_ANAK))    ?: "",
        namaOrtu   = getString(getColumnIndexOrThrow(DatabaseHelper.COL_IMN_NAMA_ORTU))   ?: "",
        namaVaksin = getString(getColumnIndexOrThrow(DatabaseHelper.COL_IMN_NAMA_VAKSIN)) ?: "",
        tglVaksin  = getString(getColumnIndexOrThrow(DatabaseHelper.COL_IMN_TGL_VAKSIN))  ?: "",
        status     = getInt   (getColumnIndexOrThrow(DatabaseHelper.COL_IMN_STATUS))
    )
}