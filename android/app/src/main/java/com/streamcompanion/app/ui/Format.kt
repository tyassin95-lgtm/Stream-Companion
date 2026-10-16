package com.streamcompanion.app.ui

import com.streamcompanion.app.data.VideoItem
import java.util.Locale

fun formatDuration(seconds: Double): String = formatMillis((seconds * 1000).toLong())

fun formatMillis(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return if (i >= 3) String.format(Locale.US, "%.1f %s", v, units[i]) else String.format(Locale.US, "%.0f %s", v, units[i])
}

fun resolutionLabel(width: Int, height: Int): String = when {
    height <= 0 -> ""
    width >= 3200 || height >= 2000 -> "4K"
    width >= 2400 || height >= 1300 -> "1440p"
    width >= 1800 || height >= 1000 -> "1080p"
    width >= 1200 || height >= 700 -> "720p"
    height >= 450 -> "480p"
    else -> "${height}p"
}

fun prettyCodec(codec: String): String = when (codec.lowercase()) {
    "h264" -> "H.264"
    "hevc" -> "HEVC"
    "av1" -> "AV1"
    "vp9" -> "VP9"
    "mpeg4" -> "MPEG-4"
    "aac" -> "AAC"
    "ac3" -> "AC-3"
    "eac3" -> "E-AC-3"
    "dts" -> "DTS"
    "truehd" -> "TrueHD"
    "opus" -> "Opus"
    "flac" -> "FLAC"
    "mp3" -> "MP3"
    else -> codec.uppercase()
}

fun videoMeta(v: VideoItem): String =
    listOf(resolutionLabel(v.width, v.height), prettyCodec(v.videoCodec).takeIf { v.videoCodec.isNotEmpty() }, formatSize(v.size))
        .filter { !it.isNullOrBlank() }.joinToString(" · ")

private val languageNames = mapOf(
    "eng" to "English", "en" to "English", "spa" to "Spanish", "es" to "Spanish", "fre" to "French", "fra" to "French", "fr" to "French",
    "ger" to "German", "deu" to "German", "de" to "German", "ita" to "Italian", "it" to "Italian", "por" to "Portuguese", "pt" to "Portuguese",
    "rus" to "Russian", "ru" to "Russian", "jpn" to "Japanese", "ja" to "Japanese", "kor" to "Korean", "ko" to "Korean",
    "chi" to "Chinese", "zho" to "Chinese", "zh" to "Chinese", "ara" to "Arabic", "ar" to "Arabic", "heb" to "Hebrew", "he" to "Hebrew",
    "hin" to "Hindi", "hi" to "Hindi", "dut" to "Dutch", "nld" to "Dutch", "nl" to "Dutch", "pol" to "Polish", "pl" to "Polish",
    "tur" to "Turkish", "tr" to "Turkish", "swe" to "Swedish", "sv" to "Swedish", "nor" to "Norwegian", "dan" to "Danish", "fin" to "Finnish",
    "cze" to "Czech", "ces" to "Czech", "gre" to "Greek", "ell" to "Greek", "hun" to "Hungarian", "tha" to "Thai", "vie" to "Vietnamese",
    "ind" to "Indonesian", "ukr" to "Ukrainian", "rum" to "Romanian", "ron" to "Romanian",
)

fun languageName(code: String): String =
    if (code.isBlank()) "" else languageNames[code.lowercase()] ?: code.replaceFirstChar { it.uppercase() }

fun channelLabel(channels: Int): String = when (channels) {
    1 -> "Mono"; 2 -> "Stereo"; 6 -> "5.1"; 8 -> "7.1"; 0 -> ""; else -> "$channels ch"
}
