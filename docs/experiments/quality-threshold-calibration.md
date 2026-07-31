# M4.6-2 质量阈值标定记录

- **任务：** M4.6-2 质量检测测试套件与阈值标定
- **日期：** 2026-07-31
- **状态：** DONE（合成夹具标定；真机人声待 M5.8/M10 复审）
- **代码：** `core/audio/.../algorithm/QualityConfig.kt`、`QualityAnalyzer.kt`

## 1. 阈值清单（QualityConfig v1.0）

| 阈值 | 默认值 | 依据 | 标定结果 |
|---|---|---|---|
| Q-1 silenceRmsThreshold | 0.01 | M-1.5 Spike 实测（静音测试） | ✅ 合成静音夹具 silenceRatio≈1.0 → SILENT 命中 |
| Q-2 quietRmsThreshold | 0.02 | [推测]（须 > Q-1） | ✅ amplitude 0.005 正弦 quietRatio>0.8 → 拒绝 |
| Q-3 clippingFullScaleMagnitude | 0.999 | [推测]（PCM16 满幅 ≈0.99997） | ✅ 1.5 幅值硬削波触顶 → CLIPPING 命中 |
| Q-3 clippingConsecutiveFullScaleSamples | 3 | data-model §5.1 | ✅ 连续 ≥3 满幅样本判削波帧 |
| Q-3 clippingRatioLimit | 0.05 | [推测] | ✅ 削波夹具 clippingRatio>0.05 → CLIPPING |
| Q-4 minActiveVoiceDurationMs | 5000 | [推测]（SPEC §6 有效演唱） | ✅ 2s 有效/15s 录音 → INSUFFICIENT_VOICE |
| Q-5 minActiveFrameRatio | 0.30 | [推测] | 由 Q-4 时长条件主导（ratio 为兜底） |
| R-3 minDurationMs | 10000 | SPEC §6（≥10s） | ✅ 3s 录音 → TOO_SHORT |

## 2. 检测逻辑标定（M4.6-2 过程中修正）

### 噪声判定（NOISY）—— 从"RMS 中位数启发式"修正为"过零率"

- **初版**：`noiseEstimate / averageRms > 0.7`（低幅值帧中位数占比）。
  **问题**：白噪声无低幅值帧（quietRmsValues 空 → 中位数 0）；纯正弦稳态信号中位数≈均值 → 误判 NOISY。
- **终版**：**平均过零率（ZCR）> 0.3** 且 averageRms > 0.02 → NOISY。
  - 白噪声 ZCR ≈ 0.5（每样本约半概率变号）→ 命中 ✅
  - 纯正弦 440Hz ZCR = 880/44100 ≈ 0.02 → 不命中 ✅
  - 人声浊音段 ZCR 低（周期性强）→ 不误判 [推测，真机复审]
- **ZCR 在 FrameStats 中新增**（AudioFramePipeline.computeStats）。

### 门禁优先级（多原因并存）

`TOO_SHORT → SILENT → TOO_QUIET → NOISY → CLIPPING → INSUFFICIENT_VOICE`
（过短且静音 → 优先提示 TOO_SHORT；测试锁定顺序确定性）

## 3. 测试套件结果（55/55 通过）

| 场景 | 夹具 | 断言 |
|---|---|---|
| 正常可用 | 0.5 幅值正弦 15s | isUsable=true, ANALYZE, confidence≥0.5 |
| 纯静音 | SILENCE 15s | SILENT + RETRY（ACC-7） |
| 严重削波 | 1.5 幅值硬削 15s | CLIPPING（ACC-8） |
| 过短 | 正弦 3s | TOO_SHORT |
| 音量过低 | 0.005 幅值正弦 15s | 拒绝（quietRatio>0.8） |
| 白噪声 | NOISE 15s | 拒绝（NOISY，ZCR） |
| 有效片段不足 | 15s 截前 2s | INSUFFICIENT_VOICE |

## 4. 遗留

- **真机人声未参与标定**：合成夹具与真实演唱（颤音/滑音/气息/环境噪声）差异可能使 ZCR 阈值偏紧/偏松；M5.8 真机人声测试时复审并递增 qualityVersion。
- **模拟器低增益录音**（FIX-EMU-15S）：模拟器虚拟麦克风增益低，可能误判 TOO_QUIET/SILENT —— 属质量门禁预期行为（提示用户重录），真机复核。
- **AGC（VOICE_RECOGNITION）**：自动增益可能抑制削波触顶，CLIPPING 检出率降低 —— ADR-002 遗留项，M5 对比 MIC 源。
