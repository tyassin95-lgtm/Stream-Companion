package com.streamcompanion.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiException(val code: Int, message: String) : IOException(message) {
    val unauthorized get() = code == 401
}

val AppJson = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

/** Typed access to the server's REST API. */
class ServerApi(val server: SavedServer, val client: OkHttpClient) {
    fun absolute(path: String): String = if (path.startsWith("http")) path else server.baseUrl + path

    private suspend fun get(path: String, query: Map<String, String?> = emptyMap()): String = withContext(Dispatchers.IO) {
        val url = (server.baseUrl + path).toHttpUrl().newBuilder().apply {
            query.forEach { (k, v) -> if (v != null) addQueryParameter(k, v) }
        }.build()
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ApiException(r.code, errorMessage(body, r.code))
            body
        }
    }

    private fun errorMessage(body: String, code: Int): String = runCatching {
        (AppJson.parseToJsonElement(body) as JsonObject)["error"]?.jsonPrimitive?.content
    }.getOrNull() ?: "HTTP $code"

    suspend fun browse(folderId: String?): BrowseResult =
        AppJson.decodeFromString(get("/api/browse", mapOf("folder" to folderId)))

    suspend fun revision(): Long = AppJson.decodeFromString<Revision>(get("/api/revision")).revision

    suspend fun search(q: String): List<VideoItem> =
        AppJson.decodeFromString<VideosResult>(get("/api/search", mapOf("q" to q))).videos

    suspend fun recent(limit: Int = 20): List<VideoItem> =
        AppJson.decodeFromString<VideosResult>(get("/api/recent", mapOf("limit" to limit.toString()))).videos

    suspend fun detail(videoId: String): VideoDetail = AppJson.decodeFromString(get("/api/videos/$videoId"))

    suspend fun playback(videoId: String, quality: String, caps: String, audio: Int, adaptive: Boolean): Playback =
        AppJson.decodeFromString(
            get(
                "/api/videos/$videoId/playback",
                mapOf("quality" to quality, "caps" to caps, "audio" to audio.toString(), "adaptive" to if (adaptive) "1" else "0"),
            ),
        )

    fun thumbUrl(videoId: String) = absolute("/api/videos/$videoId/thumb")
    fun subtitleUrl(videoId: String, key: String) = absolute("/api/videos/$videoId/subtitles/$key.vtt")
}

/** Pairing handshake. Throws [PairingException] with a user-presentable message. */
class PairingException(message: String) : Exception(message)

object Pairing {
    /** Learns a server's identity and certificate fingerprint without trusting it yet. */
    suspend fun probe(host: String, port: Int): Pair<ServerInfo, String> = withContext(Dispatchers.IO) {
        val (client, tm) = Http.capturing()
        try {
            client.newCall(Request.Builder().url("https://$host:$port/api/info").build()).execute().use { r ->
                if (!r.isSuccessful) throw PairingException("That address did not respond like a Stream Companion server.")
                val info = AppJson.decodeFromString<ServerInfo>(r.body?.string().orEmpty())
                if (info.app != "stream-companion") throw PairingException("That is not a Stream Companion server.")
                val fp = tm.fingerprint ?: throw PairingException("Could not read the server's certificate.")
                info to fp
            }
        } catch (e: PairingException) {
            throw e
        } catch (e: Exception) {
            throw PairingException("Could not reach $host:$port. Is the Windows app running and on the same Wi-Fi?")
        }
    }

    suspend fun pair(host: String, port: Int, pin: String, deviceName: String, deviceId: String): SavedServer {
        val (info, fingerprint) = probe(host, port)
        val nonce = Crypto.randomHex(16)
        val body = buildJsonObject {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("clientNonce", nonce)
            put("proof", Crypto.pairProof(pin, nonce, deviceId, fingerprint))
        }.toString()
        return withContext(Dispatchers.IO) {
            val client = Http.pinned(fingerprint)   // the connection must present the certificate we just saw
            val req = Request.Builder().url("https://$host:$port/api/pair")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            try {
                client.newCall(req).execute().use { r ->
                    val text = r.body?.string().orEmpty()
                    when (r.code) {
                        200 -> {
                            val resp = AppJson.decodeFromString<PairResponse>(text)
                            if (!Crypto.constantTimeEquals(resp.serverProof, Crypto.expectedServerProof(pin, nonce, fingerprint))) {
                                throw PairingException("The server could not prove it knows the code. Pairing was cancelled for your safety.")
                            }
                            SavedServer(info.id, info.name, host, port, fingerprint, resp.token)
                        }
                        403 -> throw PairingException("That code is not correct. Check the code shown in the Windows app.")
                        429 -> throw PairingException("Too many wrong attempts. Wait a minute and try again.")
                        else -> throw PairingException("Pairing failed (HTTP ${r.code}).")
                    }
                }
            } catch (e: PairingException) {
                throw e
            } catch (e: Exception) {
                throw PairingException("Connection lost while pairing. Please try again.")
            }
        }
    }
}
