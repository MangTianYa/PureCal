# PureCal · 日历

[![PureCal CI & Release](https://github.com/MangTianYa/PureCal/actions/workflows/release.yml/badge.svg)](https://github.com/MangTianYa/PureCal/actions/workflows/release.yml)
[![Release](https://img.shields.io/github/v/release/MangTianYa/PureCal?label=Release&color=0B57D0)](https://github.com/MangTianYa/PureCal/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://android.com)
[![SDK](https://img.shields.io/badge/Compile%20SDK-Android%2017%20(API%2037)-blue.svg)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64--v8a-orange.svg)](https://developer.android.com/ndk/guides/abis)

一款采用 100% Kotlin + Jetpack Compose 编写的现代中国日历 App。整体视觉与交互完全遵循 **Google Pixel / Material Design 3 (Material You)** 设计语言：原生支持 Android 动态壁纸取色、Pixel 经典配色、现代化无衬线字体排版体系、流线型圆角卡片与标准 Material Icons。

---

## ✨ 核心特性

- 📱 **Google Pixel / Material You 风格**：原生支持 Android 12+ 动态色彩壁纸取色（Dynamic Color），默认采用 Google 经典 Pixel 蓝（`#0B57D0`）与 Tonal 容器层级；深色模式舒适防刺眼。
- ⚡ **月份切换 GPU 纯硬件加速动效**：在 Draw 渲染阶段延迟读取滑动偏移量，彻底消除 Compose 重组丢帧，达成丝滑满帧的 Scale + Fade 与垂直年月滚动翻转动效。
- 📅 **公历 + 农历 + 节气 + 节日一体**：横向滑动或快捷点击 `<` / `>` 切换月份，公历、农历、二十四节气、法定节假日与调休安排清晰呈现。
- 🔮 **每日宜忌与完整黄历**：公历对应农历、干支纪年/月/日、生肖、每日宜忌择吉、值神、建除十二神、星宿、六曜、月相、纳音、冲煞、胎神、彭祖百忌、吉神凶煞及喜神/财神/福神方位。
- 🇨🇳 **法定节假日与调休倒计时**：内置国务院办公厅权威安排，支持按年浏览假期；清晰标记加班补班日期，并提供下一个假期的实时倒计时。
- 🔄 **在线自动更新与多源容灾**：支持 Hi8、提莫 API、聚合数据等多是在线数据源自动轮询更新，24 小时防频次节流，离线模式无网络请求零打扰。
- 🛠️ **专属 64 位原生构建**：仅编译打包 `arm64-v8a`，移除冗余 ABI，包体精简至约 1.4MB，充分发挥现代 64 位芯片极致性能。
- 🟢 **圆形自适应图标**：规范适配 66dp 安全区与独立矢量圆形底盘，完美契合 Pixel Launcher 与各品牌启动器。
- 🤖 **CI/CD 自动化持续集成**：推送代码或发布标签时，GitHub Actions 自动执行单元测试、编译 Release 签名包并自动推送到 Release 发布页。

---

## 🚀 架构与技术栈

| 模块 / 层面 | 技术栈与规范 |
|---|---|
| **编程语言** | Kotlin 2.0.21 |
| **UI 框架** | Jetpack Compose + Material 3 (Material You) |
| **系统架构** | MVVM + 单向数据流 (UDF) + Repository Pattern |
| **农历 / 黄历** | [lunar-java](https://github.com/6tail/lunar-java) 1.7.7（MIT） |
| **内置节假日** | [holiday-cn](https://github.com/NateScarlet/holiday-cn)（MIT） |
| **在线节假日 API** | Hi8 / 提莫 API / 聚合数据（设置内自由切换） |
| **编译目标** | Android 17 最新 SDK (`compileSdk = 37`, `targetSdk = 37`) |
| **最低支持** | Android 8.0（API 26） |
| **原生架构** | `arm64-v8a` 专属编译 |
| **自动化构建** | GitHub Actions 持续集成与 Release 自动发布 |

---

## 🛠️ 本地编译与运行

### 1. 环境要求
- JDK 17
- Android SDK Platform 35 / 37
- Android SDK Build-Tools 35.0.0 / 37.0.0

### 2. 构建 Debug APK
```bash
./gradlew assembleDebug
```
产物位置：`app/build/outputs/apk/debug/app-debug.apk`

### 3. 构建 Release 签名 APK
```bash
./gradlew assembleRelease
```
产物位置：`app/build/outputs/apk/release/app-release.apk`  
*(注：项目已包含自动化版本递增机制，每次执行 assemble 会自动递增 `version.properties` 中的版本号)*

### 4. 运行单元测试
```bash
./gradlew testDebugUnitTest
```

---

## 📂 项目结构导览

```text
app/src/main/java/com/dotcalendar/app/
├── MainActivity.kt                 # 入口、Edge-to-Edge、主题与 Splash 初始化
├── DotCalendarApplication.kt       # Application 容器与后台更新器调度
├── almanac/
│   ├── Almanac.kt                  # 黄历与宜忌完整数据模型
│   └── AlmanacRepository.kt        # lunar-java 历法计算与不可变快照
├── data/
│   ├── HolidaySource.kt            # 在线数据源元数据定义
│   ├── HolidayProviders.kt         # 严苛超时的全 HTTPS 客户端与多源解析器
│   ├── HolidayNormalizer.kt        # 假期名称归一化映射
│   ├── HolidayUpdater.kt           # 24小时防抖检查、自动回退与状态机
│   ├── HolidayRepository.kt        # 离线优先、内存/磁盘缓存与不可变数据快照
│   └── SettingsRepository.kt       # 用户偏好设置持久化
├── ui/
│   ├── DotCalendarRoot.kt          # 根导航 Scaffold 与 M3 胶囊药丸导航栏
│   ├── calendar/                   # 月视图、GPU加速动画、日期网格、今日卡片
│   ├── holidays/                   # 法定节假日、调休补班及倒计时面板
│   ├── almanac/                    # 完整黄历 BottomSheet 弹窗
│   ├── settings/                   # 主题切换、数据源选择及聚合密钥配置
│   ├── components/                 # 品牌行、胶囊药丸与 Material 矢量图标
│   └── theme/                      # Pixel 经典配色、M3 调色板、无衬线字体排版
└── util/DateFormats.kt
```

---

## 📄 开源许可

本项目源码采用 [MIT License](LICENSE) 授权开源。

本项目使用的第三方开源组件与数据许可：
- [lunar-java](https://github.com/6tail/lunar-java) — MIT License
- [holiday-cn](https://github.com/NateScarlet/holiday-cn) — MIT License
- [Material Icons](https://fonts.google.com/icons) — Apache License 2.0
- Hi8 节假日 API — 免费公共接口，署名并链接来源
- 提莫 API — 免费公共接口，数据由站长维护
- 聚合数据 — 商业 API，根据官方配额使用
