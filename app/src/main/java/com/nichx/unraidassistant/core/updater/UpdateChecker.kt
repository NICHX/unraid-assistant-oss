package com.nichx.unraidassistant.core.updater

import android.util.Log
import com.nichx.unraidassistant.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val UPDATE_TAG = "UpdateChecker"

/** 发布通道仓库：GitHub Releases（下载地址指向该仓库的 Release APK 资产）。 */
// Release 发布到开源版仓库 NICHX/unraid-assistant-oss（用户端匿名访问）。
const val GITHUB_REPO_OWNER = "NICHX"
const val GITHUB_REPO_NAME = "unraid-assistant-oss"

/**
 * GitHub Releases API 候选地址：直连优先，失败时依次回退到 gh-proxy 前缀代理
 * （与内置镜像加速源同一批代理，国内网络直连 api.github.com 超时的兜底）。
 */
private val RELEASE_API_CANDIDATES = listOf(
    "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest",
    "https://ghproxy.net/https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest",
    "https://gh-proxy.com/https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest",
    "https://ghfast.top/https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest",
)

/** 单次 Release 请求超时（含失败回退链路，整体最坏约 40s）。 */
private const val RELEASE_API_TIMEOUT_MS = 10_000L

/**
 * 更新检查状态（单例共享：启动自动检查与设置页手动检查共用同一状态流，
 * App 根组件统一订阅弹窗，避免两处各自维护一套弹窗逻辑）。
 */
sealed class UpdateCheckState {
    /** 尚未检查（App 启动后首次检查完成前的初始态）。 */
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    /** 已是最新版本（无更新可下载）。 */
    data object UpToDate : UpdateCheckState()
    /** 发现新版本：[UpdateInfo.downloadUrl] 指向 GitHub Release 的 APK 下载地址。 */
    data class Available(val info: UpdateInfo) : UpdateCheckState()
    /** 检查失败（网络异常 / 仓库无 Release / 响应缺下载地址）。 */
    data class Error(val message: String) : UpdateCheckState()
}

/**
 * 新版本信息：下载地址优先取 GitHub Release 的 APK 资产直链，
 * 资产缺失时回退到 Release 页面地址（用户可手动挑版本）。
 */
data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    /**
     * 本次检查的时间戳：UI 用它区分「同版本再次检查」（checkedAt 变化 → 重新弹窗）
     * 与「旋转屏幕/重组」（checkedAt 不变 → 保持已关闭），见根组件弹窗逻辑。
     */
    val checkedAt: Long,
)

/** GitHub Release 响应中的资产条目（仅保留需要的字段）。 */
@Serializable
data class GithubReleaseAsset(
    @SerialName("name") val name: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
)

/** GitHub `/releases/latest` 响应（忽略未知字段，兼容 API 字段增删）。 */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("draft") val draft: Boolean? = null,
    @SerialName("prerelease") val prerelease: Boolean? = null,
    @SerialName("assets") val assets: List<GithubReleaseAsset> = emptyList(),
)

/**
 * 版本检测与更新检查中枢（单例 StateFlow 模式）。
 *
 * 数据源：GitHub Releases API `/releases/latest`（tag 如 `v1.0.1`）；采用「浏览器下载」
 * 方案——应用内只做检测与跳转，APK 的下载/安装由系统浏览器与安装器完成。
 * 检查时机：App 启动（[com.nichx.unraidassistant.feature.root.AppViewModel] 静默检查，
 * 发现新版本才提示）；设置页「关于」手动点击（[com.nichx.unraidassistant.feature.settings.SettingsViewModel]）。
 *
 * 版本比较规则：tag 取首个 `数字.数字...` 前缀逐段比较（缺失段按 0 补齐），
 * `v` 前缀与预发布后缀（如 `-beta`）不参与比较，避免非数字段干扰大小判断。
 */
@Singleton
class UpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

    // 并发保护：检查进行中时忽略重复调用（启动检查与手动检查可能重叠）
    private val _checking = MutableStateFlow(false)

    private val json = Json { ignoreUnknownKeys = true }

    /** 检查 GitHub 最新 Release 并更新状态流；进行中并发调用直接忽略。 */
    suspend fun check() {
        if (_checking.value) return
        _checking.value = true
        _state.value = UpdateCheckState.Checking
        try {
            val currentVersion = BuildConfig.VERSION_NAME
            val release = fetchLatestRelease()
            if (release == null) {
                // 仓库尚无 Release 或全部候选源失败：不弹"已是最新"，如实告知无发布信息
                _state.value = UpdateCheckState.Error("GitHub 上暂无发布版本或网络不可达")
                return
            }
            val latestVersion = release.tagName ?: release.name ?: ""
            if (latestVersion.isBlank() || compareVersions(latestVersion, currentVersion) <= 0) {
                _state.value = UpdateCheckState.UpToDate
                return
            }
            val downloadUrl = release.assets
                .firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                ?.browserDownloadUrl
                ?: release.htmlUrl
            if (downloadUrl.isNullOrBlank()) {
                _state.value = UpdateCheckState.Error("Release 缺少下载地址")
                return
            }
            _state.value = UpdateCheckState.Available(
                UpdateInfo(
                    latestVersion = latestVersion,
                    currentVersion = currentVersion,
                    releaseNotes = release.body?.trim().orEmpty(),
                    downloadUrl = downloadUrl,
                    checkedAt = System.currentTimeMillis(),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(UPDATE_TAG, "版本检查失败: ${e.message}")
            _state.value = UpdateCheckState.Error(e.message ?: "网络请求失败")
        } finally {
            _checking.value = false
        }
    }

    /** 依次尝试候选地址（直连 → gh-proxy 代理），首个成功即返回；全部失败返回 null。 */
    private suspend fun fetchLatestRelease(): GithubRelease? = withContext(Dispatchers.IO) {
        val client = okHttpClient.newBuilder()
            .callTimeout(RELEASE_API_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
        for (url in RELEASE_API_CANDIDATES) {
            val release = runCatching {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    decodeRelease(resp.body.string())
                }
            }.getOrNull()
            if (release != null) return@withContext release
        }
        null
    }

    /** 反序列化 Release 响应（对外供单测直接验证 JSON 解析）。 */
    internal fun decodeRelease(raw: String): GithubRelease? =
        runCatching { json.decodeFromString<GithubRelease>(raw) }.getOrNull()

    /**
     * 版本号比较：取首个 `数字.数字...` 前缀逐段比较（缺失段按 0 补齐），
     * `v` 前缀与预发布后缀不参与；任一版本不可解析时返回 0（视为无更新）。
     */
    internal fun compareVersions(latest: String, current: String): Int {
        val a = parseVersion(latest) ?: return 0
        val b = parseVersion(current) ?: return 0
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun parseVersion(value: String): List<Int>? {
        val match = Regex("""\d+(\.\d+)*""").find(value.trim().removePrefix("v").removePrefix("V"))
            ?: return null
        return match.value.split(".").mapNotNull { it.toIntOrNull() }
    }
}
