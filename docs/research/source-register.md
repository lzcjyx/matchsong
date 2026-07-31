# 来源登记表（M-1.2）

> 本表登记 M-1.2 学术与技术研究两份文档（`academic-research.md`、`android-technical-feasibility.md`）实际引用的全部来源。编号 `[S1]`–`[S29]` 在两份文档中跨文档共用同一编号空间。
>
> 仅登记实际被引用的来源；未实际读取或未实际引用的资料不列入。类型分为：paper（学术论文/会议论文）、official-doc（官方文档，如 developer.android.com）、article（技术文章/源码注释/二手资料）。访问时间：2026-07-30。

| ID | 标题 | 作者/发布方 | 年份 | URL | 类型 | 引用文档 | 相关性（一句话） |
|---|---|---|---|---|---|---|---|
| S1 | YIN, a fundamental frequency estimator for speech and music | A. de Cheveigné, H. Kawahara（JASA） | 2002 | https://pubs.aip.org/asa/jasa/article/111/4/1917/547221/YIN-a-fundamental-frequency-estimator-for-speech | paper | academic-research.md | YIN 音高检测算法原始论文；错误率约为最佳竞争方法的三分之一。 |
| S2 | pYIN: A Fundamental Frequency Estimator Using Probabilistic Threshold Distributions | Matthias Mauch, Simon Dixon（ICASSP） | 2014 | https://webspace.eecs.qmul.ac.uk/s.e.dixon/pub/2014/MauchDixon-PYIN-ICASSP2014.pdf | paper | academic-research.md | pYIN 论文；多候选+HMM 平滑，召回/精确率优于 YIN，八度误差 0.5%–1.7%。 |
| S3 | CREPE: A Convolutional Representation for Pitch Estimation | Jong Wook Kim, Justin Salamon, Peter Li, Juan Pablo Bello | 2018 | https://arxiv.org/abs/1802.06182 | paper | academic-research.md | CREPE 论文；深度 CNN 直接对时域波形做 F0 估计，性能等同或优于 pYIN。 |
| S4 | CREPE 官方实现（model capacity tiny/small/medium/large/full） | marl/crepe（GitHub） | 2018 | https://github.com/marl/crepe | article | academic-research.md | 提供 CREPE 五档模型容量，用于端侧推理可行性讨论。 |
| S5 | Voice Acoustics: an introduction | Joe Wolfe（UNSW Physics） | — | https://www.phys.unsw.edu.au/jw/voice.html | article | academic-research.md | 演唱基频范围约 60 Hz–1500 Hz 的人声声学背景。 |
| S6 | Average Singing Frequencies: Voice Range Data by Age | VoiceScience.org | — | https://www.voicescience.org/lexicon/average-singing-frequencies/ | article | academic-research.md | 古典声部基频范围参考（Bass/Baritone/Tenor/Alto/Soprano）。 |
| S7 | Singing voices and frequencies | DPA Microphones | — | https://www.dpamicrophones.com/dict/singing-voices-and-frequencies/ | article | academic-research.md | 各声部基频范围补充参考（Soprano B3–C6 等）。 |
| S8 | EQing Vocals: What's Happening in Each Frequency Range | Soundfly/Flypaper | 2020 | https://flypaper.soundfly.com/produce/eqing-vocals-whats-happening-in-each-frequency-range-in-the-human-voice/ | article | academic-research.md | 成人男女说话基频范围（男约 80–180 Hz，女约 165–255 Hz）。 |
| S9 | Vocal Parameters of Speech and Singing Covary and Are Related to Vocal Attractiveness | Valentova et al.（PMC） | 2019 | https://pmc.ncbi.nlm.nih.gov/articles/PMC6817625/ | paper | academic-research.md | 男女演唱音域跨度（semitones）实测数据。 |
| S10 | Pitch detection algorithm（jitter/shimmer 概念） | Wikipedia | — | https://en.wikipedia.org/wiki/Pitch_detection_algorithm | article | academic-research.md | jitter/shimmer 作为音高稳定性度量的概念来源（二手资料，文中已标注性质）。 |
| S11 | Configure preprocessing effects（VOICE_RECOGNITION 不应默认开降噪） | Android Open Source（source.android.com） | — | https://source.android.com/docs/core/audio/implement-pre-processing | official-doc | academic-research.md | 官方说明 VOICE_RECOGNITION 用途下不应默认开启降噪预处理。 |
| S12 | MediaRecorder.AudioSource（可选预处理效果因设备而异） | developer.android.com | — | https://developer.android.com/reference/kotlin/android/media/MediaRecorder.AudioSource | official-doc | academic-research.md | Android 音源定义与预处理效果可用性因设备而异。 |
| S13 | On-Device Neural Net Inference with Mobile GPUs | Lee et al.（arXiv 1907.01989） | 2019 | https://ar5iv.labs.arxiv.org/html/1907.01989 | paper | academic-research.md | 移动 GPU 加速端侧 NN 推理，性能与模型结构/精度/带宽强相关。 |
| S14 | nn-Meter: Towards Accurate Latency Prediction of Deep Learning Model Inference on Diverse Edge Devices | Zhang et al. | 2021 | https://air.tsinghua.edu.cn/pdf/nn-Meter-Towards-Accurate-Latency-Prediction-of-Deep-Learning-Model-Inference-on-Diverse-Edge-Devices.pdf | paper | academic-research.md | 端侧 NN 推理延迟预测是活跃研究，受设备异构性显著影响。 |
| S15 | Permissions on Android（RECORD_AUDIO 为危险级权限） | developer.android.com | — | https://developer.android.com/guide/topics/permissions/overview | official-doc | academic-research.md | RECORD_AUDIO 为危险级权限，需运行时申请并需隐私政策。 |
| S16 | AudioRecord（API reference） | developer.android.com | — | https://developer.android.com/reference/android/media/AudioRecord | official-doc | android-technical-feasibility.md | AudioRecord 提供原始 PCM 读取，适合需直接访问采样的场景。 |
| S17 | AudioFormat（API reference，PCM 编码与帧定义） | developer.android.com | — | https://developer.android.com/reference/kotlin/android/media/AudioFormat | official-doc | android-technical-feasibility.md | 线性 PCM 编码（8/16/32 bit）与帧构成定义。 |
| S18 | MediaRecorder overview | developer.android.com | 2025 | https://developer.android.com/media/platform/mediarecorder | official-doc | android-technical-feasibility.md | MediaRecorder 为高层录音 API，输出压缩格式，不直接暴露 PCM。 |
| S19 | Foreground service types are required（Android 14） | developer.android.com | — | https://developer.android.com/about/versions/14/changes/fgs-types-required | official-doc | android-technical-feasibility.md | Android 14 起 targetSdk≥34 必须声明前台服务类型与对应权限。 |
| S20 | Foreground service types（microphone 类型说明） | developer.android.com | — | https://developer.android.com/develop/background-work/services/fgs/service-types | official-doc | android-technical-feasibility.md | microphone 前台服务类型的声明、权限、while-in-use 限制与用途。 |
| S21 | Microphone restricted when app goes into background（issue tracker） | Google Issue Tracker | 2024 | https://issuetracker.google.com/issues/327085720 | official-doc | android-technical-feasibility.md | Android 14 对后台麦克风访问与前台服务类型的要求说明。 |
| S22 | Sampling audio（Android NDK 指南） | developer.android.com | 2024 | https://developer.android.com/ndk/guides/audio/sampling-audio | official-doc | android-technical-feasibility.md | 建议采样率匹配设备（44.1/48 kHz），高于 48 kHz 多数设备不可靠。 |
| S23 | AudioRecord.java 源码注释（44100 Hz 唯一全设备保证） | AOSP 源码 | — | https://android.googlesource.com/platform/frameworks/base/+/98d4ca6/media/java/android/media/AudioRecord.java | article | android-technical-feasibility.md | 历史上 44100 Hz 为唯一保证全设备可用的录音采样率（源码注释）。 |
| S24 | Android and Audio Formats/Sampling Rates（48 kHz 重采样） | headphones.com 论坛 | 2019 | https://forum.headphones.com/t/android-and-audio-formats-sampling-rates/2934 | article | android-technical-feasibility.md | 现代 Android 设备原生多为 48 kHz，44.1 kHz 常被重采样。 |
| S25 | AudioManager（PROPERTY_OUTPUT_SAMPLE_RATE 等） | developer.android.com（via Microsoft Learn 镜像） | — | https://learn.microsoft.com/en-us/dotnet/api/android.media.audiomanager | official-doc | android-technical-feasibility.md | getProperty 可查询设备原生/最佳输出采样率。 |
| S26 | MediaRecorder.AudioSource（VOICE_RECOGNITION 定义） | developer.android.com | — | https://developer.android.com/reference/kotlin/android/media/MediaRecorder.AudioSource | official-doc | android-technical-feasibility.md | VOICE_RECOGNITION 为语音识别调校音源，不可用时退化为 DEFAULT。 |
| S27 | MediaRecorder.java 源码注释（VOICE_RECOGNITION=6，processed/raw） | AOSP 源码 | — | https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android10-release/media/java/android/media/MediaRecorder.java | article | android-technical-feasibility.md | VOICE_RECOGNITION 倾向较原始音频，编号 6。 |
| S28 | Configure preprocessing effects（VOICE_RECOGNITION 不应默认开降噪） | Android Open Source | — | https://source.android.com/docs/core/audio/implement-pre-processing | official-doc | android-technical-feasibility.md | 与 S11 同源，在可行性文档中复用，说明音源与降噪关系。 |
| S29 | MediaRecorder.AudioSource（预处理效果可用性因设备而异） | developer.android.com / Stack Overflow 讨论 | — | https://developer.android.com/reference/kotlin/android/media/MediaRecorder.AudioSource | official-doc | android-technical-feasibility.md | NoiseSuppressor/AEC/AGC 可用性因设备而异，需 isAvailable() 守卫。 |

---

## 说明

- 表中每个来源均为本次研究实际检索并（对 URL 可访问者）实际读取过的资料；不可访问者（如部分 PDF 镜像 502）已在对应文档中以可访问的等价来源替换，不虚构内容。
- 二手资料（article 类型，如 Wikipedia、AOSP 源码注释、论坛技术帖）仅用于补充背景，关键结论优先以 paper / official-doc 支撑；当结论仅由二手资料支撑时，文档中已标注 `[推测]` 或说明其性质。
- 所有未标注来源且未由研究事实支撑的判断，均以 `[推测]` 显式标记，符合 PLAN §3.2“不得虚构论文或测试结果”的要求。
