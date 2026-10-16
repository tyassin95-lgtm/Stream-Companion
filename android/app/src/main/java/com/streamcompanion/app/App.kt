package com.streamcompanion.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.streamcompanion.app.data.Discovery
import com.streamcompanion.app.data.Http
import com.streamcompanion.app.data.Prefs
import com.streamcompanion.app.data.SavedServer
import com.streamcompanion.app.data.ServerApi
import okhttp3.OkHttpClient

class App : Application(), ImageLoaderFactory {
    lateinit var prefs: Prefs
        private set
    lateinit var discovery: Discovery
        private set

    private val apis = HashMap<String, Pair<SavedServer, ServerApi>>()
    private val imageLoaders = HashMap<String, Pair<SavedServer, ImageLoader>>()

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        discovery = Discovery(this)
    }

    /** One API client per paired server; rebuilt when its address or credentials change. */
    @Synchronized
    fun apiFor(server: SavedServer): ServerApi {
        apis[server.id]?.let { (s, api) -> if (s == server) return api }
        return ServerApi(server, Http.forServer(server)).also { apis[server.id] = server to it }
    }

    /** Coil image loader that talks to the server with its pinned certificate and token. */
    @Synchronized
    fun imageLoaderFor(server: SavedServer): ImageLoader {
        imageLoaders[server.id]?.let { (s, l) -> if (s == server) return l }
        val client: OkHttpClient = apiFor(server).client
        val loader = ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("thumbs-${server.id}")).maxSizeBytes(150L * 1024 * 1024).build() }
            .respectCacheHeaders(false)
            .build()
        imageLoaders[server.id] = server to loader
        return loader
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this).crossfade(true).build()
}
