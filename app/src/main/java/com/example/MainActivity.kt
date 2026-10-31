package com.example

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        GpAiSimplePortalScreen()
      }
    }
  }
}

private const val BLOGSPOT_URL = "https://gpaitanjungsari.blogspot.com/2026/10/gpai-tanjungsari.html?m=1"
private const val LOCAL_FALLBACK_URL = "file:///android_asset/portal/index.html"

// Script untuk menghilangkan dashboard bawaan Blogger (header, drawer, sidebar, gadget dock, footer, komentar)
// dan memastikan localStorage berjalan aktif untuk seluruh data anggota, verifikasi, dan histori.
private const val JS_CLEAN_BLOGGER_AND_SYNC_LOCALSTORAGE = """
(function() {
    function hilangkanDashboardLuar() {
        var elStyle = document.getElementById('gpai-clean-style');
        if (!elStyle) {
            elStyle = document.createElement('style');
            elStyle.id = 'gpai-clean-style';
            elStyle.innerHTML = `
                /* Hilangkan seluruh elemen dashboard dan chrome bawaan Blogger */
                #header-container, #header, .header-bar, .header-drawer,
                .admin-controls, .dashboard, #gadget-dock, .article-header,
                .ribbon, .date.ribbon, .article-footer, #comments, .comments,
                #attribution, .blogger-attribution, #views, .sharing,
                .sharing-options, #mobile-header, #pages, .post-author,
                #empty-message, #sidebar, .feed-links, .blog-pager,
                .breadcrumbs, .status-msg-wrap, .ss-modal, .item-control,
                #blog-pager, .post-feeds {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0 !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                }
                html, body {
                    margin: 0 !important;
                    padding: 0 !important;
                    background: #f1f5f9 !important;
                    width: 100% !important;
                    min-height: 100vh !important;
                }
                #main, .main, #items, li.item, .article, .article-content, .entry-content, .post-body {
                    margin: 0 !important;
                    padding: 0 !important;
                    max-width: 100% !important;
                    width: 100% !important;
                    border: none !important;
                    box-shadow: none !important;
                    background: transparent !important;
                }
            `;
            (document.head || document.documentElement).appendChild(elStyle);
        }

        // Hapus elemen yang terlanjur ter-render oleh template dinamis Blogger
        var selectors = [
            '#header-container', '#header', '.header-bar', '.header-drawer',
            '.admin-controls', '.dashboard', '#gadget-dock', '.article-header',
            '.ribbon', '.date.ribbon', '.article-footer', '#comments',
            '.comments', '#attribution', '.blogger-attribution', '#views',
            '.sharing', '#sidebar', '#pages'
        ];
        selectors.forEach(function(sel) {
            var found = document.querySelectorAll(sel);
            for (var i = 0; i < found.length; i++) {
                found[i].style.setProperty('display', 'none', 'important');
            }
        });

        // Maksimalkan area konten portal
        var contentNodes = document.querySelectorAll('li.item, #items, #main, .article, .article-content');
        for (var j = 0; j < contentNodes.length; j++) {
            contentNodes[j].style.padding = '0';
            contentNodes[j].style.margin = '0';
            contentNodes[j].style.maxWidth = '100%';
            contentNodes[j].style.border = 'none';
            contentNodes[j].style.boxShadow = 'none';
        }
    }

    // Aktifkan integrasi localStorage persisten otomatis
    function aktifkanPenyimpananLocalStorage() {
        if (!window.localStorage) return;

        // Ambil cache dari localStorage jika ada
        var cachedUsers = localStorage.getItem('gpai_users');
        if (cachedUsers && typeof window.globalUsers !== 'undefined') {
            try {
                var parsed = JSON.parse(cachedUsers);
                if (Array.isArray(parsed) && parsed.length > 0) {
                    window.globalUsers = parsed;
                    if (typeof window.renderDashboardData === 'function') {
                        window.renderDashboardData();
                    }
                }
            } catch(e) {}
        }

        // Simpan otomatis ke localStorage setiap ada perubahan
        var simpanKeLS = function() {
            if (typeof window.globalUsers !== 'undefined' && Array.isArray(window.globalUsers)) {
                localStorage.setItem('gpai_users', JSON.stringify(window.globalUsers));
            }
            if (typeof window.globalExpenses !== 'undefined' && Array.isArray(window.globalExpenses)) {
                localStorage.setItem('gpai_expenses', JSON.stringify(window.globalExpenses));
            }
        };

        // Pasang interceptor pada fungsi-fungsi perubahan data
        if (typeof window.simpanPelangganKeSheet === 'function' && !window.__gpai_patched) {
            window.__gpai_patched = true;
            var origSimpan = window.simpanPelangganKeSheet;
            window.simpanPelangganKeSheet = async function(e) {
                var res = await origSimpan.apply(this, arguments);
                simpanKeLS();
                return res;
            };

            if (typeof window.konfirmasiVerifikasiAdmin === 'function') {
                var origVerif = window.konfirmasiVerifikasiAdmin;
                window.konfirmasiVerifikasiAdmin = function(uid) {
                    var res = origVerif.apply(this, arguments);
                    simpanKeLS();
                    return res;
                };
            }

            if (typeof window.hapusPelanggan === 'function') {
                var origHapus = window.hapusPelanggan;
                window.hapusPelanggan = function(uid) {
                    var res = origHapus.apply(this, arguments);
                    simpanKeLS();
                    return res;
                };
            }

            if (typeof window.buatTagihanBulanBerikutnya === 'function') {
                var origTagihan = window.buatTagihanBulanBerikutnya;
                window.buatTagihanBulanBerikutnya = async function(uid) {
                    var res = await origTagihan.apply(this, arguments);
                    simpanKeLS();
                    return res;
                };
            }
        }
    }

    hilangkanDashboardLuar();
    aktifkanPenyimpananLocalStorage();

    // Pantau perubahan DOM secara kontinu agar tetap bersih
    if (!window.__gpai_interval_set) {
        window.__gpai_interval_set = true;
        setInterval(function() {
            hilangkanDashboardLuar();
            aktifkanPenyimpananLocalStorage();
        }, 300);
    }
})();
"""

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GpAiSimplePortalScreen() {
  val context = LocalContext.current
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var canGoBackState by remember { mutableStateOf(false) }
  var progressValue by remember { mutableIntStateOf(0) }

  // Tangani tombol kembali di Android
  BackHandler(enabled = canGoBackState) {
    webViewInstance?.let { webView ->
      if (webView.canGoBack()) {
        webView.goBack()
      }
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primary,
          titleContentColor = MaterialTheme.colorScheme.onPrimary,
          actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
          navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
          if (canGoBackState) {
            IconButton(
              onClick = { webViewInstance?.goBack() },
              modifier = Modifier.testTag("action_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White
              )
            }
          }
        },
        title = {
          Text(
            text = "GPAI TANJUNGSARI",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color.White
          )
        },
        actions = {
          IconButton(
            onClick = { webViewInstance?.reload() },
            modifier = Modifier.testTag("action_refresh_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Muat Ulang Halaman",
              tint = Color.White
            )
          }
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .testTag("portal_webview"),
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true // Mengaktifkan HTML5 localStorage persisten
              databaseEnabled = true
              allowFileAccess = true
              allowContentAccess = true
              cacheMode = WebSettings.LOAD_DEFAULT
              useWideViewPort = true
              loadWithOverviewMode = true
              setSupportZoom(true)
              builtInZoomControls = false
              displayZoomControls = false
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressValue = newProgress
                // Suntikkan script sedini mungkin saat halaman mulai dimuat
                if (newProgress > 25) {
                  view?.evaluateJavascript(JS_CLEAN_BLOGGER_AND_SYNC_LOCALSTORAGE, null)
                }
                super.onProgressChanged(view, newProgress)
              }

              override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
              ): Boolean {
                AlertDialog.Builder(ctx)
                  .setTitle("GPAI TANJUNGSARI")
                  .setMessage(message ?: "")
                  .setPositiveButton("OK") { _, _ -> result?.confirm() }
                  .setOnCancelListener { result?.cancel() }
                  .show()
                return true
              }

              override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
              ): Boolean {
                AlertDialog.Builder(ctx)
                  .setTitle("Konfirmasi")
                  .setMessage(message ?: "")
                  .setPositiveButton("Ya") { _, _ -> result?.confirm() }
                  .setNegativeButton("Batal") { _, _ -> result?.cancel() }
                  .setOnCancelListener { result?.cancel() }
                  .show()
                return true
              }
            }

            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(JS_CLEAN_BLOGGER_AND_SYNC_LOCALSTORAGE, null)
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                canGoBackState = view?.canGoBack() ?: false
                // Bersihkan tampilan dashboard Blogger dan sinkronkan data ke localStorage
                view?.evaluateJavascript(JS_CLEAN_BLOGGER_AND_SYNC_LOCALSTORAGE, null)
                super.onPageFinished(view, url)
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                // Jika jaringan bermasalah saat memuat link blogspot, gunakan portal lokal yang tersimpan
                if (request?.isForMainFrame == true) {
                  view?.loadUrl(LOCAL_FALLBACK_URL)
                }
                super.onReceivedError(view, request, error)
              }

              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val uri = request?.url ?: return false
                val urlString = uri.toString()

                // Buka WhatsApp, telepon, atau mailto lewat aplikasi native perangkat
                if (urlString.startsWith("whatsapp:") ||
                  urlString.contains("wa.me") ||
                  urlString.startsWith("tel:") ||
                  urlString.startsWith("mailto:")
                ) {
                  try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    ctx.startActivity(intent)
                    return true
                  } catch (_: Exception) {
                    // Fallback jika tidak ada aplikasi yang sesuai
                  }
                }
                return false
              }
            }

            // Memuat tautan portal web GPAI Tanjungsari secara langsung
            loadUrl(BLOGSPOT_URL)
            webViewInstance = this
          }
        },
        update = { webView ->
          webViewInstance = webView
        }
      )

      // Indikator Loading Progress
      AnimatedVisibility(
        visible = progressValue in 1..99,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter)
      ) {
        LinearProgressIndicator(
          progress = { progressValue / 100f },
          modifier = Modifier.fillMaxWidth(),
          color = Color(0xFFF59E0B),
          trackColor = Color.Transparent
        )
      }
    }
  }
}
