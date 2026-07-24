package com.silisten.app.data.source

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject

data class NeteaseUser(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String
)

data class NeteaseLoginState(
    val loggedIn: Boolean = false,
    val user: NeteaseUser? = null,
    val message: String = "尚未登录网易云音乐"
)

data class NeteaseActionResult(
    val success: Boolean,
    val message: String
)

data class NeteaseQrLoginCode(
    val key: String,
    val qrUrl: String,
    val qrImg: String,
    val message: String
)

data class NeteaseQrLoginCheck(
    val code: Int,
    val message: String,
    val loginState: NeteaseLoginState? = null
)

class NeteaseApiClient(context: Context) {
    private val preferences = context.getSharedPreferences("netease_auth", Context.MODE_PRIVATE)
    private val cookieJar = PersistentCookieJar(preferences)
    private val directClient = NeteaseDirectApiClient(cookieJar)

    suspend fun sendSmsCode(phone: String): NeteaseActionResult = withContext(Dispatchers.IO) {
        val json = getJson("/captcha/sent?phone=${phone.encode()}&ctcode=86&timestamp=${System.currentTimeMillis()}")
        val success = json.optInt("code") == 200 && json.optBoolean("data", true)
        NeteaseActionResult(
            success = success,
            message = if (success) {
                "验证码已发送，请留意网易云短信"
            } else {
                val code = json.optInt("code", -1)
                if (code == -462) {
                    "网易云触发了安全验证，短信暂时不可用，请改用扫码登录"
                } else {
                    shortError(json.cleanMessage("验证码发送失败，可能触发了网易云风控"))
                }
            }
        )
    }

    suspend fun loginBySms(phone: String, captcha: String): NeteaseLoginState = withContext(Dispatchers.IO) {
        val json = getJson(
            "/login/cellphone?phone=${phone.encode()}&captcha=${captcha.encode()}&ctcode=86&timestamp=${System.currentTimeMillis()}"
        )
        val code = json.optInt("code", -1)
        Log.d(
            TAG,
            "SMS login response code=$code message=${json.cleanMessage("").take(80)} hasProfile=${json.optJSONObject("profile") != null} hasAccount=${json.optJSONObject("account") != null}"
        )
        if (code != 200) {
            return@withContext NeteaseLoginState(
                loggedIn = false,
                user = null,
                message = shortError(json.loginFailureMessage(code))
            )
        }
        json.cleanString("cookie").takeIf { it.isNotBlank() }?.let { cookieJar.saveCookieHeader(it, neteaseHttpUrl()) }
        val responseUser = json.optJSONObject("profile")?.toUser()
            ?: json.optJSONObject("account")?.toAccountUser(json.optJSONObject("profile"))
        responseUser?.let(::saveUser)
        val refreshed = runCatching { refreshLoginState() }.getOrNull()
        when {
            refreshed?.loggedIn == true -> refreshed.copy(message = "网易云音乐登录成功")
            responseUser != null -> NeteaseLoginState(true, responseUser, "网易云音乐登录成功")
            else -> NeteaseLoginState(
                loggedIn = false,
                user = null,
                message = shortError(json.cleanMessage("验证码已通过，但没有拿到登录态，请重新获取验证码后再试"))
            )
        }
    }

    suspend fun createQrLoginCode(): NeteaseQrLoginCode = withContext(Dispatchers.IO) {
        val keyJson = getJson("/login/qr/key?timestamp=${System.currentTimeMillis()}")
        val key = keyJson.optJSONObject("data")?.optString("unikey").orEmpty()
            .ifBlank { keyJson.optString("unikey").orEmpty() }
        if (key.isBlank()) error(shortError(keyJson.optString("message", "二维码 key 获取失败")))

        val qrJson = runCatching {
            getJson("/login/qr/create?key=${key.encode()}&qrimg=true&timestamp=${System.currentTimeMillis()}")
        }.getOrNull()
        val data = qrJson?.optJSONObject("data")
        val qrUrl = data?.optString("qrurl").orEmpty()
            .ifBlank { "https://music.163.com/login?codekey=$key" }
        NeteaseQrLoginCode(
            key = key,
            qrUrl = qrUrl,
            qrImg = data?.optString("qrimg").orEmpty().ifBlank { generateQrDataUrl(qrUrl) },
            message = "请使用网易云音乐 App 扫码登录"
        )
    }

