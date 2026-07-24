package com.silisten.app.data.source

import android.util.Base64
import java.math.BigInteger
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal class NeteaseDirectApiClient(
    private val cookieJar: PersistentCookieJar
) {
    init {
        ensureDeviceCookies()
    }

    private fun ensureDeviceCookies() {
        val useAndroidProfile = cookieJar.cookieValue("os") != "android" ||
            cookieJar.cookieValue("appver") == null ||
            cookieJar.cookieValue("deviceId") == null
        if (useAndroidProfile) {
            // Keep MUSIC_U / MUSIC_A if already logged in; only refresh device profile cookies.
            cookieJar.removeCookies(
                setOf(
                    "os",
                    "appver",
                    "versioncode",
                    "mobilename",
                    "osver",
                    "resolution",
                    "channel",
                    "buildver"
                )
            )
        }
        val deviceId = cookieJar.cookieValue("deviceId")
            ?: java.util.UUID.randomUUID().toString().replace("-", "").lowercase()
        val defaults = mapOf(
            "os" to "android",
            "appver" to "8.20.20",
            "versioncode" to "220",
            "mobilename" to "Pixel 7",
            "osver" to "14",
            "resolution" to "1080x2400",
            "deviceId" to deviceId,
            "channel" to "netease",
            "buildver" to (System.currentTimeMillis() / 1000).toString()
        )
        val existing = mutableMapOf<String, String>()
        defaults.forEach { (k, v) ->
            if (cookieJar.cookieValue(k) == null) {
                existing[k] = v
            }
        }
        if (existing.isNotEmpty()) {
            val header = existing.entries.joinToString("; ") { "${it.key}=${it.value}" }
            cookieJar.saveCookieHeader(header, "https://music.163.com".toHttpUrl())
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .cookieJar(cookieJar)
        .build()

    fun requestText(pathAndQuery: String): String {
        if (pathAndQuery.startsWith("http")) unsupported(pathAndQuery)
        val request = DirectRequest.from(pathAndQuery)
        return when (request.path) {
            "/captcha/sent" -> {
                // Prefer Android eapi path; fall back to weapi if needed.
                runCatching {
                    postEApi(
                        requestPath = "/eapi/sms/captcha/sent",
                        encryptPath = "/api/sms/captcha/sent",
                        data = mapOf(
                            "cellphone" to request.param("phone"),
                            "ctcode" to request.param("ctcode", "86")
                        )
                    )
                }.getOrElse {
                    postWeApi(
                        "/weapi/sms/captcha/sent",
                        mapOf(
                            "cellphone" to request.param("phone"),
                            "ctcode" to request.param("ctcode", "86")
                        )
                    )
                }
            }

            "/login/cellphone" -> {
                // Android official-style eapi login first; weapi as fallback.
                val phone = request.param("phone")
                val captcha = request.param("captcha")
                val country = request.param("ctcode", "86")
                val eapiBody = mapOf(
                    "phone" to phone,
                    "countrycode" to country,
                    "captcha" to captcha,
                    "rememberLogin" to "true",
                    "https" to "true"
                )
                val eapiText = runCatching {
                    postEApi(
                        requestPath = "/eapi/w/login/cellphone",
                        encryptPath = "/api/w/login/cellphone",
                        data = eapiBody
                    )
                }.getOrNull()
                if (eapiText != null) {
                    val eapiCode = runCatching { JSONObject(eapiText).optInt("code", -1) }.getOrDefault(-1)
                    // Prefer success, and only fall back when eapi is clearly unsupported.
                    if (eapiCode == 200 || eapiCode == -462 || eapiCode in setOf(400, 501, 502, 503, 505, 10004)) {
                        return eapiText
                    }
                }
                postWeApi(
                    "/weapi/w/login/cellphone",
                    mapOf(
                        "phone" to phone,
                        "captcha" to captcha,
                        "countrycode" to country,
                        "rememberLogin" to true,
                        "https" to true
                    )
                )
            }

            "/login/qr/key" -> postWeApi(
                "/weapi/login/qrcode/unikey",
                mapOf(
                    "type" to request.param("type", "1"),
                    "timerstamp" to System.currentTimeMillis().toString()
                )
            )

            "/login/qr/create" -> postWeApi(
                "/weapi/login/qrcode/create",
                mapOf(
                    "key" to request.param("key"),
                    "qrimg" to request.param("qrimg", "true"),
                    "timerstamp" to System.currentTimeMillis().toString()
                )
            )

            "/login/qr/check" -> postWeApi(
                "/weapi/login/qrcode/client/login",
                mapOf(
                    "key" to request.param("key"),
                    "type" to request.param("type", "1"),
                    "timerstamp" to System.currentTimeMillis().toString()
                )
            )

            "/login/status" -> normalizeLoginStatus(
                postWeApi("/weapi/nuser/account/get", emptyMap())
            )

            "/logout" -> postWeApi("/weapi/logout", emptyMap())

            "/search" -> postWeApi(
                "/weapi/cloudsearch/pc",
                mapOf(
                    "s" to request.param("keywords"),
                    "type" to request.param("type", "1"),
                    "limit" to request.param("limit", "30"),
                    "offset" to request.param("offset", "0"),
                    "total" to true
                )
            )

            "/album" -> postWeApi(
                "/weapi/v1/album/${request.param("id").encodeQuery()}",
                emptyMap()
            )

            "/artists" -> postWeApi(
                "/weapi/v1/artist/${request.param("id").encodeQuery()}",
                emptyMap()
            )

            "/artist/album" -> postWeApi(
                "/weapi/artist/albums/${request.param("id").encodeQuery()}",
                mapOf(
                    "limit" to request.param("limit", "30"),
                    "offset" to request.param("offset", "0"),
                    "total" to true
                )
            )

            "/artist/songs" -> postEApi(
                requestPath = "/eapi/v1/artist/songs",
                encryptPath = "/api/v1/artist/songs",
                data = mapOf(
                    "id" to request.param("id"),
                    "private_cloud" to "true",
                    "work_type" to 1,
                    "order" to request.param("order", "hot"),
                    "limit" to request.param("limit", "50"),
                    "offset" to request.param("offset", "0")
                )
            )

            "/song/url/v1" -> postWeApi(
                "/weapi/song/enhance/player/url/v1",
                mapOf(
                    "ids" to JSONArray().put(request.param("id")),
                    "level" to request.param("level", "exhigh"),
                    "encodeType" to request.param("encodeType", "flac")
                )
            )

            "/song/detail" -> postWeApi(
                "/weapi/v3/song/detail",
                mapOf(
                    "c" to JSONArray(
                        request.param("ids")
                            .split(",")
                            .filter { it.isNotBlank() }
                            .map { JSONObject().put("id", it.trim().toLongOrNull() ?: it.trim()) }
                    ).toString()
                )
            )

            "/lyric" -> postWeApi(
                "/weapi/song/lyric/v1",
                mapOf(
                    "id" to request.param("id"),
                    "lv" to -1,
                    "kv" to -1,
                    "tv" to -1,
                    "rv" to -1,
                    "yv" to 1
                )
            )

            "/playlist/detail" -> postWeApi(
                "/weapi/v6/playlist/detail",
                mapOf(
                    "id" to request.param("id"),
                    "n" to request.param("n", "1000"),
                    "s" to request.param("s", "8")
                )
            )

            "/personalized" -> postWeApi(
                "/weapi/personalized/playlist",
                mapOf(
                    "limit" to request.param("limit", "12"),
                    "total" to true,
                    "n" to request.param("limit", "12")
                )
            )

            "/recommend/songs" -> postWeApi("/weapi/v3/discovery/recommend/songs", emptyMap())

            "/recommend/resource" -> postWeApi("/weapi/v1/discovery/recommend/resource", emptyMap())

            "/personal_fm" -> postWeApi("/weapi/v1/radio/get", emptyMap())

            "/user/playlist" -> postWeApi(
                "/weapi/user/playlist",
                mapOf(
                    "uid" to request.param("uid"),
                    "limit" to request.param("limit", "40"),
                    "offset" to request.param("offset", "0"),
                    "includeVideo" to request.param("includeVideo", "true")
                )
            )

            "/likelist" -> postWeApi(
                "/weapi/song/like/get",
                mapOf("uid" to request.param("uid"))
            )

            "/like" -> postWeApi(
                "/weapi/radio/like?alg=${request.param("alg", "itembased").encodeQuery()}",
                mapOf(
                    "trackId" to request.param("id"),
                    "like" to (request.param("like", "true").toBooleanStrictOrNull() ?: true),
                    "time" to request.param("time", "25")
                )
            )

            "/playlist/tracks" -> postWeApi(
                "/weapi/playlist/manipulate/tracks",
                mapOf(
                    "op" to request.param("op", "add"),
                    "pid" to request.param("pid"),
                    "trackIds" to JSONArray(
                        request.param("tracks")
                            .split(",")
                            .filter { it.isNotBlank() }
                            .map { it.trim().toLongOrNull() ?: it.trim() }
                    ).toString(),
                    "imme" to true
                )
            )

            "/playlist/subscribe" -> {
                val subscribe = request.param("t", "1") == "1"
                val apiPath = if (subscribe) "/eapi/playlist/subscribe" else "/eapi/playlist/unsubscribe"
                val data = mutableMapOf<String, Any?>("id" to request.param("id"))
                if (subscribe) data["checkToken"] = true
                postEApi(
                    requestPath = apiPath,
                    encryptPath = apiPath.removePrefix("/eapi").let { "/api$it" },
                    data = data
                )
            }

            "/user/cloud" -> postWeApi(
                "/weapi/v1/cloud/get",
                mapOf(
                    "limit" to request.param("limit", "50"),
                    "offset" to request.param("offset", "0")
                )
            )

            "/comment/music" -> postWeApi(
                "/weapi/v1/resource/comments/R_SO_4_${request.param("id")}",
                commentData("R_SO_4_${request.param("id")}", request)
            )

            "/comment/playlist" -> postWeApi(
                "/weapi/v1/resource/comments/A_PL_0_${request.param("id")}",
                commentData("A_PL_0_${request.param("id")}", request)
            )

            "/comment/hot" -> {
                val resource = when (request.param("type", "0")) {
                    "2" -> "A_PL_0_${request.param("id")}"
                    else -> "R_SO_4_${request.param("id")}"
                }
                postWeApi(
                    "/weapi/v1/resource/hotcomments/$resource",
                    mapOf(
                        "rid" to resource,
                        "limit" to request.param("limit", "30"),
                        "offset" to request.param("offset", "0"),
                        "total" to true
                    )
                )
            }

            "/comment/floor" -> {
                val resource = when (request.param("type", "0")) {
                    "2" -> "A_PL_0_${request.param("id")}"
                    "3" -> "R_AL_3_${request.param("id")}"
                    else -> "R_SO_4_${request.param("id")}"
                }
                postWeApi(
                    "/weapi/resource/comment/floor/get",
                    mapOf(
                        "parentCommentId" to request.param("parentCommentId"),
                        "threadId" to resource,
                        "time" to request.param("time", "-1"),
                        "limit" to request.param("limit", "20")
                    )
                )
            }

            else -> unsupported(pathAndQuery)
        }
    }

    private fun commentData(resourceId: String, request: DirectRequest): Map<String, Any?> =
        mapOf(
            "rid" to resourceId,
            "threadId" to resourceId,
            "pageNo" to request.param("pageNo", "1"),
            "pageSize" to request.param("limit", "30"),
            "limit" to request.param("limit", "30"),
            "offset" to request.param("offset", "0"),
            "sortType" to request.param("sortType", "2"),
            "cursor" to request.param("cursor", "-1")
        )

    private fun postWeApi(apiPath: String, data: Map<String, Any?>, preferAndroid: Boolean = false): String {
        val payload = JSONObject()
        data.forEach { (key, value) -> payload.put(key, value.toJsonValue()) }
        val csrf = cookieJar.cookieValue("__csrf").orEmpty()
        if (!payload.has("csrf_token")) {
            payload.put("csrf_token", csrf)
        }
        val encrypted = NeteaseCrypto.weApi(payload.toString())
        val separator = if (apiPath.contains("?")) "&" else "?"
        val authPath = apiPath.contains("login") || apiPath.contains("captcha") || apiPath.contains("sms")
        val request = Request.Builder()
            .url("$MUSIC_URL$apiPath${separator}csrf_token=${csrf.encodeQuery()}")
            .headers(commonHeaders(preferAndroid = preferAndroid || authPath))
            .post(
                FormBody.Builder()
                    .addEncoded("params", encrypted.params)
                    .addEncoded("encSecKey", encrypted.encSecKey)
                    .build()
            )
            .build()
        return execute(request)
    }

    private fun postEApi(
        requestPath: String,
        data: Map<String, Any?>,
        encryptPath: String = requestPath.removePrefix("/eapi").let { "/api$it" }
    ): String {
        val payload = JSONObject()
        data.forEach { (key, value) -> payload.put(key, value.toJsonValue()) }
        payload.put("header", JSONObject(eApiHeader()))
        val encrypted = NeteaseCrypto.eApi(encryptPath, payload.toString())
        val body = "params=${encrypted.params}".toRequestBody(FORM_MEDIA_TYPE)
        val authPath = requestPath.contains("login") || requestPath.contains("captcha") || requestPath.contains("sms")
        val request = Request.Builder()
            .url("$INTERFACE_URL$requestPath")
            .headers(commonHeaders(preferAndroid = authPath))
            .post(body)
            .build()
        return execute(request)
    }

    private fun eApiHeader(): Map<String, Any?> = mapOf(
        "osver" to (cookieJar.cookieValue("osver") ?: "14"),
        "deviceId" to cookieJar.cookieValue("deviceId"),
        "appver" to (cookieJar.cookieValue("appver") ?: "8.20.20"),
        "versioncode" to (cookieJar.cookieValue("versioncode") ?: "220"),
        "mobilename" to (cookieJar.cookieValue("mobilename") ?: "Pixel 7"),
        "buildver" to (cookieJar.cookieValue("buildver") ?: (System.currentTimeMillis() / 1000).toString()),
        "resolution" to (cookieJar.cookieValue("resolution") ?: "1080x2400"),
        "os" to (cookieJar.cookieValue("os") ?: "android"),
        "channel" to (cookieJar.cookieValue("channel") ?: "netease"),
        "__csrf" to cookieJar.cookieValue("__csrf").orEmpty(),
        "MUSIC_U" to cookieJar.cookieValue("MUSIC_U"),
        "MUSIC_A" to cookieJar.cookieValue("MUSIC_A"),
        "requestId" to "${System.currentTimeMillis()}${(0..9999).random().toString().padStart(4, '0')}"
    )

    private fun commonHeaders(preferAndroid: Boolean = false) = okhttp3.Headers.Builder()
        .add("User-Agent", if (preferAndroid) ANDROID_USER_AGENT else USER_AGENTS.random())
        .add("Referer", MUSIC_URL)
        .add("Origin", MUSIC_URL)
        .build()

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("网易云直连 HTTP ${response.code}")
            val text = response.body?.string().orEmpty()
            if (text.isBlank()) error("网易云直连返回为空")
            return text
        }
    }

    private fun normalizeLoginStatus(text: String): String {
        val json = JSONObject(text)
        if (json.optJSONObject("data")?.optJSONObject("profile") != null) return text
        val profile = json.optJSONObject("profile")
        if (profile != null) {
            return JSONObject()
                .put("code", json.optInt("code", 200))
                .put("data", JSONObject().put("profile", profile))
                .toString()
        }
        return text
    }

    private fun unsupported(pathAndQuery: String): Nothing =
        throw UnsupportedOperationException("direct netease api does not support $pathAndQuery yet")

    private data class DirectRequest(
        val path: String,
        private val params: Map<String, String>
    ) {
        fun param(name: String, fallback: String = ""): String = params[name].orEmpty().ifBlank { fallback }

        companion object {
            fun from(pathAndQuery: String): DirectRequest {
                val uri = URI.create("$LOCAL_ORIGIN$pathAndQuery")
                val params = uri.rawQuery.orEmpty()
                    .split("&")
                    .filter { it.isNotBlank() }
                    .mapNotNull { part ->
                        val pieces = part.split("=", limit = 2)
                        val key = pieces.getOrNull(0)?.let { URLDecoder.decode(it, "UTF-8") }.orEmpty()
                        val value = pieces.getOrNull(1)?.let { URLDecoder.decode(it, "UTF-8") }.orEmpty()
                        if (key.isBlank()) null else key to value
                    }
                    .toMap()
                return DirectRequest(uri.path, params)
            }
        }
    }

    private fun Any?.toJsonValue(): Any = when (this) {
        null -> JSONObject.NULL
        is JSONArray -> this
        is JSONObject -> this
        is Iterable<*> -> JSONArray().also { array -> this.forEach { array.put(it.toJsonValue()) } }
        else -> this
    }

    private fun String.encodeQuery(): String = URLEncoder.encode(this, "UTF-8")

    private companion object {
        private const val LOCAL_ORIGIN = "https://silisten.local"
        private const val MUSIC_URL = "https://music.163.com"
        private const val INTERFACE_URL = "https://interface.music.163.com"
        private val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()
        private const val ANDROID_USER_AGENT =
            "NeteaseMusic/8.20.20.220002 (Pixel 7;android 14)"
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
        )
    }
}

