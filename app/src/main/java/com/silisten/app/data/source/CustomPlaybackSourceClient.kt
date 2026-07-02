package com.silisten.app.data.source

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.silisten.app.data.model.CustomPlaybackSourceType
import com.silisten.app.data.model.CustomSourceConfig
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.Song
import com.silisten.app.data.model.neteaseIdentityId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONTokener
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.math.abs

data class CustomSourceInspectResult(
    val ok: Boolean,
    val message: String,
    val displayName: String? = null,
    val version: String? = null,
    val description: String? = null,
    val author: String? = null,
    val homepage: String? = null,
    val supportedSources: List<String> = emptyList()
)

data class CustomSourceImportResult(
    val script: String,
    val inspect: CustomSourceInspectResult
)

class CustomPlaybackSourceClient(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    private val appContext = context.applicationContext
    private val scriptCache = ConcurrentHashMap<String, String>()

    suspend fun inspect(source: CustomSourceConfig): CustomSourceInspectResult {
        if (!source.enabled) {
            return CustomSourceInspectResult(false, "音源已关闭，打开后才能参与播放解析")
        }
        return when (source.type) {
            CustomPlaybackSourceType.DirectHttp -> inspectDirectHttp(source)
            CustomPlaybackSourceType.LxScript -> inspectLxScript(source)
        }
    }

    suspend fun inspectScript(script: String): CustomSourceInspectResult =
        inspectLxScriptText(script)

    suspend fun importLxScriptFromUrl(url: String): CustomSourceImportResult? {
        val script = downloadText(url)?.takeIf { it.isNotBlank() } ?: return null
        return CustomSourceImportResult(
            script = script,
            inspect = inspectLxScriptText(script)
        )
    }

    suspend fun resolvePlayUrl(
        song: Song,
        source: CustomSourceConfig,
        quality: PlaybackQuality
    ): String? {
        if (!source.enabled) return null
        return when (source.type) {
            CustomPlaybackSourceType.DirectHttp -> resolveDirectHttp(song, source, quality)
            CustomPlaybackSourceType.LxScript -> resolveLxScript(song, source, quality)
        }
    }

    private fun inspectDirectHttp(source: CustomSourceConfig): CustomSourceInspectResult {
        val base = source.endpoint.toHttpUrlOrNull()
            ?: return CustomSourceInspectResult(false, "接口地址格式不正确")
        Log.d(TAG, "Direct HTTP source inspected host=${base.host}")
        return CustomSourceInspectResult(
            ok = true,
            message = "直接播放接口格式可用，播放失败时会把歌曲信息作为查询参数请求该接口",
            displayName = source.name.ifBlank { base.host }
        )
    }

    private suspend fun inspectLxScript(source: CustomSourceConfig): CustomSourceInspectResult {
        val script = source.script.ifBlank { downloadText(source.endpoint).orEmpty() }
            .takeIf { it.isNotBlank() }
            ?: return CustomSourceInspectResult(false, "无法下载 LX 音源脚本，请检查地址或网络")
        return inspectLxScriptText(script)
    }

    private suspend fun inspectLxScriptText(script: String): CustomSourceInspectResult {
        val metadata = LxScriptMetadata.parse(script)
        val initInfo = runLxSession(script) { session ->
            session.init()
        } ?: return CustomSourceInspectResult(
            ok = false,
            message = "LX 脚本下载成功，但初始化失败，请确认脚本兼容移动端自定义源"
        )
        val sources = initInfo.sources
            .filter { it.actions.contains(LX_ACTION_MUSIC_URL) }
            .map { "${it.name}(${it.key})" }
        return CustomSourceInspectResult(
            ok = sources.isNotEmpty(),
            message = if (sources.isEmpty()) {
                "脚本已初始化，但没有声明可用的 musicUrl 播放解析能力"
            } else {
                "LX 音源脚本可用：${sources.joinToString("、")}"
            },
            displayName = metadata.name,
            version = metadata.version,
            description = metadata.description,
            author = metadata.author,
            homepage = metadata.homepage,
            supportedSources = sources
        )
    }

    private suspend fun resolveDirectHttp(
        song: Song,
        source: CustomSourceConfig,
        quality: PlaybackQuality
    ): String? = withContext(Dispatchers.IO) {
        val url = source.endpoint.toPlaybackHttpUrl(song, quality) ?: return@withContext null
        Log.d(TAG, "Direct HTTP resolving ${song.title} - ${song.artist} via ${url.host}")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val resolved = if (response.isSuccessful) body.trim().extractPlayableUrl() else null
                Log.d(TAG, "Direct HTTP status=${response.code} success=${!resolved.isNullOrBlank()}")
                resolved
            }
        }.getOrElse { error ->
            Log.d(TAG, "Direct HTTP failed: ${error.message}")
            null
        }
    }

    private suspend fun resolveLxScript(
        song: Song,
        source: CustomSourceConfig,
        quality: PlaybackQuality
    ): String? {
        val script = source.script.ifBlank { downloadText(source.endpoint).orEmpty() }
            .takeIf { it.isNotBlank() }
            ?: return null
        return runLxSession(script, source) { session ->
            val initInfo = session.init() ?: return@runLxSession null
            Log.d(
                TAG,
                "LX script ${source.name} initialized sources=${
                    initInfo.sources.joinToString(",") { "${it.key}:${it.qualitys.joinToString("/")}" }
                }"
            )
            val directCandidates = initInfo.identitySourceInfos(song)
                .mapNotNull { sourceInfo -> LxMusicInfoCandidate.fromSong(song, sourceInfo.key) }
            Log.d(TAG, "LX direct candidates=${directCandidates.map { "${it.sourceKey}:${it.songId}" }}")
            resolveByLxCandidates(session, directCandidates, initInfo, quality)?.let { return@runLxSession it }

            val matchedCandidates = searchLxMatchedCandidates(song, initInfo)
            resolveByLxCandidates(session, matchedCandidates, initInfo, quality)?.let { return@runLxSession it }
            null
        }
    }

    private suspend fun resolveByLxCandidates(
        session: LxScriptSession,
        candidates: List<LxMusicInfoCandidate>,
        initInfo: LxInitInfo,
        quality: PlaybackQuality
    ): String? {
        for (candidate in candidates) {
            val sourceInfo = initInfo.source(candidate.sourceKey) ?: continue
            val candidateQualitys = candidate.qualityKeys()
            val supportedQualitys = sourceInfo.qualitys
                .filter { candidateQualitys.isEmpty() || it in candidateQualitys }
                .ifEmpty { sourceInfo.qualitys }
            val qualityKeys = quality.toLxQualityAttempts(supportedQualitys)
            for (qualityKey in qualityKeys) {
                val requestPayload = JSONObject()
                    .put("source", sourceInfo.key)
                    .put("action", LX_ACTION_MUSIC_URL)
                    .put(
                        "info",
                        JSONObject()
                            .put("type", qualityKey)
                            .put("musicInfo", candidate.toLxMusicInfo())
                    )
                val raw = session.invokeRequest(requestPayload)
                val url = raw?.extractPlayableUrl()
                Log.d(
                    TAG,
                    "LX resolve ${candidate.sourceKey}:${candidate.songId} quality=$qualityKey raw=${raw?.take(80)} success=${!url.isNullOrBlank()}"
                )
                if (!url.isNullOrBlank()) return url
            }
        }
        return null
    }

    private suspend fun searchLxMatchedCandidates(
        song: Song,
        initInfo: LxInitInfo
    ): List<LxMusicInfoCandidate> = withContext(Dispatchers.IO) {
        val supportedKeys = initInfo.sources
            .filter { it.actions.contains(LX_ACTION_MUSIC_URL) }
            .map { it.key }
            .filter { it != "wy" && it != "local" }
            .toSet()
        if (supportedKeys.isEmpty()) return@withContext emptyList()

        val query = "${song.title} ${song.artist}".trim()
        val candidates = buildList {
            if ("kg" in supportedKeys) addAll(searchKugouCandidates(query))
            if ("kw" in supportedKeys) addAll(searchKuwoCandidates(query))
        }
        candidates
            .map { candidate -> candidate to scoreLxCandidate(song, candidate) }
            .filter { (_, score) -> score >= 70 }
            .sortedWith(
                compareByDescending<Pair<LxMusicInfoCandidate, Int>> { it.second }
                    .thenBy { sourceOrder(it.first.sourceKey) }
            )
            .map { it.first }
            .also {
                Log.d(TAG, "LX matched ${it.size} candidates for ${song.title} - ${song.artist}")
            }
    }

    private fun searchKugouCandidates(query: String): List<LxMusicInfoCandidate> =
        runCatching {
            val url = "https://songsearch.kugou.com/song_search_v2" +
                "?keyword=${query.urlEncode()}" +
                "&page=1&pagesize=25&userid=0&clientver=&platform=WebFilter" +
                "&filter=2&iscorrection=1&privilege_filter=0&area_code=1"
            val list = getJson(url)
                ?.optJSONObject("data")
                ?.optJSONArray("lists")
                ?: return@runCatching emptyList()
            buildList {
                for (index in 0 until list.length()) {
                    val item = list.optJSONObject(index) ?: continue
                    val songId = item.optString("Audioid").takeIf { it.isNotBlank() } ?: continue
                    val fileHash = item.optString("FileHash")
                    val qualitys = buildList {
                        item.kugouQuality("128k", "FileSize", "FileHash")?.let(::add)
                        item.kugouQuality("320k", "HQFileSize", "HQFileHash")?.let(::add)
                        item.kugouQuality("flac", "SQFileSize", "SQFileHash")?.let(::add)
                        item.kugouQuality("flac24bit", "ResFileSize", "ResFileHash")?.let(::add)
                    }.ifEmpty { defaultLxQualitys() }
                    add(
                        LxMusicInfoCandidate(
                            sourceKey = "kg",
                            songId = songId,
                            name = item.optString("SongName"),
                            singer = item.optString("SingerName"),
                            albumName = item.optString("AlbumName"),
                            albumId = item.optString("AlbumID").takeIf { it.isNotBlank() },
                            interval = item.optLong("Duration").secondsToLxIntervalString(),
                            qualitys = qualitys,
                            hash = fileHash.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrElse { error ->
            Log.d(TAG, "LX Kugou search failed: ${error.message}")
            emptyList()
        }

    private fun searchKuwoCandidates(query: String): List<LxMusicInfoCandidate> =
        runCatching {
            val url = "http://search.kuwo.cn/r.s" +
                "?client=kt&all=${query.urlEncode()}&pn=0&rn=25&uid=794762570" +
                "&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1" +
                "&ft=music&cluster=0&strategy=2012&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1"
            val list = getJson(url)?.optJSONArray("abslist") ?: return@runCatching emptyList()
            buildList {
                for (index in 0 until list.length()) {
                    val item = list.optJSONObject(index) ?: continue
                    val songId = item.optString("MUSICRID")
                        .removePrefix("MUSIC_")
                        .takeIf { it.isNotBlank() }
                        ?: continue
                    val durationSeconds = item.optLong("DURATION", 0L)
                    val qualitys = item.optString("N_MINFO")
                        .parseKuwoQualitys()
                        .ifEmpty { defaultLxQualitys() }
                    add(
                        LxMusicInfoCandidate(
                            sourceKey = "kw",
                            songId = songId,
                            name = item.optString("SONGNAME").htmlEntityDecode(),
                            singer = item.optString("ARTIST").htmlEntityDecode(),
                            albumName = item.optString("ALBUM").htmlEntityDecode(),
                            albumId = item.optString("ALBUMID").takeIf { it.isNotBlank() },
                            interval = durationSeconds.secondsToLxIntervalString(),
                            qualitys = qualitys
                        )
                    )
                }
            }
        }.getOrElse { error ->
            Log.d(TAG, "LX Kuwo search failed: ${error.message}")
            emptyList()
        }

    private fun getJson(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body?.string().orEmpty()
            JSONTokener(body).nextValue() as? JSONObject
        }
    }

    private suspend fun downloadText(url: String): String? = withContext(Dispatchers.IO) {
        scriptCache[url]?.let { cached ->
            Log.d(TAG, "Using cached source script bytes=${cached.length} url=$url")
            return@withContext cached
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "Download source failed status=${response.code} url=$url")
                    return@use null
                }
                response.body?.string()?.also {
                    scriptCache[url] = it
                    Log.d(TAG, "Downloaded source script bytes=${it.length} url=$url")
                }
            }
        }.getOrElse { error ->
            Log.d(TAG, "Download source failed: ${error.message}")
            null
        }
    }

    private suspend fun <T> runLxSession(
        script: String,
        source: CustomSourceConfig? = null,
        block: suspend (LxScriptSession) -> T?
    ): T? = withContext(Dispatchers.Main.immediate) {
        val session = LxScriptSession(
            context = appContext,
            client = client,
            scriptName = source?.name,
            allowShowUpdateAlert = source?.allowShowUpdateAlert == true
        )
        try {
            session.prepare(script)
            block(session)
        } finally {
            session.destroy()
        }
    }

    companion object {
        private const val TAG = "CustomPlaybackSource"
        private const val USER_AGENT = "Mozilla/5.0 SiListen/0.3 Android"
        private const val LX_ACTION_MUSIC_URL = "musicUrl"
    }
}

