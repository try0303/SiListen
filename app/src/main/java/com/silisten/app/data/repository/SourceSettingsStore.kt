package com.silisten.app.data.repository

import android.content.Context
import com.silisten.app.data.model.BuiltInCommentPlatforms
import com.silisten.app.data.model.BuiltInSearchPlatforms
import com.silisten.app.data.model.CustomPlaybackSourceType
import com.silisten.app.data.model.CustomSourceConfig
import com.silisten.app.data.model.SourcePlatformIds
import com.silisten.app.data.model.SourceSettingsState
import com.silisten.app.data.model.inferCustomSourceType
import com.silisten.app.data.model.normalizeBuiltInPlatformId
import com.silisten.app.data.model.normalizeCustomSourceType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SourceSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("source_settings", Context.MODE_PRIVATE)

    fun load(): SourceSettingsState {
        val defaults = SourceSettingsState()
        val raw = preferences.getString(KEY_STATE, null) ?: return defaults
        return runCatching {
            val json = JSONObject(raw)
            SourceSettingsState(
                autoSourceFallbackEnabled = json.optBoolean(
                    "autoSourceFallbackEnabled",
                    defaults.autoSourceFallbackEnabled
                ),
                enabledSearchPlatformIds = if (json.has("enabledSearchPlatformIds")) {
                    json.optJSONArray("enabledSearchPlatformIds").toPlatformIdSet()
                } else {
                    migrateLegacyNeteaseSwitch(
                        enabled = json.optBoolean("searchUsesNeteaseCatalog", true),
                        defaults = defaults.enabledSearchPlatformIds
                    )
                },
                enabledCommentPlatformIds = if (json.has("enabledCommentPlatformIds")) {
                    json.optJSONArray("enabledCommentPlatformIds").toPlatformIdSet()
                } else {
                    migrateLegacyNeteaseSwitch(
                        enabled = json.optBoolean("commentsUseNeteaseIdentity", true),
                        defaults = defaults.enabledCommentPlatformIds
                    )
                },
                customSources = json.optJSONArray("customSources").toCustomSources()
            )
        }.getOrDefault(defaults)
    }

    fun save(state: SourceSettingsState) {
        val json = JSONObject()
            .put("autoSourceFallbackEnabled", state.autoSourceFallbackEnabled)
            .put(
                "enabledSearchPlatformIds",
                state.enabledSearchPlatformIds.toPlatformJsonArray(BuiltInSearchPlatforms.map { it.id }.toSet())
            )
            .put(
                "enabledCommentPlatformIds",
                state.enabledCommentPlatformIds.toPlatformJsonArray(BuiltInCommentPlatforms.map { it.id }.toSet())
            )
            .put("customSources", state.customSources.toCustomSourceJsonArray())
        preferences.edit().putString(KEY_STATE, json.toString()).apply()
    }

    private fun migrateLegacyNeteaseSwitch(
        enabled: Boolean,
        defaults: Set<String>
    ): Set<String> =
        if (enabled) {
            defaults
        } else {
            defaults - SourcePlatformIds.NETEASE
        }.ifEmpty {
            setOf(SourcePlatformIds.NETEASE)
        }

    companion object {
        private const val KEY_STATE = "state"

        fun newCustomSourceId(): String = "custom_${UUID.randomUUID().toString().replace("-", "")}"
    }
}

private fun JSONArray?.toCustomSources(): List<CustomSourceConfig> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val json = optJSONObject(index) ?: continue
            val id = json.optString("id")
            val name = json.optString("name")
            val endpoint = json.optString("endpoint")
            if (id.isBlank() || name.isBlank() || endpoint.isBlank()) continue
            add(
                CustomSourceConfig(
                    id = id,
                    name = name,
                    endpoint = endpoint,
                    enabled = json.optBoolean("enabled", true),
                    type = normalizeCustomSourceType(
                        endpoint,
                        json.optString("type").toCustomPlaybackSourceType(endpoint)
                    ),
                    description = json.optString("description"),
                    author = json.optString("author"),
                    homepage = json.optString("homepage"),
                    version = json.optString("version"),
                    allowShowUpdateAlert = json.optBoolean("allowShowUpdateAlert", true),
                    supportedSources = json.optJSONArray("supportedSources").toStringList(),
                    script = json.optString("script")
                )
            )
        }
    }
}

private fun String.toCustomPlaybackSourceType(endpoint: String): CustomPlaybackSourceType =
    runCatching { CustomPlaybackSourceType.valueOf(this) }
        .getOrNull()
        ?: inferCustomSourceType(endpoint)

private fun List<CustomSourceConfig>.toCustomSourceJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { source ->
        array.put(
            JSONObject()
                .put("id", source.id)
                .put("name", source.name)
                .put("endpoint", source.endpoint)
                .put("enabled", source.enabled)
                .put("type", source.type.name)
                .put("description", source.description)
                .put("author", source.author)
                .put("homepage", source.homepage)
                .put("version", source.version)
                .put("allowShowUpdateAlert", source.allowShowUpdateAlert)
                .put("script", source.script)
                .put("supportedSources", JSONArray().also { supported ->
                    source.supportedSources.forEach(supported::put)
                })
        )
    }
}

private fun JSONArray?.toPlatformIdSet(): Set<String> {
    if (this == null) return emptySet()
    return buildSet {
        for (index in 0 until length()) {
            val id = normalizeBuiltInPlatformId(optString(index))
            if (id.isNotBlank()) add(id)
        }
    }
}

private fun Set<String>.toPlatformJsonArray(allowedIds: Set<String>): JSONArray =
    JSONArray().also { array ->
        map(::normalizeBuiltInPlatformId)
            .filter { it in allowedIds }
            .distinct()
            .forEach(array::put)
    }

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}
