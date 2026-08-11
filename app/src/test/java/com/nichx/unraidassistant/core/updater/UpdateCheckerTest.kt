package com.nichx.unraidassistant.core.updater

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 更新检查器单元测试：覆盖版本号比较规则与 GitHub Releases 响应解析。
 * （网络请求部分不在此测试：依赖真实 GitHub API，由集成环境验证。）
 */
class UpdateCheckerTest {

    private val checker = UpdateChecker(OkHttpClient())

    @Test
    fun `新版本号大于当前版本`() {
        assertTrue(checker.compareVersions("v1.0.1", "1.0") > 0)
        assertTrue(checker.compareVersions("1.0.1", "1.0.0") > 0)
        assertTrue(checker.compareVersions("2.0", "1.9.9") > 0)
        assertTrue(checker.compareVersions("1.10", "1.9") > 0)
    }

    @Test
    fun `同版本或旧版本不判定为更新`() {
        assertEquals(0, checker.compareVersions("v1.0", "1.0"))
        assertEquals(0, checker.compareVersions("1.0.0", "1.0"))
        assertEquals(0, checker.compareVersions("1.0.1-beta", "1.0.1"))
        assertTrue(checker.compareVersions("1.0", "1.0.1") < 0)
        assertTrue(checker.compareVersions("1.0", "2.0") < 0)
    }

    @Test
    fun `不可解析的版本号按无更新处理`() {
        assertEquals(0, checker.compareVersions("", "1.0"))
        assertEquals(0, checker.compareVersions("abc", "1.0"))
        assertEquals(0, checker.compareVersions("v1.0", ""))
    }

    @Test
    fun `解析最新 Release 响应`() {
        val raw = """
            {
              "tag_name": "v1.0.1",
              "name": "v1.0.1",
              "body": "修复若干问题\n- 修复 A\n- 优化 B",
              "html_url": "https://github.com/NICHX/unraid-assistant-releases/releases/tag/v1.0.1",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "unraid-assistant-v1.0.1.apk",
                  "browser_download_url": "https://github.com/NICHX/unraid-assistant-releases/releases/download/v1.0.1/unraid-assistant-v1.0.1.apk",
                  "size": 12345678
                }
              ]
            }
        """.trimIndent()
        val release = checker.decodeRelease(raw)
        assertNotNull(release)
        assertEquals("v1.0.1", release?.tagName)
        assertEquals("修复若干问题\n- 修复 A\n- 优化 B", release?.body)
        val apk = release?.assets?.firstOrNull()
        assertEquals("unraid-assistant-v1.0.1.apk", apk?.name)
        assertTrue(apk?.browserDownloadUrl?.endsWith(".apk") == true)
    }

    @Test
    fun `未知字段与缺失字段不报错`() {
        val raw = """{"tag_name":"v2.0.0"}"""
        val release = checker.decodeRelease(raw)
        assertNotNull(release)
        assertEquals("v2.0.0", release?.tagName)
        assertTrue(release?.assets.isNullOrEmpty())
        assertNull(release?.htmlUrl)
    }

    @Test
    fun `非 JSON 响应解析返回 null`() {
        assertNull(checker.decodeRelease("not json"))
        assertNull(checker.decodeRelease(""))
    }

    @Test
    fun `未知字段被忽略且空对象可解析`() {
        // ignoreUnknownKeys=true：错误响应 `{"error":"Not Found"}` 也能解析成空 Release 对象
        val release = checker.decodeRelease("""{"error":"Not Found"}""")
        assertNotNull(release)
        assertNull(release?.tagName)
    }

    @Test
    fun `无 apk 资产时下载地址回退到 Release 页面`() {
        val raw = """
            {
              "tag_name": "v1.1.0",
              "html_url": "https://github.com/NICHX/unraid-assistant-releases/releases/tag/v1.1.0",
              "assets": []
            }
        """.trimIndent()
        val release = checker.decodeRelease(raw)
        assertNotNull(release)
        assertEquals("https://github.com/NICHX/unraid-assistant-releases/releases/tag/v1.1.0", release?.htmlUrl)
        assertFalse(release?.assets?.isNotEmpty() == true)
    }
}