private data class LxScriptMetadata(
    val name: String? = null,
    val description: String? = null,
    val author: String? = null,
    val homepage: String? = null,
    val version: String? = null
) {
    companion object {
        fun parse(script: String): LxScriptMetadata {
            fun tag(name: String, maxLength: Int): String? =
                Regex("^\\s*\\*\\s*@$name\\s+(.+)$", RegexOption.MULTILINE)
                    .find(script)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { value ->
                        if (value.length > maxLength) value.take(maxLength) + "..." else value
                    }
            return LxScriptMetadata(
                name = tag("name", 24),
                description = tag("description", 36),
                author = tag("author", 56),
                homepage = tag("homepage", 1024),
                version = tag("version", 36)
            )
        }
    }
}

private data class LxInitInfo(
    val sources: List<LxSourceInfo>
) {
    fun source(key: String): LxSourceInfo? =
        sources.firstOrNull { it.key == key && it.actions.contains("musicUrl") }

    fun identitySourceInfos(song: Song): List<LxSourceInfo> {
        val identityKeys = buildList {
            song.sourceId.toLxSourceKey()?.let(::add)
            song.playbackSourceId?.toLxSourceKey()?.let(::add)
            song.providerIds.keys.mapNotNullTo(this) { it.toLxSourceKey() }
            if (!song.neteaseIdentityId().isNullOrBlank()) add("wy")
        }.distinct()
        val playableSources = sources.filter { it.actions.contains("musicUrl") }
        val preferred = identityKeys.mapNotNull { key ->
            playableSources.firstOrNull { it.key == key }
        }
        return preferred
    }
}

