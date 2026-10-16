package com.streamcompanion.app.data

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val app: String = "",
    val protocol: Int = 1,
    val version: String = "",
    val id: String,
    val name: String,
    val fingerprint: String = "",
)

@Serializable
data class PairResponse(val token: String, val serverProof: String, val deviceId: String)

@Serializable
data class Crumb(val id: String, val name: String)

@Serializable
data class FolderMeta(
    val id: String? = null,
    val name: String = "",
    val parentId: String? = null,
    val breadcrumbs: List<Crumb> = emptyList(),
)

@Serializable
data class FolderItem(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val videoCount: Int = 0,
    val previewIds: List<String> = emptyList(),
)

@Serializable
data class VideoItem(
    val id: String,
    val title: String,
    val fileName: String = "",
    val size: Long = 0,
    val duration: Double = 0.0,
    val width: Int = 0,
    val height: Int = 0,
    val videoCodec: String = "",
    val audioCodec: String = "",
    val container: String = "",
    val addedAt: Long = 0,
    val modifiedAt: Long = 0,
    val thumb: String = "",
    val ready: Boolean = true,
)

@Serializable
data class BrowseResult(
    val folder: FolderMeta,
    val folders: List<FolderItem> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val revision: Long = 0,
)

@Serializable
data class VideosResult(val videos: List<VideoItem> = emptyList())

@Serializable
data class Revision(val revision: Long = 0)

@Serializable
data class AudioTrack(
    val n: Int,
    val codec: String = "",
    val channels: Int = 0,
    val lang: String = "",
    val title: String = "",
    val default: Boolean = false,
)

@Serializable
data class SubtitleTrack(
    val key: String,
    val lang: String = "",
    val title: String = "",
    val codec: String = "",
    val external: Boolean = false,
    val forced: Boolean = false,
    val default: Boolean = false,
)

@Serializable
data class VideoDetail(
    val id: String,
    val title: String,
    val duration: Double = 0.0,
    val width: Int = 0,
    val height: Int = 0,
    val videoCodec: String = "",
    val audioCodec: String = "",
    val container: String = "",
    val size: Long = 0,
    val thumb: String = "",
    val bitrate: Long = 0,
    val fps: Double = 0.0,
    val hdr: Boolean = false,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
)

@Serializable
data class Playback(val mode: String, val url: String, val reason: String = "", val audio: Int = 0) {
    val isHls get() = mode == "hls"
}

/** A server the user has paired with (or, with an empty token, one that was only discovered). */
@Serializable
data class SavedServer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val fingerprint: String,
    val token: String = "",
) {
    val baseUrl get() = "https://$host:$port"
    val paired get() = token.isNotEmpty()
}

/** A server announced on the local network. */
data class FoundServer(val id: String, val name: String, val host: String, val port: Int, val fingerprint: String)

@Serializable
data class Progress(
    val serverId: String,
    val videoId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val title: String,
    val thumb: String,
    val width: Int = 0,
    val height: Int = 0,
) {
    val fraction get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/** User-facing playback preferences. */
data class Settings(
    val quality: String = "auto",       // auto | original | 1080 | 720 | 480 | 360
    val alwaysAdaptive: Boolean = false,
    val autoSubtitles: Boolean = false,
)
