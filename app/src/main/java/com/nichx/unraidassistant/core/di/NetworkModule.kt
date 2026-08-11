package com.nichx.unraidassistant.core.di

import com.nichx.unraidassistant.BuildConfig
import com.nichx.unraidassistant.core.network.CleartextPolicyCache
import com.nichx.unraidassistant.core.network.CleartextWhitelistInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(policyCache: CleartextPolicyCache): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            // API Key 等同 admin 权限，日志中强制脱敏
            redactHeader("x-api-key")
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .addInterceptor(CleartextWhitelistInterceptor(policyCache))
            .addInterceptor(logger)
            .build()
    }
}
