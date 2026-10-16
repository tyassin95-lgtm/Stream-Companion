package com.streamcompanion.app.ui.servers

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.streamcompanion.app.App
import com.streamcompanion.app.data.FoundServer
import com.streamcompanion.app.data.SavedServer
import com.streamcompanion.app.ui.Chip
import com.streamcompanion.app.ui.theme.Palette
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.catch

private data class Row(val id: String, val name: String, val host: String, val port: Int, val paired: Boolean, val online: Boolean, val saved: SavedServer?)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServersScreen(onOpen: (SavedServer) -> Unit, onPair: (host: String, port: Int) -> Unit, onSettings: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val saved by app.prefs.servers.collectAsState()
    val found = remember { mutableStateMapOf<String, FoundServer>() }
    var manual by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var confirmForget by remember { mutableStateOf<SavedServer?>(null) }

    LaunchedEffect(Unit) {
        app.discovery.discover().catch { }.collect { f ->
            found[f.id] = f
            // keep addresses of paired servers fresh (the PC may have a new IP after a reboot)
            app.prefs.updateAddress(f.id, f.host, f.port)
        }
    }

    val rows = buildList {
        saved.forEach { s ->
            val f = found[s.id]
            add(Row(s.id, f?.name ?: s.name, f?.host ?: s.host, f?.port ?: s.port, true, f != null, s))
        }
        found.values.filter { f -> saved.none { it.id == f.id } }.forEach { add(Row(it.id, it.name, it.host, it.port, false, true, null)) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Stream Companion", style = MaterialTheme.typography.headlineMedium)
                Text("Your PC's videos, on your phone", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings", tint = Palette.Muted) }
        }
        Spacer(Modifier.height(22.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Palette.Accent)
            Spacer(Modifier.width(10.dp))
            Text(if (rows.isEmpty()) "Looking for your PC on this Wi-Fi…" else "Computers on your network", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows, key = { it.id }) { r ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Palette.Surface),
                    border = BorderStroke(1.dp, Palette.Outline),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(
                        onClick = { if (r.saved != null) onOpen(r.saved) else onPair(r.host, r.port) },
                        onLongClick = { if (r.saved != null) menuFor = r.id },
                    ),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(Palette.SurfaceHigh), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Computer, null, tint = Palette.Accent)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text("${r.host}:${r.port}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (r.paired) Chip(if (r.online) "Online" else "Not found", if (r.online) Palette.Ok else Palette.Warn)
                            else Chip("Tap to pair", Palette.Accent)
                        }
                        DropdownMenu(expanded = menuFor == r.id, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(
                                text = { Text("Forget this computer") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                onClick = { menuFor = null; confirmForget = r.saved },
                            )
                        }
                    }
                }
            }
            item {
                if (rows.isEmpty()) {
                    Text(
                        "Make sure Stream Companion is running on your Windows PC and that both devices use the same Wi-Fi network.",
                        color = Palette.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }

        TextButton(onClick = { manual = true }, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Connect with an address")
        }
    }

    if (manual) ManualDialog(onDismiss = { manual = false }, onConnect = { h, p -> manual = false; onPair(h, p) })
    confirmForget?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmForget = null },
            title = { Text("Forget ${s.name}?") },
            text = { Text("You will need the pairing code from the Windows app to connect again.") },
            confirmButton = { TextButton(onClick = { app.prefs.forgetServer(s.id); confirmForget = null }) { Text("Forget") } },
            dismissButton = { TextButton(onClick = { confirmForget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ManualDialog(onDismiss: () -> Unit, onConnect: (String, Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val parsed = remember(text) {
        val t = text.trim()
        val idx = t.lastIndexOf(':')
        if (t.isEmpty()) null
        else if (idx > 0 && t.substring(idx + 1).all { it.isDigit() } && t.substring(idx + 1).isNotEmpty()) t.substring(0, idx) to t.substring(idx + 1).toInt()
        else t to 47800
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect with an address") },
        text = {
            Column {
                Text("Enter the address shown on the Overview page of the Windows app, for example 192.168.1.20:47800.", color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(text, { text = it }, singleLine = true, placeholder = { Text("192.168.1.20:47800") })
            }
        },
        confirmButton = { TextButton(enabled = parsed != null, onClick = { parsed?.let { onConnect(it.first, it.second) } }) { Text("Connect") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val settings by app.prefs.settings.collectAsState()
    val qualities = listOf("auto" to "Automatic", "original" to "Original (no conversion)", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p")
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
        }
        Text("Playback", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp))
        Text("Default quality", style = MaterialTheme.typography.labelLarge, color = Palette.Muted, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        qualities.forEach { (id, label) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { app.prefs.updateSettings { it.copy(quality = id) } }.padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.RadioButton(selected = settings.quality == id, onClick = { app.prefs.updateSettings { it.copy(quality = id) } })
                Text(label, Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Always use adaptive streaming", style = MaterialTheme.typography.bodyLarge)
                Text("Adjusts quality on the fly for slow or unstable Wi-Fi. Uses more PC power.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
            Switch(checked = settings.alwaysAdaptive, onCheckedChange = { v -> app.prefs.updateSettings { it.copy(alwaysAdaptive = v) } })
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Show subtitles automatically", style = MaterialTheme.typography.bodyLarge)
                Text("Turn on the first available subtitle track when a video starts.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
            Switch(checked = settings.autoSubtitles, onCheckedChange = { v -> app.prefs.updateSettings { it.copy(autoSubtitles = v) } })
        }
    }
}