    suspend fun createQrLoginCodeLocal(): NeteaseQrLoginCode = withContext(Dispatchers.IO) {
        val keyJson = getJson("/login/qr/key?timestamp=${System.currentTimeMillis()}")
        Log.d(TAG, "QR key response: $keyJson")
        val key = keyJson.optJSONObject("data")?.optString("unikey").orEmpty()
            .ifBlank { keyJson.optString("unikey").orEmpty() }
            .ifBlank { keyJson.optJSONObject("data")?.optString("code").orEmpty() }
        if (key.isBlank()) {
            val msg = keyJson.optString("message").ifBlank {
                keyJson.optJSONObject("data")?.optString("message").orEmpty()
            }.ifBlank { "二维码 key 获取失败" }
            error(shortError(msg))
        }
        val qrUrl = "https://music.163.com/login?codekey=$key"
        Log.d(TAG, "QR URL: $qrUrl")
        NeteaseQrLoginCode(
            key = key,
            qrUrl = qrUrl,
            qrImg = generateQrDataUrl(qrUrl),
            message = "请使用网易云音乐 App 扫码登录"
        )
    }

    suspend fun checkQrLogin(key: String): NeteaseQrLoginCheck = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val json = runCatching {
            getJson("/login/qr/check?key=${key.encode()}&timestamp=$timestamp")
        }.getOrElse {
            runCatching {
                getJson("/login/qr/check?key=${key.encode()}&noCookie=true&timestamp=$timestamp")
            }.getOrElse { inner ->
                return@withContext NeteaseQrLoginCheck(
                    code = -1,
                    message = shortError(inner.message.orEmpty().ifBlank { "二维码检查网络失败" })
                )
            }
        }
        val code = json.optInt("code")
        Log.d(TAG, "QR check response code=$code body=${json.toString().take(200)}")
        if (code == 502) {
            val retryJson = runCatching {
                getJson("/login/qr/check?key=${key.encode()}&noCookie=true&timestamp=${System.currentTimeMillis()}")
            }.getOrNull()
            if (retryJson != null && retryJson.optInt("code") != 502) {
                val retryCode = retryJson.optInt("code")
                Log.d(TAG, "QR check retry code=$retryCode")
                if (retryCode == 803) {
                    retryJson.optString("cookie").takeIf { it.isNotBlank() }?.let {
                        cookieJar.saveCookieHeader(it, neteaseHttpUrl())
                    }
                    val loginState = runCatching { refreshLoginState() }.getOrElse {
                        NeteaseLoginState(true, loadUser(), "扫码成功，正在获取用户信息")
                    }
                    return@withContext NeteaseQrLoginCheck(retryCode, "登录成功", loginState)
                }
                val retryMessage = when (retryCode) {
                    800 -> "二维码已过期，请重新生成"
                    801 -> "等待扫码"
                    802 -> "已扫码，请在手机上确认登录"
                    else -> retryJson.optString("message", "等待扫码")
                }
                return@withContext NeteaseQrLoginCheck(retryCode, retryMessage)
            }
            return@withContext NeteaseQrLoginCheck(801, "等待扫码")
        }
        val message = when (code) {
            800 -> "二维码已过期，请重新生成"
            801 -> "等待扫码"
            802 -> "已扫码，请在手机上确认登录"
            803 -> "登录成功"
            else -> json.optString("message", "二维码状态未知：$code").let { shortError(it) }
        }
        if (code == 803) {
            json.optString("cookie").takeIf { it.isNotBlank() }?.let {
                cookieJar.saveCookieHeader(it, neteaseHttpUrl())
            }
            val loginState = runCatching { refreshLoginState() }.getOrElse {
                NeteaseLoginState(true, loadUser(), "扫码成功，正在获取用户信息")
            }
            return@withContext NeteaseQrLoginCheck(code, message, loginState)
        }
        NeteaseQrLoginCheck(code, message)
    }

    suspend fun refreshLoginState(): NeteaseLoginState = withContext(Dispatchers.IO) {
        val localUser = loadUser()
        val json = runCatching {
            getJson("/login/status?timestamp=${System.currentTimeMillis()}")
        }.getOrNull()
        val data = json?.optJSONObject("data")
        val remoteUser = data?.optJSONObject("profile")?.toUser()
            ?: json?.optJSONObject("profile")?.toUser()
            ?: data?.let { it.optJSONObject("account")?.toAccountUser(it.optJSONObject("profile")) }
            ?: json?.optJSONObject("account")?.toAccountUser(json.optJSONObject("profile"))
        val code = json?.optInt("code", -1) ?: -1
        val user = remoteUser ?: if (json == null || (code == 200 && localUser != null)) localUser else null
        if (remoteUser != null) {
            saveUser(remoteUser)
            NeteaseLoginState(true, remoteUser, "已登录网易云音乐")
        } else if (json == null && localUser != null) {
            NeteaseLoginState(true, localUser, "当前离线，先使用上次登录信息")
        } else if (user != null) {
            NeteaseLoginState(true, user, "已登录网易云音乐")
        } else {
            preferences.edit().remove("user_id").remove("nickname").remove("avatar_url").apply()
            NeteaseLoginState(false, null, "尚未登录网易云音乐")
        }
    }

    suspend fun logout(): NeteaseLoginState = withContext(Dispatchers.IO) {
        runCatching { getJson("/logout?timestamp=${System.currentTimeMillis()}") }
        cookieJar.clear()
        preferences.edit()
            .remove("user_id")
            .remove("nickname")
            .remove("avatar_url")
            .remove(LEGACY_ACTIVE_BASE_URL)
            .apply()
        NeteaseLoginState(false, null, "已退出网易云音乐")
    }

    suspend fun getJson(pathAndQuery: String, raceGateways: Boolean = true): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(requestTextDirect(pathAndQuery))
    }

    suspend fun getJsonArray(pathAndQuery: String, raceGateways: Boolean = true): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(requestTextDirect(pathAndQuery))
    }

    private fun requestTextDirect(pathAndQuery: String): String {
        if (pathAndQuery.startsWith("http")) {
            error("网易云直连接口不支持外部地址")
        }
        return runCatching { directClient.requestText(pathAndQuery) }
            .onSuccess { Log.d(TAG, "Direct Netease request succeeded: $pathAndQuery") }
            .getOrElse {
                Log.w(TAG, "Direct Netease request failed: $pathAndQuery", it)
                error(shortError(it.message.orEmpty().ifBlank { "网易云接口暂时不可用" }))
            }
    }

    private fun neteaseHttpUrl(): HttpUrl = MUSIC_URL.toHttpUrl()

    private fun JSONObject.toUser(): NeteaseUser? {
        val id = optLong("userId", 0L)
        if (id == 0L) return null
        return NeteaseUser(
            userId = id,
            nickname = cleanString("nickname").ifBlank { "网易云用户" },
            avatarUrl = cleanString("avatarUrl")
        )
    }

    private fun JSONObject.toAccountUser(profile: JSONObject? = null): NeteaseUser? {
        val id = optLong("id", optLong("userId", 0L))
        if (id == 0L) return null
        return NeteaseUser(
            userId = id,
            nickname = profile?.cleanString("nickname")
                ?.ifBlank { cleanString("userName") }
                ?.ifBlank { cleanString("nickname") }
                ?.ifBlank { "网易云用户" }
                ?: cleanString("userName").ifBlank { cleanString("nickname") }.ifBlank { "网易云用户" },
            avatarUrl = profile?.cleanString("avatarUrl").orEmpty()
        )
    }

    private fun saveUser(user: NeteaseUser) {
        preferences.edit()
            .putLong("user_id", user.userId)
            .putString("nickname", user.nickname)
            .putString("avatar_url", user.avatarUrl)
            .apply()
    }

    private fun loadUser(): NeteaseUser? {
        val id = preferences.getLong("user_id", 0L)
        if (id == 0L) return null
        return NeteaseUser(
            userId = id,
            nickname = preferences.getString("nickname", "网易云用户").orEmpty(),
            avatarUrl = preferences.getString("avatar_url", "").orEmpty()
        )
    }

    private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")

    private fun generateQrDataUrl(content: String, size: Int = 720): String {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return "data:image/png;base64,${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}"
    }

    private fun JSONObject.cleanMessage(fallback: String): String =
        listOf("message", "msg", "error", "errmsg", "msgCode")
            .firstNotNullOfOrNull { key -> cleanString(key).takeIf { it.isNotBlank() } }
            ?: optJSONObject("data")?.let { data ->
                listOf("message", "msg", "error", "errmsg", "url")
                    .firstNotNullOfOrNull { key -> data.cleanString(key).takeIf { it.isNotBlank() } }
            }
            ?: fallback

    private fun JSONObject.cleanString(key: String): String {
        val value = opt(key) ?: return ""
        if (value == JSONObject.NULL) return ""
        return value.toString().replace(Regex("\\s+"), " ").trim().takeUnless {
            it.equals("null", ignoreCase = true) ||
                it.equals("undefined", ignoreCase = true)
        }.orEmpty()
    }

    private fun JSONObject.loginFailureMessage(code: Int): String {
        val serverMessage = cleanMessage("")
        return when (code) {
            -462 -> "网易云触发了安全验证，短信登录被拦截。请改用下方扫码登录，或稍后再试"
            10004 -> humanizeServerMessage(serverMessage)
                .ifBlank { "当前登录存在安全风险，请改用扫码登录或稍后再试" }
            400, 502 -> humanizeServerMessage(serverMessage)
                .ifBlank { "登录请求被网易云拒绝，请重新获取验证码后再试" }
            501 -> humanizeServerMessage(serverMessage)
                .ifBlank { "手机号未注册或暂不支持该账号登录" }
            503 -> humanizeServerMessage(serverMessage)
                .ifBlank { "验证码错误或已过期，请重新获取验证码" }
            505 -> humanizeServerMessage(serverMessage)
                .ifBlank { "账号存在安全限制，请在网易云音乐官方 App 处理后再试" }
            else -> humanizeServerMessage(serverMessage)
                .ifBlank { "登录失败，网易云返回状态码 $code，请重新获取验证码后再试" }
        }
    }

    private fun humanizeServerMessage(message: String): String {
        val cleaned = message.replace(Regex("\\s+"), " ").trim()
        if (cleaned.isBlank()) return ""
        val lower = cleaned.lowercase()
        return when {
            lower.contains("encrypt-pages") ||
                lower.contains("st.music.163.com") ||
                lower.startsWith("http://") ||
                lower.startsWith("https://") ->
                "网易云触发了安全验证，短信登录被拦截。请改用扫码登录"
            cleaned.contains("验证码") || cleaned.contains("captcha", ignoreCase = true) -> cleaned
            cleaned.length > 48 -> cleaned.take(48) + "..."
            else -> cleaned
        }
    }

    private fun shortError(message: String): String {
        val cleaned = message.replace(Regex("\\s+"), " ").trim()
        val humanized = humanizeServerMessage(cleaned).ifBlank { cleaned }
        return when {
            humanized.isBlank() || humanized.equals("null", ignoreCase = true) -> "请求失败，请稍后重试"
            humanized.contains("failed to connect", ignoreCase = true) ||
                humanized.contains("connection refused", ignoreCase = true) ->
                "网易云接口连接失败，请检查网络后重试"
            humanized.contains("timeout", ignoreCase = true) ->
                "网易云接口响应超时，请稍后重试"
            humanized.length > 48 -> humanized.take(48) + "..."
            else -> humanized
        }
    }

    private companion object {
        private const val TAG = "NeteaseApiClient"
        private const val MUSIC_URL = "https://music.163.com"
        private const val LEGACY_ACTIVE_BASE_URL = "active_base_url"
    }
}

