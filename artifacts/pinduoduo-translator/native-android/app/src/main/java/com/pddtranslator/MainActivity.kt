package com.pddtranslator

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val START_URL = "https://mobile.yangkeduo.com/"
private const val BRIDGE_NAME = "PinduoduoRussianBridge"
private const val MAX_TRANSLATION_ITEMS = 18
private const val MAX_NODE_TEXT_LENGTH = 400
private val BrowserBackground = Color(0xFFF6F5F2)
private val BrowserInk = Color(0xFF202327)
private val BrowserAccent = Color(0xFFE8503A)

private val PINDUODUO_HOSTS = setOf(
    "yangkeduo.com",
    "pinduoduo.com",
    "pinduoduo.net",
)

private val PINDUODUO_ORIGIN_RULES = setOf(
    "https://yangkeduo.com",
    "https://*.yangkeduo.com",
    "https://pinduoduo.com",
    "https://*.pinduoduo.com",
    "https://pinduoduo.net",
    "https://*.pinduoduo.net",
)

class MainActivity : ComponentActivity() {
    private var browserWebView: WebView? = null
    private var translationBridgeInstalled = false
    private lateinit var translations: TranslationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        translations = TranslationRepository(this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = BrowserAccent,
                    background = BrowserBackground,
                    surface = Color.White,
                    onBackground = BrowserInk,
                    onSurface = BrowserInk,
                ),
            ) {
                PinduoduoBrowser(
                    activity = this,
                    translations = translations,
                    onBrowserReady = { view, bridgeInstalled ->
                        browserWebView = view
                        translationBridgeInstalled = bridgeInstalled
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        browserWebView?.onResume()
    }

    override fun onPause() {
        browserWebView?.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        browserWebView?.let { view ->
            view.stopLoading()
            if (
                translationBridgeInstalled &&
                WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
            ) {
                WebViewCompat.removeWebMessageListener(view, BRIDGE_NAME)
            }
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        if (::translations.isInitialized) translations.close()
        super.onDestroy()
    }
}

@Composable
private fun PinduoduoBrowser(
    activity: MainActivity,
    translations: TranslationRepository,
    onBrowserReady: (WebView, Boolean) -> Unit,
) {
    var progress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(START_URL) }
    var pageStatus by remember { mutableStateOf("Открываем мобильный сайт Pinduoduo…") }
    var loadError by remember { mutableStateOf(false) }

    val webView = remember(activity) {
        createPinduoduoWebView(
            activity = activity,
            translations = translations,
            onProgress = { progress = it },
            onUrlChanged = { currentUrl = it },
            onStatus = { pageStatus = it },
            onLoadError = { message ->
                loadError = message != null
                if (message != null) pageStatus = message
            },
        ).also { (view, bridgeInstalled) ->
            onBrowserReady(view, bridgeInstalled)
        }.first
    }

    BackHandler(enabled = webView.canGoBack()) {
        webView.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowserBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                enabled = webView.canGoBack(),
                onClick = { webView.goBack() },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            IconButton(
                enabled = webView.canGoForward(),
                onClick = { webView.goForward() },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Вперёд")
            }
            IconButton(
                onClick = {
                    loadError = false
                    webView.reload()
                },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
            IconButton(
                onClick = {
                    loadError = false
                    webView.loadUrl(START_URL)
                },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Default.Home, contentDescription = "На главную Pinduoduo")
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = BrowserAccent,
                trackColor = Color(0xFFE9E6E1),
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = Color(0xFFE9E6E1),
            ) {}
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = Uri.parse(currentUrl).host ?: "Pinduoduo",
                color = BrowserInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pageStatus,
                color = Color(0xFF6F7479),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
            if (loadError) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrowserBackground,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = pageStatus,
                            color = BrowserInk,
                            fontSize = 15.sp,
                        )
                        IconButton(
                            onClick = {
                                loadError = false
                                webView.reload()
                            },
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Повторить загрузку")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createPinduoduoWebView(
    activity: MainActivity,
    translations: TranslationRepository,
    onProgress: (Int) -> Unit,
    onUrlChanged: (String) -> Unit,
    onStatus: (String) -> Unit,
    onLoadError: (String?) -> Unit,
): Pair<WebView, Boolean> {
    val webView = WebView(activity)
    with(webView.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadsImagesAutomatically = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(false)
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        allowFileAccess = false
        allowContentAccess = false
        cacheMode = WebSettings.LOAD_DEFAULT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
    }
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }

    val bridgeInstalled = installTranslationBridge(
        webView = webView,
        activity = activity,
        translations = translations,
        onStatus = onStatus,
    )
    val translationScript = runCatching {
        activity.assets.open("pdd_translator.js").bufferedReader().use { it.readText() }
    }.getOrDefault("")

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgress(newProgress)
        }
    }
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean =
            handleNavigation(activity, view, request.url)

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean =
            handleNavigation(activity, view, Uri.parse(url))

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (url != null) onUrlChanged(url)
            onLoadError(null)
            onStatus("Загрузка страницы…")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            if (url == null || view == null) return
            onUrlChanged(url)
            if (isPinduoduoHost(Uri.parse(url).host)) {
                if (bridgeInstalled && translationScript.isNotBlank()) {
                    view.evaluateJavascript(translationScript, null)
                    onStatus("Страница Pinduoduo открыта · перевод включён")
                } else {
                    onStatus("Сайт открыт; перевод недоступен в текущей версии Android System WebView")
                }
            } else {
                onStatus("Внешняя страница открыта · перевод работает только на Pinduoduo")
            }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            if (request?.isForMainFrame == true) {
                onLoadError("Не удалось загрузить сайт. Проверьте интернет и нажмите обновить.")
            }
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?,
        ) {
            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                onStatus("Сайт ответил ошибкой HTTP ${errorResponse?.statusCode}")
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: android.net.http.SslError?,
        ) {
            handler?.cancel()
            onLoadError("Сертификат сайта не прошёл проверку. Соединение отменено.")
        }

        override fun onSafeBrowsingHit(
            view: WebView?,
            request: WebResourceRequest?,
            threatType: Int,
            callback: SafeBrowsingResponse?,
        ) {
            callback?.backToSafety(true)
            onLoadError("Android заблокировал небезопасную страницу.")
        }
    }
    webView.loadUrl(START_URL)
    return webView to bridgeInstalled
}

