package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray

object SyncPosyandu {

    suspend fun syncAll(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                syncPosyandu(context)
                syncJadwal(context)
                syncVaksinRef(context)
            } catch (e: Exception) {
                Log.e("SyncPosyandu", "Sync gagal: ${e.message}", e)
            }
        }
    }

    private fun syncPosyandu(context: Context) {
        val json = fetchJson("https://myposyandu.gt.tc/api_posyandu/posyandu.php") ?: return
        val db = DatabaseHelper(context).writableDatabase

        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_POSYANDU_ID,        obj.getString("id"))
                put(DatabaseHelper.COL_POSYANDU_NAMA,      obj.getString("nama"))
                put(DatabaseHelper.COL_POSYANDU_KELURAHAN, obj.getString("kelurahan"))
                put(DatabaseHelper.COL_POSYANDU_RW,        obj.getString("rw"))
                put(DatabaseHelper.COL_POSYANDU_ALAMAT,    obj.getString("alamat"))
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_POSYANDU,
                null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
        Log.d("SyncPosyandu", "Posyandu selesai: ${array.length()} data")
    }

    private fun syncJadwal(context: Context) {
        val json = fetchJson("https://myposyandu.gt.tc/api_posyandu/jadwal_posyandu.php") ?: return
        val db = DatabaseHelper(context).writableDatabase

        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_JADWAL_ID,          obj.getString("id"))
                put(DatabaseHelper.COL_JADWAL_POSYANDU_ID, obj.getString("posyandu_id"))
                put(DatabaseHelper.COL_JADWAL_TANGGAL,     obj.getString("tanggal"))
                put(DatabaseHelper.COL_JADWAL_JAM_MULAI,   obj.getString("jam_mulai"))
                put(DatabaseHelper.COL_JADWAL_JAM_SELESAI, obj.getString("jam_selesai"))
                put(DatabaseHelper.COL_JADWAL_LOKASI,      obj.getString("lokasi"))
                put(DatabaseHelper.COL_JADWAL_STATUS,      obj.getString("status"))
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_JADWAL_POSYANDU,
                null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
        Log.d("SyncPosyandu", "Jadwal selesai: ${array.length()} data")
    }

    private fun syncVaksinRef(context: Context) {
        val json = fetchJson("https://myposyandu.gt.tc/api_posyandu/vaksin_referensi.php") ?: return
        val db = DatabaseHelper(context).writableDatabase

        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_VAKSIN_REF_ID,          obj.getString("id"))
                put(DatabaseHelper.COL_VAKSIN_REF_KODE,        obj.getString("kode"))
                put(DatabaseHelper.COL_VAKSIN_REF_NAMA,        obj.getString("nama"))
                put(DatabaseHelper.COL_VAKSIN_REF_USIA_BULAN,  obj.getInt("usia_bulan"))
                put(DatabaseHelper.COL_VAKSIN_REF_BATAS_BULAN, obj.getInt("batas_bulan"))
                put(DatabaseHelper.COL_VAKSIN_REF_KELOMPOK,    obj.getString("kelompok"))
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_VAKSIN_REF,
                null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
        Log.d("SyncPosyandu", "VaksinRef selesai: ${array.length()} data")
    }

    private fun fetchJson(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()
            // httpClient sudah public dari ApiService.kt
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            Log.d("SyncPosyandu", "Response [$url]: code=${response.code} body=${body?.take(100)}")
            if (response.isSuccessful && body?.trimStart()?.startsWith("[") == true) body else null
        } catch (e: Exception) {
            Log.e("SyncPosyandu", "Fetch gagal [$url]: ${e.message}")
            null
        }
    }
}