private object NeteaseCrypto {
    private const val IV = "0102030405060708"
    private const val PRESET_KEY = "0CoJUm6Qyw8W8jud"
    private const val EAPI_KEY = "e82ckenh8dichen8"
    private const val BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val PUBLIC_KEY_PEM =
        "-----BEGIN PUBLIC KEY-----\n" +
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgtQn2JZ34ZC28NWYpAUd98iZ37BUrX/aKzmFbt7clFSs6sXqHauqKWqdtLkF2KexO40H1YTX8z2lSgBBOAxLsvaklV8k4cBFK9snQXE9/DDaFt6Rr7iVZMldczhC0JNgTz+SHXT6CBHuX3e9SdB1Ua44oncaTWz7OBGLbCiK45wIDAQAB\n" +
            "-----END PUBLIC KEY-----"

    private val random = SecureRandom()
    private val publicKey: RSAPublicKey by lazy {
        val base64 = PUBLIC_KEY_PEM
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s+"), "")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(bytes)) as RSAPublicKey
    }

    fun weApi(text: String): WeApiPayload {
        val secret = randomSecret()
        val first = aesCbcBase64(text, PRESET_KEY)
        val second = aesCbcBase64(first, secret)
        return WeApiPayload(
            params = second.encodeQuery(),
            encSecKey = rsaNoPadding(secret.toByteArray(Charsets.UTF_8)).encodeQuery()
        )
    }

