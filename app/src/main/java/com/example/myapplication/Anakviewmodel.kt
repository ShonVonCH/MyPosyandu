package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Menyimpan hasil pemeriksaan (BB & TB) per nama anak.
 * Key = namaAnak, Value = Pair(beratBadan, tinggiBadan) sebagai String.
 */
class AnakViewModel : ViewModel() {

    // Map nama anak → Pair(bb, tb)
    private val _hasilPemeriksaan = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val hasilPemeriksaan: StateFlow<Map<String, Pair<String, String>>> = _hasilPemeriksaan.asStateFlow()

    // ── Legacy single-value (masih dipakai PemeriksaanScreen lama) ──
    private val _beratBadanTerakhir  = MutableStateFlow("")
    val beratBadanTerakhir: StateFlow<String> = _beratBadanTerakhir.asStateFlow()

    private val _tinggiBadanTerakhir = MutableStateFlow("")
    val tinggiBadanTerakhir: StateFlow<String> = _tinggiBadanTerakhir.asStateFlow()

    // Nama anak yang sedang dibuka di RiwayatScreen — diset dari MainActivity
    private val _namaAnakAktif = MutableStateFlow("")
    val namaAnakAktif: StateFlow<String> = _namaAnakAktif.asStateFlow()

    fun setAnakAktif(nama: String) {
        _namaAnakAktif.value = nama
        // Restore nilai terakhir supaya PemeriksaanScreen bisa pra-isi
        val hasil = _hasilPemeriksaan.value[nama]
        _beratBadanTerakhir.value  = hasil?.first  ?: ""
        _tinggiBadanTerakhir.value = hasil?.second ?: ""
    }

    /** Dipanggil setiap kali user menekan "Analisis Dan Simpan" di PemeriksaanScreen */
    fun simpanHasilPemeriksaan(beratBadan: String, tinggiBadan: String) {
        val nama = _namaAnakAktif.value
        if (beratBadan.isNotBlank())  _beratBadanTerakhir.value  = beratBadan
        if (tinggiBadan.isNotBlank()) _tinggiBadanTerakhir.value = tinggiBadan

        if (nama.isNotBlank()) {
            val current = _hasilPemeriksaan.value.toMutableMap()
            current[nama] = Pair(
                if (beratBadan.isNotBlank())  beratBadan  else current[nama]?.first  ?: "",
                if (tinggiBadan.isNotBlank()) tinggiBadan else current[nama]?.second ?: ""
            )
            _hasilPemeriksaan.value = current
        }
    }

    /** Ambil BB untuk satu anak (untuk ditampilkan di DataAnakScreen list) */
    fun getBeratBadan(namaAnak: String): String = _hasilPemeriksaan.value[namaAnak]?.first ?: ""

    /** Ambil TB untuk satu anak */
    fun getTinggiBadan(namaAnak: String): String = _hasilPemeriksaan.value[namaAnak]?.second ?: ""
}