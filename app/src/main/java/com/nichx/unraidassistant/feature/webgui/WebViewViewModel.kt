package com.nichx.unraidassistant.feature.webgui

import androidx.lifecycle.ViewModel
import com.nichx.unraidassistant.core.network.CleartextPolicyCache
import com.nichx.unraidassistant.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * WebView 页守卫：向导航回调提供同步的明文放行判定与 SSL 错误放行开关。
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val cleartextPolicyCache: CleartextPolicyCache,
    sessionManager: SessionManager,
) : ViewModel() {

    fun isHostAllowed(host: String): Boolean = cleartextPolicyCache.isHostAllowed(host)

    /** 当前激活服务器是否开启"跳过证书校验"（WebGUI/容器 WebUI 均属激活服务器资产）。 */
    val insecureSkipVerify: Boolean =
        sessionManager.activeServer.value?.insecureSkipVerify == true
}
