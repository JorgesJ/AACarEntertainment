package com.jorso.carapp.auto

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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

    private val adUrlPatterns = listOf(
        "/api/stats/ads", "/pagead/", "/ptracking", "&adformat=",
        "/get_midroll_info", "ad_type=", "/youtubei/v1/log_event",
        "/youtubei/v1/player/ad_break", "yt_ad=1", "instream_ad"
    )

    private val emptyResponse = WebResourceResponse("text/plain", "utf-8", null)

    private val adblockJs = """
        (function() {
            var style = document.createElement('style');
            style.innerHTML = [
                /* Ocultar anuncios */
                '.ytd-display-ad-renderer,',
                '.ytd-ad-slot-renderer,',
                'ytd-ad-slot-renderer,',
                'ytd-in-feed-ad-layout-renderer,',
                'ytd-banner-promo-renderer,',
                '#masthead-ad,',
                '.video-ads,',
                '.ytp-ad-module,',
                '.ytp-ad-overlay-container,',
                '.ytp-ad-player-overlay,',
                '.ytp-ad-progress,',
                '#player-ads',
                '{ display: none !important; }'
            ].join('');
            document.head.appendChild(style);
            setInterval(function() {
                var skip = document.querySelector('.ytp-skip-ad-button,.ytp-ad-skip-button,.ytp-ad-skip-button-modern');
                if (skip) skip.click();
                var close = document.querySelector('.ytp-ad-overlay-close-button');
                if (close) close.click();
                var adVideo = document.querySelector('video.ad-showing');
                if (adVideo && !adVideo.paused && adVideo.duration) {
                    adVideo.currentTime = adVideo.duration;
                }
            }, 200);
        })();
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = buildUI()
        root.fitsSystemWindows = true
        setContentView(root)
        setupWebView()
        webView.loadUrl("https://m.youtube.com")
    }

    private fun buildUI(): View {
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt())
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP
            )
        }

        val btnHome = TextView(this).apply {
            text = "🏠"
            textSize = 20f
            setBackgroundColor(0xAA000000.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                bottomMargin = dp(16)
                marginEnd = dp(16)
            }
            setOnClickListener { webView.loadUrl("https://m.youtube.com") }
        }

        root.addView(webView)
        root.addView(progressBar)
        root.addView(btnHome)
        return root
    }

    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // UserAgent teléfono normal — YouTube sirve m.youtube.com estándar
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = false
            useWideViewPort = false
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
                if (adUrlPatterns.any { pattern -> url.contains(pattern) }) {
                    return emptyResponse
                }
                return null
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
                view?.evaluateJavascript(adblockJs, null)
                // Forzar cuadrícula de 3 columnas reduciendo tamaño de tarjetas
                view?.evaluateJavascript("""
                    (function() {
                        var style = document.createElement('style');
                        style.innerHTML = [
                            'ytm-rich-item-renderer { width: 33.33% !important; }',
                            'ytm-compact-video-renderer { font-size: 12px !important; }',
                            '.compact-media-item-image { height: 120px !important; }',
                            'ytm-thumbnail-overlay-time-status-renderer { font-size: 10px !important; }'
                        ].join('');
                        document.head.appendChild(style);
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
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
        webView.destroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
