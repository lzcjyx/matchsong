# 测试夹具清单（Audio Test Fixture Manifest）

- **版本：** 0.1.0
- **日期：** 2026-07-31
- **里程碑：** M0.4（M4.6 验收：测试夹具拥有来源和预期；M5.8 人工样本验证）
- **配套：** TESTING.md §5 音频测试策略

## 1. 目的与约定

- 本文档是全部音频测试夹具的唯一登记处；新增夹具必须在此登记后才能被测试引用。
- 每条夹具必须包含**来源**与**预期**（PLAN M4.6 验收条件）：
  - **来源** = 生成方式（合成脚本/录制设备/程序化假流）与可复现命令；
  - **预期** = 输入该夹具后质量检测与分析模块应输出的结果（含容差）。
- 格式约定（与 FR-REC-1 一致）：**44.1kHz / 16bit / mono PCM WAV**（合成与录制夹具）；假流为内存 PCM 帧流。
- 断言容差：纯音频率相对误差 ≤ 1% 或 MIDI 音分偏差 < 50 音分[推测，见 TESTING.md §5.3]；拒绝路径断言 `isUsable=false` 且原因精确匹配 SPEC §6。
- 状态列：`已生成`（文件已存在或可一键再生成）/ `待录制`（需真机或模拟器录制）/ `待生成`（脚本待实现）。

## 2. 夹具清单

### 2.1 合成 WAV（复用 experiments/pitch-detection 已验证信号）

信号生成代码：`experiments/pitch-detection/src/main/kotlin/matchsong/pitch/Signals.kt`（Spike 工程，JDK 17 + Kotlin 2.1.0，`gradle test` 内置 11 个测试可复现全部信号）。分析输入帧 2048、hop 1024。

| 夹具 ID | 类型 | 描述 | 时长 | 预期输出（质量 → 分析） | 来源 | 格式 | 用途 | 状态 |
|---|---|---|---|---|---|---|---|---|
| FIX-SINE-130 | WAV 合成 | 130Hz 纯正弦（男低音区，≈C3） | 生成器参数决定（Spike 未记录，约数秒）[推测] | isUsable=true；稳定音域 ≈ C3±0.5 半音；置信度 High | experiments/pitch-detection `sine_130_C3_maleLow` | 44.1k/16bit/mono | M4 质量合格路径；M5 YIN 精度下限（65-1046Hz 内偏低端） | 已生成 |
| FIX-SINE-220 | WAV 合成 | 220Hz 纯正弦（A3，男声中低区） | 同上 | isUsable=true；稳定音域 ≈ A3；置信度 High | `sine_220_A3` | 同上 | M5 YIN 精度 | 已生成 |
| FIX-SINE-440 | WAV 合成 | 440Hz 纯正弦（A4，标准音） | 同上 | isUsable=true；稳定音域 ≈ A4；置信度 High | `sine_440_A4` | 同上 | M5 YIN 精度基线（Spike 实测 440.02Hz，误差 0.004%） | 已生成 |
| FIX-SINE-880 | WAV 合成 | 880Hz 纯正弦（A5，女声中高区） | 同上 | isUsable=true；稳定音域 ≈ A5；置信度 High | `sine_880_A5` | 同上 | M5 YIN 精度 | 已生成 |
| FIX-SINE-1046 | WAV 合成 | 1046Hz 纯正弦（C6，女高音区，工作范围上限） | 同上 | isUsable=true；稳定音域 ≈ C6；置信度 High | `sine_1046_C6_femaleHigh` | 同上 | M5 边界频率上限（1046Hz） | 已生成 |
| FIX-SCALE-C3-E3-G3-C4 | WAV 合成 | 音阶 C3→E3→G3→C4（每音 0.5s） | 2s | isUsable=true；分段音高 ≈ 130.81/164.81/196.00/261.63Hz；稳定音域估计 ≈ C3-C4（P5/P95，非裸极值）[推测：以后处理实现为准]；置信度 High | `scale_C3_E3_G3_C4` | 同上 | M5 音阶分段、音域估计、舒适音区 | 已生成 |
| FIX-SILENCE | WAV 合成 | 静音（幅值 1e-5） | 生成器参数决定 | isUsable=false，原因=静音（ACC-7：提示"没有检测到声音"）；不进入分析 | `silence` | 同上 | M4 静音拒绝（ACC-7）；YIN 无效帧拒绝 | 已生成 |
| FIX-NOISE-WHITE | WAV 合成 | 白噪声 | 生成器参数决定 | isUsable=false，原因=嘈杂[推测：质量层按近似噪声水平/有效声音比例判定；YIN 层已证实正确拒绝（NaN）]；不进入分析 | `whiteNoise` | 同上 | M4 嘈杂拒绝；YIN 噪声拒绝（Spike 证明 ACF/FFT 会误报，回归防护） | 已生成 |
| FIX-CLIPPED-440 | WAV 合成 | 440Hz 硬削波（限幅 ±0.3） | 生成器参数决定 | 质量：削波比例超阈值 → isUsable=false，原因=削波（ACC-8）；分析层参考：YIN 实测 440.03Hz（削波免疫验证） | `clipped_440` | 同上 | M4 削波检测（ACC-8）；M5 YIN 削波免疫 | 已生成 |
| FIX-TALK-150 | WAV 合成 | 150Hz 基频 + 谐波 + 3Hz AM + 噪声（类说话声近似） | 生成器参数决定 | 质量：通过或低置信[推测]；分析：YIN 实测 75Hz（50% 误差，35% 帧未检出）→ 预期无效帧比例高、有效帧不足 → 不输出正式音域（ACC-9）或输出 Low 置信警告 | `talkLike_150` | 同上 | M5 无效帧过滤、数据不足路径（ACC-9）、连续性过滤 | 已生成 |

