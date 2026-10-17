package hu.unideb.studyversity.ui.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.WebviewFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.community.member_detail.MemberDetailActivity
import hu.unideb.studyversity.ui.community.selfview.SelfProfileDisplayAndEditActivity
import hu.unideb.studyversity.utils.ConnectivityUtil

/**
 * In-app browser for university pages, documents and the community deep links.
 * Arguments: `fileName` (url / asset name / html), `fileMode` (see MODE_*), `title`, optional `jsInterface`.
 */
class WebViewFragment : BaseFragment() {
    private var _binding: WebviewFragmentBinding? = null
    private val binding get() = _binding!!

    /** Object exposed to the page as `Android` (the mood statistics page uses this). */
    var webAppInterface: Any? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = WebviewFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(arguments?.getString("title"))
        configureWebView()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (_binding?.webView?.canGoBack() == true) binding.webView.goBack()
                else { isEnabled = false; requireActivity().onBackPressedDispatcher.onBackPressed() }
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun configureWebView() {
        val web = binding.webView
        web.setInitialScale(1)
        web.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        web.isScrollbarFadingEnabled = false
        web.setBackgroundColor(0)
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { showLoading() }
            override fun onPageFinished(view: WebView?, url: String?) { hideLoading(); super.onPageFinished(view, url) }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError()
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handleUrl(view, request.url.toString())
        }
        (webAppInterface ?: pendingInterface)?.let { web.addJavascriptInterface(it, "Android") }

        val name = arguments?.getString("fileName")
        if (name != null) {
            web.visibility = View.VISIBLE
            binding.emptyLayout.visibility = View.GONE
            when (arguments?.getString("fileMode") ?: MODE_URL) {
                MODE_LOCAL_ASSET -> web.loadUrl("file:///android_asset/$name")
                MODE_URL, MODE_PDF -> if (ConnectivityUtil.isNetworkAvailable(requireContext())) web.loadUrl(name) else showError()
                MODE_HTML_STRING -> web.loadDataWithBaseURL(null, name, "text/html", "utf-8", null)
                else -> showError()
            }
        }
    }

    private fun showError() {
        hideLoading()
        _binding?.let {
            it.webView.visibility = View.GONE
            it.emptyLayout.visibility = View.VISIBLE
        }
    }

    /** Deep links `hu.unideb.studyversity://showCommunityMemberDetail?memberId=…` open the community profile. */
    private fun handleUrl(view: WebView, url: String): Boolean {
        if (!ConnectivityUtil.isNetworkAvailable(requireContext())) { showError(); return false }
        val uri = Uri.parse(url)
        if (url.startsWith(APP_SCHEME)) {
            if (uri.host != "showCommunityMemberDetail") { view.loadUrl(url); return false }
            val memberId = uri.getQueryParameter("memberId")?.toLongOrNull()
            if (memberId != null) {
                startActivity(Intent(requireContext(), MemberDetailActivity::class.java).putExtra("memberId", memberId))
            } else {
                Toast.makeText(requireContext(), R.string.profile_error, Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), SelfProfileDisplayAndEditActivity::class.java))
            }
            return true
        }
        if (url.contains("https://www.google.com/maps/")) {
            return runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")); true
            }.getOrDefault(false)
        }
        view.loadUrl(url)
        return false
    }

    override fun onDestroyView() {
        _binding?.webView?.destroy()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val APP_SCHEME = "hu.unideb.studyversity"
        const val MODE_LOCAL_ASSET = "LOCAL_ASSET"
        const val MODE_URL = "URL"
        const val MODE_PDF = "PDF"
        const val MODE_NOT_FOUND = "NOT_FOUND"
        const val MODE_HTML_STRING = "HTML_STRING"

        /** Set by callers right before navigating, consumed once (nav arguments cannot carry objects). */
        var pendingInterface: Any? = null
    }
}
