package com.nichx.unraidassistant.core.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 桥接系统 CookieManager（与 WebView 会话实时同步）的 OkHttp CookieJar。
 *
 * Coil 加载插件图标等 WebGUI 静态资源时必须携带 WebView 登录后的会话 Cookie，
 * 否则服务器会把请求 302 重定向到 /login（实测 curl 无 Cookie 请求图标返回
 * 302 → /login，Coil 跟随重定向后拿到登录页 HTML 导致解码失败）。
 * 读写均透传系统 [CookieManager]，使 Coil/OkHttp 与 WebView 共享同一份会话。
 */
class WebViewCookieJar : CookieJar {

    private val manager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            manager.setCookie(url.toString(), cookie.toString(), null)
        }
        if (cookies.isNotEmpty()) {
            runCatching { manager.flush() }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = manager.getCookie(url.toString()) ?: return emptyList()
        return raw.split(';').mapNotNull { Cookie.parse(url, it.trim()) }
    }
}
