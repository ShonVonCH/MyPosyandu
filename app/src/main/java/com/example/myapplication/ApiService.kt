package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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

private fun aesDecrypt(cHex: String, aHex: String, bHex: String): String {
    val key     = SecretKeySpec(hexToBytes(aHex), "AES")
    val iv      = IvParameterSpec(hexToBytes(bHex))
    val cipher  = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    val decrypted = cipher.doFinal(hexToBytes(cHex))
    return bytesToHex(decrypted)
}

// ── Cookie Jar ───────────────────────────────────────────────────────────────
private val cookieStore = HashMap<String, MutableList<Cookie>>()

internal val httpClient = OkHttpClient.Builder()
    .cookieJar(object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore.getOrPut(url.host) { mutableListOf() }.addAll(cookies)
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    })
    .followRedirects(false)
    .build()

// ── Parse challenge dari HTML ─────────────────────────────────────────────────
private data class AesChallenge(val a: String, val b: String, val c: String, val redirectUrl: String)

private fun parseChallenge(html: String, baseUrl: String): AesChallenge? {
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

// ── Simpan User Login ke SQLite (hanya 1 baris) ───────────────────────────────
private fun saveUserToLocal(context: Context, user: UserApi, password: String) {
    val db = DatabaseHelper(context).writableDatabase

    // Hapus data lama dulu, tabel ini hanya boleh 1 baris
    db.delete(DatabaseHelper.TABLE_USERS, null, null)

    val values = ContentValues().apply {
        put(DatabaseHelper.COL_USERS_ID,         user.id)
        put(DatabaseHelper.COL_USERS_NAMA,        user.nama)
        put(DatabaseHelper.COL_USERS_USERNAME,    user.username)
        put(DatabaseHelper.COL_USERS_PASSWORD,    password)
        put(DatabaseHelper.COL_USERS_ROLE,        user.role)
        put(DatabaseHelper.COL_USERS_POSYANDU_ID, user.posyanduId)
        put(DatabaseHelper.COL_USERS_CREATED_AT,  System.currentTimeMillis().toString())
    }

    db.insertWithOnConflict(
        DatabaseHelper.TABLE_USERS,
        null,
        values,
        SQLiteDatabase.CONFLICT_REPLACE
    )

    android.util.Log.d("LOGIN_DEBUG", "User disimpan ke SQLite: ${user.username}")
}

// ── Fetch Login ───────────────────────────────────────────────────────────────
suspend fun fetchLoginFromApi(
    username: String,
    password: String,
    role: String,
    context: Context
): UserApi? = withContext(Dispatchers.IO) {

    try {
        val apiUrl    = "https://myposyandu.gt.tc/api_posyandu/users.php"
        val userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        // STEP 1
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

        // STEP 2 - Solve challenge
        val challenge = parseChallenge(body1, apiUrl)
        if (challenge == null) {
            android.util.Log.e("LOGIN_DEBUG", "Gagal parse challenge")
            return@withContext null
        }

        val cookieValue = aesDecrypt(challenge.c, challenge.a, challenge.b)
        android.util.Log.d("LOGIN_DEBUG", "Cookie __test=$cookieValue")

        val host = apiUrl.toHttpUrl()
        val testCookie = Cookie.Builder()
            .name("__test")
            .value(cookieValue)
            .domain("myposyandu.gt.tc")
            .path("/")
            .build()

        cookieStore.getOrPut(host.host) { mutableListOf() }.add(testCookie)

        // STEP 3
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