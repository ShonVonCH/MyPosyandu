package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ══════════════════════════════════════════════════════════════════════════
//  ANTRIAN API SERVICE
//
//  Tabel di server:
//    ANTRIAN      { id PK, jadwal_id FK, nomor_saat_ini, total_antrian, status }
//    ANTRIAN_ITEM { id PK, antrian_id FK, anak_id FK, ortu_id FK,
//                   nomor, waktu_ambil, waktu_dipanggil, status }
//
//  Endpoint:
//    antrian.php      → GET  (baca data antrian)
//    antrian_item.php → POST (ortu kirim ambil antrian)
//    antrian.php      → POST (kader: panggil / tidak hadir / lanjut)
//
//  WAJIB: pakai httpClient & helper dari ApiService.kt (sudah ada CookieJar
//  + AES challenge solver). Server pakai DDoS-protection yang butuh cookie
//  __test sebelum semua request bisa dapat JSON.
// ══════════════════════════════════════════════════════════════════════════

object AntrianApiService {

    private const val API_ANTRIAN      = "https://myposyandu.gt.tc/api_posyandu/antrian.php"
    private const val API_ANTRIAN_ITEM = "https://myposyandu.gt.tc/api_posyandu/antrian_item.php"
    private const val HOST             = "myposyandu.gt.tc"
    private val UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