private fun installTranslationBridge(
    webView: WebView,
    activity: MainActivity,
    translations: TranslationRepository,
    onStatus: (String) -> Unit,
): Boolean {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return false
    return runCatching {
        WebViewCompat.addWebMessageListener(
            webView,
            BRIDGE_NAME,
            PINDUODUO_ORIGIN_RULES,
            object : WebViewCompat.WebMessageListener {
                override fun onPostMessage(
                    view: WebView,
                    message: WebMessageCompat,
                    sourceOrigin: Uri,
                    isMainFrame: Boolean,
                    replyProxy: JavaScriptReplyProxy,
                ) {
                    if (!isMainFrame || !isPinduoduoHost(sourceOrigin.host)) return
                    processTranslationMessage(
                        message = message.data.orEmpty(),
                        activity = activity,
                        translations = translations,
                        replyProxy = replyProxy,
                        onStatus = onStatus,
                    )
                }
            },
        )
        true
    }.getOrDefault(false)
}

private fun processTranslationMessage(
    message: String,
    activity: MainActivity,
    translations: TranslationRepository,
    replyProxy: JavaScriptReplyProxy,
    onStatus: (String) -> Unit,
) {
    val requestItems = runCatching { JSONObject(message).optJSONArray("items") }.getOrNull()
    if (requestItems == null || requestItems.length() !in 1..MAX_TRANSLATION_ITEMS) return

    activity.runOnUiThread { onStatus("Перевод текста страницы…") }
    val responses = arrayOfNulls<JSONObject>(requestItems.length())
    val remaining = AtomicInteger(requestItems.length())
    for (index in 0 until requestItems.length()) {
        val request = requestItems.optJSONObject(index)
        val id = request?.optString("id").orEmpty()
        val text = request?.optString("text").orEmpty()
        val response = JSONObject().put("id", id)
        if (id.isBlank() || text.isBlank() || text.length > MAX_NODE_TEXT_LENGTH) {
            responses[index] = response.put("text", text).put("error", true)
            if (remaining.decrementAndGet() == 0) postTranslationResponse(activity, replyProxy, responses)
            continue
        }

        translations.translatePreservingValues(text) { result ->
            result.onSuccess { translated ->
                responses[index] = response.put("text", translated)
                activity.runOnUiThread { onStatus("Страница Pinduoduo открыта · перевод включён") }
            }.onFailure { error ->
                responses[index] = response.put("text", text).put("error", true)
                activity.runOnUiThread {
                    onStatus(
                        error.message?.let { "Перевод не выполнен; исходный текст сохранён: $it" }
                            ?: "Перевод не выполнен; исходный текст сохранён",
                    )
                }
            }
            if (remaining.decrementAndGet() == 0) {
                postTranslationResponse(activity, replyProxy, responses)
            }
        }
    }
}

private fun postTranslationResponse(
    activity: MainActivity,
    replyProxy: JavaScriptReplyProxy,
    responses: Array<JSONObject?>,
) {
    val items = JSONArray()
    responses.forEach { item -> if (item != null) items.put(item) }
    val result = JSONObject().put("items", items).toString()
    activity.runOnUiThread {
        runCatching { replyProxy.postMessage(result) }
    }
}

private fun handleNavigation(activity: MainActivity, webView: WebView?, uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme == "http" || scheme == "https") {
        if (isPinduoduoWebUri(uri)) return false
        return openExternalWebPage(activity, uri)
    }

    if (scheme == "intent") {
        val parsedIntent = runCatching {
            Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
        }.getOrNull() ?: return true

        val internalWebTarget = parsedIntent.data
            ?.takeIf(::isPinduoduoWebUri)
            ?: parsedIntent.getStringExtra("browser_fallback_url")
                ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                ?.takeIf(::isPinduoduoWebUri)

        if (internalWebTarget != null) {
            webView?.loadUrl(internalWebTarget.toString())
        }
        return true
    }

    // Keep WebView-managed document/data URLs working, but never dispatch other schemes
    // (for example market://, pdd://, tel:, or arbitrary app links) to Android.
    return scheme !in setOf("about", "blob", "data")
}

private fun openExternalWebPage(activity: MainActivity, uri: Uri): Boolean {
    if (uri.host.isNullOrBlank()) return true
    return try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: ActivityNotFoundException) {
        true
    } catch (_: Exception) {
        true
    }
}

private fun isPinduoduoWebUri(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    return (scheme == "http" || scheme == "https") && isPinduoduoHost(uri.host)
}

private fun isPinduoduoHost(host: String?): Boolean {
    val normalized = host?.lowercase(Locale.ROOT) ?: return false
    return PINDUODUO_HOSTS.any { normalized == it || normalized.endsWith(".$it") }
}
