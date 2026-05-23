package com.jorso.carapp.auto

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class YoutubeActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private val adDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adservice.google.es", "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com", "ads.youtube.com", "ad.youtube.com",
        "static.doubleclick.net", "m.doubleclick.net", "googleads.g.doubleclick.net",
        "securepubads.g.doubleclick.net", "pubads.g.doubleclick.net",
        "imasdk.googleapis.com", "advertising.yahoo.com", "amazon-adsystem.com",
        "connect.facebook.net", "google-analytics.com", "googletagmanager.com"
    )

    private val emptyResponse = WebResourceResponse("text/plain", "utf-8", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUI())
        loadYoutube()
    }

    private fun buildUI(): View {
        // FrameLayout raíz — WebView ocupa toda la pantalla
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt())
        }

        // WebView ocupa todo
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Progress bar arriba
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3),
                Gravity.TOP
            )
        }

        // Botón home flotante — esquina inferior derecha
        val btnHome = TextView(this).apply {
            text = "🏠"
            textSize = 20f
            setBackgroundColor(0xAA000000.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                bottomMargin = dp(16)
                marginEnd = dp(16)
            }
            setOnClickListener { loadYoutube() }
        }

        root.addView(webView)
        root.addView(progressBar)
        root.addView(btnHome)

        return root
    }

    private fun loadYoutube() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                val host = request.url?.host ?: return null
                if (adDomains.any { domain -> host.contains(domain) || url.contains(domain) }) {
                    return emptyResponse
                }
                if (url.contains("/api/stats/ads") || url.contains("/pagead/") ||
                    url.contains("/ptracking") || url.contains("&adformat=") ||
                    url.contains("/get_midroll_info") || url.contains("ad_type=") ||
                    url.contains("/youtubei/v1/log_event")) {
                    return emptyResponse
                }
                return null
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript("""
                    (function() {
                        var style = document.createElement('style');
                        style.innerHTML = '.ytm-promoted-video-renderer,.video-ads,.ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-player-overlay{display:none!important}';
                        document.head.appendChild(style);
                        setInterval(function() {
                            var s = document.querySelector('.ytp-skip-ad-button,.ytp-ad-skip-button');
                            if(s) s.click();
                        }, 500);
                    })();
                """.trimIndent(), null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                val decorView = window.decorView as FrameLayout
                decorView.addView(view, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))
            }

            override fun onHideCustomView() {
                val decorView = window.decorView as FrameLayout
                customView?.let { decorView.removeView(it) }
                customView = null
                customViewCallback?.onCustomViewHidden()
            }
        }

        webView.loadUrl("https://m.youtube.com")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
