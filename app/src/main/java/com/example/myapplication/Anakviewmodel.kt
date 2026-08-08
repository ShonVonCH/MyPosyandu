package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnakViewModel : ViewModel() {

    // ── Hasil pemeriksaan BB & TB per anak ────────────────────────
    private val _hasilPemeriksaan = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val hasilPemeriksaan: StateFlow<Map<String, Pair<String, String>>> = _hasilPemeriksaan.asStateFlow()

    // ── Hasil analisis WHO per anak ───────────────────────────────
    private val _hasilAnalisis = MutableStateFlow<Map<String, HasilAnalisis>>(emptyMap())
    val hasilAnalisis: StateFlow<Map<String, HasilAnalisis>> = _hasilAnalisis.asStateFlow()

    // ── Vaksin per anak ───────────────────────────────────────────
    private val _vaksinDiberikan = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val vaksinDiberikan: StateFlow<Map<String, Map<String, String>>> = _vaksinDiberikan.asStateFlow()

    // ── Kehadiran: Set nama anak yang sudah hadir bulan ini ───────
    // Hadir = sudah pemeriksaan ATAU sudah imunisasi (dihitung 1x per anak)
    private val _anakHadir = MutableStateFlow<Set<String>>(emptySet())
    val anakHadir: StateFlow<Set<String>> = _anakHadir.asStateFlow()

    // ── Legacy single-value ───────────────────────────────────────
    private val _beratBadanTerakhir  = MutableStateFlow("")
    val beratBadanTerakhir: StateFlow<String> = _beratBadanTerakhir.asStateFlow()

    private val _tinggiBadanTerakhir = MutableStateFlow("")
    val tinggiBadanTerakhir: StateFlow<String> = _tinggiBadanTerakhir.asStateFlow()

    private val _namaAnakAktif = MutableStateFlow("")
    val namaAnakAktif: StateFlow<String> = _namaAnakAktif.asStateFlow()

    fun setAnakAktif(nama: String) {
        _namaAnakAktif.value = nama
        val hasil = _hasilPemeriksaan.value[nama]
        _beratBadanTerakhir.value  = hasil?.first  ?: ""
        _tinggiBadanTerakhir.value = hasil?.second ?: ""
    }

    /** Dipanggil saat "Analisis Dan Simpan" di PemeriksaanScreen */
    fun simpanHasilPemeriksaan(
        beratBadan : String,
        tinggiBadan: String,
        analisis   : HasilAnalisis? = null
    ) {
        val nama = _namaAnakAktif.value
        if (beratBadan.isNotBlank())  _beratBadanTerakhir.value  = beratBadan
        if (tinggiBadan.isNotBlank()) _tinggiBadanTerakhir.value = tinggiBadan

        if (nama.isNotBlank()) {
            // Simpan BB & TB
            val currentPemeriksaan = _hasilPemeriksaan.value.toMutableMap()
            currentPemeriksaan[nama] = Pair(
                if (beratBadan.isNotBlank())  beratBadan  else currentPemeriksaan[nama]?.first  ?: "",
                if (tinggiBadan.isNotBlank()) tinggiBadan else currentPemeriksaan[nama]?.second ?: ""
            )
            _hasilPemeriksaan.value = currentPemeriksaan

            // Simpan analisis WHO
            if (analisis != null) {
                val currentAnalisis = _hasilAnalisis.value.toMutableMap()
                currentAnalisis[nama] = analisis
                _hasilAnalisis.value = currentAnalisis
            }

            // Tandai hadir — pemeriksaan sudah cukup untuk dianggap hadir
            tandaiHadir(nama)
        }
    }

    /** Dipanggil saat kader mencatat vaksin di ImunisasiScreen */
    fun simpanVaksin(namaVaksin: String, tanggal: String) {
        val nama = _namaAnakAktif.value
        if (nama.isBlank()) return

        val current    = _vaksinDiberikan.value.toMutableMap()
        val vaksinAnak = (current[nama] ?: emptyMap()).toMutableMap()
        vaksinAnak[namaVaksin] = tanggal
        current[nama]          = vaksinAnak
        _vaksinDiberikan.value = current

        // Tandai hadir — imunisasi juga dianggap hadir
        tandaiHadir(nama)
    }

    private fun tandaiHadir(nama: String) {
        if (nama.isBlank()) return
        _anakHadir.value = _anakHadir.value + nama
    }

    // ── Helper getter ─────────────────────────────────────────────
    fun getVaksinAnak(namaAnak: String): Map<String, String> =
        _vaksinDiberikan.value[namaAnak] ?: emptyMap()

    fun getAnalisis(namaAnak: String): HasilAnalisis? =
        _hasilAnalisis.value[namaAnak]

    fun getBeratBadan(namaAnak: String): String  = _hasilPemeriksaan.value[namaAnak]?.first  ?: ""
    fun getTinggiBadan(namaAnak: String): String = _hasilPemeriksaan.value[namaAnak]?.second ?: ""
}