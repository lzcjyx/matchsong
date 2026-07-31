# M4 里程碑验收记录

- **里程碑：** M4 音频质量检测
- **验收日期：** 2026-07-31
- **验收人：** Coding Agent
- **总体状态：** **DONE** —— 10/10 任务完成，退出条件全部满足（真机人声标定待 M5.8）

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M4.1-1 帧分割、窗函数与帧统计 | **DONE** | AudioFramePipeline（2048/1024@44.1k，ADR-003）+ Frame/FrameStats（RMS/峰值/满幅游程/**ZCR**） |
| M4.1-2 多输入源适配 | **DONE** | AudioFrameSource 接口 + WavFileSource（主输入）；Fake 源可注入 |
| M4.2-1 质量阈值集中配置 | **DONE** | QualityConfig（Q-1~Q-5 + R-3，含校验与 DEFAULTS）；**合并 M3.6 的 QualityThresholds**（记录偏差） |
| M4.2-2 静音/低音量/有效声音判定 | **DONE** | silenceRatio/quietRatio/activeRatio + 有效时长/比例判定（注入 QualityConfig） |
| M4.3-1 削波检测 | **DONE** | 连续满幅≥3 判削波帧（Q-3）+ clippingRatio + 短时峰值容忍 |
| M4.4-1 质量报告聚合与门禁 | **DONE** | QualityAnalyzer（AudioQualityReport 全字段 + 六警告优先级门禁 + confidence）+ AudioQualityReport 模型 |
| M4.5-1 质量失败 UX | **DONE** | QualityResultScreen 双态（可用→报告+分析入口；失败→QualityWarningState+重录）+ QualityWarning→UI 原因映射 |
| M4.6-1 音频夹具库 | **DONE** | **10 个合成夹具**（正弦 130-1046/静音/白噪声/削波/音阶/说话近似）+ 元数据 JSON + MANIFEST + 清单校验测试 |
| M4.6-2 测试套件与阈值标定 | **DONE** | QualityAnalyzerTest 8 场景 + 55 测试全过；docs/experiments/quality-threshold-calibration.md |

## 2. 退出条件核对（PLAN §10.4）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| 无效录音可以被稳定拒绝 | ✅ | 静音→SILENT、削波→CLIPPING、过短→TOO_SHORT、噪声→NOISY、片段不足→INSUFFICIENT_VOICE（测试锁定） |
| 质量阈值可配置 | ✅ | QualityConfig 集中注入（VolumeMeter 与 QualityAnalyzer 共用） |
| 每种拒绝状态有明确提示 | ✅ | 六类警告 → QualityWarningState 文案 + 重录（ACC-7/8 映射测试） |
| 测试夹具拥有来源和预期 | ✅ | 10 夹具 + 元数据（来源/参数/预期）+ 清单校验测试强制一致 |
| 不合格音频不会进入正式分析 | ✅ | isUsable=false → recommendedAction=RETRY，UI 无分析入口（M5 消费端强制执行） |

## 3. 构建与测试状态

- 构建：assembleDebug + assembleRelease 均 BUILD SUCCESSFUL
- 单元测试：testDebugUnitTest 全过（core:audio 55 测试含 8 质量场景；全模块 150+）
- 仪器测试：connectedDebugAndroidTest **13/13**（M2/M3 套件无回归）
- 覆盖率门禁：core:common/core:testing ≥80% 通过
- 静态检查：checkQuality 全绿（lint + detekt + ktlint；2 处规则豁免已注释理由）

## 4. 标定过程中发现并修正的真实问题

1. **噪声判定初版失效**（RMS 中位数启发式）：白噪声无低幅值帧 → 中位数 0 → 不判 NOISY；纯正弦稳态 → 误判 NOISY。**修正为平均过零率（ZCR）> 0.3**：白噪声 ZCR≈0.5 命中、正弦 ZCR≈0.02 不命中（FrameStats 新增 ZCR 字段）。
2. **测试夹具字节序错误**：DataOutputStream.writeShort 写 big-endian，WAV PCM 需 little-endian → 样本损坏致判定随机。修正为手写低字节在前。
3. **削波夹具信号错误**：限幅到 ±0.3 不触顶（无满幅样本）→ 不触发 CLIPPING。修正为 1.5 幅值硬削 ±1.0（触顶）。

## 5. 遗留风险

| # | 风险 | 归属 |
|---|---|---|
| R-1 | 真机人声未参与阈值标定（合成夹具 vs 真实演唱差异；ZCR 阈值可能偏紧/偏松） | M5.8 真机复审 + qualityVersion 递增 |
| R-2 | 模拟器低增益录音可能误判 TOO_QUIET/SILENT（质量门禁预期行为） | 真机复核 |
| R-3 | VOICE_RECOGNITION 的 AGC 可能抑制削波触顶（CLIPPING 检出率降低） | M5 对比 MIC 源 |
| R-4 | QualityThresholds typealias 兼容层（M4.2-1 合并后遗留） | M4 后清理 |
| R-5 | 实时 PCM 源（RealtimePcmSource）未实现（MVP 录制后分析，接口已留） | M8 视需要 |

## 6. 验收结论

**M4 里程碑全部任务完成，验收通过。** 满足 PLAN §3.4 质量门禁：
- 无效录音稳定拒绝（六类警告测试锁定）✅
- 阈值集中可配置（QualityConfig）✅
- 拒绝提示明确（ACC-7/8）✅
- 夹具带来源与预期（10 合成 + 清单校验）✅
- 不合格不入分析（门禁在 domain 层强制）✅
- 静态检查 + 覆盖率 + 仪器测试无回归 ✅

**建议下一步：** 进入 **M5（音高与音域分析）** —— 按 task-breakdown.md M5.1-M5.8 将 Spike 验证的 YIN 重构为生产实现（复用 M4.1 帧管线）、音高后处理、稳定音域/舒适音区/稳定性估计、VoiceAnalysisResult 模型与结果页面、真机人声测试（含 M4 阈值复审）。首个任务 M5.1-1：YinPitchDetector 生产化重构。