private data class LxSourceInfo(
    val key: String,
    val name: String,
    val actions: List<String>,
    val qualitys: List<String>
)

@SuppressLint("SetJavaScriptEnabled")
private class LxScriptSession(
    context: Context,
    private val client: OkHttpClient,
    private val scriptName: String?,
    private val allowShowUpdateAlert: Boolean
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val invokeResults = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val callIds = AtomicInteger(1)
    private val initDeferred = CompletableDeferred<LxInitInfo?>()
    private val bridge = LxBridge(
        client = client,
        mainHandler = mainHandler,
        evaluateOnMain = ::evaluateRawOnMain,
        onSend = ::handleSend,
        onInvokeResult = ::handleInvokeResult,
        scope = scope
    )
    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        addJavascriptInterface(bridge, "SiListenLxBridge")
    }

    suspend fun prepare(script: String) {
        val metadata = LxScriptMetadata.parse(script)
        loadBlankPage()
        evaluate(PRELUDE)
        evaluate(
            "globalThis.lx.currentScriptInfo = {" +
                "name:${JSONObject.quote(metadata.name.orEmpty())}," +
                "description:${JSONObject.quote(metadata.description.orEmpty())}," +
                "version:${JSONObject.quote(metadata.version.orEmpty())}," +
                "author:${JSONObject.quote(metadata.author.orEmpty())}," +
                "homepage:${JSONObject.quote(metadata.homepage.orEmpty())}," +
                "rawScript:${JSONObject.quote(script)}" +
                "};"
        )
        evaluate(script)
    }

    suspend fun init(): LxInitInfo? =
        withTimeoutOrNull(10_000L) { initDeferred.await() }

    suspend fun invokeRequest(payload: JSONObject): String? {
        val callId = callIds.getAndIncrement().toString()
        val deferred = CompletableDeferred<String>()
        invokeResults[callId] = deferred
        evaluate(
            "globalThis.__siListenInvokeAsync(" +
                "${JSONObject.quote(callId)}," +
                "${JSONObject.quote("request")}," +
                "${JSONObject.quote(payload.toString())}" +
                ");"
        )
        val raw = withTimeoutOrNull(14_000L) { deferred.await() }
        invokeResults.remove(callId)
        val json = raw?.let { JSONObject(it) } ?: return null
        if (!json.optBoolean("ok")) {
            Log.d("CustomPlaybackSource", "LX invoke failed: ${json.optString("error")}")
            return null
        }
        val result = json.opt("result") ?: return null
        return when (result) {
            is String -> result
            is JSONObject -> result.optString("url").ifBlank { result.optString("data") }
            else -> result.toString()
        }
    }

    fun destroy() {
        scope.cancel()
        mainHandler.post {
            runCatching {
                webView.removeJavascriptInterface("SiListenLxBridge")
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    private suspend fun loadBlankPage() = suspendCancellableCoroutine<Unit> { continuation ->
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
        webView.loadDataWithBaseURL("https://silisten.local/", "", "text/html", "UTF-8", null)
    }

    private suspend fun evaluate(script: String): String? = suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(script) { value ->
            if (continuation.isActive) continuation.resume(value?.decodeEvaluateJavascriptString())
        }
    }

    private fun evaluateRawOnMain(script: String) {
        mainHandler.post { webView.evaluateJavascript(script, null) }
    }

    private fun handleSend(payload: String) {
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return
        when (json.optString("eventName")) {
            "inited" -> handleInit(json)
            "updateAlert" -> handleUpdateAlert(json)
        }
    }

    private fun handleInit(json: JSONObject) {
        val data = json.optJSONObject("datas")
        val sourcesJson = data?.optJSONObject("sources") ?: JSONObject()
        val sources = buildList {
            val keys = sourcesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val source = sourcesJson.optJSONObject(key) ?: continue
                add(
                    LxSourceInfo(
                        key = key,
                        name = source.optString("name").ifBlank { key },
                        actions = source.optJSONArray("actions").toStringList(),
                        qualitys = source.optJSONArray("qualitys").toStringList()
                    )
                )
            }
        }
        if (!initDeferred.isCompleted) {
            initDeferred.complete(LxInitInfo(sources))
        }
    }

    private fun handleUpdateAlert(json: JSONObject) {
        if (!allowShowUpdateAlert) return
        val data = json.optJSONObject("datas") ?: return
        val log = data.optString("log").take(160).ifBlank { "发现自定义源更新" }
        val name = scriptName?.takeIf { it.isNotBlank() } ?: data.optString("name").ifBlank { "自定义源" }
        Log.d("CustomPlaybackSource", "LX update alert source=$name log=$log url=${data.optString("updateUrl")}")
        mainHandler.post {
            Toast.makeText(appContext, "自定义源「$name」发现更新：$log", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleInvokeResult(callId: String, payload: String) {
        invokeResults.remove(callId)?.complete(payload)
    }

    companion object {
        private val PRELUDE = """
            (function() {
              if (globalThis.__siListenLxReady) return;
              globalThis.__siListenLxReady = true;
              const handlers = {};
              const pendingHttp = {};
              let httpSeq = 1;
              const safeJsonParse = function(value, fallback) {
                try { return JSON.parse(value); } catch (error) { return fallback; }
              };
              const EVENT_NAMES = {
                inited: 'inited',
                request: 'request',
                updateAlert: 'updateAlert',
              };
              const musicSources = {
                kw: 'kw',
                kg: 'kg',
                tx: 'tx',
                wy: 'wy',
                mg: 'mg',
                local: 'local',
              };
              const toByteArray = function(value, encoding) {
                if (value instanceof Uint8Array) return value;
                if (Array.isArray(value)) return new Uint8Array(value);
                if (value instanceof ArrayBuffer) return new Uint8Array(value);
                if (typeof value !== 'string') return new Uint8Array([]);
                const enc = String(encoding || 'utf8').toLowerCase();
                if (enc === 'base64') {
                  const binary = atob(value);
                  const bytes = new Uint8Array(binary.length);
                  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
                  return bytes;
                }
                if (enc === 'hex') {
                  const pairs = value.match(/.{1,2}/g) || [];
                  return new Uint8Array(pairs.map(function(byte) { return parseInt(byte, 16) || 0; }));
                }
                return new TextEncoder().encode(value);
              };
              const bytesToString = function(value, format) {
                const bytes = toByteArray(value);
                const fmt = String(format || 'utf8').toLowerCase();
                if (fmt === 'hex') {
                  return Array.from(bytes).map(function(byte) {
                    return byte.toString(16).padStart(2, '0');
                  }).join('');
                }
                if (fmt === 'base64') {
                  let binary = '';
                  for (let i = 0; i < bytes.length; i += 0x8000) {
                    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
                  }
                  return btoa(binary);
                }
                return new TextDecoder().decode(bytes);
              };
              globalThis.MUSIC_SOURCE = musicSources;
              globalThis.musicSources = musicSources;
              globalThis.lx = {
                version: '2.10.0',
                env: 'mobile',
                currentScriptInfo: {},
                EVENT_NAMES,
                on(eventName, handler) {
                  handlers[eventName] = handler;
                  return Promise.resolve();
                },
                send(eventName, datas) {
                  SiListenLxBridge.onSend(JSON.stringify({ eventName, datas: datas || null }));
                  return Promise.resolve();
                },
                request(url, options, callback) {
                  const id = String(httpSeq++);
                  pendingHttp[id] = callback;
                  SiListenLxBridge.httpRequest(id, String(url), JSON.stringify(options || {}));
                  return function cancelHttp() {
                    delete pendingHttp[id];
                    SiListenLxBridge.cancelRequest(id);
                  };
                },
                utils: {
                  buffer: {
                    from(value, encoding) { return toByteArray(value, encoding); },
                    bufToString(value, format) { return bytesToString(value, format); },
                  },
                  crypto: {
                    md5(value) { return SiListenLxBridge.md5(String(value)); },
                    aesEncrypt(buffer, mode, key, iv) {
                      return new Uint8Array(safeJsonParse(SiListenLxBridge.aesEncrypt(
                        bytesToString(buffer, 'base64'),
                        String(mode || ''),
                        bytesToString(key, 'base64'),
                        iv == null ? '' : bytesToString(iv, 'base64')
                      ), []));
                    },
                    rsaEncrypt(buffer, key) {
                      return new Uint8Array(safeJsonParse(SiListenLxBridge.rsaEncrypt(
                        bytesToString(buffer, 'base64'),
                        String(key || '')
                      ), []));
                    },
                    randomBytes(size) { return new Uint8Array(safeJsonParse(SiListenLxBridge.randomBytes(Number(size || 0)), [])); },
                  },
                },
              };
              globalThis.EVENT_NAMES = EVENT_NAMES;
              globalThis.Buffer = {
                from(value, encoding) { return toByteArray(value, encoding); },
              };
              globalThis.request = globalThis.lx.request;
              globalThis.on = globalThis.lx.on;
              globalThis.send = globalThis.lx.send;
              globalThis.httpFetch = function(url, options) {
                return new Promise(function(resolve, reject) {
                  globalThis.lx.request(url, options || {}, function(err, resp, body) {
                    if (err) {
                      reject(err);
                      return;
                    }
                    const response = resp || {};
                    if (body !== undefined) response.body = body;
                    response.text = function() { return Promise.resolve(String(response.rawBody == null ? response.body || '' : response.rawBody)); };
                    response.json = function() {
                      if (response.body != null && typeof response.body === 'object') return Promise.resolve(response.body);
                      return Promise.resolve(safeJsonParse(response.body || '{}', {}));
                    };
                    resolve(response);
                  });
                });
              };
              globalThis.__siListenHttpResponse = function(id, error, respJson, body) {
                const callback = pendingHttp[id];
                if (!callback) return;
                delete pendingHttp[id];
                const resp = safeJsonParse(respJson || '{}', {});
                const parsedBody = typeof body === 'string' ? safeJsonParse(body, body) : body;
                resp.rawBody = body == null ? '' : String(body);
                if (body !== undefined && body !== null) resp.body = parsedBody;
                callback(error ? { message: String(error) } : null, resp, parsedBody);
              };
              globalThis.__siListenInvokeAsync = function(callId, eventName, dataJson) {
                Promise.resolve()
                  .then(function() {
                    const handler = handlers[eventName];
                    if (!handler) throw new Error('LX 脚本没有注册 ' + eventName + ' 事件');
                    return handler(safeJsonParse(dataJson, {}));
                  })
                  .then(function(result) {
                    SiListenLxBridge.onInvokeResult(callId, JSON.stringify({
                      ok: true,
                      result: result == null ? null : result,
                    }));
                  })
                  .catch(function(error) {
                    SiListenLxBridge.onInvokeResult(callId, JSON.stringify({
                      ok: false,
                      error: String(error && (error.stack || error.message) || error),
                    }));
                  });
              };
            })();
        """.trimIndent()
    }
}

private class LxBridge(
    private val client: OkHttpClient,
    private val mainHandler: Handler,
    private val evaluateOnMain: (String) -> Unit,
    private val onSend: (String) -> Unit,
    private val onInvokeResult: (String, String) -> Unit,
    private val scope: CoroutineScope
) {
    private val calls = ConcurrentHashMap<String, okhttp3.Call>()
    private val random = SecureRandom()

    @JavascriptInterface
    fun onSend(payload: String) {
        onSend.invoke(payload)
    }

    @JavascriptInterface
    fun onInvokeResult(callId: String, payload: String) {
        onInvokeResult.invoke(callId, payload)
    }

    @JavascriptInterface
    fun httpRequest(id: String, url: String, optionsJson: String) {
        scope.launch {
            val result = runCatching {
                val request = buildRequest(url, optionsJson)
                val call = client.newCall(request)
                calls[id] = call
                call.execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    Log.d(
                        "CustomPlaybackSource",
                        "LX HTTP ${request.method} ${request.url.host}${request.url.encodedPath} status=${response.code} body=${body.take(120)}"
                    )
                    val resp = JSONObject()
                        .put("statusCode", response.code)
                        .put("status", response.code)
                        .put("url", response.request.url.toString())
                        .put("headers", JSONObject().also { headers ->
                            response.headers.forEach { header ->
                                headers.put(header.first, header.second)
                            }
                        })
                    HttpBridgeResult(resp, body, null)
                }
            }.getOrElse { error ->
                Log.d("CustomPlaybackSource", "LX HTTP failed url=$url error=${error.message}")
                HttpBridgeResult(JSONObject(), "", error.message ?: error.javaClass.simpleName)
            }
            calls.remove(id)
            mainHandler.post {
                val errorArg = result.error?.let(JSONObject::quote) ?: "null"
                evaluateOnMain(
                    "globalThis.__siListenHttpResponse(" +
                        "${JSONObject.quote(id)}," +
                        "$errorArg," +
                        "${JSONObject.quote(result.response.toString())}," +
                        "${JSONObject.quote(result.body)}" +
                        ");"
                )
            }
        }
    }

    @JavascriptInterface
    fun cancelRequest(id: String) {
        calls.remove(id)?.cancel()
    }

    @JavascriptInterface
    fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    @JavascriptInterface
    fun aesEncrypt(dataBase64: String, mode: String, keyBase64: String, ivBase64: String): String =
        runCatching {
            val transformation = when (mode.lowercase()) {
                "aes-128-cbc" -> "AES/CBC/PKCS5Padding"
                "aes-128-ecb" -> "AES/ECB/NoPadding"
                else -> return "[]"
            }
            val data = dataBase64.decodeBase64Bytes()
            val key = keyBase64.decodeBase64Bytes()
            val cipher = Cipher.getInstance(transformation)
            val secretKey = SecretKeySpec(key, "AES")
            if (transformation.contains("/CBC/")) {
                val iv = ByteArray(16)
                val decodedIv = ivBase64.decodeBase64Bytes()
                decodedIv.copyInto(iv, endIndex = decodedIv.size.coerceAtMost(16))
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            }
            cipher.doFinal(data).toJsonByteArray()
        }.getOrElse { error ->
            Log.d("CustomPlaybackSource", "LX AES encrypt failed: ${error.message}")
            "[]"
        }

    @JavascriptInterface
    fun rsaEncrypt(dataBase64: String, publicKey: String): String =
        runCatching {
            val cleanKey = publicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace(Regex("\\s+"), "")
            val keySpec = X509EncodedKeySpec(cleanKey.decodeBase64Bytes())
            val key = KeyFactory.getInstance("RSA").generatePublic(keySpec)
            val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.doFinal(dataBase64.decodeBase64Bytes()).toJsonByteArray()
        }.getOrElse { error ->
            Log.d("CustomPlaybackSource", "LX RSA encrypt failed: ${error.message}")
            "[]"
        }

    @JavascriptInterface
    fun randomBytes(size: Int): String {
        val length = size.coerceIn(0, 4096)
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return JSONArray().also { array ->
            bytes.forEach { byte -> array.put(byte.toInt() and 0xff) }
        }.toString()
    }

    private fun buildRequest(url: String, optionsJson: String): Request {
        val options = runCatching { JSONObject(optionsJson) }.getOrNull() ?: JSONObject()
        val method = options.optString("method", "GET").ifBlank { "GET" }.uppercase()
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 SiListen/0.3 Android")
        options.optJSONObject("headers")?.let { headers ->
            val keys = headers.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                builder.header(key, headers.optString(key))
            }
        }
        val body = when {
            options.has("body") -> options.optString("body").toRequestBody(
                options.optString("contentType", "application/json; charset=utf-8").toMediaTypeOrNull()
            )
            options.has("form") -> {
                val form = options.optJSONObject("form") ?: JSONObject()
                val formBuilder = FormBody.Builder()
                val keys = form.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    formBuilder.add(key, form.optString(key))
                }
                formBuilder.build()
            }
            options.has("formData") -> {
                val form = options.optJSONObject("formData") ?: JSONObject()
                val formBuilder = FormBody.Builder()
                val keys = form.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    formBuilder.add(key, form.optString(key))
                }
                formBuilder.build()
            }
            method == "GET" || method == "HEAD" -> null
            else -> ByteArray(0).toRequestBody(null)
        }
        return if (body == null) {
            builder.method(method, null).build()
        } else {
            builder.method(method, body).build()
        }
    }
}

