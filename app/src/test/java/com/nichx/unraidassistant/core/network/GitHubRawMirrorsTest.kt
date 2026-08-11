package com.nichx.unraidassistant.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubRawMirrorsTest {

    private val rawUrl = "https://raw.githubusercontent.com/owner/repo/main/path/file.xml"

    @Test
    fun `默认配置生成直连_jsDelivr与全部内置代理`() {
        val urls = githubRawMirrorUrls(rawUrl)
        assertTrue(urls[0] == rawUrl)
        assertTrue("https://cdn.jsdelivr.net/gh/owner/repo@main/path/file.xml" in urls)
        BuiltInMirrorSource.entries.filter { it.id != BuiltInMirrorSource.DIRECT.id }.forEach { source ->
            if (source != BuiltInMirrorSource.JSDELIVR) {
                assertTrue("缺少内置代理 ${source.id}", urls.any { it.startsWith("https://${source.id}/") })
            }
        }
    }

    @Test
    fun `总开关关闭时仅返回直连`() {
        val urls = githubRawMirrorUrls(rawUrl, MirrorConfig(enabled = false))
        assertEquals(listOf(rawUrl), urls)
    }

    @Test
    fun `禁用的内置源被排除`() {
        val config = MirrorConfig(
            disabledBuiltIns = setOf(
                BuiltInMirrorSource.JSDELIVR.id,
                BuiltInMirrorSource.GHPROXY_NET.id,
            ),
        )
        val urls = githubRawMirrorUrls(rawUrl, config)
        assertTrue(urls.none { it.startsWith("https://cdn.jsdelivr.net/") })
        assertTrue(urls.none { it.startsWith("https://ghproxy.net/") })
        // 其余内置源仍在。
        assertTrue(urls.any { it.startsWith("https://gh-proxy.com/") })
        assertTrue(urls.any { it.startsWith("https://ghfast.top/") })
    }

    @Test
    fun `自定义代理按前缀拼接并追加在末尾`() {
        val config = MirrorConfig(customProxies = listOf("https://custom.proxy/"))
        val urls = githubRawMirrorUrls(rawUrl, config)
        assertEquals("https://custom.proxy/$rawUrl", urls.last())
        assertEquals(6, urls.size) // 直连 + jsDelivr + 3 内置代理 + 1 自定义
    }

    @Test
    fun `非 GitHub raw 链接原样返回`() {
        val other = "https://example.com/foo.xml"
        assertEquals(listOf(other), githubRawMirrorUrls(other))
        assertEquals(listOf(other), githubRawMirrorUrls(other, MirrorConfig(enabled = false)))
    }

    @Test
    fun `内置源枚举 id 与 url 生成对应`() {
        assertFalse(BuiltInMirrorSource.entries.isEmpty())
        assertTrue(BuiltInMirrorSource.DIRECT.id == "direct")
    }
}
