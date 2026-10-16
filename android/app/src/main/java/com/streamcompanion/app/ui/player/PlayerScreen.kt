package com.streamcompanion.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamcompanion.app.App
import com.streamcompanion.app.data.SavedServer
import com.streamcompanion.app.ui.CenterMessage
import com.streamcompanion.app.ui.channelLabel
import com.streamcompanion.app.ui.formatMillis
import com.streamcompanion.app.ui.languageName
import com.streamcompanion.app.ui.prettyCodec
import com.streamcompanion.app.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class Panel { None, Quality, Audio, Subtitles, Speed }

@UnstableApi
@Composable
fun PlayerScreen(serverId: String, videoId: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val server = app.prefs.server(serverId)
    if (server == null) { CenterMessage("Not paired", "This computer is no longer paired."); return }
    PlayerContent(server, videoId, onClose)
}

@UnstableApi
@Composable
private fun PlayerContent(server: SavedServer, videoId: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val activity = context as? Activity
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val c = remember(videoId) { PlayerController(context, scope, app, server, videoId) }

    var controlsVisible by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf(Panel.None) }
    var fill by remember { mutableStateOf(false) }
    var hudText by remember { mutableStateOf<String?>(null) }
    var seekHint by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }   // (forward?, time stamp)
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var lockTick by remember { mutableLongStateOf(0L) }

    // ---- window: immersive, landscape, screen on, restored on exit
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        if (window != null && controller != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
            c.release()
        }
    }
    LaunchedEffect(c.detail) {
        val d = c.detail ?: return@LaunchedEffect
        activity?.requestedOrientation =
            if (d.height > d.width && d.width > 0) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    // pause when the app goes to the background
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) { c.player.pause(); c.saveProgress() } }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // auto-hide the controls while playing
    LaunchedEffect(controlsVisible, c.isPlaying, panel, scrubbing, lockTick) {
        if (controlsVisible && c.isPlaying && panel == Panel.None && !scrubbing) { delay(3500); controlsVisible = false }
    }
    LaunchedEffect(c.resumedFrom, c.loading) {
        if (c.resumedFrom > 0 && !c.loading) { hudText = "Resuming from ${formatMillis(c.resumedFrom)}"; delay(2200); hudText = null }
    }
    LaunchedEffect(seekHint) { if (seekHint != null) { delay(700); seekHint = null } }

    BackHandler { if (panel != Panel.None) panel = Panel.None else onClose() }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = c.player
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    subtitleView?.setBottomPaddingFraction(0.14f)
                    keepScreenOn = true
                }
            },
            update = { it.resizeMode = if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT },
        )

        // ---- gesture layer: tap = toggle controls, double-tap = ±10 s, vertical drag = brightness / volume
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (panel != Panel.None) panel = Panel.None else controlsVisible = !controlsVisible },
                            onDoubleTap = { o ->
                                val forward = o.x > widthPx / 2
                                c.seekBy(if (forward) 10_000 else -10_000)
                                seekHint = forward to System.nanoTime()
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        var startBrightness = 0.5f
                        var startVolume = 0
                        var acc = 0f
                        detectVerticalDragGestures(
                            onDragStart = { o ->
                                acc = 0f
                                startBrightness = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f
                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            },
                            onDragEnd = { hudText = null },
                            onDragCancel = { hudText = null },
                        ) { change, drag ->
                            change.consume()
                            acc -= drag / heightPx
                            val left = change.position.x < widthPx / 2
                            if (left) {
                                val b = (startBrightness + acc * 1.2f).coerceIn(0.02f, 1f)
                                activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = b } }
                                hudText = "Brightness ${(b * 100).toInt()}%"
                            } else {
                                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val v = (startVolume + acc * max * 1.2f).toInt().coerceIn(0, max)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                                hudText = "Volume ${(v * 100f / max).toInt()}%"
                            }
                        }
                    },
            )
        }

        // ---- double-tap hint
        seekHint?.let { (forward, _) ->
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier.align(if (forward) Alignment.CenterEnd else Alignment.CenterStart).padding(horizontal = 64.dp)
                        .clip(RoundedCornerShape(99.dp)).background(Color(0x66000000)).padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (forward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(if (forward) "+10 s" else "−10 s", color = Color.White)
                }
            }
        }

        // ---- transient HUD (resume message, brightness, volume)
        hudText?.let {
            Text(
                it, color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x99000000)).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // ---- buffering / loading
        if ((c.buffering || c.loading) && c.error == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(52.dp), color = Palette.Accent, strokeWidth = 4.dp)
        }

        // ---- controls
        AnimatedVisibility(
            visible = controlsVisible && c.error == null,
            enter = fadeIn(tween(150)), exit = fadeOut(tween(250)),
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))

                // top bar
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
                    Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Text(c.detail?.title.orEmpty(), color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val pb = c.playback
                        if (pb != null) Text(
                            if (pb.isHls) "Converted by your PC for smooth playback" else "Original quality",
                            color = Color(0xB3FFFFFF), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    val d = c.detail
                    if (d != null && d.audioTracks.size > 1) TopIcon(Icons.Rounded.GraphicEq, "Audio") { panel = Panel.Audio }
                    if (d != null && d.subtitles.isNotEmpty()) TopIcon(Icons.Rounded.ClosedCaption, "Subtitles", active = c.subtitleKey != null) { panel = Panel.Subtitles }
                    TopIcon(Icons.Rounded.Speed, "Speed", active = c.speed != 1f) { panel = Panel.Speed }
                    TopIcon(Icons.Rounded.HighQuality, "Quality") { panel = Panel.Quality }
                    TopIcon(Icons.Rounded.AspectRatio, "Fit / fill", active = fill) { fill = !fill; hudText = if (fill) "Fill screen" else "Fit to screen" }
                }

                // centre transport
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { c.seekBy(-10_000); lockTick++ }, Modifier.size(56.dp)) { Icon(Icons.Rounded.Replay10, "Back 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp)) }
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(Palette.AccentBrush).clickable { c.togglePlay(); lockTick++ },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (c.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (c.isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(44.dp)) }
                    IconButton(onClick = { c.seekBy(10_000); lockTick++ }, Modifier.size(56.dp)) { Icon(Icons.Rounded.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp)) }
                }

                // bottom: seek bar
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
                    val dur = c.duration.coerceAtLeast(1L)
                    val shownFraction = if (scrubbing) scrubFraction else (c.position.toFloat() / dur).coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMillis(if (scrubbing) (scrubFraction * dur).toLong() else c.position), color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(52.dp))
                        SeekBar(
                            fraction = shownFraction,
                            buffered = (c.buffered.toFloat() / dur).coerceIn(0f, 1f),
                            modifier = Modifier.weight(1f),
                            onScrub = { f -> scrubbing = true; scrubFraction = f },
                            onScrubEnd = { f -> c.seekTo((f * dur).toLong()); scrubbing = false; lockTick++ },
                        )
                        Text("-" + formatMillis((dur - c.position).coerceAtLeast(0)), color = Color(0xCCFFFFFF), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }

        // ---- error
        c.error?.let { msg ->
            Box(Modifier.fillMaxSize().background(Color(0xDD000000)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("Playback problem", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = Color(0xCCFFFFFF), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = onClose) { Text("Close") }
                        TextButton(onClick = { c.retry() }) { Text("Try again", color = Palette.Accent) }
                    }
                }
            }
        }

        // ---- option panels
        when (panel) {
            Panel.Quality -> OptionPanel(
                "Quality", listOf("auto" to "Auto", "original" to "Original", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p"),
                selected = c.quality, hint = "Lower qualities use less Wi-Fi bandwidth. The PC converts the video on the fly.",
                onSelect = { c.changeQuality(it); panel = Panel.None }, onDismiss = { panel = Panel.None },
            )
            Panel.Speed -> OptionPanel(
                "Playback speed", listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).map { it.toString() to (if (it == 1f) "Normal" else "${it}×") },
                selected = c.speed.toString(), onSelect = { c.changeSpeed(it.toFloat()); panel = Panel.None }, onDismiss = { panel = Panel.None },
            )
            Panel.Audio -> OptionPanel(
                "Audio", c.detail?.audioTracks.orEmpty().map { t ->
                    t.n.toString() to listOf(languageName(t.lang).ifBlank { "Track ${t.n + 1}" }, t.title.takeIf { it.isNotBlank() }, prettyCodec(t.codec), channelLabel(t.channels))
                        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                },
                selected = c.audioIndex.toString(), onSelect = { c.setAudio(it.toInt()); panel = Panel.None }, onDismiss = { panel = Panel.None },
            )
            Panel.Subtitles -> OptionPanel(
                "Subtitles", listOf("off" to "Off") + c.detail?.subtitles.orEmpty().map { t ->
                    t.key to listOf(languageName(t.lang).ifBlank { "Subtitles" }, t.title.takeIf { it.isNotBlank() && !it.equals(languageName(t.lang), true) }, if (t.forced) "Forced" else null)
                        .filterNotNull().joinToString(" · ")
                },
                selected = c.subtitleKey ?: "off", onSelect = { c.setSubtitle(if (it == "off") null else it); panel = Panel.None }, onDismiss = { panel = Panel.None },
            )
            Panel.None -> Unit
        }
    }
}