private data class HttpBridgeResult(
    val response: JSONObject,
    val body: String,
    val error: String?
)

private fun String.decodeBase64Bytes(): ByteArray =
    Base64.decode(this, Base64.DEFAULT)

private fun ByteArray.toJsonByteArray(): String =
    JSONArray().also { array ->
        forEach { byte -> array.put(byte.toInt() and 0xff) }
    }.toString()

private data class LxQualityInfo(
    val type: String,
    val size: String? = null,
    val hash: String? = null
)

private data class LxMusicInfoCandidate(
    val sourceKey: String,
    val songId: String,
    val name: String,
    val singer: String,
    val albumName: String,
    val albumId: String? = null,
    val interval: String,
    val coverUrl: String? = null,
    val qualitys: List<LxQualityInfo> = defaultLxQualitys(),
    val hash: String? = null,
    val strMediaMid: String? = null,
    val albumMid: String? = null,
    val numericSongId: String? = null,
    val copyrightId: String? = null
) {
    fun qualityKeys(): List<String> = qualitys.map { it.type }

    fun toLxMusicInfo(): JSONObject {
        val typesJson = JSONArray()
        val typeMapJson = JSONObject()
        qualitys.forEach { quality ->
            val typeItem = JSONObject()
                .put("type", quality.type)
                .put("size", quality.size ?: JSONObject.NULL)
            if (!quality.hash.isNullOrBlank()) typeItem.put("hash", quality.hash)
            typesJson.put(typeItem)

            val mapItem = JSONObject().put("size", quality.size ?: JSONObject.NULL)
            if (!quality.hash.isNullOrBlank()) mapItem.put("hash", quality.hash)
            typeMapJson.put(quality.type, mapItem)
        }

        val meta = JSONObject()
            .put("songId", songId)
            .put("albumName", albumName)
            .put("picUrl", coverUrl.orEmpty())
            .put("qualitys", typesJson)
            .put("_qualitys", typeMapJson)
        albumId?.takeIf { it.isNotBlank() }?.let { meta.put("albumId", it) }
        hash?.takeIf { it.isNotBlank() }?.let { meta.put("hash", it) }
        strMediaMid?.takeIf { it.isNotBlank() }?.let { meta.put("strMediaMid", it) }
        albumMid?.takeIf { it.isNotBlank() }?.let { meta.put("albumMid", it) }
        numericSongId?.takeIf { it.isNotBlank() }?.let { meta.put("id", it) }
        copyrightId?.takeIf { it.isNotBlank() }?.let { meta.put("copyrightId", it) }

        return JSONObject()
            .put("id", "${sourceKey}_$songId")
            .put("name", name)
            .put("title", name)
            .put("singer", singer)
            .put("singerName", singer)
            .put("artist", singer)
            .put("source", sourceKey)
            .put("songmid", songId)
            .put("mid", songId)
            .put("interval", interval)
            .put("albumName", albumName)
            .put("album", albumName)
            .put("albumId", albumId.orEmpty())
            .put("img", coverUrl.orEmpty())
            .put("pic", coverUrl.orEmpty())
            .put("typeUrl", JSONObject())
            .put("types", typesJson)
            .put("_types", typeMapJson)
            .put("meta", meta)
            .apply {
                hash?.takeIf { it.isNotBlank() }?.let { put("hash", it) }
                strMediaMid?.takeIf { it.isNotBlank() }?.let { put("strMediaMid", it) }
                albumMid?.takeIf { it.isNotBlank() }?.let { put("albumMid", it) }
                numericSongId?.takeIf { it.isNotBlank() }?.let { put("songId", it) }
                copyrightId?.takeIf { it.isNotBlank() }?.let { put("copyrightId", it) }
            }
    }

    companion object {
        fun fromSong(song: Song, sourceKey: String): LxMusicInfoCandidate? {
            val sourceSongId = when (sourceKey) {
                "wy" -> song.neteaseIdentityId().orEmpty()
                else -> song.providerIds[sourceKey].orEmpty()
            }.ifBlank { return null }
            return LxMusicInfoCandidate(
                sourceKey = sourceKey,
                songId = sourceSongId,
                name = song.title,
                singer = song.artist,
                albumName = song.album,
                interval = song.durationMs.toLxIntervalString(),
                coverUrl = song.coverUrl
            )
        }
    }
}

