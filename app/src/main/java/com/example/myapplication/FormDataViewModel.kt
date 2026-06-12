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
    val noHp      : String = "",
    val password  : String = "",
    val jumlahAnak: Int    = 0
)

data class RegisteredAnakEntry(
    val formAnak    : FormAnakData,
    val namaOrangTua: String
)

class FormDataViewModel : ViewModel() {

    var formAnak       by mutableStateOf(FormAnakData())
    var formOrangTua   by mutableStateOf(FormOrangTuaData())
    var dariKonfirmasi by mutableStateOf(false)

    var lastSavedAnak     by mutableStateOf(FormAnakData())
    var lastSavedOrangTua by mutableStateOf(FormOrangTuaData())

    var loggedInOrangTuaUsername by mutableStateOf("")
    var loggedInKaderId          by mutableStateOf("")

    // ── In-memory list (tetap dipakai untuk UI realtime) ───────────
    val akunOrangTuaList   = mutableStateListOf<OrangTuaAccount>()
    val akunPasswordMap    = mutableMapOf<String, String>()
    val registeredAnakList = mutableStateListOf<RegisteredAnakEntry>()

    /**
     * Dipanggil dari KonfirmasiDataScreen setelah SQLite berhasil di-insert.
     * Update state UI in-memory agar Dashboard & DataAnak langsung reflect data baru.
     */
    fun simpanAnak() {
        lastSavedAnak     = formAnak
        lastSavedOrangTua = formOrangTua

        // Tambah ke list anak terdaftar
        registeredAnakList.add(
            RegisteredAnakEntry(
                formAnak     = formAnak,
                namaOrangTua = formOrangTua.nama
            )
        )

        // Update jumlahAnak di akunOrangTuaList
        val idx = akunOrangTuaList.indexOfFirst { it.username == formOrangTua.username }
        if (idx != -1) {
            val akun = akunOrangTuaList[idx]
            akunOrangTuaList[idx] = akun.copy(jumlahAnak = akun.jumlahAnak + 1)
        }

        // Reset form
        formAnak     = FormAnakData()
        formOrangTua = FormOrangTuaData()
    }

    /**
     * Tambah akun ortu baru ke in-memory list.
     * Dipanggil dari HubungOrangTuaScreen saat method = "new".
     */
    fun tambahAkunOrangTua(nama: String, username: String, noHp: String, password: String) {
        // Hindari duplikat
        if (akunOrangTuaList.none { it.username == username }) {
            akunOrangTuaList.add(OrangTuaAccount(nama, username, noHp, password, 0))
        }
        akunPasswordMap[username] = password
        formOrangTua = FormOrangTuaData(
            nama     = nama,
            username = username,
            noHp     = noHp,
            password = password
        )
    }

    /**
     * Sync akun ortu dari SQLite ke in-memory list.
     * Dipanggil setelah login agar CariAkunCard dan dashboard mencerminkan data DB.
     */
    fun syncAkunOrangTuaFromDb(list: List<OrangTuaRepository.OrtuSummary>) {
        akunOrangTuaList.clear()
        list.forEach { ortu ->
            akunOrangTuaList.add(
                OrangTuaAccount(
                    nama       = ortu.namaOrtu,
                    username   = ortu.usernameOrtu,
                    noHp       = ortu.noHpOrtu,
                    password   = ortu.passOrtu,
                    jumlahAnak = ortu.jumlahAnak
                )
            )
            akunPasswordMap[ortu.usernameOrtu] = ortu.passOrtu
        }
    }

    /**
     * Sync anak terdaftar dari SQLite ke in-memory list.
     * Dipanggil setelah login agar DataAnak & Dashboard tidak kosong.
     */
    fun syncAnakFromDb(list: List<OrangTuaRepository.OrangTuaAnakRow>) {
        registeredAnakList.clear()
        list.forEach { row ->
            if (!row.namaAnak.isNullOrBlank()) {
                registeredAnakList.add(
                    RegisteredAnakEntry(
                        formAnak = FormAnakData(
                            namaLengkap  = row.namaAnak     ?: "",
                            nik          = row.nikAnak      ?: "",
                            tanggalLahir = row.tglLahirAnak ?: "",
                            jenisKelamin = row.genderAnak   ?: "",
                            alamat       = row.alamatAnak   ?: ""
                        ),
                        namaOrangTua = row.namaOrtu
                    )
                )
            }
        }
    }

    fun loginOrangTua(username: String, password: String): OrangTuaAccount? {
        val akun = akunOrangTuaList.find { it.username == username } ?: return null
        return if (akunPasswordMap[username] == password) akun else null
    }
}