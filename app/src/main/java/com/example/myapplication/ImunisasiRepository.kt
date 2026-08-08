package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class ImunisasiRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val vaksinRepo = VaksinRiwayatRepository(context)

    data class ImunisasiRow(
        val id         : Long,
        val namaAnak   : String,
        val nikAnak    : String,
        val namaOrtu   : String,
        val namaVaksin : String,
        val tglVaksin  : String,
        val status     : Int    // 0 = belum, 1 = sudah
    )

    /**
     * DEPRECATED: Gunakan VaksinRiwayatRepository.insertRiwayat()
     * Insert atau update vaksin — sekarang diarahkan ke vaksin_riwayat
     */
    fun upsertImunisasi(
        namaAnak  : String,
        nikAnak   : String,
        namaOrtu  : String,
        namaVaksin: String,
        tglVaksin : String,
        status    : Int = 1
    ) {
        android.util.Log.w("ImunisasiRepository", "upsertImunisasi deprecated, use VaksinRiwayatRepository instead")
    }

    /**
     * DEPRECATED: Gunakan VaksinRiwayatRepository.getRiwayatByAnak()
     * Returns empty list since imunisasi table no longer exists
     */
    fun getImunisasiByAnak(namaAnak: String): List<ImunisasiRow> {
        android.util.Log.w("ImunisasiRepository", "getImunisasiByAnak deprecated, returning empty list")
        return emptyList()
    }

    private fun Cursor.toRow() = ImunisasiRow(
        id         = 0,
        namaAnak   = "",
        nikAnak    = "",
        namaOrtu   = "",
        namaVaksin = "",
        tglVaksin  = "",
        status     = 0
    )
}