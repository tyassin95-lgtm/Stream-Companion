package com.streamcompanion.app.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Builds HTTP clients for talking to a Stream Companion server.
 *
 * The server uses a self-signed certificate, so instead of the system CA store we pin the
 * certificate's SHA-256 fingerprint, which is learned (and PIN-verified) while pairing.
 */
object Http {
    class PinnedTrustManager(private val fingerprint: String) : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = throw CertificateException("client certs unsupported")
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            val actual = Crypto.sha256Hex(chain[0].encoded)
            if (!Crypto.constantTimeEquals(actual, fingerprint.lowercase())) {
                throw CertificateException("Server certificate does not match the paired server")
            }
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /** Accepts any certificate but remembers its fingerprint. Only used to *learn* it before pairing. */
    class CapturingTrustManager : X509TrustManager {
        @Volatile var fingerprint: String? = null
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            fingerprint = Crypto.sha256Hex(chain[0].encoded)
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private fun base(tm: X509TrustManager, token: String?, readSeconds: Long): OkHttpClient {
        val ctx = SSLContext.getInstance("TLS").apply { init(null, arrayOf(tm), SecureRandom()) }
        val builder = OkHttpClient.Builder()
            .sslSocketFactory(ctx.socketFactory, tm)
            // Identity is established by the pinned certificate, not by the (LAN IP) host name.
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(readSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        if (token != null) {
            builder.addInterceptor(Interceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
            })
        }
        return builder.build()
    }

    /** Client for an already-paired server. Long read timeout: the first transcoded segment can take a while. */
    fun forServer(server: SavedServer): OkHttpClient = base(PinnedTrustManager(server.fingerprint), server.token.ifEmpty { null }, 90)

    fun pinned(fingerprint: String): OkHttpClient = base(PinnedTrustManager(fingerprint), null, 15)

    fun capturing(): Pair<OkHttpClient, CapturingTrustManager> {
        val tm = CapturingTrustManager()
        return base(tm, null, 10) to tm
    }
}
