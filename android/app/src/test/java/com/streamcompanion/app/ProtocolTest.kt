package com.streamcompanion.app

import com.streamcompanion.app.data.AppJson
import com.streamcompanion.app.data.BrowseResult
import com.streamcompanion.app.data.Crypto
import com.streamcompanion.app.data.Playback
import com.streamcompanion.app.ui.formatDuration
import com.streamcompanion.app.ui.formatMillis
import com.streamcompanion.app.ui.formatSize
import com.streamcompanion.app.ui.resolutionLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {
    private val fp = "ab".repeat(32)
    private val nonce = "0123456789abcdef0123456789abcdef"

    // Reference values produced by the Windows server's Node.js implementation (windows/src/core/security.js).
    @Test fun pairProofMatchesServerImplementation() {
        assertEquals("f60736bf3e116452010db49d54cbf5e956e6ed305b56b349761e93c03b091e5d", Crypto.pairProof("123456", nonce, "dev-1", fp))
    }

    @Test fun serverProofMatchesServerImplementation() {
        assertEquals("02ddfade379fa09d448f01518eb916c452cc9067781ed9e3957676fc9bf270c9", Crypto.expectedServerProof("123456", nonce, fp))
    }

    @Test fun proofDependsOnPinAndFingerprint() {
        assertFalse(Crypto.pairProof("123456", nonce, "dev-1", fp) == Crypto.pairProof("123457", nonce, "dev-1", fp))
        assertFalse(Crypto.pairProof("123456", nonce, "dev-1", fp) == Crypto.pairProof("123456", nonce, "dev-1", "cd".repeat(32)))
    }

    @Test fun randomHexIsRandomAndRightLength() {
        val a = Crypto.randomHex(16)
        assertEquals(32, a.length)
        assertFalse(a == Crypto.randomHex(16))
    }

    @Test fun parsesBrowseResponseFromServer() {
        val json = """{"folder":{"id":null,"name":"Library","parentId":null,"breadcrumbs":[]},
            "folders":[{"id":"abc","name":"Movies","parentId":null,"videoCount":3,"previewIds":["1","2"]}],
            "videos":[{"id":"v1","title":"Clip","fileName":"Clip.mp4","size":1024,"duration":12.5,"width":1920,"height":1080,
            "videoCodec":"h264","audioCodec":"aac","container":"mp4","addedAt":1,"modifiedAt":2,"thumb":"/api/videos/v1/thumb?v=0","ready":true,"futureField":1}],
            "revision":7}"""
        val r = AppJson.decodeFromString<BrowseResult>(json)
        assertEquals("Movies", r.folders.single().name)
        assertEquals(2, r.folders.single().previewIds.size)
        assertEquals(1920, r.videos.single().width)
        assertEquals(7L, r.revision)
    }

    @Test fun playbackModes() {
        val p = AppJson.decodeFromString<Playback>("""{"mode":"hls","url":"/hls/x/master.m3u8?max=0&audio=0","reason":"x","audio":0}""")
        assertTrue(p.isHls)
        assertFalse(AppJson.decodeFromString<Playback>("""{"mode":"direct","url":"/api/videos/x/stream"}""").isHls)
    }

    @Test fun formatting() {
        assertEquals("1:05", formatMillis(65_000))
        assertEquals("1:02:03", formatMillis(3_723_000))
        assertEquals("0:00", formatMillis(-5))
        assertEquals("2:00", formatDuration(120.0))
        assertEquals("1.5 GB", formatSize(1_610_612_736))
        assertEquals("1080p", resolutionLabel(1920, 1080))
        assertEquals("720p", resolutionLabel(1280, 720))
        assertEquals("4K", resolutionLabel(3840, 2160))
        assertEquals("360p", resolutionLabel(640, 360))
        assertEquals("1080p", resolutionLabel(1920, 800))   // cinema-scope 1080p
    }
}
