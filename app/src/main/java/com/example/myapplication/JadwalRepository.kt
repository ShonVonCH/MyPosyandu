package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class JadwalData(
    val id: Int,
    val posyanduId: String,
    val tanggal: String,
    val jamMulai: String,
    val jamSelesai: String,
    val lokasi: String,
    val status: String
)

// ── AES helpers ──────────────────────────────────────────────────────
private fun hexToBytes(hex: String): ByteArray {
    val len = hex.length
    val result = ByteArray(len / 2)
    for (i in 0 until len / 2) {
        result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
    return result
}

private fun bytesToHex(bytes: ByteArray): String =
    bytes.joinToString("") { "%02x".format(it) }

fun aesDecryptJadwal(cHex: String, aHex: String, bHex: String): String {
    val key = SecretKeySpec(hexToBytes(aHex), "AES")
    val iv = IvParameterSpec(hexToBytes(bHex))
    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    val decrypted = cipher.doFinal(hexToBytes(cHex))
    return bytesToHex(decrypted)
}

// ── Parse challenge ───────────────────────────────────────────────────
data class AesChallengeJadwal(
    val a: String,
    val b: String,
    val c: String,
    val redirectUrl: String
)

fun parseChallengeJadwal(html: String, baseUrl: String): AesChallengeJadwal? {
    val aMatch = Regex("""toNumbers\("([0-9a-f]+)"\)\s*,b=""").find(html)
        ?: Regex("""var a=toNumbers\("([0-9a-f]+)"\)""").find(html)
    val bMatch = Regex("""b=toNumbers\("([0-9a-f]+)"\)""").find(html)
    val cMatch = Regex("""c=toNumbers\("([0-9a-f]+)"\)""").find(html)
    val rMatch = Regex("""location\.href="([^"]+)"""").find(html)

    val a = aMatch?.groupValues?.getOrNull(1) ?: return null
    val b = bMatch?.groupValues?.getOrNull(1) ?: return null
    val c = cMatch?.groupValues?.getOrNull(1) ?: return null
    val r = rMatch?.groupValues?.getOrNull(1) ?: return null

    return AesChallengeJadwal(a = a, b = b, c = c, redirectUrl = r)
}

// ── Simple OkHttp client (tanpa CookieJar) ──────────────────────────
val jadwalHttpClient = OkHttpClient.Builder()
    .followRedirects(false)
    .build()

class JadwalRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val apiUrl = "https://myposyandu.gt.tc/api_posyandu/jadwal_posyandu.php"
    private val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

    // Simpan cookie di variable class
    private var currentCookie: String? = null

    fun getJadwalByPosyandu(posyanduId: String): List<JadwalData> {
        val list = mutableListOf<JadwalData>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_JADWAL_POSYANDU,
            null,
            "${DatabaseHelper.COL_JADWAL_POSYANDU_ID} = ?",
            arrayOf(posyanduId),
            null, null, "${DatabaseHelper.COL_JADWAL_ID} ASC"
        )

        while (cursor.moveToNext()) {
            list.add(
                JadwalData(
                    id         = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JADWAL_ID)),
                    posyanduId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JADWAL_POSYANDU_ID)),
                    tanggal    = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JADWAL_TANGGAL)),
                    jamMulai   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JADWAL_JAM_MULAI)),
                    jamSelesai = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JADWAL_JAM_SELESAI)),
                    lokasi     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JADWAL_LOKASI)),
                    status     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JADWAL_STATUS))
                )
            )
        }
        cursor.close()
        return list
    }

    fun addJadwalLocal(jadwal: JadwalData): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_JADWAL_POSYANDU_ID, jadwal.posyanduId)
            put(DatabaseHelper.COL_JADWAL_TANGGAL,     jadwal.tanggal)
            put(DatabaseHelper.COL_JADWAL_JAM_MULAI,   jadwal.jamMulai)
            put(DatabaseHelper.COL_JADWAL_JAM_SELESAI, jadwal.jamSelesai)
            put(DatabaseHelper.COL_JADWAL_LOKASI,      jadwal.lokasi)
            put(DatabaseHelper.COL_JADWAL_STATUS,      jadwal.status)
        }
        val result = db.insert(DatabaseHelper.TABLE_JADWAL_POSYANDU, null, values)
        return result != -1L
    }

    suspend fun addJadwal(jadwal: JadwalData): Result<Unit> = withContext(Dispatchers.IO) {
        val localOk = addJadwalLocal(jadwal)
        if (!localOk) return@withContext Result.failure(Exception("Gagal menyimpan ke database lokal"))

        try {
            // ── Step 1: GET apiUrl untuk dapetin challenge ─────────
            val req1 = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/json")
                .build()

            val res1 = jadwalHttpClient.newCall(req1).execute()
            val body1 = res1.body?.string() ?: ""
            res1.close()

            android.util.Log.d("JADWAL_API", "Step1 code=${res1.code}, body: ${body1.take(200)}")

            // ── Step 2: Parse challenge & solve AES ─────────────────
            val challenge = parseChallengeJadwal(body1, apiUrl)
            if (challenge == null) {
                android.util.Log.d("JADWAL_API", "Tidak ada challenge, coba langsung POST")
                return@withContext doPost(jadwal, null)
            }

            val cookieValue = aesDecryptJadwal(challenge.c, challenge.a, challenge.b)
            currentCookie = "__test=$cookieValue"
            android.util.Log.d("JADWAL_API", "Cookie $currentCookie")

            // ── Step 3: GET ke redirectUrl dengan cookie MANUAL ─────
            val redirectUrl = if (challenge.redirectUrl.startsWith("http"))
                challenge.redirectUrl
            else "https://myposyandu.gt.tc${challenge.redirectUrl}"

            val req2 = Request.Builder()
                .url(redirectUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/json")
                .header("Referer", apiUrl)
                .header("Cookie", currentCookie!!)  // ← MANUAL!
                .build()

            val res2 = jadwalHttpClient.newCall(req2).execute()
            val body2 = res2.body?.string() ?: ""
            res2.close()

            android.util.Log.d("JADWAL_API", "Step2 code=${res2.code}, body: ${body2.take(200)}")

            // Kalau masih challenge, solve lagi (kadang perlu 2x)
            if (body2.contains("slowAES") || body2.contains("toNumbers")) {
                android.util.Log.d("JADWAL_API", "Challenge lagi di Step2, solve ulang...")
                val challenge2 = parseChallengeJadwal(body2, apiUrl)
                if (challenge2 != null) {
                    val cookieValue2 = aesDecryptJadwal(challenge2.c, challenge2.a, challenge2.b)
                    currentCookie = "__test=$cookieValue2"
                    android.util.Log.d("JADWAL_API", "Cookie2 $currentCookie")
                }
            }

            // ── Step 4: POST data dengan cookie ─────────────────────
            return@withContext doPost(jadwal, currentCookie)

        } catch (e: Exception) {
            android.util.Log.e("JADWAL_API", "Error: ${e.message}", e)
            Result.success(Unit)
        }
    }

    // ── Helper: POST dengan cookie manual ──────────────────────────────
    private fun doPost(jadwal: JadwalData, cookie: String?): Result<Unit> {
        val json = JSONObject().apply {
            put("posyandu_id", jadwal.posyanduId)
            put("tanggal",     jadwal.tanggal)
            put("jam_mulai",   jadwal.jamMulai)
            put("jam_selesai", jadwal.jamSelesai)
            put("lokasi",      jadwal.lokasi)
            put("status",      jadwal.status)
        }.toString()

        android.util.Log.d("JADWAL_API", "JSON body: $json")

        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val builder = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Referer", apiUrl)
            .post(requestBody)

        // Tambah cookie kalau ada
        if (cookie != null) {
            builder.header("Cookie", cookie)
        }

        val postRequest = builder.build()

        val postResponse = jadwalHttpClient.newCall(postRequest).execute()
        val postBody = postResponse.body?.string() ?: ""
        postResponse.close()

        android.util.Log.d("JADWAL_API", "POST code=${postResponse.code}, body=${postBody.take(500)}")

        return if (postResponse.isSuccessful && !postBody.contains("slowAES")) {
            Result.success(Unit)
        } else {
            android.util.Log.e("JADWAL_API", "POST failed: ${postResponse.code}")
            Result.success(Unit)
        }
    }

    fun getCurrentPosyanduId(): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COL_USERS_POSYANDU_ID} FROM ${DatabaseHelper.TABLE_USERS} LIMIT 1",
            null
        )
        var id: String? = null
        if (cursor.moveToFirst()) {
            id = cursor.getString(0)
        }
        cursor.close()
        return id
    }
}