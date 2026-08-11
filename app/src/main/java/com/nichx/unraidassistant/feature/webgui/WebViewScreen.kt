package com.nichx.unraidassistant.feature.webgui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing
import org.json.JSONObject

/**
 * 应用内 WebView 页面（WebGUI 深链）。
 *
 * 从 Dashboard（服务器 WebGUI）或容器详情（容器 WebUI）跳转进入，在应用内直接
 * 渲染 unRAID WebGUI / 容器管理界面，免去跳出到浏览器。登录态由 WebView 内部
 * Cookie 保持，与系统浏览器隔离。
 *
 * 明文安全：WebView 每次导航（含重定向）经白名单守卫 [isBlockedCleartext] 判定，
 * 不在白名单的 http:// 主机立即拦截并展示提示页，HTTPS 不受影响。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
    viewModel: WebViewViewModel = hiltViewModel(),
) {
    val obsidian = LocalObsidianPalette.current
    val context = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    var blockedHost by remember { mutableStateOf<String?>(null) }

    @Suppress("UseKtx")
    fun isBlockedCleartext(rawUrl: String): Boolean {
        val parsed = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
        if (!parsed.scheme.equals("http", ignoreCase = true)) return false
        val host = parsed.host ?: return false
        if (viewModel.isHostAllowed(host)) return false
        blockedHost = host
        return true
    }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            setBackgroundColor(AndroidColor.TRANSPARENT)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (url != null && isBlockedCleartext(url)) {
                        view?.stopLoading()
                    }
                }

                @Suppress("DEPRECATION")
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null && isBlockedCleartext(url)) return true
                    return super.shouldOverrideUrlLoading(view, url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val target = request?.url?.toString()
                    if (target != null && isBlockedCleartext(target)) return true
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler,
                    error: SslError,
                ) {
                    // 自签证书兜底：仅当激活服务器开启"跳过证书校验"时放行，
                    // 否则交给系统默认行为（拦截并提示）。
                    if (viewModel.insecureSkipVerify) {
                        handler.proceed()
                    } else {
                        super.onReceivedSslError(view, handler, error)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectMobileCss(view, url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    LaunchedEffect(url) {
        if (url.isNotBlank() && !isBlockedCleartext(url)) {
            webView.loadUrl(url)
        }
    }

    ObsidianScreenScaffold(
        title = title,
        navigationIcon = {
            IconButton(onClick = {
                if (blockedHost != null || !webView.canGoBack()) {
                    onBack()
                } else {
                    webView.goBack()
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = obsidian.TextSecondary,
                )
            }
        },
        actions = {
            IconButton(onClick = { webView.reload() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "刷新",
                    tint = obsidian.TextSecondary,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
            if (progress < 100 && blockedHost == null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = obsidian.Cyan,
                    trackColor = Color.Transparent,
                )
            }
            blockedHost?.let { host ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(obsidian.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = Spacing.xxxxxl),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = obsidian.Amber,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.size(Spacing.xxl))
                        Text(
                            text = "明文访问被拦截",
                            style = MaterialTheme.typography.titleMedium,
                            color = obsidian.TextPrimary,
                        )
                        Spacer(Modifier.size(Spacing.md))
                        Text(
                            text = "$host 不在明文白名单中\n请到「系统 → 明文白名单」添加该地址后重试",
                            style = MaterialTheme.typography.bodyMedium,
                            color = obsidian.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(Spacing.xxxl))
                        OutlinedButton(onClick = onBack) {
                            Text("返回")
                        }
                    }
                }
            }
        }
    }
}

/** 通用移动端基础样式：防止表单聚焦自动放大、图片撑破布局。 */
private val MOBILE_BASE_CSS = """
    html { -webkit-text-size-adjust: 100%; }
    img, video, iframe { max-width: 100%; height: auto; }
    body { margin: 0; }
    input, select, textarea, button { font-size: 16px; }
""".trimIndent()

/**
 * Unraid 容器编辑/新建页（/UpdateContainer、/AddContainer）移动端适配。
 * 页面自带 <meta name="viewport" content="width=1300">，若不覆盖视口，布局宽度恒为
 * 1300px，整个页面被缩小显示、媒体查询永不生效——因此注入时先替换 viewport meta。
 * 本样式不依赖媒体查询：WebView 必然运行在手机上，直接强制表单纵向堆叠。
 */
private val DOCKER_EDIT_CSS = """
    html, body { width: 100% !important; max-width: 100% !important; margin: 0 !important; }
    #canvas, #content, #main, .panel, .ui-layout-pane, .dashboard, .page-content {
      width: 100% !important; max-width: 100% !important; padding: 8px !important;
      box-sizing: border-box !important;
    }
    input[type=text], input[type=number], input[type=password], input[type=url], select, textarea {
      max-width: 100% !important; box-sizing: border-box !important; font-size: 16px !important;
    }
    #pathRows, #portRows, #envRows {
      width: 100% !important; max-width: 100% !important;
    }
    #pathRows td, #pathRows tbody td,
    #portRows td, #portRows tbody td,
    #envRows td, #envRows tbody td {
      display: block !important; width: 100% !important; box-sizing: border-box !important;
      text-align: left !important; padding: 2px 0 !important;
    }
    .textPath, .textPort, .textEnv { width: 100% !important; box-sizing: border-box !important; }
    .fileTree { width: 100% !important; box-sizing: border-box !important; }
    input[type=button], input[type=submit], button { min-height: 44px; font-size: 16px; }
""".trimIndent()

/** 在页面加载完成后注入移动端适配样式，仅作用于当次页面 DOM。 */
private fun injectMobileCss(view: WebView?, url: String?) {
    val path = url?.substringBefore('?').orEmpty()
    val isDockerForm = path.contains("/UpdateContainer") || path.contains("/AddContainer")
    val css = buildString {
        append(MOBILE_BASE_CSS)
        if (isDockerForm) append('\n').append(DOCKER_EDIT_CSS)
    }
    val script = buildString {
        append("var metas=document.querySelectorAll('meta[name=viewport]');for(var i=0;i<metas.length;i++){metas[i].parentNode.removeChild(metas[i]);}")
        if (isDockerForm) {
            append("var m=document.createElement('meta');m.name='viewport';m.content='width=device-width, initial-scale=1.0';document.head.appendChild(m);")
        }
        append("var e=document.getElementById('ua-mobile-css');if(e)e.remove();")
        append("var s=document.createElement('style');s.id='ua-mobile-css';")
        append("s.textContent=").append(JSONObject.quote(css))
        append(";(document.head||document.documentElement).appendChild(s);")
    }
    view?.evaluateJavascript(script, null)
}
