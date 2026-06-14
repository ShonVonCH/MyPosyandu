package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class UserApi(
    val id: String,
    val nama: String,
    val username: String,
    val role: String,
    val posyanduId: String
)

data class OrtuApi(
    val id: String,
    val nama: String,
    val username: String,
    val password: String,
    val role: String = "ortu",
    val posyanduId: String,
    val createdAt: String
)

data class AnakApi(
    val id: String,
    val nama: String,
    val tanggalLahir: String,
    val jenisKelamin: String,
    val ortuId: String,
    val posyanduId: String,
    val createdAt: String
)

data class PemeriksaanApi(
    val id            : String,
    val anakId        : String,
    val kaderId       : String,
    val tanggal       : String,
    val beratBadan    : Double,
    val tinggiBadan   : Double,
    val lingkarKepala : Double,
    val lingkarLengan : Double,
    val zScoreTbu     : Double,
    val zScoreBbu     : Double,
    val statusGizi    : String,
    val catatan       : String
)

// SyncResult HANYA di ApiService.kt — jangan deklarasikan ulang di file lain!
data class SyncResult(
    val success: Boolean,
    val inserted: Int,
    val skipped: Int,
    val failed: Int,
    val message: String
)

// ── AES-CBC decrypt (slowAES mode 2) ─────────────────────────────────────────
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

fun aesDecrypt(cHex: String, aHex: String, bHex: String): String {
    val key     = SecretKeySpec(hexToBytes(aHex), "AES")
    val iv      = IvParameterSpec(hexToBytes(bHex))
    val cipher  = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    val decrypted = cipher.doFinal(hexToBytes(cHex))
    return bytesToHex(decrypted)
}

// ── Cookie Jar ───────────────────────────────────────────────────────────────
val cookieStore = HashMap<String, MutableList<Cookie>>()

// Helper: set cookie __test
fun setTestCookie(host: String, value: String) {
    val list = cookieStore.getOrPut(host) { mutableListOf() }
    list.removeAll { it.name == "__test" }
    list.add(
        Cookie.Builder()
            .name("__test").value(value)
            .domain("myposyandu.gt.tc").path("/")
            .build()
    )
}

val httpClient = OkHttpClient.Builder()
    .cookieJar(object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            for (newCookie in cookies) {
                list.removeAll { it.name == newCookie.name }
                list.add(newCookie)
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    })
    .followRedirects(false)
    .build()

// ── Parse challenge dari HTML ─────────────────────────────────────────────────
data class AesChallenge(
    val a: String,
    val b: String,
    val c: String,
    val redirectUrl: String
)

