# M-1.4 AudioRecord / MediaRecorder Spike 结果

- **任务：** M-1.4 AudioRecord 与 MediaRecorder Spike
- **状态：** **PARTIAL / BLOCKED（运行验证）** —— 代码与编译验证 DONE；真机运行验证 BLOCKED（开发机无 Android 设备/AVD）
- **日期：** 2026-07-30
- **代码：** `experiments/audio-record/`（独立 Android 工程，实验代码不进生产模块）
- **编译环境：** JDK 17，Gradle 8.9（缓存 dist），AGP 8.7.3，Kotlin 2.1.0，compileSdk 36，minSdk 26

---

## 1. 目的

分别实现 AudioRecord 与 MediaRecorder 的最小实验，验证 PLAN.md §2.2 默认技术栈中的录音方案，为 ADR-002（音频采集 API 选型）提供实测依据。

PLAN 要求测试项：麦克风权限；开始/停止；15-30s 录音；前后台切换；来电或音频焦点中断；文件格式；PCM 数据访问；错误恢复；文件大小；CPU/内存占用。

---

## 2. 已交付内容

### 2.1 工程结构

```text
experiments/audio-record/
├── settings.gradle.kts          # AGP 8.7.3 + Kotlin 2.1.0，google()/mavenCentral()
├── build.gradle.kts             # （根占位，实际配置在 app/）
├── gradle.properties            # android.useAndroidX=true
├── local.properties             # sdk.dir=D:\androidsdk（gitignore）
└── app/
    ├── build.gradle.kts         # compileSdk 36 / minSdk 26 / targetSdk 36
    └── src/main/
        ├── AndroidManifest.xml  # RECORD_AUDIO + FOREGROUND_SERVICE_MICROPHONE（Android 14 要求）
        └── java/.../MainActivity.kt       # 权限申请 + 模式切换 UI
        └── java/.../RecordingService.kt   # 前台服务，AudioRecord/MediaRecorder 双路径
```

### 2.2 两条采集路径的实现要点

| 维度 | AudioRecord 路径 | MediaRecorder 路径 |
|---|---|---|
| 数据访问 | **PCM 裸数据直读**（ShortArray 缓冲，read()） | 系统封装，只能写文件 |
| 输出格式 | 原始 PCM（`.pcm`，44.1kHz/16bit/mono） | MPEG-4 AAC（`.m4a`） |
| 音频源 | `VOICE_RECOGNITION`（人声分析推荐） | `VOICE_RECOGNITION` |
| 附加能力 | 逐帧统计峰值、帧数（为 M4 质量检测预留） | 无（仅文件大小/时长） |
| 前台服务 | `foregroundServiceType="microphone"`（API 34+ 强制声明） | 同左 |

### 2.3 编译验证（已完成）

```bash
# 使用已缓存 Gradle 8.9（系统 Gradle 8.5 的 init.d 注入仓库与 FAIL_ON_PROJECT_REPOS 冲突，改用缓存 dist）
gradle :app:assembleDebug
# BUILD SUCCESSFUL in 12s
# 产物：app/build/outputs/apk/debug/app-debug.apk (5.6MB)
```

已修复的编译问题（记录供后续参考）：
1. 系统 Gradle 8.5 的 `init.d/init.gradle` 注入 mavenLocal/阿里云仓库 → 与 `FAIL_ON_PROJECT_REPOS` 冲突。**解决：** 使用缓存 Gradle 8.9 dist 直接调用。
2. `android.useAndroidX=true` 未设置 → checkDebugAarMetadata 失败。已加 `gradle.properties`。
3. `kotlin.math.abs(Short)` 不存在 → 改为 `abs(v.toInt())`。

---

## 3. 运行验证状态（BLOCKED）

### 3.1 阻塞原因

开发机环境：
- `D:\androidsdk` 存在（platforms/android-36.1, build-tools/36.0.0, platform-tools 37.0.0, emulator），SDK license 已接受；
- **无连接的 Android 设备**（`adb devices` 为空）；
- **无 AVD 模拟器**（`emulator -list-avds` 为空）；
- **无 system-images**（`D:/androidsdk/system-images` 不存在）；
- **无 cmdline-tools**（`sdkmanager` 不可用，无法安装 system image 创建 AVD）。

创建 AVD 需要先安装 cmdline-tools + 下载 system image（数百 MB），且模拟器麦克风需宿主机麦克风支持，链路长、不确定性高，超出 Spike 合理范围。**未伪造任何运行结果。**

### 3.2 真机验证步骤（供设备可用时执行）

