package com.example.myapplication.repository

import android.content.ContentValues
import android.content.Context
import com.example.myapplication.DatabaseHelper

class PosyanduRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insertOrUpdate(
        id: String,
        nama: String,
        kelurahan: String,
        rw: String,
        alamat: String
    ) {

        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("id", id)
            put("nama", nama)
            put("kelurahan", kelurahan)
            put("rw", rw)
            put("alamat", alamat)
        }

        db.insertWithOnConflict(
            "posyandu",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}