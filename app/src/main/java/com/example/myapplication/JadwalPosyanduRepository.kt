package com.example.myapplication.repository

import android.content.ContentValues
import android.content.Context
import com.example.myapplication.DatabaseHelper

class JadwalPosyanduRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insertOrUpdate(
        id: String,
        posyanduId: String,
        tanggal: String,
        jamMulai: String,
        jamSelesai: String,
        lokasi: String,
        status: String
    ) {

        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("id", id)
            put("posyandu_id", posyanduId)
            put("tanggal", tanggal)
            put("jam_mulai", jamMulai)
            put("jam_selesai", jamSelesai)
            put("lokasi", lokasi)
            put("status", status)
        }

        db.insertWithOnConflict(
            "jadwal_posyandu",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}