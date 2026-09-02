package com.openminis.app.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.openminis.app.ui.theme.ChatColors

/**
 * Inline web browser preview panel for the chat composer.
 * Shows a WebView with URL bar and navigation controls.
 */
@Composable
fun InlineWebPreview(
    modifier: Modifier = Modifier,
    initialUrl: String = "https://openminis.app",
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 更新地址栏文本（由导航按钮/加载完成回调调用）
    fun updateUrl(newUrl: String?) {
        if (!newUrl.isNullOrBlank()) url = newUrl
    }
    
    // Create WebView holder
    val holder = remember(context, initialUrl) {
        InlineWebHolder(context, initialUrl).apply {
            pageTitle?.let { pageTitle = it }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .border(1.dp, ChatColors.thumbnailBorder, RoundedCornerShape(8.dp))
            .background(ChatColors.secondaryBg, RoundedCornerShape(8.dp))
    ) {
        // URL bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { holder.goBack(); updateUrl(holder.currentUrl) },
                enabled = holder.canGoBack
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (holder.canGoBack) Color.Unspecified else ChatColors.secondaryText.copy(alpha = 0.3f)
                )
            }
            
            IconButton(
                onClick = { holder.goForward(); updateUrl(holder.currentUrl) },
                enabled = holder.canGoForward
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (holder.canGoForward) Color.Unspecified else ChatColors.secondaryText.copy(alpha = 0.3f)
                )
            }
            
            IconButton(
                onClick = { holder.reload() }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = ChatColors.secondaryText
                )
            }
            
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = ChatColors.thumbnailBorder,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            
            IconButton(
                onClick = { 
                    val normalizedUrl = normalizeUrl(url)
                    holder.loadUrl(normalizedUrl)
                    updateUrl(normalizedUrl)
                }
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = "Go",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
        
        // WebView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 4.dp)
        ) {
            AndroidView(
                factory = { holder.webView },
                modifier = Modifier.fillMaxSize()
            )
            
            // Page title overlay (optional)
            if (pageTitle.isNotEmpty()) {
                Text(
                    text = pageTitle,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Normalize URL - add https:// if missing */
private fun normalizeUrl(raw: String): String {
    return when {
        raw.isEmpty() -> "https://openminis.app"
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        raw.startsWith("minis://") -> resolveMinisUrl(raw) ?: "about:blank"
        raw.contains(".") && !raw.contains(" ") -> "https://$raw"
        else -> "https://www.google.com/search?q=${Uri.encode(raw)}"
    }
}

/** Resolve minis:// protocol to local file path */
private fun resolveMinisUrl(url: String): String? {
    return when {
        url.startsWith("minis://workspace/") -> "file:///var/minis${url.removePrefix("minis://")}"
        url.startsWith("minis://attachments/") -> "file:///var/minis${url.removePrefix("minis://")}"
        url.startsWith("minis://shared/") -> "file:///var/minis${url.removePrefix("minis://")}"
        url.startsWith("minis://memory/") -> "file:///var/minis${url.removePrefix("minis://")}"
        url == "minis://workspace" -> "file:///var/minis/workspace/"
        url == "minis://attachments" -> "file:///var/minis/attachments/"
        url == "minis://shared" -> "file:///var/minis/shared/"
        url == "minis://memory" -> "file:///var/minis/memory/"
        else -> null
    }
}

/** Simple WebView holder that manages lifecycle */
class InlineWebHolder(
    appContext: Context,
    initialUrl: String,
) {
    var currentUrl by mutableStateOf(initialUrl)
        private set
    var pageTitle by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var canGoBack by mutableStateOf(false)
        private set
    var canGoForward by mutableStateOf(false)
        private set

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(appContext).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        
        val ua = settings.userAgentString
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
        
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: android.webkit.WebResourceRequest,
            ): Boolean {
                val urlStr = request.url?.toString() ?: return false
                // Handle minis:// URLs
                val resolved = resolveMinisUrl(urlStr)
                if (resolved != null) {
                    loadUrl(resolved)
                    return true
                }
                // Route external schemes
                when {
                    urlStr.startsWith("intent://") -> {
                        try {
                            val intent = android.content.Intent.parseUri(urlStr, android.content.Intent.URI_INTENT_SCHEME)
                            view.context.startActivity(intent)
                        } catch (_: Exception) {}
                        return true
                    }
                    urlStr.startsWith("tel:") || urlStr.startsWith("mailto:") -> {
                        view.context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(urlStr)))
                        return true
                    }
                }
                return false
            }
            
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                isLoading = true
                currentUrl = url
                canGoBack = view.canGoBack()
                canGoForward = view.canGoForward()
            }
            
            override fun onPageFinished(view: WebView, url: String) {
                isLoading = false
                pageTitle = view.title?.takeIf { it.isNotEmpty() }
                canGoBack = view.canGoBack()
                canGoForward = view.canGoForward()
                // Trigger resize for vh-based layouts
                view.postDelayed({
                    view.evaluateJavascript("window.dispatchEvent(new Event('resize'));", null)
                }, 100)
            }
        }
        
        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrEmpty()) pageTitle = title
            }
            
            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?,
            ): Boolean {
                val ctx = view?.context ?: return false.also { result?.cancel() }
                if (ctx !is android.app.Activity) { result?.cancel(); return true }
                android.app.AlertDialog.Builder(ctx)
                    .setMessage(message.orEmpty())
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }
            
            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?,
            ): Boolean {
                val ctx = view?.context ?: return false.also { result?.cancel() }
                if (ctx !is android.app.Activity) { result?.cancel(); return true }
                android.app.AlertDialog.Builder(ctx)
                    .setMessage(message.orEmpty())
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }
        }
        
        addOnLayoutChangeListener { v, _, top, _, bottom, _, oldTop, _, oldBottom ->
            val oldH = oldBottom - oldTop
            val newH = bottom - top
            if (newH > 0 && newH != oldH) {
                (v as WebView).evaluateJavascript(
                    "window.dispatchEvent(new Event('resize'));",
                    null,
                )
            }
        }
    }
    
    fun loadUrl(newUrl: String) {
        currentUrl = newUrl
        webView.loadUrl(newUrl)
    }
    
    fun reload() {
        webView.reload()
    }
    
    fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
            currentUrl = webView.url ?: currentUrl
            canGoBack = webView.canGoBack()
            canGoForward = webView.canGoForward()
        }
    }
    
    fun goForward() {
        if (webView.canGoForward()) {
            webView.goForward()
            currentUrl = webView.url ?: currentUrl
            canGoBack = webView.canGoBack()
            canGoForward = webView.canGoForward()
        }
    }
    
    fun destroy() {
        webView.destroy()
    }
}