    // ─────────────────────────────────────────────────────────────────────
    //  CORE: Pastikan cookie __test valid untuk host ini.
    //  Dipanggil sekali sebelum setiap rangkaian request.
    //  Pakai httpClient + parseChallenge + aesDecrypt + setTestCookie
    //  dari ApiService.kt (semua top-level fun/val di package yang sama).
    // ─────────────────────────────────────────────────────────────────────
    private fun ensureCookie(baseUrl: String) {
        try {
            val req  = Request.Builder().url(baseUrl).header("User-Agent", UA).build()
            val res  = httpClient.newCall(req).execute()
            val body = res.body?.string() ?: ""
            res.close()

            if (body.trimStart().startsWith("{") || body.trimStart().startsWith("[")) {
                // Cookie sudah valid, tidak perlu solve
                return
            }

            val ch = parseChallenge(body, baseUrl) ?: return
            val cookieVal = aesDecrypt(ch.c, ch.a, ch.b)
            setTestCookie(HOST, cookieVal)
            android.util.Log.d("ANTRIAN_COOKIE", "Cookie solved: ${cookieVal.take(8)}...")

            // Hit redirect agar server tahu cookie sudah di-set
            val redirectUrl = if (ch.redirectUrl.startsWith("http")) ch.redirectUrl
            else "https://$HOST${ch.redirectUrl}"
            val r2  = Request.Builder().url(redirectUrl).header("User-Agent", UA).build()
            val res2 = httpClient.newCall(r2).execute()
            res2.body?.string()
            res2.close()

        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_COOKIE", "ensureCookie error: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GET helper — solve challenge kalau perlu, return JSON string
    // ─────────────────────────────────────────────────────────────────────
    private fun getJson(url: String): String {
        ensureCookie(url)
        val req  = Request.Builder().url(url).header("User-Agent", UA)
            .header("Accept", "application/json").build()
        val res  = httpClient.newCall(req).execute()
        val body = res.body?.string() ?: ""
        res.close()
        android.util.Log.d("ANTRIAN_API", "GET $url → ${body.take(200)}")
        return body
    }

    // ─────────────────────────────────────────────────────────────────────
    //  POST helper — pastikan cookie valid dulu, baru POST
    // ─────────────────────────────────────────────────────────────────────
    private fun postJson(url: String, form: FormBody): String {
        ensureCookie(url)  // ← pastikan cookie __test ada sebelum POST
        val req  = Request.Builder().url(url).header("User-Agent", UA)
            .header("Accept", "application/json").post(form).build()
        val res  = httpClient.newCall(req).execute()
        val body = res.body?.string() ?: ""
        res.close()
        android.util.Log.d("ANTRIAN_API", "POST $url → ${body.take(300)}")
        return body
    }

    private fun isJsonOk(body: String) =
        body.isNotBlank() && !body.contains("<html", ignoreCase = true)

    // ═════════════════════════════════════════════════════════════════════
    //  1. GET antrian aktif hari ini
    //     antrian.php?tanggal=YYYY-MM-DD
    // ═════════════════════════════════════════════════════════════════════
    suspend fun getAntrianAktifHariIni(context: Context): AntrianApi? = withContext(Dispatchers.IO) {
        try {
            val today = getCurrentDate()
            val body  = getJson("$API_ANTRIAN?tanggal=$today")
            if (!isJsonOk(body)) {
                // Offline fallback → pakai data lokal
                android.util.Log.w("ANTRIAN_API", "getAntrianAktif: server tidak bisa dijangkau, pakai lokal")
                return@withContext getAntrianAktifFromLocal(context)
            }

            val antrian: AntrianApi? = when {
                body.trimStart().startsWith("[") -> {
                    val arr = JSONArray(body)
                    var found: AntrianApi? = null
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.optString("tanggal") == today &&
                            obj.optString("status")  == "aktif") {
                            found = toAntrianApi(obj)
                            break
                        }
                    }
                    found
                }
                body.trimStart().startsWith("{") -> {
                    val j = JSONObject(body)
                    when {
                        j.optBoolean("success") && j.has("data") -> toAntrianApi(j.getJSONObject("data"))
                        j.has("id") -> toAntrianApi(j)
                        else -> null
                    }
                }
                else -> null
            }

            // ── Sync ke local DB setiap kali berhasil GET dari server ──
            if (antrian != null) {
                saveAntrianToLocal(context, antrian, today)
                android.util.Log.d("ANTRIAN_LOCAL", "Antrian aktif di-sync ke local DB: ${antrian.id}")
            }

            antrian
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "getAntrianAktif error: ${e.message}", e)
            // Fallback ke lokal jika ada exception (network, dll)
            getAntrianAktifFromLocal(context)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  2. GET daftar item antrian (GET antrian.php?antrian_id=...)
    //     context wajib agar bisa sync & fallback ke local DB
    // ═════════════════════════════════════════════════════════════════════
    suspend fun getAntrianItems(context: Context, antrianId: String): List<AntrianItemApi> = withContext(Dispatchers.IO) {
        try {
            val body = getJson("$API_ANTRIAN?antrian_id=$antrianId")
            var list = parseItems(body)

            if (list.isEmpty()) {
                // Fallback endpoint: antrian_item.php?antrian_id=...
                val body2 = getJson("$API_ANTRIAN_ITEM?antrian_id=$antrianId")
                list = parseItems(body2)
            }

            if (list.isNotEmpty()) {
                // ── Sync items ke local DB ──
                saveAntrianItemsToLocal(context, list)
                android.util.Log.d("ANTRIAN_LOCAL", "${list.size} antrian_item di-sync ke local DB")
                return@withContext list
            }

            // Tidak ada data dari server → pakai lokal
            android.util.Log.w("ANTRIAN_API", "getAntrianItems: server kosong, pakai lokal")
            getAntrianItemsFromLocal(context, antrianId)

        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "getAntrianItems error: ${e.message}", e)
            // Fallback ke lokal jika network error
            getAntrianItemsFromLocal(context, antrianId)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  3. POST antrian_item.php — Ortu ambil antrian
    // ═════════════════════════════════════════════════════════════════════
    suspend fun ambilAntrian(
        context: Context,
        anakId: String,
        ortuId: String
    ): AntrianResponse = withContext(Dispatchers.IO) {
        try {
            /* ── Cek duplikat ── */
            val aktif = getAntrianAktifHariIni(context)
            if (aktif != null) {
                val existing = getAntrianItems(context, aktif.id).find { it.anakId == anakId && it.status == 1 }
                if (existing != null) {
                    return@withContext AntrianResponse(
                        success      = true,
                        message      = "Sudah ambil antrian nomor ${existing.nomor}",
                        antrianId    = aktif.id,
                        nomorAntrian = existing.nomor
                    )
                }
            }

            /* ── Buat header antrian baru kalau belum ada ── */
            val antrianId: String
            val nomorBaru: Int

            if (aktif == null) {
                val newId = UUID.randomUUID().toString()
                val form  = FormBody.Builder()
                    .add("id",             newId)
                    .add("jadwal_id",      "")
                    .add("tanggal",        getCurrentDate())
                    .add("nomor_saat_ini", "0")
                    .add("total_antrian",  "0")
                    .add("status",         "aktif")
                    .build()
                val resp = postJson(API_ANTRIAN, form)
                android.util.Log.d("ANTRIAN_API", "Buat antrian baru: ${resp.take(200)}")
                antrianId = newId
                nomorBaru = 1
            } else {
                antrianId = aktif.id
                nomorBaru = aktif.totalAntrian + 1
            }

            /* ── POST item ke antrian_item.php ── */
            val itemForm = FormBody.Builder()
                .add("id",          UUID.randomUUID().toString())
                .add("antrian_id",  antrianId)
                .add("anak_id",     anakId)
                .add("ortu_id",     ortuId)
                .add("nomor",       nomorBaru.toString())
                .add("waktu_ambil", getCurrentTimestamp())
                .add("status",      "1")
                .build()

            val itemBody = postJson(API_ANTRIAN_ITEM, itemForm)

            val sukses = if (isJsonOk(itemBody)) {
                try {
                    val j = JSONObject(itemBody)
                    j.optBoolean("success", false) ||
                            j.optString("status") == "success" ||
                            j.optString("message").isNotBlank()
                } catch (e: Exception) { true }
            } else {
                android.util.Log.e("ANTRIAN_API", "POST antrian_item gagal HTML: ${itemBody.take(100)}")
                false
            }

            if (!sukses) {
                return@withContext AntrianResponse(
                    success = false,
                    message = "Server menolak: ${itemBody.take(80)}"
                )
            }

            /* ── Update total_antrian di header ── */
            updateTotal(antrianId, nomorBaru)

            val nomorFinal = try { JSONObject(itemBody).optInt("nomor", nomorBaru) }
            catch (e: Exception) { nomorBaru }

            // ── Simpan header antrian ke local DB ──
            val headerToSave = AntrianApi(
                id           = antrianId,
                jadwalId     = aktif?.jadwalId ?: "",
                nomorSaatIni = aktif?.nomorSaatIni ?: 0,
                totalAntrian = nomorBaru,
                status       = "aktif"
            )
            saveAntrianToLocal(context, headerToSave, getCurrentDate())

            // ── Simpan item baru ke local DB ──
            val itemId = try { JSONObject(itemBody).optString("id", UUID.randomUUID().toString()) }
                         catch (e: Exception) { UUID.randomUUID().toString() }
            val newItem = AntrianItemApi(
                id             = itemId,
                antrianId      = antrianId,
                anakId         = anakId,
                ortuId         = ortuId,
                nomor          = nomorFinal,
                waktuAmbil     = getCurrentTimestamp(),
                waktuDipanggil = null,
                status         = 1
            )
            saveAntrianItemsToLocal(context, listOf(newItem))
            android.util.Log.d("ANTRIAN_LOCAL", "ambilAntrian: header + item disimpan ke local DB, nomor=$nomorFinal")

            AntrianResponse(
                success      = true,
                message      = "Berhasil ambil antrian nomor $nomorFinal",
                antrianId    = antrianId,
                nomorAntrian = nomorFinal
            )

        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "ambilAntrian error: ${e.message}", e)
            AntrianResponse(success = false, message = "Gagal: ${e.message}")
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  4. Kader: panggil antrian berikutnya (status item → 0)
    // ═════════════════════════════════════════════════════════════════════
    suspend fun panggilAntrian(itemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val form = FormBody.Builder()
                .add("id",              itemId)
                .add("status",          "0")
                .add("waktu_dipanggil", getCurrentTimestamp())
                .build()
            val body = postJson(API_ANTRIAN, form)
            isJsonOk(body).also {
                if (!it) android.util.Log.e("ANTRIAN_API", "panggilAntrian gagal: ${body.take(100)}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "panggilAntrian error: ${e.message}", e)
            false
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  5. Kader: tidak hadir (status item → 2) lalu lanjut nomor
    // ═════════════════════════════════════════════════════════════════════
    suspend fun tidakHadir(itemId: String, antrianId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val form = FormBody.Builder()
                .add("id",     itemId)
                .add("status", "2")
                .build()
            postJson(API_ANTRIAN, form)
            lanjutAntrianBerikutnya(antrianId)
            true
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "tidakHadir error: ${e.message}", e)
            false
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  6. Lanjut nomor berikutnya (increment nomor_saat_ini di header)
    // ═════════════════════════════════════════════════════════════════════
    suspend fun lanjutAntrianBerikutnya(antrianId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val form = FormBody.Builder()
                .add("id",     antrianId)
                .add("action", "lanjut")
                .build()
            postJson(API_ANTRIAN, form)
            true
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "lanjutAntrian error: ${e.message}", e)
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PRIVATE: update total_antrian di header antrian
    // ─────────────────────────────────────────────────────────────────────
    private suspend fun updateTotal(antrianId: String, total: Int) = withContext(Dispatchers.IO) {
        try {
            val form = FormBody.Builder()
                .add("id",            antrianId)
                .add("total_antrian", total.toString())
                .build()
            postJson(API_ANTRIAN, form)
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "updateTotal error: ${e.message}", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LOCAL DB: simpan antrian header ke SQLite lokal
    // ═════════════════════════════════════════════════════════════════════
    private fun saveAntrianToLocal(context: Context, antrian: AntrianApi, tanggal: String) {
        try {
            val db = DatabaseHelper(context).writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_ANT_ID,             antrian.id)
                put(DatabaseHelper.COL_ANT_JADWAL_ID,      antrian.jadwalId)
                put(DatabaseHelper.COL_ANT_TANGGAL,        tanggal)
                put(DatabaseHelper.COL_ANT_NOMOR_SAAT_INI, antrian.nomorSaatIni)
                put(DatabaseHelper.COL_ANT_TOTAL_ANTRIAN,  antrian.totalAntrian)
                put(DatabaseHelper.COL_ANT_STATUS,         antrian.status)
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_ANTRIAN, null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            db.close()
            android.util.Log.d("ANTRIAN_LOCAL", "Saved antrian to local DB: ${antrian.id}")
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_LOCAL", "saveAntrianToLocal error: ${e.message}", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LOCAL DB: simpan antrian_item ke SQLite lokal (upsert)
    // ═════════════════════════════════════════════════════════════════════
    private fun saveAntrianItemsToLocal(context: Context, items: List<AntrianItemApi>) {
        if (items.isEmpty()) return
        try {
            val db = DatabaseHelper(context).writableDatabase
            db.beginTransaction()
            try {
                for (item in items) {
                    val values = ContentValues().apply {
                        put(DatabaseHelper.COL_ANTITEM_ID,              item.id)
                        put(DatabaseHelper.COL_ANTITEM_ANTRIAN_ID,      item.antrianId)
                        put(DatabaseHelper.COL_ANTITEM_ANAK_ID,         item.anakId)
                        put(DatabaseHelper.COL_ANTITEM_ORTU_ID,         item.ortuId)
                        put(DatabaseHelper.COL_ANTITEM_NOMOR,           item.nomor)
                        put(DatabaseHelper.COL_ANTITEM_WAKTU_AMBIL,     item.waktuAmbil)
                        put(DatabaseHelper.COL_ANTITEM_WAKTU_DIPANGGIL, item.waktuDipanggil)
                        put(DatabaseHelper.COL_ANTITEM_STATUS,          item.status)
                    }
                    db.insertWithOnConflict(
                        DatabaseHelper.TABLE_ANTRIAN_ITEM, null, values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                db.setTransactionSuccessful()
                android.util.Log.d("ANTRIAN_LOCAL", "Saved ${items.size} antrian_item to local DB")
            } finally {
                db.endTransaction()
            }
            db.close()
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_LOCAL", "saveAntrianItemsToLocal error: ${e.message}", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LOCAL DB: ambil antrian aktif dari local DB (fallback offline)
    // ═════════════════════════════════════════════════════════════════════
    fun getAntrianAktifFromLocal(context: Context): AntrianApi? {
        return try {
            val today = getCurrentDate()
            val db = DatabaseHelper(context).readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ANT_ID}, ${DatabaseHelper.COL_ANT_JADWAL_ID}, " +
                "${DatabaseHelper.COL_ANT_NOMOR_SAAT_INI}, ${DatabaseHelper.COL_ANT_TOTAL_ANTRIAN}, " +
                "${DatabaseHelper.COL_ANT_STATUS} " +
                "FROM ${DatabaseHelper.TABLE_ANTRIAN} " +
                "WHERE ${DatabaseHelper.COL_ANT_TANGGAL} = ? AND ${DatabaseHelper.COL_ANT_STATUS} = 'aktif' " +
                "LIMIT 1",
                arrayOf(today)
            )
            val result = if (cursor.moveToFirst()) {
                AntrianApi(
                    id           = cursor.getString(0),
                    jadwalId     = cursor.getString(1) ?: "",
                    nomorSaatIni = cursor.getInt(2),
                    totalAntrian = cursor.getInt(3),
                    status       = cursor.getString(4)
                )
            } else null
            cursor.close()
            db.close()
            result
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_LOCAL", "getAntrianAktifFromLocal error: ${e.message}", e)
            null
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LOCAL DB: ambil antrian_item dari local DB (fallback offline)
    // ═════════════════════════════════════════════════════════════════════
    fun getAntrianItemsFromLocal(context: Context, antrianId: String): List<AntrianItemApi> {
        return try {
            val db = DatabaseHelper(context).readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ANTITEM_ID}, ${DatabaseHelper.COL_ANTITEM_ANTRIAN_ID}, " +
                "${DatabaseHelper.COL_ANTITEM_ANAK_ID}, ${DatabaseHelper.COL_ANTITEM_ORTU_ID}, " +
                "${DatabaseHelper.COL_ANTITEM_NOMOR}, ${DatabaseHelper.COL_ANTITEM_WAKTU_AMBIL}, " +
                "${DatabaseHelper.COL_ANTITEM_WAKTU_DIPANGGIL}, ${DatabaseHelper.COL_ANTITEM_STATUS} " +
                "FROM ${DatabaseHelper.TABLE_ANTRIAN_ITEM} " +
                "WHERE ${DatabaseHelper.COL_ANTITEM_ANTRIAN_ID} = ? " +
                "ORDER BY ${DatabaseHelper.COL_ANTITEM_NOMOR} ASC",
                arrayOf(antrianId)
            )
            val list = mutableListOf<AntrianItemApi>()
            while (cursor.moveToNext()) {
                list.add(AntrianItemApi(
                    id             = cursor.getString(0),
                    antrianId      = cursor.getString(1),
                    anakId         = cursor.getString(2),
                    ortuId         = cursor.getString(3),
                    nomor          = cursor.getInt(4),
                    waktuAmbil     = cursor.getString(5) ?: "",
                    waktuDipanggil = cursor.getString(6)?.takeIf { it.isNotBlank() },
                    status         = cursor.getInt(7)
                ))
            }
            cursor.close()
            db.close()
            list
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_LOCAL", "getAntrianItemsFromLocal error: ${e.message}", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────
    private fun toAntrianApi(obj: JSONObject) = AntrianApi(
        id           = obj.getString("id"),
        jadwalId     = obj.optString("jadwal_id", ""),
        nomorSaatIni = obj.optInt("nomor_saat_ini", 0),
        totalAntrian = obj.optInt("total_antrian", 0),
        status       = obj.optString("status", "aktif")
    )

    private fun toAntrianItem(obj: JSONObject) = AntrianItemApi(
        id             = obj.optString("id", ""),
        antrianId      = obj.optString("antrian_id", ""),
        anakId         = obj.optString("anak_id", ""),
        ortuId         = obj.optString("ortu_id", ""),
        nomor          = obj.optInt("nomor", 0),
        waktuAmbil     = obj.optString("waktu_ambil", ""),
        waktuDipanggil = obj.optString("waktu_dipanggil", null)?.takeIf { it.isNotBlank() },
        status         = obj.optInt("status", 1)
    )

    private fun parseItems(body: String): List<AntrianItemApi> {
        val result = mutableListOf<AntrianItemApi>()
        if (!isJsonOk(body)) return result
        try {
            when {
                body.trimStart().startsWith("[") -> {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) result.add(toAntrianItem(arr.getJSONObject(i)))
                }
                body.trimStart().startsWith("{") -> {
                    val j = JSONObject(body)
                    for (key in listOf("data", "items")) {
                        j.optJSONArray(key)?.let { arr ->
                            for (i in 0 until arr.length()) result.add(toAntrianItem(arr.getJSONObject(i)))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ANTRIAN_API", "parseItems error: ${e.message}", e)
        }
        return result
    }
}