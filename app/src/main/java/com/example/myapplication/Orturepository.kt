package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OrtuRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // Ambil posyandu_id dari user yang sedang login
    private fun getPosyanduIdFromLoggedInUser(): String {
        val db     = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_USERS_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_USERS} LIMIT 1",
            null
        )
        val posyanduId = if (cursor.moveToFirst()) cursor.getString(0) ?: "" else ""
        cursor.close()
        return posyanduId
    }

    // Insert akun ortu baru ke tabel ortu
    fun insertOrtu(
        nama    : String,
        username: String,
        password: String
    ): Boolean {
        val db         = dbHelper.writableDatabase
        val posyanduId = getPosyanduIdFromLoggedInUser()

        // Format created_at: yyyy-MM-dd HH:mm:ss
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = sdf.format(Date())

        val id = UUID.randomUUID().toString()

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_ORTU_ID,          id)
            put(DatabaseHelper.COL_ORTU_NAMA,         nama)
            put(DatabaseHelper.COL_ORTU_USERNAME,     username)
            put(DatabaseHelper.COL_ORTU_PASSWORD,     password)
            put(DatabaseHelper.COL_ORTU_ROLE,         "ortu")
            put(DatabaseHelper.COL_ORTU_POSYANDU_ID,  posyanduId)
            put(DatabaseHelper.COL_ORTU_CREATED_AT,   now)
        }

        val result = db.insertWithOnConflict(
            DatabaseHelper.TABLE_ORTU,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE  // ignore jika username sudah ada
        )

        android.util.Log.d("OrtuRepository", "Insert ortu '$username': result=$result posyanduId=$posyanduId createdAt=$now")
        return result != -1L
    }

    // Cek apakah username ortu sudah ada
    fun isUsernameExists(username: String): Boolean {
        val db     = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_ORTU_ID} FROM ${DatabaseHelper.TABLE_ORTU} WHERE ${DatabaseHelper.COL_ORTU_USERNAME} = ? LIMIT 1",
            arrayOf(username)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // Ambil semua ortu (untuk keperluan list & sync ke API)
    fun getAllOrtu(): List<OrtuData> {
        val db     = dbHelper.readableDatabase
        val result = mutableListOf<OrtuData>()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_ORTU} ORDER BY ${DatabaseHelper.COL_ORTU_NAMA} ASC",
            null
        )
        while (cursor.moveToNext()) {
            result.add(
                OrtuData(
                    id         = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_ID)),
                    nama       = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_NAMA)),
                    username   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_USERNAME)),
                    password   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_PASSWORD)),
                    posyanduId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_POSYANDU_ID)) ?: "",
                    createdAt  = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORTU_CREATED_AT)) ?: ""
                )
            )
        }
        cursor.close()
        return result
    }
}

data class OrtuData(
    val id        : String,
    val nama      : String,
    val username  : String,
    val password  : String,
    val posyanduId: String,
    val createdAt : String
)