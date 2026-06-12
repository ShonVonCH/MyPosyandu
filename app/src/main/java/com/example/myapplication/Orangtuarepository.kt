package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class OrangTuaRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // ── DATA CLASSES ──────────────────────────────────────────────

    data class OrangTuaAnakRow(
        val id           : Long,
        val namaOrtu     : String,
        val usernameOrtu : String,
        val noHpOrtu     : String,
        val passOrtu     : String,
        val namaAnak     : String?,
        val nikAnak      : String?,   // = anak.id
        val tglLahirAnak : String?,
        val genderAnak   : String?,
        val alamatAnak   : String?
    )

    data class OrtuSummary(
        val namaOrtu    : String,
        val usernameOrtu: String,
        val noHpOrtu    : String,
        val passOrtu    : String,
        val jumlahAnak  : Int
    )

    // ── READ ──────────────────────────────────────────────────────

    /** Semua ortu dengan jumlah anak masing-masing */
    fun getAllOrtu(): List<OrtuSummary> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<OrtuSummary>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                """
                SELECT
                    o.${DatabaseHelper.COL_ORTU_ID},
                    o.${DatabaseHelper.COL_ORTU_NAMA},
                    o.${DatabaseHelper.COL_ORTU_USERNAME},
                    o.${DatabaseHelper.COL_ORTU_PASSWORD},
                    COUNT(a.${DatabaseHelper.COL_ANAK_ID}) AS jumlah_anak
                FROM ${DatabaseHelper.TABLE_ORTU} o
                LEFT JOIN ${DatabaseHelper.TABLE_ANAK} a
                       ON a.${DatabaseHelper.COL_ANAK_ORTU_ID} = o.${DatabaseHelper.COL_ORTU_ID}
                GROUP BY o.${DatabaseHelper.COL_ORTU_ID}
                ORDER BY o.${DatabaseHelper.COL_ORTU_NAMA} ASC
                """.trimIndent(),
                null
            )
            while (cursor.moveToNext()) {
                result.add(
                    OrtuSummary(
                        namaOrtu     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_NAMA)),
                        usernameOrtu = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_USERNAME)),
                        noHpOrtu     = "",   // kolom no_hp tidak ada di skema baru
                        passOrtu     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_PASSWORD)) ?: "",
                        jumlahAnak   = cursor.getInt(cursor.getColumnIndexOrThrow("jumlah_anak"))
                    )
                )
            }
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    /** Semua anak milik satu username ortu */
    fun getAnakByUsername(usernameOrtu: String): List<OrangTuaAnakRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<OrangTuaAnakRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                """
                SELECT
                    a.${DatabaseHelper.COL_ANAK_ID},
                    o.${DatabaseHelper.COL_ORTU_NAMA},
                    o.${DatabaseHelper.COL_ORTU_USERNAME},
                    o.${DatabaseHelper.COL_ORTU_PASSWORD},
                    a.${DatabaseHelper.COL_ANAK_NAMA},
                    a.${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                    a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN}
                FROM ${DatabaseHelper.TABLE_ANAK} a
                JOIN ${DatabaseHelper.TABLE_ORTU} o
                  ON a.${DatabaseHelper.COL_ANAK_ORTU_ID} = o.${DatabaseHelper.COL_ORTU_ID}
                WHERE o.${DatabaseHelper.COL_ORTU_USERNAME} = ?
                ORDER BY a.${DatabaseHelper.COL_ANAK_ID} ASC
                """.trimIndent(),
                arrayOf(usernameOrtu)
            )
            while (cursor.moveToNext()) result.add(cursor.toRowFromJoin())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    /** Semua baris anak+ortu */
    fun getAllRows(): List<OrangTuaAnakRow> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<OrangTuaAnakRow>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                """
                SELECT
                    a.${DatabaseHelper.COL_ANAK_ID},
                    o.${DatabaseHelper.COL_ORTU_NAMA},
                    o.${DatabaseHelper.COL_ORTU_USERNAME},
                    o.${DatabaseHelper.COL_ORTU_PASSWORD},
                    a.${DatabaseHelper.COL_ANAK_NAMA},
                    a.${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                    a.${DatabaseHelper.COL_ANAK_JENIS_KELAMIN}
                FROM ${DatabaseHelper.TABLE_ANAK} a
                JOIN ${DatabaseHelper.TABLE_ORTU} o
                  ON a.${DatabaseHelper.COL_ANAK_ORTU_ID} = o.${DatabaseHelper.COL_ORTU_ID}
                ORDER BY a.${DatabaseHelper.COL_ANAK_ID} DESC
                """.trimIndent(),
                null
            )
            while (cursor.moveToNext()) result.add(cursor.toRowFromJoin())
        } finally {
            cursor?.close()
            db.close()
        }
        return result
    }

    fun isUsernameExists(usernameOrtu: String): Boolean {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                DatabaseHelper.TABLE_ORTU,
                arrayOf(DatabaseHelper.COL_ORTU_ID),
                "${DatabaseHelper.COL_ORTU_USERNAME} = ?",
                arrayOf(usernameOrtu),
                null, null, null, "1"
            )
            cursor.count > 0
        } finally {
            cursor?.close()
            db.close()
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────

    fun loginOrangTua(username: String, password: String): OrtuSummary? {
        val db     = dbHelper.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery(
                """
                SELECT
                    o.${DatabaseHelper.COL_ORTU_NAMA},
                    o.${DatabaseHelper.COL_ORTU_USERNAME},
                    o.${DatabaseHelper.COL_ORTU_PASSWORD},
                    COUNT(a.${DatabaseHelper.COL_ANAK_ID}) AS jumlah_anak
                FROM ${DatabaseHelper.TABLE_ORTU} o
                LEFT JOIN ${DatabaseHelper.TABLE_ANAK} a
                       ON a.${DatabaseHelper.COL_ANAK_ORTU_ID} = o.${DatabaseHelper.COL_ORTU_ID}
                WHERE o.${DatabaseHelper.COL_ORTU_USERNAME} = ?
                  AND o.${DatabaseHelper.COL_ORTU_PASSWORD} = ?
                GROUP BY o.${DatabaseHelper.COL_ORTU_ID}
                """.trimIndent(),
                arrayOf(username, password)
            )
            if (cursor.moveToFirst()) {
                OrtuSummary(
                    namaOrtu     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_NAMA)),
                    usernameOrtu = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_USERNAME)),
                    noHpOrtu     = "",
                    passOrtu     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_PASSWORD)) ?: "",
                    jumlahAnak   = cursor.getInt(cursor.getColumnIndexOrThrow("jumlah_anak"))
                )
            } else null
        } finally {
            cursor?.close()
            db.close()
        }
    }

    // ── PRIVATE HELPER ────────────────────────────────────────────

    private fun Cursor.toRowFromJoin() = OrangTuaAnakRow(
        id           = getLong  (getColumnIndexOrThrow(DatabaseHelper.COL_ANAK_ID)).let {
            // anak.id adalah String di skema baru, fallback ke rowid
            try { it } catch (e: Exception) { 0L }
        },
        namaOrtu     = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_NAMA))         ?: "",
        usernameOrtu = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_USERNAME))      ?: "",
        noHpOrtu     = "",
        passOrtu     = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_PASSWORD))      ?: "",
        namaAnak     = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ANAK_NAMA)),
        nikAnak      = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ANAK_ID)),           // NIK = anak.id
        tglLahirAnak = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ANAK_TGL_LAHIR)),
        genderAnak   = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ANAK_JENIS_KELAMIN)),
        alamatAnak   = null   // kolom alamat tidak ada di skema baru
    )
}