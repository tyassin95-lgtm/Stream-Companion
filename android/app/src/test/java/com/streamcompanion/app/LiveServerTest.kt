package com.streamcompanion.app

import com.streamcompanion.app.data.ApiException
import com.streamcompanion.app.data.Http
import com.streamcompanion.app.data.Pairing
import com.streamcompanion.app.data.PairingException
import com.streamcompanion.app.data.ServerApi
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * End-to-end check of the real Android networking code against a running server.
 * Run: SC_TEST_HOST=127.0.0.1 SC_TEST_PORT=47800 SC_TEST_PIN=123456 ./gradlew testDebugUnitTest
 * (skipped when those variables are not set)
 */
class LiveServerTest {
    private val host = System.getenv("SC_TEST_HOST")
    private val port = System.getenv("SC_TEST_PORT")?.toIntOrNull() ?: 47800
    private val pin = System.getenv("SC_TEST_PIN")

    @Test fun pairBrowseAndStream() = runBlocking {
        assumeTrue(host != null && pin != null)
        try { Pairing.pair(host!!, port, "000000", "JVM test", "jvm-test-bad"); fail("wrong PIN accepted") } catch (_: PairingException) {}
        val server = Pairing.pair(host!!, port, pin!!, "JVM test", "jvm-test-1")
        assertTrue(server.paired)
        val api = ServerApi(server, Http.forServer(server))

        val root = api.browse(null)
        assertTrue("shared folders listed", root.folders.isNotEmpty())
        val inner = api.browse(root.folders.first().id)
        assertTrue(inner.folders.isNotEmpty() || inner.videos.isNotEmpty())
        val video = api.search("Sample").first()
        val detail = api.detail(video.id)
        assertTrue(detail.subtitles.isNotEmpty())

        val direct = api.playback(video.id, "auto", "h264,aac", 0, false)
        assertEquals("direct", direct.mode)
        val hls = api.playback(video.id, "480", "h264,aac", 0, false)
        assertTrue(hls.isHls)

        api.client.newCall(Request.Builder().url(api.absolute(direct.url)).header("Range", "bytes=0-99").build()).execute().use {
            assertEquals(206, it.code); assertEquals(100, it.body!!.bytes().size)
        }
        val master = api.client.newCall(Request.Builder().url(api.absolute(hls.url)).build()).execute().use { it.body!!.string() }
        assertTrue(master.contains("#EXT-X-STREAM-INF"))
        api.client.newCall(Request.Builder().url(api.thumbUrl(video.id)).build()).execute().use { assertEquals(200, it.code) }
        api.client.newCall(Request.Builder().url(api.subtitleUrl(video.id, detail.subtitles.first().key)).build()).execute().use {
            assertTrue(it.body!!.string().startsWith("WEBVTT"))
        }

        // a client pinned to a different certificate must refuse to talk
        val other = server.copy(fingerprint = "00".repeat(32))
        try { ServerApi(other, Http.forServer(other)).browse(null); fail("accepted wrong certificate") } catch (_: java.io.IOException) {}
        // a bad token is rejected
        val bad = server.copy(token = "x".repeat(43))
        try { ServerApi(bad, Http.forServer(bad)).browse(null); fail("bad token accepted") } catch (e: ApiException) { assertTrue(e.unauthorized) }
    }
}