private fun defaultLxQualitys(): List<LxQualityInfo> = listOf(
    LxQualityInfo("128k"),
    LxQualityInfo("320k"),
    LxQualityInfo("flac"),
    LxQualityInfo("flac24bit")
)

private fun JSONObject.kugouQuality(
    type: String,
    sizeKey: String,
    hashKey: String
): LxQualityInfo? {
    val hash = optString(hashKey).takeIf { it.isNotBlank() } ?: return null
    val size = optLong(sizeKey, 0L).takeIf { it > 0L }?.bytesToSizeLabel()
    return LxQualityInfo(type = type, size = size, hash = hash)
}

private fun String.parseKuwoQualitys(): List<LxQualityInfo> {
    if (isBlank()) return emptyList()
    val regex = Regex("level:(\\w+),bitrate:(\\d+),format:(\\w+),size:([\\w.]+)")
    return split(';').mapNotNull { raw ->
        val match = regex.find(raw) ?: return@mapNotNull null
        val type = when (match.groupValues[2]) {
            "4000" -> "flac24bit"
            "2000" -> "flac"
            "320" -> "320k"
            "128" -> "128k"
            else -> return@mapNotNull null
        }
        LxQualityInfo(type = type, size = match.groupValues[4].uppercase())
    }.asReversed()
}

