package com.example.myapplication

fun hitungOrangDiDepan(semuaItem: List<AntrianItemApi>, nomorSaya: Int): Int =
    semuaItem.count { it.status == 1 && it.nomor < nomorSaya }

fun getAntrianMenungguUrut(semuaItem: List<AntrianItemApi>): List<AntrianItemApi> =
    semuaItem.filter { it.status == 1 }.sortedBy { it.nomor }

fun getNomorBerikutnya(semuaItem: List<AntrianItemApi>): AntrianItemApi? =
    getAntrianMenungguUrut(semuaItem).firstOrNull()

fun formatNomorAntrian(nomor: Int): String = nomor.toString().padStart(3, '0')