package com.streamcompanion.app.data

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

/** Reports which codecs this phone can decode, so the server only transcodes when really needed. */
object Caps {
    private val cache: String by lazy { compute() }

    fun build(): String = cache

    private fun compute(): String {
        val infos = runCatching { MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { !it.isEncoder } }
            .getOrDefault(emptyList())

        fun supports(mime: String, hardwareOnly: Boolean): Boolean = infos.any { info ->
            info.supportedTypes.any { it.equals(mime, ignoreCase = true) } && (!hardwareOnly || isHardware(info))
        }

        val caps = mutableListOf("h264", "aac", "mp3", "opus", "vorbis", "flac")
        // Software HEVC/VP9/AV1 decoding is too slow on most phones, so only hardware decoders count.
        if (supports("video/hevc", true)) caps += "hevc"
        if (supports("video/x-vnd.on2.vp9", true)) caps += "vp9"
        if (supports("video/av01", true)) caps += "av1"
        if (supports("audio/ac3", false)) caps += "ac3"
        if (supports("audio/eac3", false)) caps += "eac3"
        return caps.joinToString(",")
    }

    private fun isHardware(info: MediaCodecInfo): Boolean =
        if (Build.VERSION.SDK_INT >= 29) info.isHardwareAccelerated && !info.isSoftwareOnly
        else !info.name.startsWith("OMX.google.") && !info.name.startsWith("c2.android.")
}