### 2.2 模拟器录音（仪器层真实采集链路）

| 夹具 ID | 类型 | 描述 | 时长 | 预期输出 | 来源 | 格式 | 用途 | 状态 |
|---|---|---|---|---|---|---|---|---|
| FIX-EMU-15S | WAV 录制 | spike_avd 虚拟麦克风 15s 录音（audio-record Spike 产物，样本范围 [-4733, 3055]，极低音量近静音） | 14.59s（643456 帧，1286912 字节） | 质量失败：音量过小/静音[推测：模拟器虚拟源峰值远低于真实人声]；用于验证"采集→写盘→读取"真实链路而非算法结果 | experiments/audio-record 于 spike_avd 录制 | 44.1k/16bit/mono PCM（spike 未写 WAV header，需加 header 归档） | M3 仪器测试（AudioRecord 链路）；M4 低音量边界 | 已生成（实验产物，需归档进 core:testing） |

### 2.3 计划记录夹具（真实人声，待录制）

录制方式见 §3.2。录制设备优先：真机（矩阵设备）；无真机时可先用 spike_avd 录制占位，但**真实人声验证（M5.8 男声/女声）必须真机完成**（SPEC：模拟器虚拟麦克风 ≠ 真实人声采集）。

| 夹具 ID | 类型 | 描述 | 时长 | 预期输出 | 来源 | 格式 | 用途 | 状态 |
|---|---|---|---|---|---|---|---|---|
| FIX-REC-QUIET-SPEECH | WAV 录制 | 安静说话（贴近麦克风但音量低） | 15s | isUsable=false，原因=音量过小（FR-QUAL-3，建议靠近麦克风） | 待录制（真机或 spike_avd） | 44.1k/16bit/mono | M4 低音量拒绝 | 待录制 |
| FIX-REC-LOUD-SPEECH | WAV 录制 | 近麦克风大声喊唱 | 15s | isUsable=false，原因=削波（ACC-8，建议降低音量） | 待录制 | 同上 | M4 削波拒绝（真实削波） | 待录制 |
| FIX-REC-PARTIAL-SILENCE | WAV 录制 | 前 10s 演唱 + 后 20s 静音 | 30s | isUsable=false 或有效片段不足（有效声音比例低于阈值，FR-QUAL-3） | 待录制 | 同上 | M4 有效片段不足；部分静音 | 待录制 |
| FIX-REC-TOO-SHORT | WAV 录制 | 演唱仅 5s | 5s | isUsable=false，原因=过短（SPEC §6："请至少演唱 10 秒"；最小有效声音时长阈值约 10s[推测]） | 待录制 | 同上 | M4 过短拒绝（FR-QUAL-3） | 待录制 |
| FIX-REC-MALE-VOICE | WAV 录制 | 男声演唱（中低音区，含伴奏） | 27.3s | isUsable=true；真值平均 F0 184.7Hz（MIR-1K 标签）；YIN 可分析但存在子谐波锁定（已知限制，M10 优化）；用于 M10 性能基准 | 已落地（MIR-1K example3，44.1k/16bit/mono，含真值标签 .ref.txt） | 同上 | M5 男声验证（M5.8）；M10 性能基准 | 已落地 |
| FIX-REC-FEMALE-VOICE | WAV 录制 | 女声演唱（中高音区，含伴奏） | 24.5s | isUsable=true；真值平均 F0 284.7Hz（MIR-1K 标签）；YIN 可分析但存在子谐波锁定（已知限制） | 已落地（MIR-1K example1，44.1k/16bit/mono，含真值标签 .ref.txt） | 同上 | M5 女声验证（M5.8）；M10 性能基准 | 已落地 |

