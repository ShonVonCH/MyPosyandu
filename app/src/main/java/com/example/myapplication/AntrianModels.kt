package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────
//  DATA CLASS: ANTRIAN (Header Antrian per Jadwal)
// ─────────────────────────────────────────────────────────────
data class AntrianApi(
    val id: String,
    val jadwalId: String,
    val nomorSaatIni: Int,
    val totalAntrian: Int,
    val status: String  // "aktif", "selesai", "batal"
)

// ─────────────────────────────────────────────────────────────
//  DATA CLASS: ANTRIAN ITEM (Detail per Anak)
// ─────────────────────────────────────────────────────────────
data class AntrianItemApi(
    val id: String,
    val antrianId: String,
    val anakId: String,
    val ortuId: String,
    val nomor: Int,
    val waktuAmbil: String,      // yyyy-MM-dd HH:mm:ss
    val waktuDipanggil: String?, // yyyy-MM-dd HH:mm:ss (null jika belum)
    val status: Int              // 1 = menunggu, 0 = dipanggil/tidak hadir
)

// ─────────────────────────────────────────────────────────────
//  DATA CLASS: RESPONSE DARI API
// ─────────────────────────────────────────────────────────────
data class AntrianResponse(
    val success: Boolean,
    val message: String,
    val antrianId: String? = null,
    val nomorAntrian: Int? = null,
    val data: List<AntrianItemApi>? = null
)

// ─────────────────────────────────────────────────────────────
//  HELPER: Format timestamp
// ─────────────────────────────────────────────────────────────
fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}