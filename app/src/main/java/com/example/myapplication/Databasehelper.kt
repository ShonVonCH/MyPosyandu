package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME    = "posyandu.db"
        const val DATABASE_VERSION = 17

        const val TABLE_MENU_KATEGORI    = "menu_kategori"
        const val COL_MK_ID              = "id"
        const val COL_MK_NAMA            = "nama"
        const val COL_MK_RANGE_USIA      = "range_usia"

        const val TABLE_MENU_SEHAT       = "menu_sehat"
        const val COL_MS_ID              = "id"
        const val COL_MS_KATEGORI_ID     = "kategori_id"
        const val COL_MS_JUDUL           = "judul"
        const val COL_MS_RANGE_USIA      = "range_usia"
        const val COL_MS_DURASI          = "durasi_menit"
        const val COL_MS_BAHAN           = "bahan"
        const val COL_MS_CARA_MEMBUAT    = "cara_membuat"
        const val COL_MS_GIZI            = "kandungan_gizi"

        private const val SQL_CREATE_MENU_KATEGORI = """
            CREATE TABLE IF NOT EXISTS $TABLE_MENU_KATEGORI (
                $COL_MK_ID         TEXT PRIMARY KEY,
                $COL_MK_NAMA       TEXT NOT NULL,
                $COL_MK_RANGE_USIA TEXT
            )
        """

        private const val SQL_CREATE_MENU_SEHAT = """
            CREATE TABLE IF NOT EXISTS $TABLE_MENU_SEHAT (
                $COL_MS_ID           TEXT PRIMARY KEY,
                $COL_MS_KATEGORI_ID  TEXT,
                $COL_MS_JUDUL        TEXT NOT NULL,
                $COL_MS_RANGE_USIA   TEXT,
                $COL_MS_DURASI       INTEGER,
                $COL_MS_BAHAN        TEXT,
                $COL_MS_CARA_MEMBUAT TEXT,
                $COL_MS_GIZI         TEXT,
                FOREIGN KEY($COL_MS_KATEGORI_ID) REFERENCES $TABLE_MENU_KATEGORI($COL_MK_ID)
            )
        """
        const val TABLE_PEMERIKSAAN      = "pemeriksaan"
        const val COL_PMRK_ID            = "id"
        const val COL_PMRK_ANAK_ID       = "anak_id"
        const val COL_PMRK_KADER_ID      = "kader_id"
        const val COL_PMRK_TGL           = "tanggal"
        const val COL_PMRK_BB            = "berat_badan"
        const val COL_PMRK_TB            = "tinggi_badan"
        const val COL_PMRK_LK            = "lingkar_kepala"
        const val COL_PMRK_LL            = "lingkar_lengan"
        const val COL_PMRK_Z_SCORE_TBU   = "z_score_tbu"
        const val COL_PMRK_Z_SCORE_BBU   = "z_score_bbu"
        const val COL_PMRK_STATUS_GIZI   = "status_gizi"
        const val COL_PMRK_CATATAN       = "catatan"

        const val TABLE_IMUNISASI        = "imunisasi"
        const val COL_IMN_ID             = "id"
        const val COL_IMN_NAMA_ANAK      = "nama_anak"
        const val COL_IMN_NIK_ANAK       = "nik_anak"
        const val COL_IMN_NAMA_ORTU      = "nama_ortu"
        const val COL_IMN_NAMA_VAKSIN    = "nama_vaksin"
        const val COL_IMN_TGL_VAKSIN     = "tgl_vaksin"
        const val COL_IMN_STATUS         = "status"

        const val TABLE_POSYANDU         = "posyandu"
        const val COL_POSYANDU_ID        = "id"
        const val COL_POSYANDU_NAMA      = "nama"
        const val COL_POSYANDU_KELURAHAN = "kelurahan"
        const val COL_POSYANDU_RW        = "rw"
        const val COL_POSYANDU_ALAMAT    = "alamat"

        const val TABLE_JADWAL_POSYANDU    = "jadwal_posyandu"
        const val COL_JADWAL_ID            = "id"
        const val COL_JADWAL_POSYANDU_ID   = "posyandu_id"
        const val COL_JADWAL_TANGGAL       = "tanggal"
        const val COL_JADWAL_JAM_MULAI     = "jam_mulai"
        const val COL_JADWAL_JAM_SELESAI   = "jam_selesai"
        const val COL_JADWAL_LOKASI        = "lokasi"
        const val COL_JADWAL_STATUS        = "status"

        const val TABLE_VAKSIN_REF            = "vaksin_referensi"
        const val COL_VAKSIN_REF_ID           = "id"
        const val COL_VAKSIN_REF_KODE         = "kode"
        const val COL_VAKSIN_REF_NAMA         = "nama"
        const val COL_VAKSIN_REF_USIA_BULAN   = "usia_bulan"
        const val COL_VAKSIN_REF_BATAS_BULAN  = "batas_bulan"
        const val COL_VAKSIN_REF_KELOMPOK     = "kelompok"

        const val TABLE_VAKSIN_RIWAYAT         = "vaksin_riwayat"
        const val COL_VR_ID                    = "id"
        const val COL_VR_ANAK_ID               = "anak_id"
        const val COL_VR_VAKSIN_REF_ID         = "vaksin_ref_id"
        const val COL_VR_KADER_ID              = "kader_id"
        const val COL_VR_TANGGAL_PEMBERIAN     = "tanggal_pemberian"
        const val COL_VR_LOKASI                = "lokasi"

        const val TABLE_USERS          = "users"
        const val COL_USERS_ID         = "id"
        const val COL_USERS_NAMA       = "nama"
        const val COL_USERS_USERNAME   = "username"
        const val COL_USERS_PASSWORD   = "password"
        const val COL_USERS_ROLE       = "role"
        const val COL_USERS_POSYANDU_ID= "posyandu_id"
        const val COL_USERS_CREATED_AT = "created_at"

        const val TABLE_ANAK              = "anak"
        const val COL_ANAK_ID             = "id"
        const val COL_ANAK_NAMA           = "nama"
        const val COL_ANAK_TGL_LAHIR      = "tanggal_lahir"
        const val COL_ANAK_JENIS_KELAMIN  = "jenis_kelamin"
        const val COL_ANAK_ORTU_ID        = "ortu_id"
        const val COL_ANAK_POSYANDU_ID    = "posyandu_id"
        const val COL_ANAK_CREATED_AT     = "created_at"

        const val TABLE_ORTU              = "ortu"
        const val COL_ORTU_ID             = "id"
        const val COL_ORTU_NAMA           = "nama"
        const val COL_ORTU_USERNAME       = "username"
        const val COL_ORTU_PASSWORD       = "password"
        const val COL_ORTU_ROLE           = "role"
        const val COL_ORTU_POSYANDU_ID    = "posyandu_id"
        const val COL_ORTU_CREATED_AT     = "created_at"

        const val TABLE_ANTRIAN              = "antrian"
        const val COL_ANT_ID                 = "id"
        const val COL_ANT_JADWAL_ID          = "jadwal_id"
        const val COL_ANT_TANGGAL            = "tanggal"
        const val COL_ANT_NOMOR_SAAT_INI     = "nomor_saat_ini"
        const val COL_ANT_TOTAL_ANTRIAN      = "total_antrian"
        const val COL_ANT_STATUS             = "status"

        const val TABLE_ANTRIAN_ITEM         = "antrian_item"
        const val COL_ANTITEM_ID             = "id"
        const val COL_ANTITEM_ANTRIAN_ID     = "antrian_id"
        const val COL_ANTITEM_ANAK_ID        = "anak_id"
        const val COL_ANTITEM_ORTU_ID        = "ortu_id"
        const val COL_ANTITEM_NOMOR          = "nomor"
        const val COL_ANTITEM_WAKTU_AMBIL    = "waktu_ambil"
        const val COL_ANTITEM_WAKTU_DIPANGGIL= "waktu_dipanggil"
        const val COL_ANTITEM_STATUS         = "status"

        const val TABLE_LAPORAN                   = "laporan"
        const val COL_LAP_ID                      = "id"
        const val COL_LAP_JADWAL_ID               = "jadwal_id"
        const val COL_LAP_KADER_ID                = "kader_id"
        const val COL_LAP_TOTAL_HADIR             = "total_hadir"
        const val COL_LAP_TOTAL_STUNTING          = "total_stunting"
        const val COL_LAP_TOTAL_VAKSIN_TERLAMBAT  = "total_vaksin_terlambat"
        const val COL_LAP_RINGKASAN               = "ringkasan"
        const val COL_LAP_GENERATED_AT            = "generated_at"

        private const val SQL_CREATE_PEMERIKSAAN = """
            CREATE TABLE IF NOT EXISTS $TABLE_PEMERIKSAAN (
                $COL_PMRK_ID          TEXT PRIMARY KEY,
                $COL_PMRK_ANAK_ID     TEXT NOT NULL,
                $COL_PMRK_KADER_ID    TEXT NOT NULL,
                $COL_PMRK_TGL         TEXT,
                $COL_PMRK_BB          REAL,
                $COL_PMRK_TB          REAL,
                $COL_PMRK_LK          REAL,
                $COL_PMRK_LL          REAL,
                $COL_PMRK_Z_SCORE_TBU REAL,
                $COL_PMRK_Z_SCORE_BBU REAL,
                $COL_PMRK_STATUS_GIZI TEXT CHECK($COL_PMRK_STATUS_GIZI IN (
                    'normal','gizi_kurang','gizi_buruk','gizi_lebih','obesitas'
                )),
                $COL_PMRK_CATATAN     TEXT,
                FOREIGN KEY($COL_PMRK_ANAK_ID)  REFERENCES $TABLE_ANAK($COL_ANAK_ID),
                FOREIGN KEY($COL_PMRK_KADER_ID) REFERENCES $TABLE_USERS($COL_USERS_ID)
            )
        """

        private const val SQL_CREATE_POSYANDU = """
            CREATE TABLE IF NOT EXISTS $TABLE_POSYANDU (
                $COL_POSYANDU_ID        TEXT PRIMARY KEY,
                $COL_POSYANDU_NAMA      TEXT NOT NULL,
                $COL_POSYANDU_KELURAHAN TEXT,
                $COL_POSYANDU_RW        TEXT,
                $COL_POSYANDU_ALAMAT    TEXT
            )
        """

        private const val SQL_CREATE_JADWAL_POSYANDU = """
            CREATE TABLE IF NOT EXISTS $TABLE_JADWAL_POSYANDU (
                $COL_JADWAL_ID           TEXT PRIMARY KEY,
                $COL_JADWAL_POSYANDU_ID  TEXT NOT NULL,
                $COL_JADWAL_TANGGAL      TEXT,
                $COL_JADWAL_JAM_MULAI    TEXT,
                $COL_JADWAL_JAM_SELESAI  TEXT,
                $COL_JADWAL_LOKASI       TEXT,
                $COL_JADWAL_STATUS       TEXT,
                FOREIGN KEY($COL_JADWAL_POSYANDU_ID)
                    REFERENCES $TABLE_POSYANDU($COL_POSYANDU_ID)
            )
        """

        private const val SQL_CREATE_VAKSIN_REF = """
            CREATE TABLE IF NOT EXISTS $TABLE_VAKSIN_REF (
                $COL_VAKSIN_REF_ID          TEXT PRIMARY KEY,
                $COL_VAKSIN_REF_KODE        TEXT,
                $COL_VAKSIN_REF_NAMA        TEXT NOT NULL,
                $COL_VAKSIN_REF_USIA_BULAN  INTEGER,
                $COL_VAKSIN_REF_BATAS_BULAN INTEGER,
                $COL_VAKSIN_REF_KELOMPOK    TEXT
            )
        """

        private const val SQL_CREATE_VAKSIN_RIWAYAT = """
            CREATE TABLE IF NOT EXISTS $TABLE_VAKSIN_RIWAYAT (
                $COL_VR_ID               TEXT PRIMARY KEY,
                $COL_VR_ANAK_ID          TEXT NOT NULL,
                $COL_VR_VAKSIN_REF_ID    TEXT NOT NULL,
                $COL_VR_KADER_ID         TEXT,
                $COL_VR_TANGGAL_PEMBERIAN TEXT,
                $COL_VR_LOKASI           TEXT,
                FOREIGN KEY($COL_VR_ANAK_ID)
                    REFERENCES $TABLE_ANAK($COL_ANAK_ID),
                FOREIGN KEY($COL_VR_VAKSIN_REF_ID)
                    REFERENCES $TABLE_VAKSIN_REF($COL_VAKSIN_REF_ID),
                UNIQUE($COL_VR_ANAK_ID, $COL_VR_VAKSIN_REF_ID)
            )
        """

        private const val SQL_CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COL_USERS_ID          TEXT PRIMARY KEY,
                $COL_USERS_NAMA        TEXT NOT NULL,
                $COL_USERS_USERNAME    TEXT NOT NULL,
                $COL_USERS_PASSWORD    TEXT NOT NULL,
                $COL_USERS_ROLE        TEXT NOT NULL,
                $COL_USERS_POSYANDU_ID TEXT,
                $COL_USERS_CREATED_AT  TEXT,
                FOREIGN KEY($COL_USERS_POSYANDU_ID)
                    REFERENCES $TABLE_POSYANDU($COL_POSYANDU_ID)
            )
        """

        private const val SQL_CREATE_ANAK = """
            CREATE TABLE IF NOT EXISTS $TABLE_ANAK (
                $COL_ANAK_ID            TEXT PRIMARY KEY,
                $COL_ANAK_NAMA          TEXT NOT NULL,
                $COL_ANAK_TGL_LAHIR     TEXT,
                $COL_ANAK_JENIS_KELAMIN TEXT,
                $COL_ANAK_ORTU_ID       TEXT,
                $COL_ANAK_POSYANDU_ID   TEXT,
                $COL_ANAK_CREATED_AT    TEXT,
                FOREIGN KEY($COL_ANAK_ORTU_ID)
                    REFERENCES $TABLE_ORTU($COL_ORTU_ID),
                FOREIGN KEY($COL_ANAK_POSYANDU_ID)
                    REFERENCES $TABLE_POSYANDU($COL_POSYANDU_ID)
            )
        """

        private const val SQL_CREATE_ORTU = """
            CREATE TABLE IF NOT EXISTS $TABLE_ORTU (
                $COL_ORTU_ID           TEXT PRIMARY KEY,
                $COL_ORTU_NAMA         TEXT NOT NULL,
                $COL_ORTU_USERNAME     TEXT NOT NULL,
                $COL_ORTU_PASSWORD     TEXT NOT NULL,
                $COL_ORTU_ROLE         TEXT NOT NULL DEFAULT 'ortu',
                $COL_ORTU_POSYANDU_ID  TEXT,
                $COL_ORTU_CREATED_AT   TEXT,
                FOREIGN KEY($COL_ORTU_POSYANDU_ID)
                    REFERENCES $TABLE_POSYANDU($COL_POSYANDU_ID)
            )
        """

        private const val SQL_CREATE_ANTRIAN = """
            CREATE TABLE IF NOT EXISTS $TABLE_ANTRIAN (
                $COL_ANT_ID             TEXT PRIMARY KEY,
                $COL_ANT_JADWAL_ID      TEXT,
                $COL_ANT_TANGGAL        TEXT,
                $COL_ANT_NOMOR_SAAT_INI INTEGER NOT NULL DEFAULT 0,
                $COL_ANT_TOTAL_ANTRIAN  INTEGER NOT NULL DEFAULT 0,
                $COL_ANT_STATUS         TEXT NOT NULL DEFAULT 'aktif'
            )
        """

        private const val SQL_CREATE_ANTRIAN_ITEM = """
            CREATE TABLE IF NOT EXISTS $TABLE_ANTRIAN_ITEM (
                $COL_ANTITEM_ID              TEXT PRIMARY KEY,
                $COL_ANTITEM_ANTRIAN_ID      TEXT NOT NULL,
                $COL_ANTITEM_ANAK_ID         TEXT NOT NULL,
                $COL_ANTITEM_ORTU_ID         TEXT NOT NULL,
                $COL_ANTITEM_NOMOR           INTEGER NOT NULL,
                $COL_ANTITEM_WAKTU_AMBIL     TEXT,
                $COL_ANTITEM_WAKTU_DIPANGGIL TEXT,
                $COL_ANTITEM_STATUS          INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY($COL_ANTITEM_ANTRIAN_ID)
                    REFERENCES $TABLE_ANTRIAN($COL_ANT_ID),
                FOREIGN KEY($COL_ANTITEM_ANAK_ID)
                    REFERENCES $TABLE_ANAK($COL_ANAK_ID),
                FOREIGN KEY($COL_ANTITEM_ORTU_ID)
                    REFERENCES $TABLE_ORTU($COL_ORTU_ID)
            )
        """

        private const val SQL_CREATE_LAPORAN = """
            CREATE TABLE IF NOT EXISTS $TABLE_LAPORAN (
                $COL_LAP_ID                     TEXT PRIMARY KEY,
                $COL_LAP_JADWAL_ID              TEXT,
                $COL_LAP_KADER_ID               TEXT NOT NULL,
                $COL_LAP_TOTAL_HADIR            INTEGER NOT NULL DEFAULT 0,
                $COL_LAP_TOTAL_STUNTING         INTEGER NOT NULL DEFAULT 0,
                $COL_LAP_TOTAL_VAKSIN_TERLAMBAT INTEGER NOT NULL DEFAULT 0,
                $COL_LAP_RINGKASAN              TEXT,
                $COL_LAP_GENERATED_AT           TEXT NOT NULL,
                FOREIGN KEY($COL_LAP_JADWAL_ID)
                    REFERENCES $TABLE_JADWAL_POSYANDU($COL_JADWAL_ID),
                FOREIGN KEY($COL_LAP_KADER_ID)
                    REFERENCES $TABLE_USERS($COL_USERS_ID)
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_POSYANDU)
        db.execSQL(SQL_CREATE_JADWAL_POSYANDU)
        db.execSQL(SQL_CREATE_VAKSIN_REF)
        db.execSQL(SQL_CREATE_USERS)
        db.execSQL(SQL_CREATE_ORTU)
        db.execSQL(SQL_CREATE_ANAK)
        db.execSQL(SQL_CREATE_PEMERIKSAAN)
        db.execSQL(SQL_CREATE_VAKSIN_RIWAYAT)
        db.execSQL(SQL_CREATE_LAPORAN)
        db.execSQL(SQL_CREATE_ANTRIAN)
        db.execSQL(SQL_CREATE_ANTRIAN_ITEM)
        db.execSQL(SQL_CREATE_MENU_KATEGORI)
        db.execSQL(SQL_CREATE_MENU_SEHAT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 6) {
            db.execSQL(SQL_CREATE_POSYANDU)
            db.execSQL(SQL_CREATE_JADWAL_POSYANDU)
        }
        if (oldVersion < 7) {
            db.execSQL(SQL_CREATE_VAKSIN_REF)
        }
        if (oldVersion < 8) {
            db.execSQL(SQL_CREATE_USERS)
        }
        if (oldVersion < 9) {
            db.execSQL("DROP TABLE IF EXISTS orangtua")
            db.execSQL(SQL_CREATE_ANAK)
        }
        if (oldVersion < 10) {
            db.execSQL(SQL_CREATE_ORTU)
        }
        if (oldVersion < 11) {
            db.execSQL("DROP TABLE IF EXISTS anak_pemeriksaan")
            db.execSQL(SQL_CREATE_PEMERIKSAAN)
        }
        if (oldVersion < 12) {
            db.execSQL("DROP TABLE IF EXISTS imunisasi")
            db.execSQL(SQL_CREATE_VAKSIN_RIWAYAT)
        }
        if (oldVersion < 13) {
            db.execSQL(SQL_CREATE_LAPORAN)
        }
        if (oldVersion < 15) {
            db.execSQL(SQL_CREATE_MENU_KATEGORI)
            db.execSQL(SQL_CREATE_MENU_SEHAT)
        }
        if (oldVersion < 16) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MENU_SEHAT")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MENU_KATEGORI")
            db.execSQL(SQL_CREATE_MENU_KATEGORI)
            db.execSQL(SQL_CREATE_MENU_SEHAT)
        }
        if (oldVersion < 17) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MENU_SEHAT")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MENU_KATEGORI")
            db.execSQL(SQL_CREATE_MENU_KATEGORI)
            db.execSQL(SQL_CREATE_MENU_SEHAT)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}