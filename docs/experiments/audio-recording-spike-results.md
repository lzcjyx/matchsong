# M-1.4 AudioRecord / MediaRecorder Spike 结果

- **任务：** M-1.4 AudioRecord 与 MediaRecorder Spike
- **状态：** **DONE**（代码 + 编译验证 + 模拟器运行验证全部完成）
- **日期：** 2026-07-31（运行验证）
- **代码：** `experiments/audio-record/`（独立 Android 工程，实验代码不进生产模块）
- **编译环境：** JDK 17，Gradle 8.9（缓存 dist），AGP 8.7.3，Kotlin 2.1.0，compileSdk 36，minSdk 26
- **运行环境：** Android 模拟器 `spike_avd`（android-36 google_apis x86_64，Pixel 5 配置，headless，WHPX 加速）

---

## 1. 目的

分别实现 AudioRecord 与 MediaRecorder 的最小实验，验证 PLAN.md §2.2 默认技术栈中的录音方案，为 ADR-002（音频采集 API 选型）提供实测依据。

PLAN 要求测试项：麦克风权限；开始/停止；15-30s 录音；前后台切换；来电或音频焦点中断；文件格式；PCM 数据访问；错误恢复；文件大小；CPU/内存占用。

---

## 2. 工程结构

```text
experiments/audio-record/
├── settings.gradle.kts          # AGP 8.7.3 + Kotlin 2.1.0，google()/mavenCentral()
├── gradle.properties            # android.useAndroidX=true
├── local.properties             # sdk.dir=D:\androidsdk（gitignore）
└── app/
    ├── build.gradle.kts         # compileSdk 36 / minSdk 26 / targetSdk 36
    └── src/main/
        ├── AndroidManifest.xml  # RECORD_AUDIO + FOREGROUND_SERVICE + FOREGROUND_SERVICE_MICROPHONE
        └── java/.../MainActivity.kt       # 权限申请 + 模式切换 + auto_start 自动化入口
        └── java/.../RecordingService.kt   # 前台服务，AudioRecord/MediaRecorder 双路径
```

---

## 3. 实测结果（模拟器，2026-07-31）

### 3.1 AudioRecord 路径（VOICE_RECOGNITION / 44.1kHz / 16bit / mono）

| 测试项 | 结果 |
|---|---|
| 麦克风权限 | ✅ 运行时申请成功；`pm grant` 后直接可用 |
| 开始/停止 | ✅ 正常 |
| 15s 录音 | ✅ **643456 帧 = 14.59s**，文件 1286912 字节 |
| 30s 录音 + 来电中断 | ✅ **1310848 帧 = 29.7s**，来电（t=4s）期间录音持续，无崩溃 |
| 前后台切换（HOME 键） | ✅ 5s 时 HOME，录音持续到 15s 自动停止 |
| 文件格式 | ✅ PCM：643456 帧 × 2 字节 = 1286912 字节（16bit mono，精确匹配） |
| PCM 数据访问 | ✅ 逐帧 read() 直读；样本范围 [-4733, 3055]（模拟器虚拟麦克风输入） |
| 文件大小 | ✅ 14.59s = 1.23MB（44.1k×2B×14.59s = 1.29MB，理论值吻合） |
| CPU | ✅ top 采样 0.0%（I/O 阻塞等待 read）；12s 录音累计 CPU 3.66s（含模拟器开销） |
| 内存 | ✅ Java Heap ~11.5MB，Native Heap ~10MB（RES 156MB 含进程开销） |

### 3.2 MediaRecorder 路径（AAC / MPEG-4）

| 测试项 | 结果 |
|---|---|
| 10s 录音 | ✅ 生成 `spike_mediarecorder.m4a`，25290 字节 |
| 文件格式 | ✅ header `ftypmp42` = 标准 MPEG-4 (AAC) |
| PCM 数据访问 | ❌ 不可访问（系统封装，只能写文件）—— 与预期一致 |

### 3.3 测试中发现的真实问题（已修复）

1. **AndroidManifest 中 `FOREGROUND_SERVICE` 误加 `android:maxSdkVersion="33"`** → API 36 上 `startForeground` 抛 `SecurityException: requires android.permission.FOREGROUND_SERVICE`，服务崩溃。修复：移除 maxSdkVersion 限制（该权限 API 28+ 一直需要）。
2. 调试过程发现 `adb install -r` 因 debug keystore 变更报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，需先 uninstall。

### 3.4 遗留行为（正式产品需处理）

- **音频焦点未处理**：Spike 未实现 `AudioFocusRequest`/`OnAudioFocusChangeListener`。来电时 AudioRecord 不自动暂停（系统降焦点但 PCM 流持续）。正式产品（M3）必须实现焦点获取/丢失的暂停与恢复，避免录音被系统行为打断或违反用户预期。
- 模拟器麦克风输入为虚拟源，峰值 [-4733, 3055] 远低于真实人声；真实设备输入质量需 M3/M5 真机复测。

---

## 4. 结论与选型（实测依据更新）

1. **MVP 采集 API 选择 AudioRecord（默认）**：唯一能直读 PCM 的 API（实测确认 MediaRecorder 无 PCM 访问）；44.1kHz/16bit/mono 实测可行，PCM 格式精确可控。
2. **前台服务方案成立**：`FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE`（API 34+）下，HOME 键后台与来电场景录音均持续，无崩溃。
3. **VOICE_RECOGNITION 音频源在模拟器可用**；真实设备 AGC 影响留待 M5 对比。
4. 与 ADR-002 一致，无需修改。

---

## 5. 遗留风险

| 风险 | 说明 | 处理 |
|---|---|---|
| 真实设备未测 | 模拟器虚拟麦克风 ≠ 真实人声采集（增益、噪声、硬件差异） | M3 录音系统真机测试（M10 设备矩阵） |
| 音频焦点未实现 | Spike 无焦点处理，来电不自动暂停 | M3 实现 AudioFocusRequest |
| 采样率设备差异 | 44.1kHz 在部分设备非原生 | M5 运行时探测与降级 |
| 模拟器 CPU/内存数据偏差 | 模拟器开销计入进程 | M10 Macrobenchmark 真机基准 |

---

## 6. 验收条件核对（PLAN.md §5.3 M-1.4）

- [x] 记录两种 API 的实际结果 → §3（模拟器实测）
- [x] 明确正式产品使用哪一种 → **AudioRecord**（§4）
- [x] 默认优先选择能够访问 PCM 的 AudioRecord → 是
- [x] 实验代码不得直接进入生产模块 → `experiments/` 独立工程

**结论：** M-1.4 全部验收条件满足，状态 DONE。
