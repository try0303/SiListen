package com.silisten.app.data.model

object SourcePlatformIds {
    const val ALL = "all"
    const val NETEASE = "netease"
    const val LOCAL = "local"
    const val DEMO = "demo"

    // Legacy ids are kept only so older saved/search code can be migrated safely.
    const val KUWO = "kuwo"
    const val QQ = "qq"
    const val KUGOU = "kugou"
    const val MIGU = "migu"
}

data class SourcePlatformOption(
    val id: String,
    val lxId: String,
    val label: String,
    val description: String
)

val BuiltInSearchPlatforms = listOf(
    SourcePlatformOption(SourcePlatformIds.NETEASE, "wy", "网易云音乐", "网易云曲库、歌单、专辑、歌手与账号体系"),
    SourcePlatformOption(SourcePlatformIds.KUWO, "kw", "酷我音乐", "LX 同款酷我 musicSearch / comment 模块"),
    SourcePlatformOption(SourcePlatformIds.KUGOU, "kg", "酷狗音乐", "LX 同款酷狗 musicSearch / comment 模块"),
    SourcePlatformOption(SourcePlatformIds.QQ, "tx", "QQ 音乐", "QQ 搜索与评论模块，播放仍交给自定义音源解析"),
    SourcePlatformOption(SourcePlatformIds.MIGU, "mg", "咪咕音乐", "LX 同款咪咕 musicSearch / comment 模块")
)

val BuiltInCommentPlatforms = BuiltInSearchPlatforms

fun normalizeBuiltInPlatformId(id: String): String = when (id) {
    "wy" -> SourcePlatformIds.NETEASE
    "kw" -> SourcePlatformIds.KUWO
    "kg" -> SourcePlatformIds.KUGOU
    "tx" -> SourcePlatformIds.QQ
    "mg" -> SourcePlatformIds.MIGU
    else -> id
}

fun builtInPlatformName(id: String): String =
    BuiltInSearchPlatforms.firstOrNull { it.id == normalizeBuiltInPlatformId(id) }?.label ?: when (id) {
        SourcePlatformIds.ALL -> "全部音源"
        SourcePlatformIds.LOCAL -> "本地音乐"
        SourcePlatformIds.DEMO -> "示例音乐"
        else -> id
    }

enum class CustomPlaybackSourceType(
    val label: String,
    val description: String
) {
    LxScript(
        label = "LX 音源脚本",
        description = "兼容洛雪音乐自定义源脚本，用脚本解析真实播放地址"
    ),
    DirectHttp(
        label = "直接播放接口",
        description = "普通 HTTP 接口，传入歌曲信息后直接返回播放地址"
    )
}

data class CustomSourceConfig(
    val id: String,
    val name: String,
    val endpoint: String,
    val enabled: Boolean = true,
    val type: CustomPlaybackSourceType = CustomPlaybackSourceType.LxScript,
    val description: String = "",
    val author: String = "",
    val homepage: String = "",
    val version: String = "",
    val allowShowUpdateAlert: Boolean = true,
    val supportedSources: List<String> = emptyList(),
    val script: String = ""
)

data class SourceSettingsState(
    val autoSourceFallbackEnabled: Boolean = true,
    val enabledSearchPlatformIds: Set<String> = BuiltInSearchPlatforms.mapTo(linkedSetOf()) { it.id },
    val enabledCommentPlatformIds: Set<String> = BuiltInCommentPlatforms.mapTo(linkedSetOf()) { it.id },
    val customSources: List<CustomSourceConfig> = emptyList()
) {
    fun sourceName(id: String): String =
        customSources.firstOrNull { it.id == id }?.name ?: when (id) {
            SourcePlatformIds.NETEASE,
            SourcePlatformIds.KUWO,
            SourcePlatformIds.KUGOU,
            SourcePlatformIds.QQ,
            SourcePlatformIds.MIGU,
            SourcePlatformIds.ALL -> builtInPlatformName(id)
            SourcePlatformIds.LOCAL -> "本地音乐"
            SourcePlatformIds.DEMO -> "示例音乐"
            else -> id
        }
}

fun inferCustomSourceType(endpoint: String): CustomPlaybackSourceType {
    val normalized = endpoint.trim().substringBefore('?').lowercase()
    return if (normalized.endsWith(".js")) {
        CustomPlaybackSourceType.LxScript
    } else {
        CustomPlaybackSourceType.DirectHttp
    }
}

fun normalizeCustomSourceType(
    endpoint: String,
    selectedType: CustomPlaybackSourceType
): CustomPlaybackSourceType =
    if (inferCustomSourceType(endpoint) == CustomPlaybackSourceType.LxScript) {
        CustomPlaybackSourceType.LxScript
    } else {
        selectedType
    }
