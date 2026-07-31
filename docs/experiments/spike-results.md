# M-1 Spike 结果汇总

- **任务：** M-1.6 MVP 技术决策的前置依据（汇总 M-1.4 / M-1.5 两个 Spike）
- **状态：** DONE（音高检测 Spike 完整；录音 Spike 编译级完成、运行验证 BLOCKED）
- **日期：** 2026-07-30

---

## 1. Spike 一览

| Spike | 代码位置 | 状态 | 详细结果 |
|---|---|---|---|
| M-1.5 音高检测（YIN / ACF / FFT） | `experiments/pitch-detection/` | **DONE**（合成信号实测） | `docs/experiments/pitch-detection-results.md` |
| M-1.4 录音（AudioRecord / MediaRecorder） | `experiments/audio-record/` | **PARTIAL**（编译通过；真机运行 BLOCKED） | `docs/experiments/audio-recording-spike-results.md` |

## 2. 关键实测数据（M-1.5，合成信号，帧 2048@44.1k）

| 信号 | YIN 中位误差 | ACF 中位误差 | FFT 中位误差 |
|---|---|---|---|
| 纯正弦 130-1046Hz | **< 0.03%** | 15-89%（子谐波锁定） | < 0.15% |
| 削波 440Hz | **0.007%** | 子谐波错误 | 0.05% |
| 静音 | **正确拒绝** | 正确拒绝 | 正确拒绝 |
| 白噪声 | **正确拒绝** | 误报（~99Hz） | 误报（~599Hz） |
| 说话近似 150Hz | 50% 误差 / 35% 帧未检出 | 23% 误差 | 27% 误差 |
| 性能 | ~1.04 ms/帧 | ~0.99 ms/帧 | ~0.09-0.18 ms/帧 |

**核心结论：** YIN 是唯一在全部干净信号上零错误的方法，且对削波免疫、正确拒绝静音/白噪声；ACF 存在教科书级子谐波锁定（220→73.4Hz）；FFT 主峰法对谐波/噪声不可靠。性能上三者在桌面 JVM 均满足实时（YIN ~1ms/帧 << 46ms 帧预算）。

## 3. 关键事实（M-1.4，代码级）

- AudioRecord 可直读 PCM 帧流（音高分析的必需输入）；MediaRecorder 只能输出编码文件，无法逐帧读 PCM。
- Android 14+（targetSdk 34+）前台录音必须声明 `FOREGROUND_SERVICE_MICROPHONE` 并以前台服务承载。
- `VOICE_RECOGNITION` 音频源适合人声分析（AGC/降噪倾向），`MIC` 更原始。
- 工程已编译通过（APK 5.6MB，AGP 8.7.3 / Gradle 8.9 / compileSdk 36 / minSdk 26）。
- 真机运行验证因开发机无设备/AVD 而 BLOCKED（详见该 Spike 文档 §3）。

## 4. 对 MVP 技术决策的影响

| 决策点 | Spike 依据 | 决策 |
|---|---|---|
| 音高检测方法 | YIN 全信号零错误 + 抗削波 + 拒噪 | **YIN（纯 Kotlin）** → ADR-003 |
| 采集 API | 仅 AudioRecord 可读 PCM | **AudioRecord** → ADR-002 |
| 推荐方向 | 音色识别不可靠（研究 + PLAN §2.1 禁止）；音域可解释 | **音域推荐** → ADR-001 |
| TFLite / CREPE | 端侧实时性与功耗无 Spike 实测支持 | **MVP 不引入**（待性能证明不足时再评估） |
| 后端 | 全部核心流程可端侧完成 | **MVP 无后端**（原始音频不上传） |
| 原始音频保存 | 隐私风险（研究 S15） | **不默认保存**，仅保留派生特征 |

---

## 5. 遗留风险（进入 M0 前需知晓）

1. **M-1.4 运行验证 BLOCKED**：真机实测（麦克风、前后台、中断、CPU/内存）未完成。不影响 M-1.5 结论，但 M3（录音系统）实现前必须补测。
2. **真实人声未测**：M-1.5 基于合成信号，真实颤音/滑音/噪声环境表现需设备矩阵验证（M5/M10）。
3. **舒适音区无统一客观定义**：需 M0 在 SPEC 中给出产品化定义（如"稳定演唱的 80% 分布区间"）。
4. **无 Gradle Wrapper**：M1 需 `gradle wrapper`（本机系统 Gradle 8.5 init.d 注入与 FAIL_ON_PROJECT_REPOS 冲突，Android 工程需用缓存 8.9 dist 或独立 wrapper）。
