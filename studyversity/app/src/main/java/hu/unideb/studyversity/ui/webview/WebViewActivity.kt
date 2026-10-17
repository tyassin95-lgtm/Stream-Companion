package hu.unideb.studyversity.ui.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import hu.unideb.studyversity.databinding.ActivityWebviewBinding

/** Full screen page with a "Close" button; the url comes in the `url` extra. Used by the mood game sign-up. */
class WebViewActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean { view.loadUrl(url); return false }
        }
        intent.getStringExtra("url")?.let { binding.webView.loadUrl(it) }
        binding.closeButton.setOnClickListener { setResult(RESULT_OK); finish() }
    }
}
