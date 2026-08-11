plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.apollo)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Gradle Kotlin DSL 中 `java` 可能被扩展解析遮蔽，全限定名 java.util.Properties 会解析失败；
// 且 AGP DSL 作用域内存在同名 `Properties` 类型，故使用别名导入彻底规避歧义。
import java.util.Properties as JvmProperties

android {
    namespace = "com.nichx.unraidassistant"
    compileSdk = 37

    defaultConfig {
        // 独立 applicationId（.oss 后缀），便于与其它构建产物共存安装；
        // namespace 保持不变（代码包路径/R 类/graphql 均不受影响）。
        applicationId = "com.nichx.unraidassistant.oss"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        // X.Y.Z 三段式版本号：设置页「关于」显示为 vX.Y.Z，且必须与 GitHub Release tag 去 v 前缀后完全一致
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        getByName("debug") {
            // 复用项目专属 debug.keystore（保证多设备签名一致）
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Release 签名：从项目根目录 keystore.properties（已在 .gitignore 排除）读取。
        // 文件缺失或任一字段缺失时跳过注册 → release 构建保持未签名（本地/CI 无密钥时仍可构建）；
        // CI 发布时通过 secrets 写入该文件后自动挂接正式签名（依据：docs/GitHubRelease流程设计.md §4）。
        val releaseProps = rootProject.file("keystore.properties").let { f ->
            if (!f.exists()) null
            else JvmProperties().apply { f.inputStream().use { load(it) } }
        }
        val storeFilePath = releaseProps?.getProperty("storeFile")
        val storePassword = releaseProps?.getProperty("storePassword")
        val keyAlias = releaseProps?.getProperty("keyAlias")
        val keyPassword = releaseProps?.getProperty("keyPassword")
        if (storeFilePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                // storeFile 相对路径以 app/ 模块目录为基准（与 debug.keystore 同规则）
                storeFile = file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // keystore.properties 存在时挂接正式签名；缺失时保持未签名（产物为 app-release-unsigned.apk）
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // 本地 JVM 单测中未 mock 的 Android 框架方法（如 android.util.Log）返回默认值，
        // 避免被测试代码里的 Log.d/Log.w 抛出 "not mocked" 异常（官方推荐做法）。
        unitTests.isReturnDefaultValues = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

apollo {
    service("unraid") {
        packageName.set("com.nichx.unraidassistant.data.remote.graphql")
        // 自定义 scalar 映射：BigInt→Long（磁盘 KB 容量/计数），DateTime→String（ISO 时间戳），
        // JSON→Any（labels 等非结构化字段），Port→Int（容器端口）
        mapScalar("BigInt", "kotlin.Long")
        mapScalar("DateTime", "kotlin.String")
        mapScalar("JSON", "kotlin.Any")
        mapScalar("Port", "kotlin.Int")
        mapScalar("PrefixedID", "kotlin.String")
    }
}

dependencies {
    // Core / Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Apollo GraphQL
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.cache)

    // Network（GitHub raw / composerize HTTP API）
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    // Image
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Chart
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // YAML / SSH（Compose 模块）
    implementation(libs.kaml)
    implementation(libs.sshj)

    // Coroutines / Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Storage
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kxml2)
}
