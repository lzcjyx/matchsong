# M10.1 性能基准（基线）

- **日期：** 2026-08-01
- **设备：** spike_avd 模拟器（Android 16 / API 36，x86_64，google_apis，Pixel 5 配置，headless + WHPX 加速）
- **构建：** debug（未 minify）——Release（R8）预计更优
- **对照：** `SPEC.md` §11 非功能性能指标
- **说明：** 模拟器数据为开发期参考；**真机指标（低端/中端/Pixel）必须在 M10.3 设备矩阵完成**（模拟器虚拟麦克风/性能不代表真机）

## 1. 分析耗时（SPEC：30s 音频 ≤ 10s，中端设备）

| 测量点 | 结果 | 目标 | 判定 |
|---|---|---|---|
| 30s 录音端到端分析（预热后，设备端 ART） | **2795ms**（优化前 2866ms，M10.2 后） | ≤ 10000ms | ✅ 余量 3.6× |
| 同场景 JVM 桌面参考（AnalysisPerformanceTest） | ~1300ms（历史记录） | — | 参考 |

测量方式：`app/src/androidTest/.../perf/PerfBaselineTest.baseline30sAnalysisTimeAndMemory`（30s 220Hz 正弦 WAV → AnalyzeRecordingUseCase 全管线）。

## 2. 内存（SPEC：分析峰值 ≤ 200MB）

| 测量点 | 结果 | 目标 | 判定 |
|---|---|---|---|
| 分析期进程 PSS（Debug.getMemoryInfo） | **136MB**（native 15MB / dalvik 18MB） | ≤ 200MB | ✅ 余量 1.5× |

> 注意：模拟器 PSS 含测试进程开销；真机分析峰值以 Macrobenchmark 为准（M10.3）。

## 3. 冷启动（SPEC：冷启动到首页 ≤ 3s）

| 测量点 | 结果 | 目标 | 判定 |
|---|---|---|---|
| am start -W 冷启动（force-stop 后，3 次） | 1835 / 2078 / 2108ms（均值 ~2007ms） | ≤ 3000ms | ✅ |

> debug APK 未 minify + 模拟器；Release 冷启动预计更低。真机以 Macrobenchmark 为准。

## 4. APK/AAB 大小（SPEC §11：APK/AAB 大小基准）

| 构建 | 大小 |
|---|---|
| debug APK（未 minify） | 17.9MB |
| **release APK（R8 minify）** | **1.58MB** |

## 5. 录音期 CPU（待 M10.3 真机）

- 模拟器无法代表真实麦克风采集路径；录音期 CPU/电量（SPEC：单次完整流程 ≤1% 电量）列入真机矩阵（M10.3）与手工清单。

## 6. M10.2 优化记录（PLAN §16.2 顺序 1-2：减少分配 / 复用缓冲）

| 优化 | 位置 | 说明 |
|---|---|---|
| VolumeMeter 实例复用 | `RecordingSessionRunner.collectFrames` | 原每 chunk `VolumeMeter()` 新建 → 提升至流外复用 |
| PCM 批量编码写盘 | `RecordingSessionRunner.writePcmChunk` | 原逐样本 `writeByte`×2（44.1kHz ≈ 88k 次/秒）→ 每 chunk 一次 `write(byte[])`，缓冲按需增长复用 |
| ZCR 均值随主循环累加 | `QualityAnalyzer.analyze` | 去掉 `frames.map{}.average()` 二次分配 |

效果：分析耗时 2866→2795ms（同设备同条件）。录音路径为实时分配削减，CPU 收益待真机（M10.3）量化。

> PLAN §16.2 顺序 3-7（帧长/批处理/协程/UI 节流/NDK）评估：帧长 2048/hop 1024 已由 ADR-003 冻结并验证；UI 音量节流 ≤10Hz 已在 M3.6 落地；YIN 批量模式 M5.6 落地；分析耗时 2.8s ≪ 10s 预算 → **无需引入 C++/NDK（PLAN §2.2 门禁不触发）**。