private fun scoreLxCandidate(song: Song, candidate: LxMusicInfoCandidate): Int {
    val originalTitle = song.title.normalizedLxText()
    val candidateTitle = candidate.name.normalizedLxText()
    val originalArtist = song.artist.normalizedLxText()
    val candidateArtist = candidate.singer.normalizedLxText()
    val originalAlbum = song.album.normalizedLxText()
    val candidateAlbum = candidate.albumName.normalizedLxText()

    var score = 0
    if (originalTitle == candidateTitle) {
        score += 50
    } else if (
        originalTitle.isNotBlank() &&
        candidateTitle.isNotBlank() &&
        (originalTitle.contains(candidateTitle) || candidateTitle.contains(originalTitle))
    ) {
        score += 32
    }

    if (originalArtist.isNotBlank() && candidateArtist.isNotBlank()) {
        val originalArtists = originalArtist.splitLxArtistTokens()
        val candidateArtists = candidateArtist.splitLxArtistTokens()
        if (originalArtists.any { it in candidateArtists }) {
            score += 28
        } else if (originalArtists.any { token -> candidateArtists.any { it.contains(token) || token.contains(it) } }) {
            score += 18
        }
    }

    if (originalAlbum.isNotBlank() && candidateAlbum.isNotBlank() && originalAlbum == candidateAlbum) {
        score += 8
    }

    val originalDuration = (song.durationMs / 1000L).coerceAtLeast(0L)
    val candidateDuration = candidate.interval.toIntervalSeconds()
    if (originalDuration > 0L && candidateDuration > 0L) {
        val diff = abs(originalDuration - candidateDuration)
        score += when {
            diff <= 4L -> 18
            diff <= 8L -> 10
            diff <= 15L -> 2
            else -> -24
        }
    }

    return score.coerceIn(0, 100)
}

