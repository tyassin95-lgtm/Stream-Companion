package com.streamcompanion.app.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Finds Stream Companion servers on the local network using mDNS/DNS-SD (NsdManager) and a UDP
 * broadcast probe, which works on networks that filter multicast.
 */
class Discovery(context: Context) {
    private val app = context.applicationContext
    private val nsd = app.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = app.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun discover(): Flow<FoundServer> = callbackFlow {
        val lock = runCatching { wifi.createMulticastLock("stream-companion").apply { setReferenceCounted(false); acquire() } }.getOrNull()
        val scope = CoroutineScope(Dispatchers.IO)

        // --- UDP broadcast probes
        scope.launch {
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 400
                    val probe = "STREAM_COMPANION_DISCOVER".toByteArray()
                    var lastSend = 0L
                    val buf = ByteArray(2048)
                    while (isActive) {
                        if (System.currentTimeMillis() - lastSend > 2000) {
                            lastSend = System.currentTimeMillis()
                            for (addr in broadcastTargets()) {
                                runCatching { socket.send(DatagramPacket(probe, probe.size, addr, DEFAULT_PORT)) }
                            }
                        }
                        try {
                            val p = DatagramPacket(buf, buf.size)
                            socket.receive(p)
                            parseReply(String(p.data, 0, p.length), p.address.hostAddress ?: continue)?.let { trySend(it) }
                        } catch (_: SocketTimeoutException) {
                        }
                    }
                }
            }
        }

        // --- mDNS / DNS-SD
        val pending = ConcurrentLinkedQueue<NsdServiceInfo>()
        var resolving = false
        fun resolveNext() {
            if (resolving) return
            val next = pending.poll() ?: return
            resolving = true
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) { resolving = false; resolveNext() }
                override fun onServiceResolved(info: NsdServiceInfo) {
                    resolving = false
                    val host = info.host?.hostAddress
                    if (host != null) {
                        val txt = info.attributes
                        fun attr(k: String) = txt[k]?.toString(Charsets.UTF_8).orEmpty()
                        trySend(FoundServer(attr("id").ifEmpty { info.serviceName }, info.serviceName, host, info.port, attr("fp")))
                    }
                    resolveNext()
                }
            })
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onServiceFound(info: NsdServiceInfo) { pending.add(info); resolveNext() }
            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        val nsdStarted = runCatching { nsd.discoverServices("_streamcompanion._tcp", NsdManager.PROTOCOL_DNS_SD, listener) }.isSuccess

        awaitClose {
            scope.cancel()
            if (nsdStarted) runCatching { nsd.stopServiceDiscovery(listener) }
            runCatching { lock?.release() }
        }
    }

    private fun parseReply(json: String, host: String): FoundServer? = runCatching {
        val o = AppJson.parseToJsonElement(json) as JsonObject
        if (o["app"]?.jsonPrimitive?.content != "stream-companion") return null
        FoundServer(
            id = o["id"]!!.jsonPrimitive.content,
            name = o["name"]!!.jsonPrimitive.content,
            host = host,
            port = o["port"]!!.jsonPrimitive.int,
            fingerprint = o["fingerprint"]?.jsonPrimitive?.content.orEmpty(),
        )
    }.getOrNull()

    /** Limited broadcast plus the directed broadcast address of every active IPv4 interface. */
    private fun broadcastTargets(): List<InetAddress> {
        val out = mutableListOf(InetAddress.getByName("255.255.255.255"))
        runCatching {
            for (ni in java.net.NetworkInterface.getNetworkInterfaces()) {
                if (!ni.isUp || ni.isLoopback) continue
                ni.interfaceAddresses.mapNotNull { it.broadcast }.forEach { out += it }
            }
        }
        return out.distinct()
    }

    companion object {
        const val DEFAULT_PORT = 47800
    }
}
