package com.nichx.unraidassistant.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore AES/GCM 加解密助手（由 [ApiKeyStore] 提炼的通用件）。
 *
 * 设计意图：
 * - AES-256 主密钥不可导出地保存在 Android Keystore，密文才能落 SharedPreferences；
 * - 密文载荷结构：`IV 长度(4B) + IV + 密文`（GCM Tag 由 Cipher.doFinal 附加在密文尾部），
 *   解密失败（Keystore 密钥失效/数据被篡改）统一返回 null，不向上抛异常；
 * - 每个调用方使用独立 [keyAlias]，互不干扰。
 */
class KeystoreAesGcm(
    private val keyAlias: String,
) {
    /** 加密明文，返回 "IV 长度 + IV + 密文" 载荷字节。 */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(plaintext)
        return ByteBuffer.allocate(Int.SIZE_BYTES + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
    }

    /** 解密载荷；载荷非法或密钥失效时返回 null（调用方按"无数据"处理）。 */
    fun decrypt(payload: ByteArray): ByteArray? {
        val buffer = ByteBuffer.wrap(payload)
        if (buffer.remaining() < Int.SIZE_BYTES) return null
        val ivSize = buffer.int
        if (ivSize < 0 || buffer.remaining() < ivSize) return null
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(encrypted)
        } catch (e: GeneralSecurityException) {
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
