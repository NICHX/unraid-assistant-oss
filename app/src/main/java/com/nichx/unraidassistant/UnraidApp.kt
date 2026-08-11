package com.nichx.unraidassistant

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.NetworkFetcher
import coil3.network.okhttp.asNetworkClient
import com.nichx.unraidassistant.core.network.WebViewCookieJar
import com.nichx.unraidassistant.session.NotificationCenter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

/**
 * unRAID 助手 Application 入口。
 *
 * Hilt 在此处生成全局组件容器，所有 @Inject 注入均依赖此声明。
 * Room / ApolloClient / WorkManager 的初始化在对应 Hilt Module 中按需完成。
 * [notificationCenter] 在启动时实例化，使通知实时订阅通道随应用进程常驻
 * （订阅挂在会话作用域下，会话未激活时为空转）。
 *
 * 同时实现 [SingletonImageLoader.Factory]：Coil 在首次加载图片时才调用
 * [newImageLoader]（远晚于 Hilt 注入完成），此时可安全引用注入的共享
 * [okHttpClient]，为所有网络图片请求附加 WebView 会话 Cookie（详见 [WebViewCookieJar]）。
 */
@HiltAndroidApp
class UnraidApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var notificationCenter: NotificationCenter

    @Inject
    lateinit var okHttpClient: OkHttpClient

    /**
     * 全局 Coil ImageLoader：在共享 OkHttpClient 上附加 WebView 会话 CookieJar，
     * 使插件图标等 WebGUI 静态资源请求携带登录会话，避免被 302 重定向到 /login。
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cookieClient = okHttpClient.newBuilder()
            .cookieJar(WebViewCookieJar())
            .build()
        return ImageLoader.Builder(context)
            .components {
                add(NetworkFetcher.Factory(networkClient = { cookieClient.asNetworkClient() }))
            }
            .build()
    }
}
