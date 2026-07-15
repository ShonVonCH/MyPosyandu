package com.example.myapplication

// ═════════════════════════════════════════════════════════════════════
//  TAMBAHAN BARU — Helper untuk sistem antrian gaya rumah sakit
//  Taruh file ini di package yang sama dengan AntrianApiService.kt
//  (tidak perlu ubah AntrianApiService.kt yang lama sama sekali)
// ═════════════════════════════════════════════════════════════════════

/**
 * Hitung berapa orang yang MASIH menunggu (status == 1) dan nomornya
 * lebih kecil dari nomor kita. Ini yang bikin muncul "masih ada 4 orang
 * di depan kamu" — persis kayak layar antrian di rumah sakit.
 */
fun hitungOrangDiDepan(semuaItem: List<AntrianItemApi>, nomorSaya: Int): Int =
    semuaItem.count { it.status == 1 && it.nomor < nomorSaya }

/**
 * Daftar antrian yang masih menunggu, diurutkan dari nomor terkecil.
 * Dipakai di layar kader (buat tau siapa berikutnya) dan layar display.
 */
fun getAntrianMenungguUrut(semuaItem: List<AntrianItemApi>): List<AntrianItemApi> =
    semuaItem.filter { it.status == 1 }.sortedBy { it.nomor }

/**
 * Nomor berikutnya yang harus dipanggil kader (nomor terkecil yang masih
 * menunggu). Null kalau antrian kosong.
 */
fun getNomorBerikutnya(semuaItem: List<AntrianItemApi>): AntrianItemApi? =
    getAntrianMenungguUrut(semuaItem).firstOrNull()

/**
 * Format nomor antrian jadi 3 digit, mis. 7 -> "007", sama seperti yang
 * sudah dipakai di layar ortu supaya konsisten di semua layar.
 */
fun formatNomorAntrian(nomor: Int): String = nomor.toString().padStart(3, '0')