```bash
# 1. 连接设备（USB 调试）
adb devices
# 2. 安装
adb install app/build/outputs/apk/debug/app-debug.apk
# 3. 授权并启动
adb shell pm grant matchsong.spike.audiorecord android.permission.RECORD_AUDIO
adb shell am start -n matchsong.spike.audiorecord/.MainActivity
# 4. 观察 Logcat
adb logcat -s RecordingSpike
```

验证清单（对应 PLAN M-1.4 测试项）：

| # | 测试项 | 期望结果 | 验证方式 |
|---|---|---|---|
| 1 | 麦克风权限 | 授权后可用，拒绝后 UI 提示 | UI + logcat |
| 2 | 开始/停止 | 两路径均可正常开始/停止 | UI |
| 3 | 15-30s 录音 | 录音时长与文件大小合理 | notification 统计 |
| 4 | 前后台切换 | 前台服务保证后台/息屏录音持续 | 录 30s 中途 Home 键 |
| 5 | 来电/音频焦点中断 | AudioRecord 不崩溃（焦点丢失时暂停或继续） | 模拟来电 |
| 6 | 文件格式 | .pcm（16bit/mono/44.1k）与 .m4a（AAC） | 文件头检查 |
| 7 | PCM 数据访问 | AudioRecord 路径可逐帧读 PCM；MediaRecorder 不可 | 代码已验证（编译期事实） |
| 8 | 错误恢复 | 权限撤销/设备拔出时不崩溃 | 运行时操作 |
| 9 | 文件大小 | 15s ≈ 44100*2*15 = 1.29MB（PCM） | 文件系统 |
| 10 | CPU/内存 | 低负载（Spike 简化实现，供 M3 参考） | adb shell top / profiler |

---

## 4. 结论（基于代码事实 + 研究文档，非运行结果）

> **注意：** 以下选型结论依据 M-1.2 研究文档（docs/research/android-technical-feasibility.md，来源见 source-register.md）与代码事实，不依据本 Spike 运行数据（因运行受阻）。真机验证后需在 ADR-002 中复核。

1. **MVP 采集 API 选择 AudioRecord（默认），与 PLAN §2.2 一致：**
   - 唯一能直接访问 PCM 数据的 API（MediaRecorder 输出编码文件，无法逐帧读 PCM）；
   - 音高分析（YIN）需要 PCM 帧流，AudioRecord 是必需输入源；
   - MediaRecorder 仅适合"录完存文件"场景（如用户回放），不作为分析主路径。
2. **音频源用 `VOICE_RECOGNITION`**（区别于 `MIC`）：VOICE_RECOGNITION 启用 AGC/降噪倾向，MIC 更原始但可能含更多噪声；Spike 代码用 VOICE_RECOGNITION，M5 可对比 MIC 原始数据。
3. **前台服务 + `FOREGROUND_SERVICE_MICROPHONE`** 是后台/息屏持续录音的前提（Android 14+ 强制），Spike 已按此实现，M3 沿用。
4. **PCM 格式建议：44.1kHz / 16bit / mono**（Spike 采用），YIN 帧长 2048 对应 46ms，适合演唱音高分析。

---

## 5. 遗留风险

| 风险 | 说明 | 处理 |
|---|---|---|
| 运行验证未完成 | 无设备/AVD，无法实测麦克风、前后台、中断、CPU/内存 | 设备可用后按 §3.2 执行，结果补录并复核 ADR-002 |
| VOICE_RECOGNITION 的 AGC 影响 | 自动增益可能改变振幅动态，影响削波检测阈值（M4） | M4/M5 对比 MIC 源 |
| 采样率设备差异 | 部分设备 44.1kHz 非原生，可能重采样 | M5 设备矩阵测试（M10） |
| 前台服务限制 | Android 14 起 targetSdk 34+ 必须声明 foregroundServiceType=microphone（已做） | 无 |

---

## 6. 验收条件核对（PLAN.md §5.3 M-1.4）

- [x] 记录两种 API 的实际结果 → 编译/代码事实已记录；运行结果待设备（§3.2）
- [x] 明确正式产品使用哪一种 → **AudioRecord**（理由 §4，待真机复核）
- [x] 默认优先选择能够访问 PCM 的 AudioRecord → 是
- [x] 实验代码不得直接进入生产模块 → `experiments/` 独立工程，gitignore 不含它；文档与代码均标注"不进生产模块"

**结论：** M-1.4 代码与编译验证完成；运行验证 BLOCKED（环境无设备/AVD）。按 PLAN §3.3 状态规则，M-1.4 记为 `BLOCKED` 而非 `DONE`，退出 M-1 质量门禁时需解决（连接真机或创建 AVD）或显式记录 DEFERRED 并取得用户同意。