    fun eApi(url: String, body: String): EApiPayload {
        val digest = md5("nobody${url}use${body}md5forencrypt")
        val data = "$url-36cd479b6b5-$body-36cd479b6b5-$digest"
        return EApiPayload(params = aesEcbHex(data, EAPI_KEY).encodeQuery())
    }

    private fun randomSecret(): String = buildString {
        repeat(16) {
            append(BASE62[random.nextInt(BASE62.length)])
        }
    }

    private fun aesCbcBase64(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(IV.toByteArray(Charsets.UTF_8))
        )
        return Base64.encodeToString(cipher.doFinal(text.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun aesEcbHex(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        return cipher.doFinal(text.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun rsaNoPadding(bytes: ByteArray): String {
        val reversed = bytes.reversedArray()
        val value = BigInteger(1, reversed)
        val encrypted = value.modPow(publicKey.publicExponent, publicKey.modulus)
        return encrypted.toHex(publicKey.modulus.bitLength().plus(7) / 8)
    }

    private fun md5(text: String): String =
        MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private fun BigInteger.toHex(byteLength: Int): String =
        toString(16).uppercase().padStart(byteLength * 2, '0')

    private fun String.encodeQuery(): String = URLEncoder.encode(this, "UTF-8")

    data class WeApiPayload(val params: String, val encSecKey: String)
    data class EApiPayload(val params: String)
}
