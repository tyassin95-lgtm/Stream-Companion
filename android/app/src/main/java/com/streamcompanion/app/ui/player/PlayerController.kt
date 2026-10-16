package com.streamcompanion.app.ui.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.streamcompanion.app.App
import com.streamcompanion.app.data.Caps
import com.streamcompanion.app.data.Playback
import com.streamcompanion.app.data.Progress
import com.streamcompanion.app.data.SavedServer
import com.streamcompanion.app.data.VideoDetail
import com.streamcompanion.app.ui.languageName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Everything the player screen needs: the ExoPlayer, the server negotiation and observable UI state. */
@UnstableApi
class PlayerController(
    context: Context,
    private val scope: CoroutineScope,
    private val app: App,
    private val server: SavedServer,
    private val videoId: String,
) {
    private val api = app.apiFor(server)
    private val prefs = app.prefs

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(api.client)))
        .setLoadControl(
            // Start quickly (1.5 s), but keep a generous cushion (up to 2 min) so slow Wi-Fi hiccups don't stall playback.
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(25_000, 120_000, 1_500, 4_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build(),
        )
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .build()

    var detail by mutableStateOf<VideoDetail?>(null); private set
    var playback by mutableStateOf<Playback?>(null); private set
    var quality by mutableStateOf(prefs.settings.value.quality); private set
    var audioIndex by mutableIntStateOf(0); private set
    var subtitleKey by mutableStateOf<String?>(null); private set
    var speed by mutableFloatStateOf(1f); private set
    var isPlaying by mutableStateOf(false); private set
    var buffering by mutableStateOf(true); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var buffered by mutableLongStateOf(0L); private set
    var error by mutableStateOf<String?>(null); private set
    var resumedFrom by mutableLongStateOf(0L); private set
    var loading by mutableStateOf(true); private set

    private var forceTranscode = false
    private var released = false
    private var lastSaved = 0L
    private var tickJob: Job? = null
    private var loadJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing; if (!playing) saveProgress() }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) { loading = false; error = null }
                if (state == Player.STATE_ENDED) { prefs.clearProgress(server.id, videoId) }
            }
            override fun onTracksChanged(tracks: Tracks) { applySubtitle(); applyAudioOverride() }
            override fun onPlayerError(e: PlaybackException) = handleError(e)
        })
        tickJob = scope.launch {
            while (isActive) {
                position = player.currentPosition
                val d = player.duration
                duration = if (d != C.TIME_UNSET && d > 0) d else ((detail?.duration ?: 0.0) * 1000).toLong()
                buffered = player.bufferedPosition
                if (player.isPlaying && System.currentTimeMillis() - lastSaved > 5_000) saveProgress()
                delay(250)
            }
        }
        start()
    }

    private fun start() {
        loadJob = scope.launch {
            try {
                val d = api.detail(videoId)
                detail = d
                val saved = prefs.progressFor(server.id, videoId)
                val resume = if (saved != null && saved.positionMs > 5_000 && saved.fraction < 0.95f) (saved.positionMs - 2_000).coerceAtLeast(0) else 0L
                resumedFrom = resume
                audioIndex = d.audioTracks.firstOrNull { it.default }?.n ?: 0
                if (prefs.settings.value.autoSubtitles) {
                    subtitleKey = (d.subtitles.firstOrNull { it.default || it.forced } ?: d.subtitles.firstOrNull())?.key
                }
                load(resume)
            } catch (e: Exception) {
                loading = false
                error = friendly(e)
            }
        }
    }

    /** Asks the server how to serve this video and (re)starts playback at [startMs]. */
    private fun load(startMs: Long) {
        loadJob?.cancel()
        loadJob = scope.launch {
            loading = true; error = null
            try {
                val d = detail ?: api.detail(videoId).also { detail = it }
                val s = prefs.settings.value
                val pb = api.playback(videoId, quality, Caps.build(), audioIndex, s.alwaysAdaptive || forceTranscode)
                playback = pb
                val subs = d.subtitles.map { t ->
                    val label = listOf(languageName(t.lang), t.title.takeIf { it.isNotBlank() && !it.equals(languageName(t.lang), true) }, if (t.forced) "Forced" else null)
                        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Subtitles" }
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(api.subtitleUrl(videoId, t.key)))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(t.lang.ifBlank { null })
                        .setLabel(label)
                        .setId("sub:${t.key}")
                        .setSelectionFlags(0)
                        .build()
                }
                val item = MediaItem.Builder()
                    .setMediaId(videoId)
                    .setUri(api.absolute(pb.url))
                    .apply { if (pb.isHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
                    .setSubtitleConfigurations(subs)
                    .build()
                player.setMediaItem(item, startMs)
                player.setPlaybackSpeed(speed)
                player.prepare()
                player.playWhenReady = true
                applySubtitle()
            } catch (e: Exception) {
                loading = false
                error = friendly(e)
            }
        }
    }

    private fun friendly(e: Throwable): String = when {
        e is com.streamcompanion.app.data.ApiException && e.unauthorized -> "Your phone is no longer paired with this PC."
        e is com.streamcompanion.app.data.ApiException && e.code == 503 -> "The PC is still analysing this file. Try again in a moment."
        e is java.io.IOException && e !is com.streamcompanion.app.data.ApiException -> "Can't reach your PC. Check that it is on and on the same Wi-Fi."
        else -> e.message ?: "Playback failed."
    }

    private fun handleError(e: PlaybackException) {
        val direct = playback?.isHls == false
        val decodeProblem = e.errorCode in setOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        )
        if (direct && decodeProblem && !forceTranscode) {
            // This phone can't play the original: let the PC convert it instead.
            forceTranscode = true
            load(position)
            return
        }
        loading = false
        error = when (e.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Connection to your PC was lost."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "The PC could not serve this video (it may have been moved or deleted)."
            else -> "This video could not be played."
        }
    }

    // ------------------------------------------------------------------ user actions
    fun retry() { error = null; load(if (position > 0) position else resumedFrom) }

    fun togglePlay() { if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() } }

    fun seekTo(ms: Long) { player.seekTo(ms.coerceIn(0, duration.takeIf { it > 0 } ?: Long.MAX_VALUE)); position = player.currentPosition }

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun changeQuality(q: String) {
        if (q == quality) return
        quality = q
        forceTranscode = false
        load(player.currentPosition)
    }

    fun changeSpeed(s: Float) { speed = s; player.setPlaybackSpeed(s) }

    fun setSubtitle(key: String?) { subtitleKey = key; applySubtitle() }

    fun setAudio(n: Int) {
        if (n == audioIndex) return
        audioIndex = n
        if (playback?.isHls == true) load(player.currentPosition) else applyAudioOverride()
    }

    private fun applySubtitle() {
        val params = player.trackSelectionParameters.buildUpon()
        val key = subtitleKey
        if (key == null) {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            params.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        } else {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            val group = player.currentTracks.groups.firstOrNull { g ->
                g.type == C.TRACK_TYPE_TEXT && (0 until g.length).any { g.getTrackFormat(it).id?.endsWith("sub:$key") == true }
            }
            if (group != null) params.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
        }
        player.trackSelectionParameters = params.build()
    }

    private fun applyAudioOverride() {
        if (playback?.isHls == true) return   // the server already delivers the chosen track
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val group = groups.getOrNull(audioIndex) ?: return
        if (group.isSelected) return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0)).build()
    }

    // ------------------------------------------------------------------ progress
    fun saveProgress() {
        val d = detail ?: return
        val dur = duration
        val pos = player.currentPosition
        lastSaved = System.currentTimeMillis()
        if (dur <= 0) return
        if (pos.toFloat() / dur > 0.95f) { prefs.clearProgress(server.id, videoId); return }
        if (pos < 5_000) return
        prefs.saveProgress(Progress(server.id, videoId, pos, dur, System.currentTimeMillis(), d.title, d.thumb, d.width, d.height))
    }

    fun release() {
        if (released) return
        saveProgress()
        released = true
        tickJob?.cancel()
        loadJob?.cancel()
        player.release()
    }
}
