package com.streamcompanion.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamcompanion.app.App
import com.streamcompanion.app.ui.library.LibraryScreen
import com.streamcompanion.app.ui.player.PlayerScreen
import com.streamcompanion.app.ui.servers.PairScreen
import com.streamcompanion.app.ui.servers.ServersScreen
import com.streamcompanion.app.ui.servers.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

@UnstableApi
@Composable
fun AppNav() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as App

    // Re-open the last used computer straight away when it is still paired.
    val start = remember0 {
        val last = app.prefs.lastServerId?.let { app.prefs.server(it) }
        if (last != null) "library/${enc(last.id)}" else "servers"
    }

    NavHost(nav, startDestination = start) {
        composable("servers") {
            ServersScreen(
                onOpen = { s -> app.prefs.lastServerId = s.id; nav.navigate("library/${enc(s.id)}") },
                onPair = { host, port -> nav.navigate("pair/${enc(host)}/$port") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(
            "pair/{host}/{port}",
            arguments = listOf(navArgument("host") { type = NavType.StringType }, navArgument("port") { type = NavType.IntType }),
        ) { e ->
            PairScreen(
                host = dec(e.arguments!!.getString("host")!!), port = e.arguments!!.getInt("port"),
                onPaired = { s ->
                    app.prefs.lastServerId = s.id
                    nav.navigate("library/${enc(s.id)}") { popUpTo("pair/{host}/{port}") { inclusive = true } }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            "library/{server}?folder={folder}",
            arguments = listOf(
                navArgument("server") { type = NavType.StringType },
                navArgument("folder") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { e ->
            val serverId = dec(e.arguments!!.getString("server")!!)
            val folder = e.arguments!!.getString("folder")
            LibraryScreen(
                serverId = serverId, folderId = folder,
                onFolder = { id -> nav.navigate("library/${enc(serverId)}?folder=${enc(id)}") },
                onPlay = { vid -> nav.navigate("player/${enc(serverId)}/${enc(vid)}") },
                onBack = { nav.popBackStack() },
                onSwitchServer = {
                    app.prefs.lastServerId = null
                    if (!nav.popBackStack("servers", false)) nav.navigate("servers") { popUpTo(nav.graph.id) { inclusive = true } }
                },
                onRepair = { s ->
                    app.prefs.saveServer(s.copy(token = ""))
                    nav.navigate("pair/${enc(s.host)}/${s.port}")
                },
            )
        }
        composable(
            "player/{server}/{video}",
            arguments = listOf(navArgument("server") { type = NavType.StringType }, navArgument("video") { type = NavType.StringType }),
        ) { e ->
            PlayerScreen(dec(e.arguments!!.getString("server")!!), dec(e.arguments!!.getString("video")!!), onClose = { nav.popBackStack() })
        }
    }
}

@Composable
private inline fun <T> remember0(crossinline calc: () -> T): T = androidx.compose.runtime.remember { calc() }
