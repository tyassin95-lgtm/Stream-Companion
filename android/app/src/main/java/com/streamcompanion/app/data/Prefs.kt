package com.streamcompanion.app.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

/** Persistent app state: paired servers, device identity, settings and watch progress. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("stream_companion", Context.MODE_PRIVATE)

    private val _servers = MutableStateFlow(readList("servers", SavedServer.serializer()))
    val servers: StateFlow<List<SavedServer>> = _servers.asStateFlow()

    private val _progress = MutableStateFlow(readList("progress", Progress.serializer()))
    val progress: StateFlow<List<Progress>> = _progress.asStateFlow()

    private val _settings = MutableStateFlow(
        Settings(
            quality = sp.getString("quality", "auto") ?: "auto",
            alwaysAdaptive = sp.getBoolean("alwaysAdaptive", false),
            autoSubtitles = sp.getBoolean("autoSubtitles", false),
        ),
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    /** Random id that identifies this install to servers. */
    val deviceId: String
        get() = sp.getString("deviceId", null) ?: Crypto.randomHex(12).also { sp.edit().putString("deviceId", it).apply() }

    val deviceName: String
        get() {
            val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
            val model = Build.MODEL.orEmpty()
            return if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model".trim()
        }

    var lastServerId: String?
        get() = sp.getString("lastServerId", null)
        set(v) { sp.edit().putString("lastServerId", v).apply() }

    private fun <T> readList(key: String, ser: kotlinx.serialization.KSerializer<T>): List<T> =
        runCatching { AppJson.decodeFromString(ListSerializer(ser), sp.getString(key, "[]") ?: "[]") }.getOrDefault(emptyList())

    // ---- servers
    fun server(id: String): SavedServer? = _servers.value.firstOrNull { it.id == id }

    fun saveServer(s: SavedServer) {
        val list = _servers.value.filter { it.id != s.id } + s
        _servers.value = list
        sp.edit().putString("servers", AppJson.encodeToString(ListSerializer(SavedServer.serializer()), list)).apply()
    }

    fun forgetServer(id: String) {
        val list = _servers.value.filter { it.id != id }
        _servers.value = list
        sp.edit().putString("servers", AppJson.encodeToString(ListSerializer(SavedServer.serializer()), list)).apply()
        if (lastServerId == id) lastServerId = null
        saveProgress(_progress.value.filter { it.serverId != id })
    }

    /** The server moved (e.g. DHCP gave the PC a new address): keep the credentials, update the address. */
    fun updateAddress(id: String, host: String, port: Int) {
        val s = server(id) ?: return
        if (s.host != host || s.port != port) saveServer(s.copy(host = host, port = port))
    }

    // ---- settings
    fun updateSettings(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        _settings.value = s
        sp.edit().putString("quality", s.quality).putBoolean("alwaysAdaptive", s.alwaysAdaptive)
            .putBoolean("autoSubtitles", s.autoSubtitles).apply()
    }

    // ---- watch progress
    fun progressFor(serverId: String, videoId: String): Progress? =
        _progress.value.firstOrNull { it.serverId == serverId && it.videoId == videoId }

    fun saveProgress(p: Progress) {
        val list = (_progress.value.filterNot { it.serverId == p.serverId && it.videoId == p.videoId } + p)
            .sortedByDescending { it.updatedAt }.take(200)
        saveProgress(list)
    }

    fun clearProgress(serverId: String, videoId: String) =
        saveProgress(_progress.value.filterNot { it.serverId == serverId && it.videoId == videoId })

    private fun saveProgress(list: List<Progress>) {
        _progress.value = list
        sp.edit().putString("progress", AppJson.encodeToString(ListSerializer(Progress.serializer()), list)).apply()
    }
}
