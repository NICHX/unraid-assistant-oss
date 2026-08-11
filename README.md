# unRAID 助手（开源版）

> 完全免费开源，全部核心管理能力开箱即用，无需任何授权。

unRAID 助手是一款 Android 端 unRAID NAS 管理应用，支持服务器状态监控、存储阵列、Docker 容器、虚拟机、Web 终端等核心管理能力。本仓库为**开源版**，代码以 MIT 协议开放。

## ✨ 功能特性

| 模块 | 能力 |
| --- | --- |
| 📊 仪表盘 | 服务器实时状态、系统负载、内存/CPU 概览 |
| 💾 存储 | 阵列/池状态、磁盘信息与温度监控 |
| 🐳 Docker | 容器列表与运行状态查看 |
| 🖥️ 虚拟机 | VM 列表与状态查看 |
| 🌐 WebGUI | 内置 Web 视图访问 unRAID 管理界面 |
| 🔔 通知 | 系统通知事件（由 unRAID 通知系统转发） |
| ⚙️ 服务器管理 | 添加/删除/切换服务器 |

## 🔄 更新与下载

新版本发布与 APK 下载见 [GitHub Releases](https://github.com/NICHX/unraid-assistant-oss/releases)，应用内设置页亦提供更新检查入口。

## 🚀 构建

环境要求：JDK 17+、Android SDK（compileSdk 35）。

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # macOS（Android Studio 自带 JBR）示例
export ANDROID_HOME="$HOME/Library/Android/sdk"                                  # 按本机 SDK 路径调整

./gradlew assembleDebug        # 构建 Debug APK
./gradlew lintDebug            # 静态检查
./gradlew testDebugUnitTest    # 单元测试
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`。

> 包名：`com.nichx.unraidassistant.oss`。

## 📚 技术栈

- Kotlin / Jetpack Compose（Material 3）、Navigation Compose
- Hilt（依赖注入）
- Apollo GraphQL（unRAID 官方 API）
- DataStore / Flow（状态管理）
- Coil（图片加载）

## 🤝 贡献指南

欢迎提交 Issue 报告 Bug 或提出功能建议，请附上复现步骤与版本信息。

## 📄 开源许可

本项目以 [MIT](LICENSE) 协议开源。

### 商标声明

unRAID 是 Lime Technology, Inc. 的商标。本项目为第三方开发的非官方应用，与 Lime Technology, Inc. 无任何关联或背书。项目中出现的 unRAID、Lime Technology 等名称仅用于描述产品兼容性。

### 第三方组件

应用使用若干第三方开源库（Jetpack、Hilt、Apollo GraphQL、Coil 等），其各自许可证见项目依赖声明。
