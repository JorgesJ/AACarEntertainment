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
        "connect.facebook.net", "google-analytics.com", "googletagmanager.com",
        "yt3.ggpht.com", "youtubei.googleapis.com"
    )

    private val adUrlPatterns = listOf(
        "/api/stats/ads", "/pagead/", "/ptracking", "&adformat=",
        "/get_midroll_info", "ad_type=", "/youtubei/v1/log_event",
        "/youtubei/v1/player/ad_break", "ctier=L", "yt_ad=1",
        "/pagead/lvz", "instream_ad", "youtube.com/api/stats/ads"
    )

    private val emptyResponse = WebResourceResponse("text/plain", "utf-8", null)

    // JS de adblock inyectado en cada página
    private val adblockJs = """
        (function() {
            // CSS para ocultar elementos publicitarios
            var style = document.createElement('style');
            style.innerHTML = [
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
                '.ytp-ad-progress-list,',
                '.ytp-ad-skip-button-container,',
                '.ytm-promoted-video-renderer,',
                '.ytm-promoted-sparkles-web-renderer,',
                '#player-ads,',
                'ad-slot-renderer,',
                '.ytd-rich-item-renderer:has(ytd-ad-slot-renderer)',
                '{ display: none !important; }',
                '.ytp-ad-text { display: none !important; }'
            ].join('');
            document.head.appendChild(style);

            // Auto-skip y auto-click en botones de anuncio
            var skipInterval = setInterval(function() {
                // Botones de saltar anuncio
                var skip = document.querySelector(
                    '.ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, [class*="skip-ad"]'
                );
                if (skip) { skip.click(); }

                // Cerrar overlays de anuncio
                var close = document.querySelector('.ytp-ad-overlay-close-button');
                if (close) { close.click(); }

                // Saltar vídeo de anuncio detectado por clase
                var adVideo = document.querySelector('video.ad-showing');
                if (adVideo && !adVideo.paused && adVideo.duration) {
                    adVideo.currentTime = adVideo.duration;
                }

                // Saltar cualquier vídeo corto (<120s) que no sea el principal
                // Los anuncios suelen ser <60s, el contenido real >60s
                var allVideos = document.querySelectorAll('video');
                allVideos.forEach(function(v) {
                    if (v.duration && v.duration < 120 && !v.paused &&
                        (document.querySelector('.ad-showing') || 
                         document.querySelector('.ytp-ad-player-overlay'))) {
                        v.currentTime = v.duration;
                    }
                });
            }, 200);

            // MutationObserver para eliminar anuncios añadidos dinámicamente
            var observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(m) {
                    m.addedNodes.forEach(function(node) {
                        if (node.nodeType === 1) {
                            var classes = node.className || '';
                            if (typeof classes === 'string' && (
                                classes.includes('ad-') || 
                                classes.includes('-ad') ||
                                classes.includes('ytp-ad') ||
                                node.tagName === 'YTD-AD-SLOT-RENDERER' ||
                                node.tagName === 'YTD-IN-FEED-AD-LAYOUT-RENDERER'
                            )) {
                                node.style.display = 'none';
                            }
                        }
                    });
                });
            });
            observer.observe(document.body || document.documentElement, {
                childList: true, subtree: true
            });
        })();
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = buildUI()
        root.fitsSystemWindows = true
        setContentView(root)
        setupWebView()
        webView.loadUrl("https://www.youtube.com")
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
            setOnClickListener { webView.loadUrl("https://www.youtube.com") }
        }

        root.addView(webView)
        root.addView(progressBar)
        root.addView(btnHome)
        return root
    }

    private fun setupWebView() {
        // Cookies persistentes
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
            // UserAgent desktop — YouTube sirve la versión completa
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                val host = request.url?.host ?: return null

                // Bloquear dominios de anuncios
                if (adDomains.any { domain -> host.contains(domain) || url.contains(domain) }) {
                    return emptyResponse
                }

                // Bloquear URLs de anuncios por patrón
                if (adUrlPatterns.any { pattern -> url.contains(pattern) }) {
                    return emptyResponse
                }

                return null
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Guardar cookies
                CookieManager.getInstance().flush()
                // Inyectar adblock JS
                view?.evaluateJavascript(adblockJs, null)
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