internal class PersistentCookieJar(
    private val preferences: SharedPreferences
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val existing = loadCookieMap().toMutableMap()
        cookies.forEach { existing[it.name] = it.value }
        saveCookieMap(existing)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return loadCookieMap().map { (name, value) ->
            Cookie.Builder()
                .domain(url.host)
                .path("/")
                .name(name)
                .value(value)
                .build()
        }
    }

    fun saveCookieHeader(cookieHeader: String, url: HttpUrl) {
        val ignoredKeys = setOf("path", "max-age", "expires", "domain", "httponly", "secure", "samesite")
        val parsed = cookieHeader.split(";").mapNotNull { part ->
            val pieces = part.trim().split("=", limit = 2)
            val name = pieces.getOrNull(0).orEmpty()
            val value = pieces.getOrNull(1).orEmpty()
            if (pieces.size != 2 || name.lowercase() in ignoredKeys) {
                null
            } else {
                name to value
            }
        }
        val existing = loadCookieMap().toMutableMap()
        parsed.forEach { (name, value) -> existing[name] = value }
        saveCookieMap(existing)
    }

    fun cookieValue(name: String): String? = loadCookieMap()[name]

    fun clear() {
        preferences.edit().remove("cookies").apply()
    }

    fun removeCookies(names: Set<String>) {
        if (names.isEmpty()) return
        val existing = loadCookieMap().toMutableMap()
        var changed = false
        names.forEach { name ->
            changed = existing.remove(name) != null || changed
        }
        if (changed) saveCookieMap(existing)
    }

    private fun loadCookieMap(): Map<String, String> {
        return preferences.getStringSet("cookies", emptySet()).orEmpty()
            .mapNotNull { raw ->
                val pieces = raw.split("=", limit = 2)
                val name = pieces.getOrNull(0).orEmpty()
                val value = pieces.getOrNull(1).orEmpty()
                if (name.isBlank()) null else name to value
            }
            .toMap()
    }

    private fun saveCookieMap(cookies: Map<String, String>) {
        preferences.edit()
            .putStringSet("cookies", cookies.map { (name, value) -> "$name=$value" }.toSet())
            .apply()
    }
}