@Composable
private fun TopIcon(icon: ImageVector, description: String, active: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, description, tint = if (active) Palette.Accent else Color.White) }
}

@Composable
private fun OptionPanel(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    hint: String? = null,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max = 340.dp).fillMaxWidth(0.55f)
                .background(Color(0xF2121520)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(vertical = 18.dp),
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                options.forEach { (id, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(id) }.padding(horizontal = 22.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, color = if (id == selected) Palette.Accent else Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (id == selected) Icon(Icons.Rounded.Check, null, tint = Palette.Accent)
                    }
                }
            }
            if (hint != null) Text(hint, color = Color(0x99FFFFFF), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
        }
    }
}

/** Seek bar showing buffered range, with tap-to-seek and drag scrubbing. */
@Composable
private fun SeekBar(fraction: Float, buffered: Float, modifier: Modifier, onScrub: (Float) -> Unit, onScrubEnd: (Float) -> Unit) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var latest by remember { mutableFloatStateOf(0f) }
    Box(
        modifier.height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { o -> val f = (o.x / widthPx).coerceIn(0f, 1f); onScrub(f); onScrubEnd(f) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragging = true; latest = (o.x / widthPx).coerceIn(0f, 1f); onScrub(latest) },
                    onDragEnd = { dragging = false; onScrubEnd(latest) },
                    onDragCancel = { dragging = false; onScrubEnd(latest) },
                ) { change, _ -> change.consume(); latest = (change.position.x / widthPx).coerceIn(0f, 1f); onScrub(latest) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            widthPx = size.width.coerceAtLeast(1f)
            val cy = size.height / 2
            val h = 4.dp.toPx()
            val r = h / 2
            val corner = androidx.compose.ui.geometry.CornerRadius(r, r)
            drawRoundRect(Color(0x40FFFFFF), Offset(0f, cy - r), Size(size.width, h), corner)
            drawRoundRect(Color(0x66FFFFFF), Offset(0f, cy - r), Size(size.width * buffered, h), corner)
            drawRoundRect(Palette.Accent, Offset(0f, cy - r), Size(size.width * fraction, h), corner)
            val thumbR = if (dragging) 9.dp.toPx() else 7.dp.toPx()
            drawCircle(Color.White, thumbR, Offset(size.width * fraction, cy))
        }
    }
}
