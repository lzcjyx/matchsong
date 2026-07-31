# Android 技术可行性研究（M-1.2）

> 本文档为 matchsong 项目 M-1.2 的一部分，聚焦 Android 录音与音频处理的技术可行性：AudioRecord vs MediaRecorder、Foreground Service、后台录音限制、采样率与设备差异、移动端噪声处理。
>
> **标记约定**：研究事实以 `[S-id]` 标注（对应 `source-register.md`）；工程推测以 `[推测]` 标注。不包含虚构测试结果。优先引用官方 Android 文档（developer.android.com）。

---

## 1. AudioRecord vs MediaRecorder

### 1.1 AudioRecord

研究事实：
- `AudioRecord` 允许 Java/Kotlin 应用从音频输入硬件读取原始 PCM 数据，适合需要直接访问音频采样的场景 [S16]。
- 支持的编码包括 `ENCODING_PCM_8BIT`、`ENCODING_PCM_16BIT`、`ENCODING_PCM_FLOAT`；线性 PCM 一帧由声道数 × 每声道样本数构成 [S17]。

工程推测 [推测]：
- 音高检测必须访问逐样本 PCM，因此 `AudioRecord` 是 MVP 的默认选择（与 PLAN §2.2 一致）。
- 16 kHz/16-bit/单声道足以覆盖人声基频（最高约 1.5 kHz，奈奎斯特 8 kHz 余量充足），且降低数据量与计算量；如需更高频段分辨率可升到 44.1/48 kHz。

### 1.2 MediaRecorder

研究事实：
- `MediaRecorder` 面向“录制并保存为文件/编码流”的高层 API，输出压缩格式（如 AAC/AMR/3GP），不直接暴露逐帧 PCM [S18]。

工程推测 [推测]：
- `MediaRecorder` 不适合音高检测主流程（无法方便地拿到 PCM）；但其可用于“用户回放录音”等仅需压缩音频的场景。
- PLAN §2.2 默认采用 `AudioRecord`；M-1.4 Spike 将对两者做最小实验对比。

---

## 2. Foreground Service 与后台录音限制

### 2.1 前台服务类型（Android 14+）

研究事实：
- 自 Android 14（API 34）起，targetSdk ≥ 34 的应用必须为每个前台服务声明服务类型，并在 manifest 中声明对应的前台服务权限（除 `FOREGROUND_SERVICE` 外）[S19]。
- “麦克风”前台服务类型对应：manifest `android:foregroundServiceType="microphone"`、权限 `FOREGROUND_SERVICE_MICROPHONE`、`startForeground()` 传 `FOREGROUND_SERVICE_TYPE_MICROPHONE`，运行时前置需获得 `RECORD_AUDIO` 运行时权限 [S20]。
- `RECORD_AUDIO` 受“使用中（while-in-use）”限制：不能在应用处于后台时创建 `microphone` 前台服务，且一般不能从 `BOOT_COMPLETED` 接收器启动（少数例外除外）[S20]。
- `microphone` 类型的描述为“从后台继续麦克风采集，如语音录音器或通信应用” [S20]。

工程推测 [推测]：
- matchsong 的录音前台服务应声明 `microphone` 类型与 `FOREGROUND_SERVICE_MICROPHONE` 权限，并确保在应用前台时启动；录音通知（`startForeground` 的 Notification）应明确告知用户正在录音。
- 若仅“前台录音 + 切后台短时继续”，`microphone` 类型即可；若需要长时后台录音，受 while-in-use 限制，不可行，且与产品定位（15–30 秒短录音）不符。

### 2.2 后台录音限制

研究事实：
- 自 Android 11 起，后台应用对麦克风访问受限；前台服务是合法的“后台继续录音”途径，但需正确声明类型 [S20][S21]。
- `microphone` 类型不能在后台冷启动（while-in-use 限制），需在前台或合规时机启动 [S20]。

工程推测 [推测]：
- MVP 录音应在用户主动操作（前台）时启动前台服务；切到后台后可短暂继续，但不应设计为“纯后台常驻录音”。
- 来电/音频焦点中断需在 M-3 处理：监听 `AudioManager` 音频焦点变化与电话状态，暂停/恢复录音。

---

## 3. 采样率与设备差异

