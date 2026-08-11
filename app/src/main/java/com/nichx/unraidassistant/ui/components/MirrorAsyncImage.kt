package com.nichx.unraidassistant.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.nichx.unraidassistant.core.network.MirrorConfig
import com.nichx.unraidassistant.core.network.githubRawMirrorUrls

private const val MIRROR_IMAGE_TAG = "MirrorImage"

/**
 * 带镜像回退的远程图片加载组件。
 *
 * Coil 3 已移除 Coil 2 中 `model = List<...>` 的多源自动回退特性（传入 List 会被当作
 * 单个数据源直接失败），因此这里自行实现：按 [githubRawMirrorUrls] 生成的候选地址
 * 依次尝试，前一个加载失败时自动切换下一个，全部失败后展示 [error]。
 */
@Composable
fun MirrorAsyncImage(
    url: String,
    mirrorConfig: MirrorConfig,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    loading: @Composable () -> Unit,
    error: @Composable () -> Unit,
) {
    val urls = remember(url, mirrorConfig) { githubRawMirrorUrls(url, mirrorConfig) }
    var index by remember(urls) { mutableIntStateOf(0) }
    val current = urls.getOrNull(index)
    Log.d(MIRROR_IMAGE_TAG, "候选列表(${urls.size}): ${urls.joinToString(" | ")}")
    if (current == null) {
        Log.w(MIRROR_IMAGE_TAG, "候选耗尽，展示兜底: $url")
        error()
        return
    }
    SubcomposeAsyncImage(
        model = current,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = {
            Log.w(MIRROR_IMAGE_TAG, "加载失败(切换候选): index=$index url=$current 剩余=${urls.size - index - 1}")
            if (index < urls.lastIndex) index++
        },
        loading = { loading() },
        error = {
            Log.w(MIRROR_IMAGE_TAG, "全部候选加载失败，展示兜底: $url")
            if (index < urls.lastIndex) loading() else error()
        },
    )
}