private fun sourceOrder(sourceKey: String): Int = when (sourceKey) {
    "kg" -> 0
    "kw" -> 1
    "tx" -> 2
    "mg" -> 3
    else -> 99
}

private fun String.toLxSourceKey(): String? = when (this) {
    "netease", "wy" -> "wy"
    "kuwo", "kw" -> "kw"
    "kugou", "kg" -> "kg"
    "qq", "tx" -> "tx"
    "migu", "mg" -> "mg"
    "local" -> "local"
    else -> null
}

private fun Long.toLxIntervalString(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    return totalSeconds.secondsToLxIntervalString()
}

private fun Long.secondsToLxIntervalString(): String {
    val totalSeconds = coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun String.toIntervalSeconds(): Long {
    if (isBlank()) return 0L
    return split(':')
        .mapNotNull { it.toLongOrNull() }
        .fold(0L) { total, value -> total * 60L + value }
}

private fun Long.bytesToSizeLabel(): String {
    if (this <= 0L) return ""
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${this}${units[unitIndex]}"
    } else {
        "%.2f%s".format(value, units[unitIndex])
    }
}

private fun String.normalizedLxText(): String =
    lowercase()
        .replace(Regex("\\s+"), "")
        .replace(Regex("[\\[\\]【】《》（）()\"'“”‘’、，。；;:：!！?？/\\\\|&\\-_.]"), "")
        .replace(Regex("(live|伴奏|纯音乐|片段|cover|翻自|完整版|新版|旧版|remix)", RegexOption.IGNORE_CASE), "")
        .trim()

