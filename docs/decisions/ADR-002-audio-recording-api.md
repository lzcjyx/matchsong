# ADR-002: 音频采集 API —— AudioRecord

- **状态：** Accepted（真机验证后复核）
- **日期：** 2026-07-30
- **决策人：** Coding Agent（M-1.6）
- **关联文档：** `docs/experiments/audio-recording-spike-results.md`、`docs/research/android-technical-feasibility.md`

## 背景

M-1.4 Spike 对比 Android 两种录音 API：
- **AudioRecord**：直读 PCM 帧流，可逐帧分析；
- **MediaRecorder**：系统封装，输出编码文件（AAC/MP3 等），无法直接访问 PCM。

产品需要将 15-30s 演唱转化为音高轨迹（YIN 输入为 PCM 帧）。

## 决策

**使用 AudioRecord 作为 MVP 唯一采集 API（默认），配置：**

- 采样率 **44.1kHz**（fallback 逻辑：运行时探测设备支持，必要时 48kHz 或 16kHz 降级）；
- 声道 **MONO**（人声单声源，节省处理量）；
- 编码 **PCM 16bit**；
- 音频源 **VOICE_RECOGNITION**（人声分析；M5 可对比 `MIC` 的原始动态）；
- 录音承载于**前台服务**，`foregroundServiceType="microphone"`（targetSdk 34+ 强制）；
- 采集线程独立于 UI 线程，数据经回调/队列交给分析管线。

## 理由

1. **PCM 直读是音高分析的硬前提**：YIN 逐帧处理 PCM 帧流，MediaRecorder 无法满足；
2. **数据可控**：可同时计算 RMS/削波/静音质量指标（M4），MediaRecorder 无此能力；
3. 与 PLAN §2.2 默认栈一致；
4. M-1.4 代码已按此实现并编译通过。

## 后果

- **积极**：分析管线（分帧 → 音高 → 音域）可直接消费采集数据，无编解码开销；可精确控制录音格式。
- **消极**：需自行处理缓冲区大小（`getMinBufferSize`）、错误恢复、采样率探测；相比 MediaRecorder 无内置编码（如需用户回放保存，M8 可用 MediaRecorder 或 MediaCodec 单独编码）。
- **成本**：M3（录音系统）需实现权限流、前台服务、中断处理、错误恢复。

## 备选方案与拒绝理由

- **MediaRecorder**：拒绝作为分析主路径（无 PCM 访问）；保留为"保存回放文件"的辅助路径（M8 视需要引入）。
- **Oboe/NDK**：PLAN §2.2 在性能证明不足前不引入，MVP 不采用。

## 待复核条件

- 模拟器（android-36 x86_64）已验证：权限、开始/停止、15-30s 录音、前后台切换（HOME）、来电中断、PCM 格式与文件大小、CPU/内存。
- 真实设备人声采集（增益/噪声/硬件差异）待 M3 录音系统真机测试时复核。
- 若 VOICE_RECOGNITION 的 AGC 显著影响削波检测，改回 `MIC` 或提供切换。
