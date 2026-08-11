package com.nichx.unraidassistant

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.nichx.unraidassistant.ui.navigation.UnraidAssistantApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * 单 Activity 宿主。所有页面由 Navigation Compose 管理。
 * 启动恢复（主题/会话/上次 tab）在 [UnraidAssistantApp] 中统一完成。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 关闭系统在手势条区域的对比度 scrim，避免底部出现多余背景带
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            UnraidAssistantApp()
        }
    }

    // 应用已在后台时点击系统通知：替换当前 intent，使 UnraidNavHost 的
    // LifecycleResumeEffect 能读到 EXTRA_OPEN_NOTIFICATIONS 并跳转通知列表页
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