研究事实：
- NDK 音频指南建议采样率匹配设备，典型为 44.1 kHz 或 48 kHz；高于 48 kHz 的采样率在多数设备上不被可靠支持 [S22]。
- 历史上 44100 Hz 是唯一保证在所有设备上可用的采样率，但 16000/22050/11025 等在部分设备可用 [S23（Android 源码注释，性质见 register）]。
- 现代 Android 设备原生采样率多为 48 kHz，44.1 kHz 内容常被重采样到 48 kHz [S24]。
- `AudioManager.getProperty(PROPERTY_OUTPUT_SAMPLE_RATE)` 可查询设备原生/最佳输出采样率 [S25]。

工程推测 [推测]：
- 为兼顾音高检测精度与设备兼容性，建议录音使用 16000 Hz 或 44100 Hz；16000 Hz 在语音/演唱基频场景下足够且更省电省内存，44100 Hz 兼容性最稳但数据量更大。
- 采样率应在运行时通过 `AudioRecord.getMinBufferSize` 与构造结果验证可用性，失败则降级（如 44100 → 16000）。
- 不同设备的麦克风增益、频响与可用音源差异显著，必须在 M-10 设备矩阵中验证；本文不虚构具体设备数据。

---

## 4. 移动端噪声处理

### 4.1 音源选择

研究事实：
- `MediaRecorder.AudioSource.VOICE_RECOGNITION` 为“语音识别调校的麦克风音源”，在不可用时表现为 `DEFAULT` [S26]。
- Android 音频源配置中 `VOICE_RECOGNITION` 倾向于输出较原始（raw）、未重度处理的音频 [S27]。
- 官方明确：语音识别用途下不应默认开启降噪预处理 [S28]。

### 4.2 可选预处理效果

研究事实：
- Android 提供 `NoiseSuppressor`、`AcousticEchoCanceler`、`AutomaticGainControl` 等可选效果，可用性因设备而异（通过 `isAvailable()` 查询）[S29]。

工程推测 [推测]：
- 为音高检测保真，建议使用 `VOICE_RECOGNITION` 音源，并谨慎启用预处理：
  - AGC 改变幅度但对 F0 影响小，可接受或关闭；
  - 降噪/回声消除可能引入伪音或削除弱信号，建议默认关闭，仅在低信噪比场景按需开启。
- 真正的“噪声处理”应在分析侧完成：用 RMS/能量门限剔除静音与低能量帧，用 F0 置信度（YIN 阈值/CREPE 概率）剔除不可信帧 [推测]。
- 设备间预处理可用性差异大，不可假设任何效果在所有设备存在；应以 `isAvailable()` 守卫并记录到 M-10 设备矩阵。

---

## 5. 端侧可完成 vs 暂不可靠（小结）

**可在端侧完成（有依据）**：
- 通过 `AudioRecord` 获取 PCM、分帧、加窗、RMS/削波/静音检测 [S16][S17][推测]。
- 在前台服务（`microphone` 类型）中进行 15–30 秒录音 [S20][推测]。
- YIN/pYIN 风格的端侧 F0 估计与轨迹平滑 [推测，详见 academic-research.md]。
- 音域/舒适音区从音高轨迹派生统计 [推测]。

**暂不可靠/需 Spike 验证**：
- 长时纯后台录音（受 while-in-use 与前台服务限制）[S20]——产品不应依赖。
- 特定采样率在所有设备可用——需运行时探测与降级 [S22][S23]。
- 预处理效果（降噪/AEC/AGC）的设备可用性与对 F0 的影响——需设备矩阵实测 [S29][推测]。
- CREPE 端侧推理的实时性与功耗——需 M-1.5 Spike [推测]。
- 跨设备一致的音高质量——麦克风硬件差异显著，需 M-10 验证 [推测]。

---

## 6. 与 PLAN 的对照

- PLAN §2.2 默认栈选择 `AudioRecord` + Foreground Service + 纯 Kotlin/JVM 音频分析，与上述可行性结论一致 [推测，基于本研究的工程判断]。
- PLAN §3.2 禁止“默认永久保存原始录音”“未经用户明确同意上传原始音频”——与 §隐私风险结论一致（详见 `academic-research.md` §6）。
- 是否引入 NDK/C++/TFLite/CREPE，应等 M-1.5 Spike 实测后再决定；本研究不预先断定。

---

## 参考文献

本文引用的来源见 `source-register.md`，编号 `[S16]`–`[S29]` 一一对应（与 academic-research.md 共用同一编号空间）。
