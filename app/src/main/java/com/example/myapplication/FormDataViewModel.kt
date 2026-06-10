package com.example.myapplication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class FormAnakData(
    val namaLengkap  : String = "",
    val nik          : String = "",
    val tanggalLahir : String = "",
    val jenisKelamin : String = "",
    val alamat       : String = ""
)

data class FormOrangTuaData(
    val nama    : String = "",
    val username: String = "",
    val noHp    : String = "",
    val password: String = ""
)

data class OrangTuaAccount(
    val nama      : String,
    val username  : String,
    val jumlahAnak: Int
)

// Entry lengkap: data anak + nama orang tua yang terhubung
data class RegisteredAnakEntry(
    val formAnak    : FormAnakData,
    val namaOrangTua: String
)

class FormDataViewModel : ViewModel() {
    var formAnak          by mutableStateOf(FormAnakData())
    var formOrangTua      by mutableStateOf(FormOrangTuaData())
    var dariKonfirmasi    by mutableStateOf(false)

    // Snapshot data terakhir yang disimpan — dipakai di SuksesDaftarScreen
    var lastSavedAnak     by mutableStateOf(FormAnakData())
    var lastSavedOrangTua by mutableStateOf(FormOrangTuaData())

    val akunOrangTuaList = mutableStateListOf(
        OrangTuaAccount("Rina Susanti", "@ortu_rina", 3),
        OrangTuaAccount("Sri Wahyuni",  "@ortu_sri",  3),
        OrangTuaAccount("Dewi Lestari", "@ortu_dewi", 3)
    )

    // Simpan entry lengkap (anak + nama ortu) agar bisa ditampilkan di DataAnakScreen
    val registeredAnakList = mutableStateListOf<RegisteredAnakEntry>()

    fun simpanAnak() {
        // Simpan snapshot SEBELUM reset
        lastSavedAnak     = formAnak
        lastSavedOrangTua = formOrangTua

        // Tambahkan entry lengkap ke daftar
        registeredAnakList.add(
            RegisteredAnakEntry(
                formAnak     = formAnak,
                namaOrangTua = formOrangTua.nama
            )
        )

        // Increment jumlahAnak pada akun orang tua yang dipilih
        val idx = akunOrangTuaList.indexOfFirst { it.username == formOrangTua.username }
        if (idx != -1) {
            val akun = akunOrangTuaList[idx]
            akunOrangTuaList[idx] = akun.copy(jumlahAnak = akun.jumlahAnak + 1)
        }

        // Reset form
        formAnak     = FormAnakData()
        formOrangTua = FormOrangTuaData()
    }

    fun tambahAkunOrangTua(nama: String, username: String, noHp: String, password: String) {
        akunOrangTuaList.add(OrangTuaAccount(nama, username, 0))
        formOrangTua = FormOrangTuaData(
            nama     = nama,
            username = username,
            noHp     = noHp,
            password = password
        )
    }
}