private fun String.splitLxArtistTokens(): Set<String> =
    split("/", "&", "、", ",", "，", ";", "；", "和", "feat", "ft")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()

private fun String.htmlEntityDecode(): String =
    replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

private fun String.toPlaybackHttpUrl(song: Song, quality: PlaybackQuality): okhttp3.HttpUrl? {
    val neteaseId = song.neteaseIdentityId().orEmpty()
    val values = mapOf(
        "id" to song.id,
        "songId" to song.id,
        "songmid" to song.id,
        "mid" to song.id,
        "neteaseId" to neteaseId.ifBlank { song.id },
        "title" to song.title,
        "name" to song.title,
        "artist" to song.artist,
        "singer" to song.artist,
        "album" to song.album,
        "duration" to song.durationMs.toString(),
        "durationMs" to song.durationMs.toString(),
        "sourceId" to song.sourceId,
        "quality" to quality.toDirectQualityKey(),
        "level" to quality.neteaseLevel,
        "br" to quality.bitrate.toString()
    )
    val templated = values.entries.fold(this.trim()) { url, (key, value) ->
        url.replace("{$key}", value.urlEncode())
    }
    val base = templated.toHttpUrlOrNull() ?: return null
    val existingNames = base.queryParameterNames
    return base.newBuilder().apply {
        values.forEach { (key, value) ->
            if (value.isNotBlank() && key !in existingNames) addQueryParameter(key, value)
        }
    }.build()
}

private fun PlaybackQuality.toDirectQualityKey(): String = when (this) {
    PlaybackQuality.Standard -> "128k"
    PlaybackQuality.Higher -> "192k"
    PlaybackQuality.ExHigh -> "320k"
    PlaybackQuality.Lossless -> "flac"
}

private fun PlaybackQuality.toLxQuality(supported: List<String>): String {
    val preferred = when (this) {
        PlaybackQuality.Standard -> listOf("128k")
        PlaybackQuality.Higher -> listOf("320k", "128k")
        PlaybackQuality.ExHigh -> listOf("320k", "flac", "128k")
        PlaybackQuality.Lossless -> listOf("flac", "flac24bit", "320k", "128k")
    }
    return preferred.firstOrNull { it in supported }
        ?: supported.firstOrNull()
        ?: "320k"
}

private fun PlaybackQuality.toLxQualityAttempts(supported: List<String>): List<String> {
    val normalizedSupported = supported.ifEmpty { listOf("320k", "128k") }
    val primary = toLxQuality(normalizedSupported)
    val fallback = listOf("320k", "128k", "flac", "flac24bit")
        .filter { it in normalizedSupported && it != primary }
    return (listOf(primary) + fallback).distinct()
}

private fun String.extractPlayableUrl(): String? {
    val trimmed = trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("{") || trimmed.startsWith("[") -> runCatching {
            JSONTokener(trimmed).nextValue().findPlayableUrl()
        }.getOrNull()
        else -> null
    }?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
}

private fun Any?.findPlayableUrl(depth: Int = 0): String? {
    if (this == null || depth > 6) return null
    if (this is String) {
        val value = trim()
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("{") || value.startsWith("[") -> runCatching {
                JSONTokener(value).nextValue().findPlayableUrl(depth + 1)
            }.getOrNull()
            else -> null
        }
    }
    if (this is JSONArray) {
        for (index in 0 until length()) {
            opt(index).findPlayableUrl(depth + 1)?.let { return it }
        }
        return null
    }
    if (this is JSONObject) {
        val likelyKeys = listOf(
            "url",
            "playUrl",
            "play_url",
            "musicUrl",
            "music_url",
            "audioUrl",
            "audio_url",
            "src",
            "link",
            "location"
        )
        for (key in likelyKeys) {
            if (has(key)) opt(key).findPlayableUrl(depth + 1)?.let { return it }
        }
        val keys = keys()
        while (keys.hasNext()) {
            opt(keys.next()).findPlayableUrl(depth + 1)?.let { return it }
        }
    }
    return null
}

private fun String.decodeEvaluateJavascriptString(): String? {
    if (this == "null") return null
    return runCatching { JSONTokener(this).nextValue() as? String }
        .getOrNull()
        ?: this
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

private fun org.json.JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = optString(index)
            if (value.isNotBlank()) add(value)
        }
    }
}
