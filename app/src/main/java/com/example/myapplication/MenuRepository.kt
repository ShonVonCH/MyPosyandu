package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class MenuKategori(
    val id: String,
    val nama: String,
    val rangeUsia: String? = null
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

class MenuRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    suspend fun getKategoriList(): List<MenuKategori> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuKategori>()
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.query(DatabaseHelper.TABLE_MENU_KATEGORI, null, null, null, null, null, null)
            while (cursor.moveToNext()) {
                list.add(MenuKategori(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MK_ID)),
                    nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MK_NAMA)),
                    rangeUsia = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MK_RANGE_USIA))
                ))
            }
            cursor.close()
        } catch (e: Exception) { Log.e("MENU_REPO", "Error getKategoriList: ${e.message}") }
        finally { db.close() }
        list
    }

    suspend fun getMenuListByAgeGroup(group: String): List<MenuSehat> = withContext(Dispatchers.IO) {
        val allMenu = getAllMenuFromLocal()
        allMenu.filter { menu ->
            val range = menu.rangeUsia.lowercase()
            when (group) {
                "6-12 Bulan" -> range.contains("bulan") && !range.contains("0-") && !range.contains("1-")
                "1-3 Tahun" -> range.contains("tahun") && (range.contains("1") || range.contains("2") || range.contains("3"))
                "4-5 Tahun" -> range.contains("tahun") && (range.contains("4") || range.contains("5"))
                "> 5 Tahun" -> range.contains("tahun") && !range.contains("1") && !range.contains("2") && !range.contains("3") && !range.contains("4")
                else -> true
            }
        }
    }

    private suspend fun getAllMenuFromLocal(): List<MenuSehat> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuSehat>()
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.query(DatabaseHelper.TABLE_MENU_SEHAT, null, null, null, null, null, null)
            while (cursor.moveToNext()) { list.add(cursorToMenuSehat(cursor)) }
            cursor.close()
        } catch (e: Exception) { Log.e("MENU_REPO", "Error getAllMenu: ${e.message}") }
        finally { db.close() }
        list
    }

    suspend fun getMenuDetail(menuId: String): MenuSehat? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        var menu: MenuSehat? = null
        try {
            val cursor = db.query(DatabaseHelper.TABLE_MENU_SEHAT, null, "${DatabaseHelper.COL_MS_ID} = ?", arrayOf(menuId), null, null, null)
            if (cursor.moveToFirst()) { menu = cursorToMenuSehat(cursor) }
            cursor.close()
        } finally { db.close() }
        menu
    }

    suspend fun getRecommendedMenu(): List<MenuSehat> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuSehat>()
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.query(DatabaseHelper.TABLE_MENU_SEHAT, null, null, null, null, null, "RANDOM()", "10")
            while (cursor.moveToNext()) { list.add(cursorToMenuSehat(cursor)) }
            cursor.close()
        } finally { db.close() }
        list
    }

    private fun cursorToMenuSehat(cursor: android.database.Cursor): MenuSehat {
        return MenuSehat(
            id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_ID)),
            kategoriId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_KATEGORI_ID)),
            judul = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_JUDUL)),
            rangeUsia = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_RANGE_USIA)),
            durasiMenit = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_DURASI)),
            bahan = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_BAHAN)),
            caraMembuat = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_CARA_MEMBUAT)),
            kandunganGizi = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MS_GIZI))
        )
    }

    suspend fun syncMenuData(): Boolean = withContext(Dispatchers.IO) {
        try {
            // URL sesuai info terakhir:
            // menu_sehat.php -> Kategori
            // menu_kategori.php -> Resep Detail
            val kategoriJson = fetchJsonFromApi("https://myposyandu.gt.tc/api_posyandu/menu_sehat.php")
            val recipeJson = fetchJsonFromApi("https://myposyandu.gt.tc/api_posyandu/menu_kategori.php")
            if (kategoriJson == null || recipeJson == null) return@withContext false

            val db = dbHelper.writableDatabase
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.beginTransaction()
            try {
                // --- Insert Kategori (dari menu_sehat.php) ---
                val katArray = parseJsonToArray(kategoriJson)
                for (i in 0 until katArray.length()) {
                    val obj = katArray.getJSONObject(i)
                    val values = ContentValues().apply {
                        put(DatabaseHelper.COL_MK_ID, obj.optString("id", ""))
                        put(DatabaseHelper.COL_MK_NAMA, obj.optString("nama", ""))
                        put(DatabaseHelper.COL_MK_RANGE_USIA, obj.optString("range_usia", ""))
                    }
                    db.insertWithOnConflict(DatabaseHelper.TABLE_MENU_KATEGORI, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }

                // --- Insert Menu Sehat (dari menu_kategori.php) ---
                val recipeArray = parseJsonToArray(recipeJson)
                for (i in 0 until recipeArray.length()) {
                    val obj = recipeArray.getJSONObject(i)

                    val durasiStr = obj.optString("durasi_menit", "0")
                    val durasiInt = durasiStr.toIntOrNull() ?: 0

                    val values = ContentValues().apply {
                        put(DatabaseHelper.COL_MS_ID, obj.optString("id", ""))
                        put(DatabaseHelper.COL_MS_KATEGORI_ID, obj.optString("kategori_id", ""))
                        put(DatabaseHelper.COL_MS_JUDUL, obj.optString("judul", ""))
                        put(DatabaseHelper.COL_MS_RANGE_USIA, obj.optString("range_usia", ""))
                        put(DatabaseHelper.COL_MS_DURASI, durasiInt)
                        put(DatabaseHelper.COL_MS_BAHAN, obj.optString("bahan", ""))
                        put(DatabaseHelper.COL_MS_CARA_MEMBUAT, obj.optString("cara_membuat", ""))
                        put(DatabaseHelper.COL_MS_GIZI, obj.optString("kandungan_gizi", ""))
                    }
                    db.insertWithOnConflict(DatabaseHelper.TABLE_MENU_SEHAT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
                db.execSQL("PRAGMA foreign_keys = ON")
                db.close()
            }
        } catch (e: Exception) {
            Log.e("MENU_SYNC", "Sync failed: ${e.message}", e)
            false
        }
    }

    private fun parseJsonToArray(json: String): JSONArray {
        val trimmed = json.trim()
        return if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            if (obj.has("data")) obj.getJSONArray("data") else JSONArray().put(obj)
        } else JSONArray(trimmed)
    }

    private suspend fun fetchJsonFromApi(url: String): String? {
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        try {
            val req = Request.Builder().url(url).header("User-Agent", userAgent).build()
            val res = httpClient.newCall(req).execute()
            val body = res.body?.string() ?: ""
            res.close()
            if (body.trimStart().startsWith("[") || body.trimStart().startsWith("{")) return body
            val challenge = parseChallenge(body, url)
            if (challenge != null) {
                setTestCookie("myposyandu.gt.tc", aesDecrypt(challenge.c, challenge.a, challenge.b))
                val res2 = httpClient.newCall(Request.Builder().url(if(challenge.redirectUrl.startsWith("http")) challenge.redirectUrl else "https://myposyandu.gt.tc${challenge.redirectUrl}")
                    .header("User-Agent", userAgent).header("Referer", url).build()).execute()
                val body2 = res2.body?.string() ?: ""
                res2.close()
                return body2
            }
        } catch (e: Exception) { Log.e("MENU_SYNC", "API Error: ${e.message}") }
        return null
    }
}