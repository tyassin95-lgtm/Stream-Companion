package com.streamcompanion.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Primitives for the PIN pairing protocol (must match windows/src/core/security.js). */
object Crypto {
    private val random = SecureRandom()

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun sha256Hex(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun hmacHex(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return hex(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }

    fun randomHex(bytes: Int = 16): String = hex(ByteArray(bytes).also { random.nextBytes(it) })

    /** Proof that we know the PIN, bound to this nonce, device and the certificate we actually saw. */
    fun pairProof(pin: String, nonce: String, deviceId: String, fingerprint: String) =
        hmacHex(pin, "sc-pair|$nonce|$deviceId|$fingerprint")

    /** What an authentic server (one that knows the PIN and owns [fingerprint]) must answer with. */
    fun expectedServerProof(pin: String, nonce: String, fingerprint: String) =
        hmacHex(pin, "sc-srv|$nonce|$fingerprint")

    fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