fun parseChallenge(html: String, baseUrl: String): AesChallenge? {
    val aMatch =
        Regex("""toNumbers\("([0-9a-f]+)"\)\s*,b=""").find(html)
            ?: Regex("""var a=toNumbers\("([0-9a-f]+)"\)""").find(html)

    val bMatch = Regex("""b=toNumbers\("([0-9a-f]+)"\)""").find(html)
    val cMatch = Regex("""c=toNumbers\("([0-9a-f]+)"\)""").find(html)
    val rMatch = Regex("""location\.href="([^"]+)"""").find(html)

    val a = aMatch?.groupValues?.getOrNull(1) ?: return null
    val b = bMatch?.groupValues?.getOrNull(1) ?: return null
    val c = cMatch?.groupValues?.getOrNull(1) ?: return null
    val r = rMatch?.groupValues?.getOrNull(1) ?: return null

    return AesChallenge(a = a, b = b, c = c, redirectUrl = r)
}

// ── Format tanggal untuk created_at: yyyy-MM-dd HH:mm:ss ─────────────────────
private fun formatCreatedAt(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ── Konversi tanggal dari dd/MM/yyyy ke yyyy-MM-dd ───────────────────────────
private fun convertTanggalLahir(tgl: String): String {
    return try {
        val cleaned = tgl.trim()
        when {
            cleaned.contains("/") -> {
                val parts = cleaned.split("/")
                if (parts.size == 3) {
                    val day   = parts[0].padStart(2, '0')
                    val month = parts[1].padStart(2, '0')
                    val year  = parts[2]
                    if (year.length == 4 && month.toIntOrNull() in 1..12 && day.toIntOrNull() in 1..31) {
                        "$year-$month-$day"
                    } else tgl
                } else tgl
            }
            cleaned.contains("-") -> {
                val parts = cleaned.split("-")
                if (parts.size == 3) {
                    if (parts[0].length == 4) {
                        cleaned
                    } else {
                        val day   = parts[0].padStart(2, '0')
                        val month = parts[1].padStart(2, '0')
                        val year  = parts[2]
                        if (year.length == 4 && month.toIntOrNull() in 1..12 && day.toIntOrNull() in 1..31) {
                            "$year-$month-$day"
                        } else cleaned
                    }
                } else cleaned
            }
            else -> tgl
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_ANAK_API", "Gagal konversi tanggal: $tgl", e)
        tgl
    }
}

// ── Konversi tanggal dari dd/MM/yyyy ke yyyy-MM-dd HH:mm:ss ───────────────
private fun convertTanggalPemeriksaan(tgl: String): String {
    return try {
        val cleaned = tgl.trim()
        when {
            cleaned.contains("/") -> {
                val parts = cleaned.split("/")
                if (parts.size == 3) {
                    val day   = parts[0].padStart(2, '0')
                    val month = parts[1].padStart(2, '0')
                    val year  = parts[2]
                    if (year.length == 4) {
                        "$year-$month-$day 00:00:00"
                    } else tgl
                } else tgl
            }
            cleaned.contains("-") -> {
                val parts = cleaned.split("-")
                if (parts.size == 3) {
                    if (parts[0].length == 4) {
                        "$cleaned 00:00:00"
                    } else {
                        val day   = parts[0].padStart(2, '0')
                        val month = parts[1].padStart(2, '0')
                        val year  = parts[2]
                        "$year-$month-$day 00:00:00"
                    }
                } else cleaned
            }
            else -> tgl
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_PEMERIKSAAN_API", "Gagal konversi tanggal: $tgl", e)
        tgl
    }
}

// ── Konversi tanggal dari dd/MM/yyyy ke yyyy-MM-dd HH:mm:ss ───────────────
private fun convertTanggalVaksin(tgl: String): String {
    return try {
        val cleaned = tgl.trim()
        when {
            cleaned.contains("/") -> {
                val parts = cleaned.split("/")
                if (parts.size == 3) {
                    val day   = parts[0].padStart(2, '0')
                    val month = parts[1].padStart(2, '0')
                    val year  = parts[2]
                    if (year.length == 4) {
                        "$year-$month-$day 00:00:00"
                    } else tgl
                } else tgl
            }
            cleaned.contains("-") -> {
                val parts = cleaned.split("-")
                if (parts.size == 3) {
                    if (parts[0].length == 4) {
                        "$cleaned 00:00:00"
                    } else {
                        val day   = parts[0].padStart(2, '0')
                        val month = parts[1].padStart(2, '0')
                        val year  = parts[2]
                        "$year-$month-$day 00:00:00"
                    }
                } else cleaned
            }
            else -> tgl
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_VAKSIN_API", "Gagal konversi tanggal: $tgl", e)
        tgl
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  KONVERSI TANGGAL UNTUK SYNC API → SQLITE LOKAL
//  API mengembalikan yyyy-MM-dd HH:mm:ss, tapi SQLite lokal pakai dd/MM/yyyy
// ═══════════════════════════════════════════════════════════════════════════

// ── Konversi tanggal lahir dari API (yyyy-MM-dd) ke SQLite lokal (dd/MM/yyyy) ──
private fun convertTanggalLahirApiToLocal(tgl: String): String {
    return try {
        val cleaned = tgl.trim()
        // Handle invalid dates from API
        if (cleaned == "0000-00-00" || cleaned == "0000-00-00 00:00:00") {
            android.util.Log.w("SYNC_DATE", "Tanggal lahir invalid dari API: $tgl, menggunakan default")
            return "01/01/2020"  // Default fallback date
        }
        when {
            // Format API: yyyy-MM-dd atau yyyy-MM-dd HH:mm:ss
            cleaned.contains("-") && cleaned.substring(0, 4).toIntOrNull() != null -> {
                // Ambil bagian tanggal saja (tanpa waktu)
                val datePart = if (cleaned.length >= 10) cleaned.substring(0, 10) else cleaned
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    val year  = parts[0]
                    // Validate year is reasonable (not 0000)
                    if (year == "0000") {
                        android.util.Log.w("SYNC_DATE", "Tahun invalid (0000) dari API: $tgl")
                        return "01/01/2020"
                    }
                    val month = parts[1].padStart(2, '0')
                    val day   = parts[2].padStart(2, '0')
                    "$day/$month/$year"
                } else tgl
            }
            // Sudah format lokal
            cleaned.contains("/") -> cleaned
            else -> tgl
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_DATE", "Gagal konversi tanggal lahir API→Local: $tgl", e)
        "01/01/2020"  // Default fallback
    }
}

// ── Konversi tanggal pemeriksaan dari API (yyyy-MM-dd atau yyyy-MM-dd HH:mm:ss) ke SQLite lokal (dd/MM/yyyy) ──
private fun convertTanggalPemeriksaanApiToLocal(tgl: String): String {
    return try {
        val cleaned = tgl.trim()
        // Handle invalid dates from API
        if (cleaned == "0000-00-00" || cleaned == "0000-00-00 00:00:00") {
            android.util.Log.w("SYNC_DATE", "Tanggal pemeriksaan invalid dari API: $tgl")
            return "01/01/2020"
        }
        when {
            // Format API: yyyy-MM-dd atau yyyy-MM-dd HH:mm:ss
            cleaned.contains("-") && cleaned.substring(0, 4).toIntOrNull() != null -> {
                // Ambil 10 karakter pertama (yyyy-MM-dd) jika ada waktu, atau semua jika tidak
                val datePart = if (cleaned.length >= 10) cleaned.substring(0, 10) else cleaned
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    val year  = parts[0]
                    if (year == "0000") {
                        android.util.Log.w("SYNC_DATE", "Tahun invalid (0000) dari API: $tgl")
                        return "01/01/2020"
                    }
                    val month = parts[1].padStart(2, '0')
                    val day   = parts[2].padStart(2, '0')
                    "$day/$month/$year"
                } else tgl
            }
            // Sudah format lokal
            cleaned.contains("/") -> cleaned
            else -> tgl
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_DATE", "Gagal konversi tanggal pemeriksaan API→Local: $tgl", e)
        "01/01/2020"
    }
}

// ── Konversi tanggal vaksin dari API (yyyy-MM-dd atau yyyy-MM-dd HH:mm:ss) ke SQLite lokal (dd/MM/yyyy) ──
private fun convertTanggalVaksinApiToLocal(tgl: String): String {
    return try {
        val cleaned = tgl.trim()
        // Handle invalid dates from API
        if (cleaned == "0000-00-00" || cleaned == "0000-00-00 00:00:00") {
            android.util.Log.w("SYNC_DATE", "Tanggal vaksin invalid dari API: $tgl")
            return "01/01/2020"
        }
        when {
            // Format API: yyyy-MM-dd atau yyyy-MM-dd HH:mm:ss
            cleaned.contains("-") && cleaned.substring(0, 4).toIntOrNull() != null -> {
                val datePart = if (cleaned.length >= 10) cleaned.substring(0, 10) else cleaned
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    val year  = parts[0]
                    if (year == "0000") {
                        android.util.Log.w("SYNC_DATE", "Tahun invalid (0000) dari API: $tgl")
                        return "01/01/2020"
                    }
                    val month = parts[1].padStart(2, '0')
                    val day   = parts[2].padStart(2, '0')
                    "$day/$month/$year"
                } else tgl
            }
            // Sudah format lokal
            cleaned.contains("/") -> cleaned
            else -> tgl
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_DATE", "Gagal konversi tanggal vaksin API→Local: $tgl", e)
        "01/01/2020"
    }
}

// ── Simpan User Login ke SQLite (hanya 1 baris) ───────────────────────────────
private fun saveUserToLocal(context: Context, user: UserApi, password: String) {
    val db = DatabaseHelper(context).writableDatabase

    try {
        db.execSQL("PRAGMA foreign_keys = OFF")
        db.beginTransaction()

        db.delete(DatabaseHelper.TABLE_USERS, null, null)

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_USERS_ID,         user.id)
            put(DatabaseHelper.COL_USERS_NAMA,        user.nama)
            put(DatabaseHelper.COL_USERS_USERNAME,    user.username)
            put(DatabaseHelper.COL_USERS_PASSWORD,    password)
            put(DatabaseHelper.COL_USERS_ROLE,        user.role)
            put(DatabaseHelper.COL_USERS_POSYANDU_ID, user.posyanduId)
            put(DatabaseHelper.COL_USERS_CREATED_AT,  formatCreatedAt(System.currentTimeMillis()))
        }

        db.insertWithOnConflict(
            DatabaseHelper.TABLE_USERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        db.setTransactionSuccessful()
        android.util.Log.d("LOGIN_DEBUG", "User disimpan ke SQLite: ${user.username}")

    } finally {
        if (db.inTransaction()) db.endTransaction()
        db.execSQL("PRAGMA foreign_keys = ON")
    }
}

// ── Fetch Login (KADER & ORTU pakai fungsi yang sama) ─────────────────────────
suspend fun fetchLoginFromApi(
    username: String,
    password: String,
    role: String,
    context: Context
): UserApi? = withContext(Dispatchers.IO) {

    try {
        val apiUrl    = "https://myposyandu.gt.tc/api_posyandu/users.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val req1 = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/json")
            .build()

        val res1  = httpClient.newCall(req1).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()

        android.util.Log.d("LOGIN_DEBUG", "Body1: ${body1.take(200)}")

        if (body1.trimStart().startsWith("[")) {
            return@withContext parseUsers(body1, username, password, role, context)
        }

        val challenge = parseChallenge(body1, apiUrl)
        if (challenge == null) {
            android.util.Log.e("LOGIN_DEBUG", "Gagal parse challenge")
            return@withContext null
        }

        val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
        android.util.Log.d("LOGIN_DEBUG", "Cookie __test=$cookieValue")

        val host = apiUrl.toHttpUrl()
        setTestCookie("myposyandu.gt.tc", cookieValue)

        val redirectUrl =
            if (challenge.redirectUrl.startsWith("http")) challenge.redirectUrl
            else "https://myposyandu.gt.tc${challenge.redirectUrl}"

        val req2 = Request.Builder()
            .url(redirectUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Referer", apiUrl)
            .build()

        val res2  = httpClient.newCall(req2).execute()
        val body2 = res2.body?.string() ?: ""
        res2.close()

        android.util.Log.d("LOGIN_DEBUG", "Body2: ${body2.take(500)}")

        if (!body2.trimStart().startsWith("[") && !body2.trimStart().startsWith("{")) {
            android.util.Log.e("LOGIN_DEBUG", "Masih bukan JSON setelah solve challenge")
            return@withContext null
        }

        parseUsers(body2, username, password, role, context)

    } catch (e: Exception) {
        android.util.Log.e("LOGIN_DEBUG", "Exception: ${e.message}", e)
        null
    }
}

// ── Parse Users JSON ──────────────────────────────────────────────────────────
private fun parseUsers(
    json: String,
    username: String,
    password: String,
    role: String,
    context: Context
): UserApi? {

    val array = JSONArray(json)

    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)

        if (
            obj.getString("username") == username &&
            obj.getString("password") == password &&
            obj.getString("role")     == role
        ) {
            val user = UserApi(
                id         = obj.getString("id"),
                nama       = obj.getString("nama"),
                username   = obj.getString("username"),
                role       = obj.getString("role"),
                posyanduId = obj.getString("posyandu_id")
            )

            saveUserToLocal(context, user, password)

            return user
        }
    }

    return null
}

// ═══════════════════════════════════════════════════════════════════════════
//  FUNGSI: SYNC ORTU KE API
// ═══════════════════════════════════════════════════════════════════════════

suspend fun fetchAllUsersFromApi(context: Context): List<OrtuApi> = withContext(Dispatchers.IO) {
    val result = mutableListOf<OrtuApi>()
    try {
        val apiUrl = "https://myposyandu.gt.tc/api_posyandu/users.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val req1 = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        val res1 = httpClient.newCall(req1).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()

        if (body1.trimStart().startsWith("[")) {
            val array = JSONArray(body1)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("role") == "ortu") {
                    result.add(OrtuApi(
                        id = obj.getString("id"),
                        nama = obj.getString("nama"),
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        role = obj.getString("role"),
                        posyanduId = obj.getString("posyandu_id"),
                        createdAt = obj.optString("created_at", "")
                    ))
                }
            }
            return@withContext result
        }

        val challenge = parseChallenge(body1, apiUrl)
        if (challenge != null) {
            val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
            setTestCookie("myposyandu.gt.tc", cookieValue)

            val redirectUrl = if (challenge.redirectUrl.startsWith("http")) challenge.redirectUrl
            else "https://myposyandu.gt.tc${challenge.redirectUrl}"

            val req2 = Request.Builder()
                .url(redirectUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Referer", apiUrl)
                .build()

            val res2 = httpClient.newCall(req2).execute()
            val body2 = res2.body?.string() ?: ""
            res2.close()

            if (body2.trimStart().startsWith("[")) {
                val array = JSONArray(body2)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("role") == "ortu") {
                        result.add(OrtuApi(
                            id = obj.getString("id"),
                            nama = obj.getString("nama"),
                            username = obj.getString("username"),
                            password = obj.getString("password"),
                            role = obj.getString("role"),
                            posyanduId = obj.getString("posyandu_id"),
                            createdAt = obj.optString("created_at", "")
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_ORTU_API", "Gagal fetch users: ${e.message}", e)
    }
    return@withContext result
}

suspend fun insertOrtuToApi(ortu: OrtuApi, context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val apiUrl    = "https://myposyandu.gt.tc/api_posyandu/users.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val formBody = FormBody.Builder()
            .add("id",          ortu.id)
            .add("nama",        ortu.nama)
            .add("username",    ortu.username)
            .add("password",    ortu.password)
            .add("role",        ortu.role)
            .add("posyandu_id", ortu.posyanduId)
            .add("created_at",  ortu.createdAt)
            .build()

        val res1  = httpClient.newCall(
            Request.Builder().url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/json")
                .build()
        ).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()
        android.util.Log.d("SYNC_ORTU_API", "Step1 body: ${body1.take(100)}")

        if (!body1.trimStart().startsWith("[")) {
            val challenge = parseChallenge(body1, apiUrl)
            if (challenge == null) {
                android.util.Log.e("SYNC_ORTU_API", "Gagal parse challenge saat insert ortu")
                return@withContext false
            }

            val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
            android.util.Log.d("SYNC_ORTU_API", "Cookie __test=$cookieValue")

            setTestCookie("myposyandu.gt.tc", cookieValue)

            val redirectUrl = if (challenge.redirectUrl.startsWith("http"))
                challenge.redirectUrl
            else "https://myposyandu.gt.tc${challenge.redirectUrl}"

            val res2 = httpClient.newCall(
                Request.Builder().url(redirectUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/json")
                    .header("Referer", apiUrl)
                    .build()
            ).execute()
            val body2 = res2.body?.string() ?: ""
            res2.close()
            android.util.Log.d("SYNC_ORTU_API", "Step2 (redirect) body: ${body2.take(100)}")
        }

        val postRequest = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Referer", apiUrl)
            .post(formBody)
            .build()

        val postResponse = httpClient.newCall(postRequest).execute()
        val postBody = postResponse.body?.string() ?: ""
        postResponse.close()

        android.util.Log.d("SYNC_ORTU_API", "Insert response: code=${postResponse.code}, body=$postBody")

        return@withContext try {
            val json = JSONObject(postBody)
            val success = json.optBoolean("success", false)
            val exists = json.optBoolean("exists", false)
            success || exists
        } catch (e: Exception) {
            postResponse.isSuccessful
        }

    } catch (e: Exception) {
        android.util.Log.e("SYNC_ORTU_API", "Gagal insert ortu: ${e.message}", e)
        return@withContext false
    }
}

suspend fun syncOrtuToApi(context: Context): SyncResult = withContext(Dispatchers.IO) {
    try {
        val ortuRepo = OrtuRepository(context)
        val allLocalOrtu = ortuRepo.getAllOrtu()

        val apiOrtuList = fetchAllUsersFromApi(context)
        val apiOrtuIds = apiOrtuList.map { it.id }.toSet()

        var inserted = 0
        var skipped = 0
        var failed = 0

        for (localOrtu in allLocalOrtu) {
            if (apiOrtuIds.contains(localOrtu.id)) {
                skipped++
                continue
            }

            val ortuApi = OrtuApi(
                id = localOrtu.id,
                nama = localOrtu.nama,
                username = localOrtu.username,
                password = localOrtu.password,
                role = "ortu",
                posyanduId = localOrtu.posyanduId,
                createdAt = formatCreatedAt(System.currentTimeMillis())
            )

            val success = insertOrtuToApi(ortuApi, context)
            if (success) inserted++ else failed++
        }

        return@withContext SyncResult(
            success = failed == 0,
            inserted = inserted,
            skipped = skipped,
            failed = failed,
            message = "Sync: $inserted ditambahkan, $skipped dilewati, $failed gagal"
        )

    } catch (e: Exception) {
        return@withContext SyncResult(
            success = false,
            inserted = 0,
            skipped = 0,
            failed = 0,
            message = "Sync gagal: ${e.message}"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  FUNGSI: SYNC ANAK KE API
// ═══════════════════════════════════════════════════════════════════════════

suspend fun fetchAllAnakFromApi(context: Context): List<AnakApi> = withContext(Dispatchers.IO) {
    val result = mutableListOf<AnakApi>()
    try {
        val apiUrl = "https://myposyandu.gt.tc/api_posyandu/anak.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val req1 = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        val res1 = httpClient.newCall(req1).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()

        android.util.Log.d("SYNC_ANAK_API", "Body1: ${body1.take(200)}")

        if (body1.trimStart().startsWith("[")) {
            val array = JSONArray(body1)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(AnakApi(
                    id = obj.getString("id"),
                    nama = obj.getString("nama"),
                    tanggalLahir = obj.getString("tanggal_lahir"),
                    jenisKelamin = obj.getString("jenis_kelamin"),
                    ortuId = obj.getString("ortu_id"),
                    posyanduId = obj.getString("posyandu_id"),
                    createdAt = obj.optString("created_at", "")
                ))
            }
            return@withContext result
        }

        val challenge = parseChallenge(body1, apiUrl)
        if (challenge == null) {
            android.util.Log.e("SYNC_ANAK_API", "Gagal parse challenge")
            return@withContext result
        }

        val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
        android.util.Log.d("SYNC_ANAK_API", "Cookie __test=$cookieValue")

        setTestCookie("myposyandu.gt.tc", cookieValue)

        val redirectUrl = if (challenge.redirectUrl.startsWith("http")) challenge.redirectUrl
        else "https://myposyandu.gt.tc${challenge.redirectUrl}"

        val req2 = Request.Builder()
            .url(redirectUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Referer", apiUrl)
            .build()

        val res2 = httpClient.newCall(req2).execute()
        val body2 = res2.body?.string() ?: ""
        res2.close()

        android.util.Log.d("SYNC_ANAK_API", "Body2: ${body2.take(500)}")

        if (body2.trimStart().startsWith("[")) {
            val array = JSONArray(body2)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(AnakApi(
                    id = obj.getString("id"),
                    nama = obj.getString("nama"),
                    tanggalLahir = obj.getString("tanggal_lahir"),
                    jenisKelamin = obj.getString("jenis_kelamin"),
                    ortuId = obj.getString("ortu_id"),
                    posyanduId = obj.getString("posyandu_id"),
                    createdAt = obj.optString("created_at", "")
                ))
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_ANAK_API", "Gagal fetch anak: ${e.message}", e)
    }
    return@withContext result
}

suspend fun insertAnakToApi(anak: AnakApi, context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val apiUrl    = "https://myposyandu.gt.tc/api_posyandu/anak.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val tglLahirApi = convertTanggalLahir(anak.tanggalLahir)
        android.util.Log.d("SYNC_ANAK_API", "Konversi tanggal: ${anak.tanggalLahir} -> $tglLahirApi")

        val formBody = FormBody.Builder()
            .add("id",            anak.id)
            .add("nama",          anak.nama)
            .add("tanggal_lahir", tglLahirApi)
            .add("jenis_kelamin", anak.jenisKelamin)
            .add("ortu_id",       anak.ortuId)
            .add("posyandu_id",   anak.posyanduId)
            .add("created_at",    anak.createdAt)
            .build()

        val res1  = httpClient.newCall(
            Request.Builder().url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/json")
                .build()
        ).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()
        android.util.Log.d("SYNC_ANAK_API", "Step1 body: ${body1.take(100)}")

        if (!body1.trimStart().startsWith("[")) {
            val challenge = parseChallenge(body1, apiUrl)
            if (challenge == null) {
                android.util.Log.e("SYNC_ANAK_API", "Gagal parse challenge saat insert")
                return@withContext false
            }

            val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
            android.util.Log.d("SYNC_ANAK_API", "Cookie __test=$cookieValue")

            setTestCookie("myposyandu.gt.tc", cookieValue)

            val redirectUrl = if (challenge.redirectUrl.startsWith("http"))
                challenge.redirectUrl
            else "https://myposyandu.gt.tc${challenge.redirectUrl}"

            val res2 = httpClient.newCall(
                Request.Builder().url(redirectUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/json")
                    .header("Referer", apiUrl)
                    .build()
            ).execute()
            val body2 = res2.body?.string() ?: ""
            res2.close()
            android.util.Log.d("SYNC_ANAK_API", "Step2 (redirect) body: ${body2.take(100)}")
        }

        val postResponse = httpClient.newCall(
            Request.Builder().url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Referer", apiUrl)
                .post(formBody)
                .build()
        ).execute()
        val postBody = postResponse.body?.string() ?: ""
        postResponse.close()

        android.util.Log.d("SYNC_ANAK_API",
            "Insert response: code=${postResponse.code}, body=$postBody")

        return@withContext try {
            val json    = JSONObject(postBody)
            val success = json.optBoolean("success", false)
            val exists  = json.optBoolean("exists",  false)
            success || exists
        } catch (e: Exception) {
            postResponse.isSuccessful
        }

    } catch (e: Exception) {
        android.util.Log.e("SYNC_ANAK_API", "Gagal insert anak: ${e.message}", e)
        return@withContext false
    }
}

suspend fun syncAnakToApi(context: Context): SyncResult = withContext(Dispatchers.IO) {
    try {
        val dbFix = DatabaseHelper(context).writableDatabase
        val ortuCountCursor = dbFix.rawQuery(
            "SELECT id FROM ${DatabaseHelper.TABLE_ORTU} WHERE id != '0' AND id != ''", null
        )
        val validOrtuList = mutableListOf<String>()
        while (ortuCountCursor.moveToNext()) {
            validOrtuList.add(ortuCountCursor.getString(0))
        }
        ortuCountCursor.close()

        if (validOrtuList.size == 1) {
            dbFix.execSQL(
                "UPDATE ${DatabaseHelper.TABLE_ANAK} " +
                        "SET ${DatabaseHelper.COL_ANAK_ORTU_ID} = ? " +
                        "WHERE (${DatabaseHelper.COL_ANAK_ORTU_ID} = '0' " +
                        "    OR ${DatabaseHelper.COL_ANAK_ORTU_ID} = '' " +
                        "    OR ${DatabaseHelper.COL_ANAK_ORTU_ID} IS NULL)",
                arrayOf(validOrtuList[0])
            )
            android.util.Log.d("SYNC_ANAK_API", "Auto-fixed ortu_id → ${validOrtuList[0]}")
        } else if (validOrtuList.size > 1) {
            android.util.Log.w("SYNC_ANAK_API",
                "Ada ${validOrtuList.size} ortu, tidak bisa auto-fix ortu_id. " +
                        "Anak dengan ortu_id='0' akan di-skip saat sync.")
        }
        dbFix.close()

        val db = DatabaseHelper(context).readableDatabase
        val allLocalAnak = mutableListOf<AnakApi>()

        val cursor = db.rawQuery(
            """
            SELECT 
                ${DatabaseHelper.COL_ANAK_ID},
                ${DatabaseHelper.COL_ANAK_NAMA},
                ${DatabaseHelper.COL_ANAK_TGL_LAHIR},
                ${DatabaseHelper.COL_ANAK_JENIS_KELAMIN},
                ${DatabaseHelper.COL_ANAK_ORTU_ID},
                ${DatabaseHelper.COL_ANAK_POSYANDU_ID},
                ${DatabaseHelper.COL_ANAK_CREATED_AT}
            FROM ${DatabaseHelper.TABLE_ANAK}
            ORDER BY ${DatabaseHelper.COL_ANAK_CREATED_AT} DESC
            """.trimIndent(),
            null
        )

        while (cursor.moveToNext()) {
            allLocalAnak.add(AnakApi(
                id = cursor.getString(0) ?: "",
                nama = cursor.getString(1) ?: "",
                tanggalLahir = cursor.getString(2) ?: "",
                jenisKelamin = cursor.getString(3) ?: "",
                ortuId = cursor.getString(4) ?: "",
                posyanduId = cursor.getString(5) ?: "",
                createdAt = cursor.getString(6) ?: formatCreatedAt(System.currentTimeMillis())
            ))
        }
        cursor.close()
        db.close()

        android.util.Log.d("SYNC_ANAK_API", "Total anak lokal: ${allLocalAnak.size}")

        val apiAnakList = fetchAllAnakFromApi(context)
        val apiAnakIds = apiAnakList.map { it.id }.toSet()

        android.util.Log.d("SYNC_ANAK_API", "Total anak di API: ${apiAnakList.size}")
        android.util.Log.d("SYNC_ANAK_API", "ID anak di API: $apiAnakIds")

        var inserted = 0
        var skipped = 0
        var failed = 0

        for ((index, localAnak) in allLocalAnak.withIndex()) {
            android.util.Log.d("SYNC_ANAK_API", "[$index/${allLocalAnak.size}] Processing: ${localAnak.nama} (id=${localAnak.id}, ortuId=${localAnak.ortuId})")

            if (localAnak.ortuId.isBlank() || localAnak.ortuId == "0") {
                android.util.Log.w("SYNC_ANAK_API", "  -> SKIP: ortu_id tidak valid ('${localAnak.ortuId}')")
                skipped++
                continue
            }

            if (apiAnakIds.contains(localAnak.id)) {
                android.util.Log.d("SYNC_ANAK_API", "  -> SKIP: sudah ada di API")
                skipped++
                continue
            }

            if (index > 0) {
                android.util.Log.d("SYNC_ANAK_API", "  -> Delay 1.5 detik...")
                kotlinx.coroutines.delay(1500)
            }

            val success = insertAnakToApi(localAnak, context)
            if (success) {
                android.util.Log.d("SYNC_ANAK_API", "  -> SUCCESS")
                inserted++
            } else {
                android.util.Log.e("SYNC_ANAK_API", "  -> FAILED")
                failed++
            }
        }

        return@withContext SyncResult(
            success = failed == 0,
            inserted = inserted,
            skipped = skipped,
            failed = failed,
            message = "Sync anak: $inserted ditambahkan, $skipped dilewati, $failed gagal"
        )

    } catch (e: Exception) {
        android.util.Log.e("SYNC_ANAK_API", "Sync anak gagal: ${e.message}", e)
        return@withContext SyncResult(
            success = false,
            inserted = 0,
            skipped = 0,
            failed = 0,
            message = "Sync anak gagal: ${e.message}"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  FUNGSI: SYNC PEMERIKSAAN KE API
// ═══════════════════════════════════════════════════════════════════════════

suspend fun fetchAllPemeriksaanFromApi(context: Context): List<PemeriksaanApi> = withContext(Dispatchers.IO) {
    val result = mutableListOf<PemeriksaanApi>()
    try {
        val apiUrl = "https://myposyandu.gt.tc/api_posyandu/pemeriksaan.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val req1 = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        val res1 = httpClient.newCall(req1).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()

        android.util.Log.d("SYNC_PMRK_API", "Body1: ${body1.take(200)}")

        if (body1.trimStart().startsWith("[")) {
            val array = JSONArray(body1)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(PemeriksaanApi(
                    id            = obj.getString("id"),
                    anakId        = obj.getString("anak_id"),
                    kaderId       = obj.getString("kader_id"),
                    tanggal       = obj.getString("tanggal"),
                    beratBadan    = obj.getDouble("berat_badan"),
                    tinggiBadan   = obj.getDouble("tinggi_badan"),
                    lingkarKepala = obj.optDouble("lingkar_kepala", 0.0),
                    lingkarLengan = obj.optDouble("lingkar_lengan", 0.0),
                    zScoreTbu     = obj.optDouble("z_score_tbu", 0.0),
                    zScoreBbu     = obj.optDouble("z_score_bbu", 0.0),
                    statusGizi    = obj.optString("status_gizi", "normal"),
                    catatan       = obj.optString("catatan", "")
                ))
            }
            return@withContext result
        }

        val challenge = parseChallenge(body1, apiUrl)
        if (challenge == null) {
            android.util.Log.e("SYNC_PMRK_API", "Gagal parse challenge")
            return@withContext result
        }

        val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
        android.util.Log.d("SYNC_PMRK_API", "Cookie __test=$cookieValue")

        setTestCookie("myposyandu.gt.tc", cookieValue)

        val redirectUrl = if (challenge.redirectUrl.startsWith("http")) challenge.redirectUrl
        else "https://myposyandu.gt.tc${challenge.redirectUrl}"

        val req2 = Request.Builder()
            .url(redirectUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Referer", apiUrl)
            .build()

        val res2 = httpClient.newCall(req2).execute()
        val body2 = res2.body?.string() ?: ""
        res2.close()

        android.util.Log.d("SYNC_PMRK_API", "Body2: ${body2.take(500)}")

        if (body2.trimStart().startsWith("[")) {
            val array = JSONArray(body2)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(PemeriksaanApi(
                    id            = obj.getString("id"),
                    anakId        = obj.getString("anak_id"),
                    kaderId       = obj.getString("kader_id"),
                    tanggal       = obj.getString("tanggal"),
                    beratBadan    = obj.getDouble("berat_badan"),
                    tinggiBadan   = obj.getDouble("tinggi_badan"),
                    lingkarKepala = obj.optDouble("lingkar_kepala", 0.0),
                    lingkarLengan = obj.optDouble("lingkar_lengan", 0.0),
                    zScoreTbu     = obj.optDouble("z_score_tbu", 0.0),
                    zScoreBbu     = obj.optDouble("z_score_bbu", 0.0),
                    statusGizi    = obj.optString("status_gizi", "normal"),
                    catatan       = obj.optString("catatan", "")
                ))
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_PMRK_API", "Gagal fetch pemeriksaan: ${e.message}", e)
    }
    return@withContext result
}

suspend fun insertPemeriksaanToApi(pemeriksaan: PemeriksaanApi, context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val apiUrl    = "https://myposyandu.gt.tc/api_posyandu/pemeriksaan.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val tglApi = convertTanggalPemeriksaan(pemeriksaan.tanggal)
        android.util.Log.d("SYNC_PMRK_API", "Konversi tanggal: ${pemeriksaan.tanggal} -> $tglApi")

        val formBody = FormBody.Builder()
            .add("id",             pemeriksaan.id)
            .add("anak_id",        pemeriksaan.anakId)
            .add("kader_id",       pemeriksaan.kaderId)
            .add("tanggal",        tglApi)
            .add("berat_badan",    pemeriksaan.beratBadan.toString())
            .add("tinggi_badan",   pemeriksaan.tinggiBadan.toString())
            .add("lingkar_kepala", pemeriksaan.lingkarKepala.toString())
            .add("lingkar_lengan", pemeriksaan.lingkarLengan.toString())
            .add("z_score_tbu",    pemeriksaan.zScoreTbu.toString())
            .add("z_score_bbu",    pemeriksaan.zScoreBbu.toString())
            .add("status_gizi",    pemeriksaan.statusGizi)
            .add("catatan",        pemeriksaan.catatan)
            .build()

        val res1  = httpClient.newCall(
            Request.Builder().url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/json")
                .build()
        ).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()
        android.util.Log.d("SYNC_PMRK_API", "Step1 body: ${body1.take(100)}")

        if (!body1.trimStart().startsWith("[")) {
            val challenge = parseChallenge(body1, apiUrl)
            if (challenge == null) {
                android.util.Log.e("SYNC_PMRK_API", "Gagal parse challenge saat insert")
                return@withContext false
            }

            val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
            android.util.Log.d("SYNC_PMRK_API", "Cookie __test=$cookieValue")

            setTestCookie("myposyandu.gt.tc", cookieValue)

            val redirectUrl = if (challenge.redirectUrl.startsWith("http"))
                challenge.redirectUrl
            else "https://myposyandu.gt.tc${challenge.redirectUrl}"

            val res2 = httpClient.newCall(
                Request.Builder().url(redirectUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/json")
                    .header("Referer", apiUrl)
                    .build()
            ).execute()
            val body2 = res2.body?.string() ?: ""
            res2.close()
            android.util.Log.d("SYNC_PMRK_API", "Step2 (redirect) body: ${body2.take(100)}")
        }

        val postResponse = httpClient.newCall(
            Request.Builder().url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Referer", apiUrl)
                .post(formBody)
                .build()
        ).execute()
        val postBody = postResponse.body?.string() ?: ""
        postResponse.close()

        android.util.Log.d("SYNC_PMRK_API",
            "Insert response: code=${postResponse.code}, body=$postBody")

        return@withContext try {
            val json    = JSONObject(postBody)
            val success = json.optBoolean("success", false)
            val exists  = json.optBoolean("exists",  false)
            success || exists
        } catch (e: Exception) {
            postResponse.isSuccessful
        }

    } catch (e: Exception) {
        android.util.Log.e("SYNC_PMRK_API", "Gagal insert pemeriksaan: ${e.message}", e)
        return@withContext false
    }
}

suspend fun syncPemeriksaanToApi(context: Context): SyncResult = withContext(Dispatchers.IO) {
    try {
        val repo = PemeriksaanRepository(context)
        val allLocalPemeriksaan = repo.getAllPemeriksaan()

        android.util.Log.d("SYNC_PMRK_API", "Total pemeriksaan lokal: ${allLocalPemeriksaan.size}")

        val apiPemeriksaanList = fetchAllPemeriksaanFromApi(context)
        val apiPemeriksaanIds = apiPemeriksaanList.map { it.id }.toSet()

        android.util.Log.d("SYNC_PMRK_API", "Total pemeriksaan di API: ${apiPemeriksaanList.size}")

        var inserted = 0
        var skipped = 0
        var failed = 0

        for ((index, localPmrk) in allLocalPemeriksaan.withIndex()) {
            android.util.Log.d("SYNC_PMRK_API", "[$index/${allLocalPemeriksaan.size}] Processing: anakId=${localPmrk.anakId}, tgl=${localPmrk.tgl}")

            if (apiPemeriksaanIds.contains(localPmrk.id)) {
                android.util.Log.d("SYNC_PMRK_API", "  -> SKIP: sudah ada di API")
                skipped++
                continue
            }

            if (index > 0) {
                android.util.Log.d("SYNC_PMRK_API", "  -> Delay 1.5 detik...")
                kotlinx.coroutines.delay(1500)
            }

            val pmrkApi = PemeriksaanApi(
                id            = localPmrk.id,
                anakId        = localPmrk.anakId,
                kaderId       = localPmrk.kaderId,
                tanggal       = localPmrk.tgl,
                beratBadan    = localPmrk.bb,
                tinggiBadan   = localPmrk.tb,
                lingkarKepala = localPmrk.lk,
                lingkarLengan = localPmrk.ll,
                zScoreTbu     = localPmrk.zScoreTbu,
                zScoreBbu     = localPmrk.zScoreBbu,
                statusGizi    = localPmrk.statusGizi,
                catatan       = localPmrk.catatan
            )

            val success = insertPemeriksaanToApi(pmrkApi, context)
            if (success) {
                android.util.Log.d("SYNC_PMRK_API", "  -> SUCCESS")
                inserted++
            } else {
                android.util.Log.e("SYNC_PMRK_API", "  -> FAILED")
                failed++
            }
        }

        return@withContext SyncResult(
            success = failed == 0,
            inserted = inserted,
            skipped = skipped,
            failed = failed,
            message = "Sync pemeriksaan: $inserted ditambahkan, $skipped dilewati, $failed gagal"
        )

    } catch (e: Exception) {
        android.util.Log.e("SYNC_PMRK_API", "Sync pemeriksaan gagal: ${e.message}", e)
        return@withContext SyncResult(
            success = false,
            inserted = 0,
            skipped = 0,
            failed = 0,
            message = "Sync pemeriksaan gagal: ${e.message}"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  DATA CLASS: VAKSIN RIWAYAT API
// ═══════════════════════════════════════════════════════════════════════════

data class VaksinRiwayatApi(
    val id               : String,
    val anakId           : String,
    val vaksinRefId      : String,
    val kaderId          : String,
    val tanggalPemberian : String,
    val lokasi           : String
)

// ═══════════════════════════════════════════════════════════════════════════
//  FUNGSI: SYNC VAKSIN RIWAYAT KE API
// ═══════════════════════════════════════════════════════════════════════════

suspend fun fetchAllVaksinRiwayatFromApi(context: Context): List<VaksinRiwayatApi> = withContext(Dispatchers.IO) {
    val result = mutableListOf<VaksinRiwayatApi>()
    try {
        val apiUrl = "https://myposyandu.gt.tc/api_posyandu/vaksin_riwayat.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val req1 = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        val res1 = httpClient.newCall(req1).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()

        android.util.Log.d("SYNC_VAKSIN_API", "Body1: ${body1.take(200)}")

        if (body1.trimStart().startsWith("[")) {
            val array = JSONArray(body1)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(VaksinRiwayatApi(
                    id               = obj.getString("id"),
                    anakId           = obj.getString("anak_id"),
                    vaksinRefId      = obj.getString("vaksin_ref_id"),
                    kaderId          = obj.getString("kader_id"),
                    tanggalPemberian = obj.getString("tanggal_pemberian"),
                    lokasi           = obj.optString("lokasi", "")
                ))
            }
            return@withContext result
        }

        val challenge = parseChallenge(body1, apiUrl)
        if (challenge == null) {
            android.util.Log.e("SYNC_VAKSIN_API", "Gagal parse challenge")
            return@withContext result
        }

        val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
        android.util.Log.d("SYNC_VAKSIN_API", "Cookie __test=$cookieValue")

        setTestCookie("myposyandu.gt.tc", cookieValue)

        val redirectUrl = if (challenge.redirectUrl.startsWith("http")) challenge.redirectUrl
        else "https://myposyandu.gt.tc${challenge.redirectUrl}"

        val req2 = Request.Builder()
            .url(redirectUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Referer", apiUrl)
            .build()

        val res2 = httpClient.newCall(req2).execute()
        val body2 = res2.body?.string() ?: ""
        res2.close()

        android.util.Log.d("SYNC_VAKSIN_API", "Body2: ${body2.take(500)}")

        if (body2.trimStart().startsWith("[")) {
            val array = JSONArray(body2)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(VaksinRiwayatApi(
                    id               = obj.getString("id"),
                    anakId           = obj.getString("anak_id"),
                    vaksinRefId      = obj.getString("vaksin_ref_id"),
                    kaderId          = obj.getString("kader_id"),
                    tanggalPemberian = obj.getString("tanggal_pemberian"),
                    lokasi           = obj.optString("lokasi", "")
                ))
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SYNC_VAKSIN_API", "Gagal fetch vaksin riwayat: ${e.message}", e)
    }
    return@withContext result
}

suspend fun insertVaksinRiwayatToApi(vaksin: VaksinRiwayatApi, context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val apiUrl    = "https://myposyandu.gt.tc/api_posyandu/vaksin_riwayat.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        val tglApi = convertTanggalVaksin(vaksin.tanggalPemberian)
        android.util.Log.d("SYNC_VAKSIN_API", "Konversi tanggal: ${vaksin.tanggalPemberian} -> $tglApi")

        val formBody = FormBody.Builder()
            .add("id",                vaksin.id)
            .add("anak_id",           vaksin.anakId)
            .add("vaksin_ref_id",     vaksin.vaksinRefId)
            .add("kader_id",          vaksin.kaderId)
            .add("tanggal_pemberian", tglApi)
            .add("lokasi",            vaksin.lokasi)
            .build()

        val res1  = httpClient.newCall(
            Request.Builder().url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/json")
                .build()
        ).execute()
        val body1 = res1.body?.string() ?: ""
        res1.close()
        android.util.Log.d("SYNC_VAKSIN_API", "Step1 body: ${body1.take(100)}")

        if (!body1.trimStart().startsWith("[")) {
            val challenge = parseChallenge(body1, apiUrl)
            if (challenge == null) {
                android.util.Log.e("SYNC_VAKSIN_API", "Gagal parse challenge saat insert")
                return@withContext false
            }

            val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
            android.util.Log.d("SYNC_VAKSIN_API", "Cookie __test=$cookieValue")

            setTestCookie("myposyandu.gt.tc", cookieValue)

            val redirectUrl = if (challenge.redirectUrl.startsWith("http"))
                challenge.redirectUrl
            else "https://myposyandu.gt.tc${challenge.redirectUrl}"

            val res2 = httpClient.newCall(
                Request.Builder().url(redirectUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/json")
                    .header("Referer", apiUrl)
                    .build()
            ).execute()
            val body2 = res2.body?.string() ?: ""
            res2.close()
            android.util.Log.d("SYNC_VAKSIN_API", "Step2 (redirect) body: ${body2.take(100)}")
        }

        val postResponse = httpClient.newCall(
            Request.Builder().url(apiUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Referer", apiUrl)
                .post(formBody)
                .build()
        ).execute()
        val postBody = postResponse.body?.string() ?: ""
        postResponse.close()

        android.util.Log.d("SYNC_VAKSIN_API",
            "Insert response: code=${postResponse.code}, body=$postBody")

        return@withContext try {
            val json    = JSONObject(postBody)
            val success = json.optBoolean("success", false)
            val exists  = json.optBoolean("exists",  false)
            success || exists
        } catch (e: Exception) {
            postResponse.isSuccessful
        }

    } catch (e: Exception) {
        android.util.Log.e("SYNC_VAKSIN_API", "Gagal insert vaksin riwayat: ${e.message}", e)
        return@withContext false
    }
}

suspend fun syncVaksinRiwayatToApi(context: Context): SyncResult = withContext(Dispatchers.IO) {
    try {
        val repo = VaksinRiwayatRepository(context)
        val allLocalVaksin = repo.getAllVaksinRiwayat()

        android.util.Log.d("SYNC_VAKSIN_API", "Total vaksin riwayat lokal: ${allLocalVaksin.size}")

        val apiVaksinList = fetchAllVaksinRiwayatFromApi(context)
        val apiVaksinIds = apiVaksinList.map { it.id }.toSet()

        android.util.Log.d("SYNC_VAKSIN_API", "Total vaksin di API: ${apiVaksinList.size}")

        var inserted = 0
        var skipped = 0
        var failed = 0

        for ((index, localVaksin) in allLocalVaksin.withIndex()) {
            android.util.Log.d("SYNC_VAKSIN_API", "[$index/${allLocalVaksin.size}] Processing: anakId=${localVaksin.anakId}, vaksinRefId=${localVaksin.vaksinRefId}")

            if (apiVaksinIds.contains(localVaksin.id)) {
                android.util.Log.d("SYNC_VAKSIN_API", "  -> SKIP: sudah ada di API")
                skipped++
                continue
            }

            if (index > 0) {
                android.util.Log.d("SYNC_VAKSIN_API", "  -> Delay 1.5 detik...")
                kotlinx.coroutines.delay(1500)
            }

            val vaksinApi = VaksinRiwayatApi(
                id               = localVaksin.id,
                anakId           = localVaksin.anakId,
                vaksinRefId      = localVaksin.vaksinRefId,
                kaderId          = localVaksin.kaderId,
                tanggalPemberian = localVaksin.tanggalPemberian,
                lokasi           = localVaksin.lokasi
            )

            val success = insertVaksinRiwayatToApi(vaksinApi, context)
            if (success) {
                android.util.Log.d("SYNC_VAKSIN_API", "  -> SUCCESS")
                inserted++
            } else {
                android.util.Log.e("SYNC_VAKSIN_API", "  -> FAILED")
                failed++
            }
        }

        return@withContext SyncResult(
            success = failed == 0,
            inserted = inserted,
            skipped = skipped,
            failed = failed,
            message = "Sync vaksin: $inserted ditambahkan, $skipped dilewati, $failed gagal"
        )

    } catch (e: Exception) {
        android.util.Log.e("SYNC_VAKSIN_API", "Sync vaksin gagal: ${e.message}", e)
        return@withContext SyncResult(
            success = false,
            inserted = 0,
            skipped = 0,
            failed = 0,
            message = "Sync vaksin gagal: ${e.message}"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  FUNGSI: SYNC SEMUA DATA DARI API KE SQLITE LOKAL (DIPANGGIL SETELAH LOGIN)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Sync SEMUA data dari API ke SQLite lokal.
 * Dipanggil setelah login berhasil, baik untuk kader maupun orang tua.
 */
suspend fun syncAllDataFromApi(context: Context): String = withContext(Dispatchers.IO) {
    val results = mutableListOf<String>()

    try {
        // 1. Sync Users (Ortu) → tabel ortu
        results.add(syncOrtuFromApiToLocal(context))

        // 2. Sync Anak → tabel anak
        results.add(syncAnakFromApiToLocal(context))

        // 3. Sync Pemeriksaan → tabel pemeriksaan
        results.add(syncPemeriksaanFromApiToLocal(context))

        // 4. Sync Vaksin Riwayat → tabel vaksin_riwayat
        results.add(syncVaksinRiwayatFromApiToLocal(context))

        // 5. Sync Menu Sehat → tabel menu_kategori & menu_sehat
        val menuRepo = MenuRepository(context)
        val menuSyncSuccess = runBlocking { menuRepo.syncMenuData() }
        results.add("Menu: ${if (menuSyncSuccess) "Success" else "Failed"}")

        return@withContext results.joinToString(" | ")
    } catch (e: Exception) {
        android.util.Log.e("SYNC_ALL", "Gagal sync all data: ${e.message}", e)
        return@withContext "Sync gagal: ${e.message}"
    }
}

// ── Sync Ortu dari API ke SQLite lokal ─────────────────────────────────────
private fun syncOrtuFromApiToLocal(context: Context): String {
    return try {
        val apiOrtuList = runBlocking { fetchAllUsersFromApi(context) }
        val db = DatabaseHelper(context).writableDatabase

        var inserted = 0
        var skipped = 0

        for (ortu in apiOrtuList) {
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ORTU_ID} FROM ${DatabaseHelper.TABLE_ORTU} WHERE ${DatabaseHelper.COL_ORTU_ID} = ? LIMIT 1",
                arrayOf(ortu.id)
            )
            val exists = cursor.moveToFirst()
            cursor.close()

            if (exists) {
                skipped++
                continue
            }

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_ORTU_ID,          ortu.id)
                put(DatabaseHelper.COL_ORTU_NAMA,        ortu.nama)
                put(DatabaseHelper.COL_ORTU_USERNAME,    ortu.username)
                put(DatabaseHelper.COL_ORTU_PASSWORD,    ortu.password)
                put(DatabaseHelper.COL_ORTU_ROLE,        ortu.role)
                put(DatabaseHelper.COL_ORTU_POSYANDU_ID, ortu.posyanduId)
                put(DatabaseHelper.COL_ORTU_CREATED_AT,  ortu.createdAt)
            }

            db.insertWithOnConflict(
                DatabaseHelper.TABLE_ORTU,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            inserted++
        }

        db.close()
        "Ortu: $inserted inserted, $skipped skipped"
    } catch (e: Exception) {
        android.util.Log.e("SYNC_ORTU_LOCAL", "Error: ${e.message}", e)
        "Ortu sync error: ${e.message}"
    }
}

// ── Sync Anak dari API ke SQLite lokal ─────────────────────────────────────
private fun syncAnakFromApiToLocal(context: Context): String {
    return try {
        val apiAnakList = runBlocking { fetchAllAnakFromApi(context) }
        val db = DatabaseHelper(context).writableDatabase

        var inserted = 0
        var skipped = 0

        for (anak in apiAnakList) {
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ANAK_ID} FROM ${DatabaseHelper.TABLE_ANAK} WHERE ${DatabaseHelper.COL_ANAK_ID} = ? LIMIT 1",
                arrayOf(anak.id)
            )
            val exists = cursor.moveToFirst()
            cursor.close()

            if (exists) {
                skipped++
                continue
            }

            val tglLahir = convertTanggalLahirApiToLocal(anak.tanggalLahir)

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_ANAK_ID,            anak.id)
                put(DatabaseHelper.COL_ANAK_NAMA,          anak.nama)
                put(DatabaseHelper.COL_ANAK_TGL_LAHIR,     tglLahir)
                put(DatabaseHelper.COL_ANAK_JENIS_KELAMIN, anak.jenisKelamin)
                put(DatabaseHelper.COL_ANAK_ORTU_ID,       anak.ortuId)
                put(DatabaseHelper.COL_ANAK_POSYANDU_ID,   anak.posyanduId)
                put(DatabaseHelper.COL_ANAK_CREATED_AT,     anak.createdAt)
            }

            db.insertWithOnConflict(
                DatabaseHelper.TABLE_ANAK,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            inserted++
        }

        db.close()
        "Anak: $inserted inserted, $skipped skipped"
    } catch (e: Exception) {
        android.util.Log.e("SYNC_ANAK_LOCAL", "Error: ${e.message}", e)
        "Anak sync error: ${e.message}"
    }
}

// ── Sync Pemeriksaan dari API ke SQLite lokal ─────────────────────────────
private fun syncPemeriksaanFromApiToLocal(context: Context): String {
    return try {
        val apiPemeriksaanList = runBlocking { fetchAllPemeriksaanFromApi(context) }
        val db = DatabaseHelper(context).writableDatabase

        // ✅ FIX 1: Matikan foreign key check agar kader_id/anak_id yang belum ada
        //           tidak memblokir insert
        db.execSQL("PRAGMA foreign_keys = OFF")

        var inserted = 0
        var skipped = 0

        for (pmrk in apiPemeriksaanList) {
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_PMRK_ID} FROM ${DatabaseHelper.TABLE_PEMERIKSAAN} WHERE ${DatabaseHelper.COL_PMRK_ID} = ? LIMIT 1",
                arrayOf(pmrk.id)
            )
            val exists = cursor.moveToFirst()
            cursor.close()

            if (exists) { skipped++; continue }

            val tgl = convertTanggalPemeriksaanApiToLocal(pmrk.tanggal)

            // ✅ FIX 2: Normalisasi status_gizi agar lolos CHECK constraint
            val validStatusGizi = setOf("normal","gizi_kurang","gizi_buruk","gizi_lebih","obesitas")
            val statusGizi = if (pmrk.statusGizi.lowercase().trim() in validStatusGizi) {
                pmrk.statusGizi.lowercase().trim()
            } else {
                "normal" // fallback default
            }

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_PMRK_ID,          pmrk.id)
                put(DatabaseHelper.COL_PMRK_ANAK_ID,     pmrk.anakId)
                put(DatabaseHelper.COL_PMRK_KADER_ID,    pmrk.kaderId)
                put(DatabaseHelper.COL_PMRK_TGL,         tgl)
                put(DatabaseHelper.COL_PMRK_BB,          pmrk.beratBadan)
                put(DatabaseHelper.COL_PMRK_TB,          pmrk.tinggiBadan)
                put(DatabaseHelper.COL_PMRK_LK,          pmrk.lingkarKepala)
                put(DatabaseHelper.COL_PMRK_LL,          pmrk.lingkarLengan)
                put(DatabaseHelper.COL_PMRK_Z_SCORE_TBU, pmrk.zScoreTbu)
                put(DatabaseHelper.COL_PMRK_Z_SCORE_BBU, pmrk.zScoreBbu)
                put(DatabaseHelper.COL_PMRK_STATUS_GIZI, statusGizi)
                put(DatabaseHelper.COL_PMRK_CATATAN,     pmrk.catatan)
            }

            val rowId = db.insertWithOnConflict(
                DatabaseHelper.TABLE_PEMERIKSAAN, null, values, SQLiteDatabase.CONFLICT_IGNORE
            )

            // ✅ FIX 3: Log supaya keliatan kalau ada yang masih gagal
            if (rowId == -1L) {
                android.util.Log.e("SYNC_PMRK_LOCAL", "Insert GAGAL: id=${pmrk.id}, anakId=${pmrk.anakId}, statusGizi=$statusGizi")
            } else {
                inserted++
            }
        }

        db.execSQL("PRAGMA foreign_keys = ON")
        db.close()
        "Pemeriksaan: $inserted inserted, $skipped skipped"
    } catch (e: Exception) {
        android.util.Log.e("SYNC_PMRK_LOCAL", "Error: ${e.message}", e)
        "Pemeriksaan sync error: ${e.message}"
    }
}

// ── Sync Vaksin Riwayat dari API ke SQLite lokal ─────────────────────────
private fun syncVaksinRiwayatFromApiToLocal(context: Context): String {
    return try {
        val apiVaksinList = runBlocking { fetchAllVaksinRiwayatFromApi(context) }
        val db = DatabaseHelper(context).writableDatabase

        var inserted = 0
        var skipped = 0

        for (vaksin in apiVaksinList) {
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_VR_ID} FROM ${DatabaseHelper.TABLE_VAKSIN_RIWAYAT} WHERE ${DatabaseHelper.COL_VR_ID} = ? LIMIT 1",
                arrayOf(vaksin.id)
            )
            val exists = cursor.moveToFirst()
            cursor.close()

            if (exists) {
                skipped++
                continue
            }

            val tgl = convertTanggalVaksinApiToLocal(vaksin.tanggalPemberian)

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_VR_ID,                vaksin.id)
                put(DatabaseHelper.COL_VR_ANAK_ID,           vaksin.anakId)
                put(DatabaseHelper.COL_VR_VAKSIN_REF_ID,     vaksin.vaksinRefId)
                put(DatabaseHelper.COL_VR_KADER_ID,          vaksin.kaderId)
                put(DatabaseHelper.COL_VR_TANGGAL_PEMBERIAN, tgl)
                put(DatabaseHelper.COL_VR_LOKASI,            vaksin.lokasi)
            }

            db.insertWithOnConflict(
                DatabaseHelper.TABLE_VAKSIN_RIWAYAT,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            inserted++
        }

        db.close()
        "Vaksin: $inserted inserted, $skipped skipped"
    } catch (e: Exception) {
        android.util.Log.e("SYNC_VAKSIN_LOCAL", "Error: ${e.message}", e)
        "Vaksin sync error: ${e.message}"
    }
}