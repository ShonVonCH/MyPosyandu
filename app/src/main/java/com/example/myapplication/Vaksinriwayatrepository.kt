package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import java.util.UUID

class VaksinRiwayatRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    data class VaksinRefRow(
        val id         : String,
        val kode       : String,
        val nama       : String,
        val usiaBulan  : Int,
        val batasBulan : Int,
        val kelompok   : String
    )

    data class VaksinRiwayatRow(
        val id                : String,
        val anakId            : String,
        val vaksinRefId       : String,
        val kaderId           : String,
        val tanggalPemberian  : String,
        val lokasi            : String
    )

    // ── READ vaksin_referensi ─────────────────────────────────

    fun getAllVaksinRef(): List<VaksinRefRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<VaksinRefRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_VAKSIN_REF,
                null, null, null, null, null,
                "${DatabaseHelper.COL_VAKSIN_REF_USIA_BULAN} ASC"
            )
            while (cursor.moveToNext()) result.add(cursor.toRefRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    fun getVaksinSudahWaktunya(umurBulan: Int): List<VaksinRefRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<VaksinRefRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_VAKSIN_REF,
                null,
                "${DatabaseHelper.COL_VAKSIN_REF_USIA_BULAN} <= ?",
                arrayOf(umurBulan.toString()),
                null, null,
                "${DatabaseHelper.COL_VAKSIN_REF_USIA_BULAN} ASC"
            )
            while (cursor.moveToNext()) result.add(cursor.toRefRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    fun getVaksinAkanDatang(umurBulan: Int): List<VaksinRefRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<VaksinRefRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_VAKSIN_REF,
                null,
                "${DatabaseHelper.COL_VAKSIN_REF_USIA_BULAN} > ?",
                arrayOf(umurBulan.toString()),
                null, null,
                "${DatabaseHelper.COL_VAKSIN_REF_USIA_BULAN} ASC"
            )
            while (cursor.moveToNext()) result.add(cursor.toRefRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    // ── READ vaksin_riwayat ───────────────────────────────────

    fun getVaksinSudahDiberikan(anakId: String): Set<String> {
        val db     = dbHelper.readableDatabase
        val result = mutableSetOf<String>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_VAKSIN_RIWAYAT,
                arrayOf(DatabaseHelper.COL_VR_VAKSIN_REF_ID),
                "${DatabaseHelper.COL_VR_ANAK_ID} = ?",
                arrayOf(anakId),
                null, null, null
            )
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0) ?: continue)
            }
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    fun getRiwayatByAnak(anakId: String): List<VaksinRiwayatRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<VaksinRiwayatRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_VAKSIN_RIWAYAT,
                null,
                "${DatabaseHelper.COL_VR_ANAK_ID} = ?",
                arrayOf(anakId),
                null, null, null
            )
            while (cursor.moveToNext()) result.add(cursor.toRiwayatRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    // ── READ alamat posyandu ──────────────────────────────────

    fun getAlamatPosyandu(): String {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery(
                """
                SELECT p.${DatabaseHelper.COL_POSYANDU_ALAMAT}
                FROM ${DatabaseHelper.TABLE_POSYANDU} p
                JOIN ${DatabaseHelper.TABLE_USERS} u
                    ON p.${DatabaseHelper.COL_POSYANDU_ID} = u.${DatabaseHelper.COL_USERS_POSYANDU_ID}
                LIMIT 1
                """.trimIndent(),
                null
            )
            if (cursor.moveToFirst()) {
                cursor.getString(0) ?: "Posyandu"
            } else "Posyandu"
        } finally {
            cursor?.close()
            db.close()
        }
    }

    // ── WRITE vaksin_riwayat ──────────────────────────────────

    fun insertRiwayat(
        anakId           : String,
        vaksinRefId      : String,
        kaderId          : String,
        tanggalPemberian : String,
        lokasi           : String = ""
    ): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_VR_ID,                UUID.randomUUID().toString())
                put(DatabaseHelper.COL_VR_ANAK_ID,           anakId)
                put(DatabaseHelper.COL_VR_VAKSIN_REF_ID,     vaksinRefId)
                put(DatabaseHelper.COL_VR_KADER_ID,          kaderId)
                put(DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN, tanggalPemberian)
                put(DatabaseHelper.COL_VR_LOKASI,            lokasi)
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_VAKSIN_RIWAYAT,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
            ) != -1L
        } finally {
            db.close()
        }
    }

    // ── HELPERS ───────────────────────────────────────────────

    private fun Cursor.toRefRow() = VaksinRefRow(
        id         = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VAKSIN_REF_ID))         ?: "",
        kode       = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VAKSIN_REF_KODE))       ?: "",
        nama       = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VAKSIN_REF_NAMA))       ?: "",
        usiaBulan  = getInt   (getColumnIndexOrThrow(DatabaseHelper.COL_VAKSIN_REF_USIA_BULAN)),
        batasBulan = getInt   (getColumnIndexOrThrow(DatabaseHelper.COL_VAKSIN_REF_BATAS_BULAN)),
        kelompok   = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VAKSIN_REF_KELOMPOK))   ?: ""
    )

    private fun Cursor.toRiwayatRow() = VaksinRiwayatRow(
        id               = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VR_ID))                ?: "",
        anakId           = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VR_ANAK_ID))           ?: "",
        vaksinRefId      = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VR_VAKSIN_REF_ID))     ?: "",
        kaderId          = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VR_KADER_ID))          ?: "",
        tanggalPemberian = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN)) ?: "",
        lokasi           = getString(getColumnIndexOrThrow(DatabaseHelper.COL_VR_LOKASI))            ?: ""
    )

    // ── READ ALL vaksin_riwayat (untuk sync ke API) ───────────────────────────

    /** Semua riwayat vaksin dari tabel vaksin_riwayat */
    fun getAllVaksinRiwayat(): List<VaksinRiwayatRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<VaksinRiwayatRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                DatabaseHelper.TABLE_VAKSIN_RIWAYAT,
                null, null, null, null, null,
                "${DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN} DESC"
            )
            while (cursor.moveToNext()) result.add(cursor.toRiwayatRow())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }
}