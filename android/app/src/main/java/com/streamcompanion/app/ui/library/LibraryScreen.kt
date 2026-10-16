package com.streamcompanion.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamcompanion.app.App
import com.streamcompanion.app.data.FolderItem
import com.streamcompanion.app.data.Progress
import com.streamcompanion.app.data.SavedServer
import com.streamcompanion.app.data.ServerApi
import com.streamcompanion.app.data.VideoItem
import com.streamcompanion.app.ui.CenterMessage
import com.streamcompanion.app.ui.GradientButton
import com.streamcompanion.app.ui.Loading
import com.streamcompanion.app.ui.formatDuration
import com.streamcompanion.app.ui.formatMillis
import com.streamcompanion.app.ui.theme.Palette
import com.streamcompanion.app.ui.videoMeta

@Composable
fun LibraryScreen(
    serverId: String,
    folderId: String?,
    onFolder: (String) -> Unit,
    onPlay: (String) -> Unit,
    onBack: () -> Unit,
    onSwitchServer: () -> Unit,
    onRepair: (SavedServer) -> Unit,
) {
    val app = LocalContext.current.applicationContext as App
    val vm: LibraryViewModel = viewModel(
        key = "lib-$serverId-$folderId",
        factory = viewModelFactory { initializer { LibraryViewModel(app, serverId, folderId) } },
    )
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val progressAll by app.prefs.progress.collectAsState()
    val progress = remember(progressAll, serverId) { progressAll.filter { it.serverId == serverId } }
    var searching by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val server = vm.server
    val api = vm.api
    val title = when (val s = state) {
        is LibState.Ready -> if (folderId == null) (server?.name ?: "Library") else s.data.folder.name
        else -> server?.name ?: "Library"
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // ---- top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (folderId != null || searching) {
                IconButton(onClick = { if (searching) { searching = false; vm.query.value = "" } else onBack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                }
            } else Spacer(Modifier.width(12.dp))
            if (searching) {
                TextField(
                    value = query, onValueChange = { vm.query.value = it }, singleLine = true,
                    placeholder = { Text("Search videos", color = Palette.Muted) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Rounded.Close, "Clear") } },
                    keyboardOptions = KeyboardOptions.Default,
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val crumbs = (state as? LibState.Ready)?.data?.folder?.breadcrumbs.orEmpty()
                    if (crumbs.size > 1) Text(crumbs.joinToString("  ›  ") { it.name }, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { searching = true }) { Icon(Icons.Rounded.Search, "Search") }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Refresh") }, onClick = { menu = false; vm.load(spinner = false) })
                        DropdownMenuItem(text = { Text("Switch computer") }, onClick = { menu = false; onSwitchServer() })
                    }
                }
            }
        }

        // ---- body
        Box(Modifier.weight(1f)) {
            val a = api
            when {
                a == null || server == null -> CenterMessage("Not paired", "This computer is no longer paired.") { GradientButton("Choose a computer", onSwitchServer, Modifier.fillMaxWidth(0.7f)) }
                searching && results != null -> VideoGrid(a, server, results!!.let { r -> r }, emptyList(), progress, null, onFolder, onPlay, emptyText = "No videos match “$query”.")
                searching -> Box(Modifier.fillMaxSize())
                else -> when (val s = state) {
                    LibState.Loading -> Loading()
                    is LibState.Error -> CenterMessage(
                        if (s.unauthorized) "Pairing required" else "Can't load your library", s.message,
                    ) {
                        if (s.unauthorized) GradientButton("Pair again", { onRepair(server) }, Modifier.fillMaxWidth(0.7f))
                        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientButton("Try again", vm::retry, Modifier.fillMaxWidth(0.7f))
                            androidx.compose.material3.TextButton(onClick = onSwitchServer) { Text("Choose another computer") }
                        }
                    }
                    is LibState.Ready -> {
                        val d = s.data
                        if (d.folders.isEmpty() && d.videos.isEmpty()) {
                            CenterMessage(
                                if (folderId == null) "No videos yet" else "This folder is empty",
                                if (folderId == null) "Add a folder in the Windows app and your videos will appear here." else "",
                            )
                        } else VideoGrid(a, server, d.videos, d.folders, progress, if (folderId == null) s.recent else null, onFolder, onPlay)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoGrid(
    api: ServerApi,
    server: SavedServer,
    videos: List<VideoItem>,
    folders: List<FolderItem>,
    progress: List<Progress>,
    recent: List<VideoItem>?,
    onFolder: (String) -> Unit,
    onPlay: (String) -> Unit,
    emptyText: String = "",
) {
    val progressById = remember(progress) { progress.associateBy { it.videoId } }
    val resumable = remember(progress) { progress.filter { it.positionMs > 5_000 && it.fraction < 0.95f }.sortedByDescending { it.updatedAt } }
    val span: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

    if (videos.isEmpty() && folders.isEmpty()) { CenterMessage("Nothing here", emptyText); return }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
    ) {
        if (recent != null && resumable.isNotEmpty()) {
            item(span = span) { SectionHeader("Continue watching") }
            item(span = span) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(resumable.take(10), key = { it.videoId }) { p -> ContinueCard(api, server, p) { onPlay(p.videoId) } }
                }
            }
        }
        if (recent != null && recent.isNotEmpty()) {
            item(span = span) { SectionHeader("Recently added") }
            item(span = span) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent, key = { it.id }) { v -> VideoCard(api, server, v, progressById[v.id], Modifier.width(200.dp)) { onPlay(v.id) } }
                }
            }
        }
        if (folders.isNotEmpty()) {
            if (recent != null || videos.isNotEmpty()) item(span = span) { SectionHeader("Folders") }
            items(folders, key = { "f" + it.id }) { f -> FolderCard(api, server, f) { onFolder(f.id) } }
        }
        if (videos.isNotEmpty()) {
            if (folders.isNotEmpty() || recent != null) item(span = span) { SectionHeader("Videos") }
            items(videos, key = { "v" + it.id }) { v -> VideoCard(api, server, v, progressById[v.id], Modifier) { onPlay(v.id) } }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun Thumb(api: ServerApi, server: SavedServer, url: String, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as App
    Box(modifier.background(Brush.linearGradient(listOf(Palette.SurfaceHigh, Palette.Surface))), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.PlayArrow, null, tint = Palette.Outline, modifier = Modifier.size(34.dp))
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(api.absolute(url)).memoryCacheKey(url).diskCacheKey(url).build(),
            imageLoader = app.imageLoaderFor(server),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun VideoCard(api: ServerApi, server: SavedServer, v: VideoItem, progress: Progress?, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))) {
            Thumb(api, server, v.thumb, Modifier.fillMaxSize())
            if (v.duration > 0) {
                Text(
                    formatDuration(v.duration), style = MaterialTheme.typography.labelMedium, color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (progress != null && progress.fraction in 0.02f..0.95f) {
                LinearProgressIndicator(
                    progress = { progress.fraction }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                    color = Palette.Accent, trackColor = Color(0x66000000),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(v.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
        val meta = videoMeta(v)
        if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun ContinueCard(api: ServerApi, server: SavedServer, p: Progress, onClick: () -> Unit) {
    Column(Modifier.width(240.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))) {
            Thumb(api, server, p.thumb, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))))
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(40.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x66000000)).padding(6.dp))
            Text(
                "${formatMillis(p.positionMs)} / ${formatMillis(p.durationMs)}", style = MaterialTheme.typography.labelMedium, color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
            LinearProgressIndicator(
                progress = { p.fraction }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                color = Palette.Accent, trackColor = Color(0x66000000),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(p.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun FolderCard(api: ServerApi, server: SavedServer, f: FolderItem, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)).background(Palette.Surface)) {
            val ids = f.previewIds.take(4)
            if (ids.isEmpty()) {
                Icon(Icons.Rounded.Folder, null, tint = Palette.Accent, modifier = Modifier.align(Alignment.Center).size(44.dp))
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val rows = if (ids.size <= 2) listOf(ids) else ids.chunked(2)
                    rows.forEach { row ->
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            row.forEach { id -> Thumb(api, server, "/api/videos/$id/thumb", Modifier.weight(1f).fillMaxSize()) }
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xAA000000)))))
            }
            Row(Modifier.align(Alignment.BottomStart).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Folder, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("${f.videoCount}", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(f.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
    }
}
