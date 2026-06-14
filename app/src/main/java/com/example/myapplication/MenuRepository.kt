package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

// ── Model ─────────────────────────────────────────────────────────────────
data class MenuKategori(
    val id: String,
    val nama: String,
    val rangeUsia: String
)

data class MenuSehat(
    val id: String,
    val kategoriId: String,
    val judul: String,
    val rangeUsia: String,
    val durasiMenit: Int,
    val bahan: String,
    val caraMembuat: String,
    val kandunganGizi: String
)

// ── Repository ────────────────────────────────────────────────────────────
class MenuRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val TAG = "MENU_REPO"

    // ── Sync dari API ke local DB ─────────────────────────────────────────
    suspend fun syncMenuData(): Boolean = withContext(Dispatchers.IO) {
        try {
            // menu_kategori.php → isi tabel menu_kategori
            // menu_sehat.php   → isi tabel menu_sehat
            val kategoriJson = fetchJson("https://myposyandu.gt.tc/api_posyandu/menu_kategori.php")
            val menuJson     = fetchJson("https://myposyandu.gt.tc/api_posyandu/menu_sehat.php")

            if (kategoriJson == null && menuJson == null) {
                Log.w(TAG, "Kedua API gagal, skip sync")
                return@withContext false
            }

            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                // ── Insert menu_kategori ──────────────────────────────────
                if (kategoriJson != null) {
                    val arr = toJsonArray(kategoriJson)
                    Log.d(TAG, "Sync kategori: ${arr.length()} data")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            put(DatabaseHelper.COL_MK_ID,         obj.optString("id"))
                            put(DatabaseHelper.COL_MK_NAMA,       obj.optString("nama"))
                            put(DatabaseHelper.COL_MK_RANGE_USIA, obj.optString("range_usia"))
                        }
                        db.insertWithOnConflict(
                            DatabaseHelper.TABLE_MENU_KATEGORI, null, cv,
                            SQLiteDatabase.CONFLICT_REPLACE
                        )
                    }
                }

                // ── Insert menu_sehat ─────────────────────────────────────
                if (menuJson != null) {
                    val arr = toJsonArray(menuJson)
                    Log.d(TAG, "Sync menu_sehat: ${arr.length()} data")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)

                        // durasi_menit dari API bisa berupa string "20" atau int 20
                        val durasi = obj.optString("durasi_menit", "0").toIntOrNull() ?: 0

                        // kandungan_gizi bisa berupa JSON array string atau plain string
                        val giziRaw = try {
                            obj.optJSONArray("kandungan_gizi")?.let { ja ->
                                (0 until ja.length()).joinToString(", ") { ja.optString(it) }
                            } ?: obj.optString("kandungan_gizi", "")
                        } catch (_: Exception) { obj.optString("kandungan_gizi", "") }

                        val cv = ContentValues().apply {
                            put(DatabaseHelper.COL_MS_ID,          obj.optString("id"))
                            put(DatabaseHelper.COL_MS_KATEGORI_ID, obj.optString("kategori_id"))
                            put(DatabaseHelper.COL_MS_JUDUL,       obj.optString("judul"))
                            put(DatabaseHelper.COL_MS_RANGE_USIA,  obj.optString("range_usia"))
                            put(DatabaseHelper.COL_MS_DURASI,      durasi)
                            put(DatabaseHelper.COL_MS_BAHAN,       obj.optString("bahan"))
                            put(DatabaseHelper.COL_MS_CARA_MEMBUAT,obj.optString("cara_membuat"))
                            put(DatabaseHelper.COL_MS_GIZI,        giziRaw)
                        }
                        db.insertWithOnConflict(
                            DatabaseHelper.TABLE_MENU_SEHAT, null, cv,
                            SQLiteDatabase.CONFLICT_REPLACE
                        )
                    }
                }

                db.setTransactionSuccessful()
                Log.d(TAG, "Sync berhasil")
                true
            } finally {
                db.endTransaction()
                db.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync gagal: ${e.message}", e)
            false
        }
    }

    // ── Baca dari local DB ────────────────────────────────────────────────

    suspend fun getKategoriList(): List<MenuKategori> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuKategori>()
        val db = dbHelper.readableDatabase
        try {
            val c = db.query(
                DatabaseHelper.TABLE_MENU_KATEGORI, null,
                null, null, null, null, DatabaseHelper.COL_MK_ID + " ASC"
            )
            while (c.moveToNext()) {
                list.add(MenuKategori(
                    id        = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MK_ID)),
                    nama      = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MK_NAMA)),
                    rangeUsia = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MK_RANGE_USIA)) ?: ""
                ))
            }
            c.close()
        } catch (e: Exception) { Log.e(TAG, "getKategoriList: ${e.message}") }
        finally { db.close() }
        list
    }

    suspend fun getMenuByKategori(kategoriId: String): List<MenuSehat> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuSehat>()
        val db = dbHelper.readableDatabase
        try {
            val c = db.query(
                DatabaseHelper.TABLE_MENU_SEHAT, null,
                "${DatabaseHelper.COL_MS_KATEGORI_ID} = ?", arrayOf(kategoriId),
                null, null, DatabaseHelper.COL_MS_JUDUL + " ASC"
            )
            while (c.moveToNext()) { list.add(cursorToMenu(c)) }
            c.close()
        } catch (e: Exception) { Log.e(TAG, "getMenuByKategori: ${e.message}") }
        finally { db.close() }
        list
    }

    suspend fun getMenuByRangeUsia(keywords: List<String>): List<MenuSehat> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuSehat>()
        val db = dbHelper.readableDatabase
        try {
            val c = db.query(DatabaseHelper.TABLE_MENU_SEHAT, null, null, null, null, null, DatabaseHelper.COL_MS_JUDUL + " ASC")
            while (c.moveToNext()) {
                val menu = cursorToMenu(c)
                val r = menu.rangeUsia.lowercase()
                if (keywords.any { kw -> r.contains(kw.lowercase()) }) {
                    list.add(menu)
                }
            }
            c.close()
        } catch (e: Exception) { Log.e(TAG, "getMenuByRangeUsia: ${e.message}") }
        finally { db.close() }
        list
    }

    suspend fun getMenuDetail(menuId: String): MenuSehat? = withContext(Dispatchers.IO) {
        var menu: MenuSehat? = null
        val db = dbHelper.readableDatabase
        try {
            val c = db.query(
                DatabaseHelper.TABLE_MENU_SEHAT, null,
                "${DatabaseHelper.COL_MS_ID} = ?", arrayOf(menuId),
                null, null, null
            )
            if (c.moveToFirst()) menu = cursorToMenu(c)
            c.close()
        } catch (e: Exception) { Log.e(TAG, "getMenuDetail: ${e.message}") }
        finally { db.close() }
        menu
    }

    suspend fun getMenuRekomendasi(limit: Int = 5): List<MenuSehat> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuSehat>()
        val db = dbHelper.readableDatabase
        try {
            val c = db.query(
                DatabaseHelper.TABLE_MENU_SEHAT, null,
                null, null, null, null, "RANDOM()", limit.toString()
            )
            while (c.moveToNext()) { list.add(cursorToMenu(c)) }
            c.close()
        } catch (e: Exception) { Log.e(TAG, "getMenuRekomendasi: ${e.message}") }
        finally { db.close() }
        list
    }

    private fun cursorToMenu(c: android.database.Cursor): MenuSehat = MenuSehat(
        id           = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_ID)),
        kategoriId   = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_KATEGORI_ID)) ?: "",
        judul        = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_JUDUL)),
        rangeUsia    = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_RANGE_USIA)) ?: "",
        durasiMenit  = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_DURASI)),
        bahan        = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_BAHAN)) ?: "",
        caraMembuat  = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_CARA_MEMBUAT)) ?: "",
        kandunganGizi= c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MS_GIZI)) ?: ""
    )

    // ── Helper HTTP ───────────────────────────────────────────────────────
    private fun fetchJson(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return null
            resp.close()
            Log.d(TAG, "fetch [$url]: ${body.take(80)}")
            val t = body.trimStart()
            if (t.startsWith("[") || t.startsWith("{")) body else null
        } catch (e: Exception) {
            Log.e(TAG, "fetchJson error [$url]: ${e.message}")
            null
        }
    }

    // Bisa terima array langsung atau {"data":[...]} wrapper
    private fun toJsonArray(json: String): JSONArray {
        val t = json.trimStart()
        return if (t.startsWith("[")) {
            JSONArray(json)
        } else {
            val obj = JSONObject(json)
            obj.optJSONArray("data") ?: obj.optJSONArray("items") ?: JSONArray()
        }
    }
}