### 2.4 假流（Fake Frame Source / Fake Audio Stream，FR-QUAL-4 / M3.7）

| 夹具 ID | 类型 | 描述 | 时长 | 预期输出 | 来源 | 格式 | 用途 | 状态 |
|---|---|---|---|---|---|---|---|---|
| FIX-STREAM-SINE-440 | Fake Stream | 程序化生成 440Hz PCM 帧流（正弦） | 持续可配（默认 15s） | 质量通过；YIN 输出 ≈440Hz | core:testing 的 FakeAudioStream 实现（程序生成） | 内存 PCM 帧（44.1k/16bit/mono 帧流） | FR-QUAL-4 假流输入；M3.7 录音状态机正常路径；M8 E2E 录音段 | 待生成 |
| FIX-STREAM-SILENCE | Fake Stream | 程序化生成静音帧流 | 持续可配（默认 15s） | isUsable=false，原因=静音 | 同上 | 同上 | FR-QUAL-4；M8 E2E 质量失败路径 | 待生成 |
| FIX-STREAM-NOISE | Fake Stream | 程序化生成噪声帧流 | 持续可配（默认 15s） | isUsable=false，原因=嘈杂 | 同上 | 同上 | FR-QUAL-4；M8 E2E 嘈杂拒绝 | 待生成 |

## 3. 夹具生成流程

### 3.1 合成 WAV

1. 复用 `experiments/pitch-detection`（Signals.kt 已能生成全部合成信号，`gradle test` / `gradle run` 可复现）；
2. 扩展该工程（或新建独立小工具 `scripts/generate-fixtures/main.kt`）：为每个信号输出 WAV 文件，含标准 RIFF/WAVE header：
   - `RIFF` + 文件大小 + `WAVE`；`fmt ` chunk（PCM=1，声道=1，采样率=44100，字节率=88200，块对齐=2，位深=16）；`data` chunk + 16bit little-endian PCM 样本；
3. 输出目录：`core:testing/src/test/resources/audio-fixtures/`（单元测试直接读取，零设备依赖）；
4. 每个夹具附带元数据文件（来源脚本/参数/生成时间），并跑一次自动校验（见 §4）。

### 3.2 录制 WAV（真实人声 / 模拟器）

1. 真机或 spike_avd 上使用应用 Debug 录音路径（或 experiments/audio-record）录制；
2. `adb pull` 拉取 WAV（应用按 FR-REC-7 直接生成含 header 的 WAV）；
3. 校验：header 字段（44.1k/16bit/mono）、时长、峰值（FIX-EMU-15S 已知峰值 ±5000 量级，真实人声应明显更高）；
4. 归档至 `core:testing/src/test/resources/audio-fixtures/`（仪器测试另可放 `app/src/androidTest/assets/audio-fixtures/`），登记来源（设备、时间、演唱内容）与预期；
5. 每条记录夹具必须标注录制设备与真实人声验证结论（M5.8 人工样本验证）。

### 3.3 Fake Stream

- 在 `core:testing` 实现 `FakeAudioStream`（实现与生产 AudioFrameSource 相同的接口），支持参数化信号（正弦频率/幅值/时长、静音、噪声、削波、说话近似）；
- 供 FR-QUAL-4（质量检测假流输入）与 M3.7（录音状态机 Fake Audio Stream）与 M8.7（E2E）复用。

## 4. 夹具自动校验（每条夹具的"来源和预期"落地）

- 每个夹具对应的测试必须：读取 WAV → 运行质量检测与分析 → 与 §2 预期断言；
- 容差：纯音频率相对误差 ≤ 1% 或 < 50 音分；拒绝路径断言 `isUsable=false` 与原因精确匹配；
- 校验脚本（CI 中 `testDebugUnitTest` 内）：扫面 `audio-fixtures/` 目录，确保清单（本文件 §2）与实际文件一致——清单登记但文件缺失、或文件存在但未登记，均为失败。

## 5. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| 0.1.0 | 2026-07-31 | 初稿（复用 Spike 信号 + 计划录制夹具） |
