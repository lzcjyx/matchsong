# M-1 里程碑验收记录

- **里程碑：** M-1 仓库检查与研究
- **验收日期：** 2026-07-30
- **验收人：** Coding Agent
- **总体状态：** **PARTIAL（1 项 BLOCKED）** —— 详见下方逐项核对

---

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M-1.1 仓库审计 | **DONE** | `docs/research/repository-audit.md` |
| M-1.2 学术与技术研究 | **DONE** | `docs/research/academic-research.md`、`android-technical-feasibility.md`、`source-register.md`（29 条来源） |
| M-1.3 竞品研究 | **DONE** | `docs/research/competitor-research.md`（17 个产品）、`product-opportunities.md` |
| M-1.4 AudioRecord/MediaRecorder Spike | **BLOCKED**（代码+编译 DONE，运行验证待设备） | `experiments/audio-record/`、`docs/experiments/audio-recording-spike-results.md` |
| M-1.5 音高检测 Spike | **DONE**（合成信号实测；真人人声待补） | `experiments/pitch-detection/`（11 测试全过）、`docs/experiments/pitch-detection-results.md` |
| M-1.6 MVP 技术决策 | **DONE** | `docs/experiments/spike-results.md`、`mvp-technical-decision.md`、`docs/decisions/ADR-001..003` |

## 2. 里程碑退出条件核对（PLAN §5.4）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| 仓库审计完成 | ✅ | repository-audit.md |
| 研究文档完成 | ✅ | 学术+技术+竞品+来源登记 |
| AudioRecord Spike 完成 | ⚠️ | 编译验证完成，**运行验证 BLOCKED**（开发机无设备/AVD） |
| 音高检测 Spike 完成 | ✅ | 合成信号全量实测（YIN/ACF/FFT），选型 YIN |
| MVP 方向确定 | ✅ | 音域推荐（ADR-001/002/003） |
| 高风险假设已有验证方案 | ✅ | 真机人声/录音验证方案已写入 Spike 文档 §3.2 与遗留风险表 |
| 未开始建设正式业务功能 | ✅ | 仅 experiments/ 实验代码与 docs/，无生产功能代码 |

## 3. 构建与测试状态

- 根工程（JVM 脚手架）：`gradle tasks` BUILD SUCCESSFUL
- experiments/pitch-detection：`gradle test` **11/11 通过**；`gradle run` 产出完整实测数据表
- experiments/audio-record：`:app:assembleDebug` **BUILD SUCCESSFUL**（app-debug.apk 5.6MB）
- 静态检查：Spike 工程未配置 Lint/Detekt/Ktlint（M1 引入，Spike 阶段不适用）

## 4. 关键研究/实验结论摘要

1. **仓库是空项目**（零起点，无历史包袱）；远程已连接，初始代码推送至 origin/main。
2. **音高检测选 YIN**：合成信号全频段误差 <0.03%，抗削波、拒静音/白噪声，~1ms/帧；ACF 有子谐波锁定缺陷，FFT 主峰法对谐波不可靠（实测数据见 pitch-detection-results.md）。
3. **采集 API 选 AudioRecord**：唯一可直读 PCM 的 API；前台服务 + microphone 类型满足 Android 14 后台录音限制。
4. **MVP 方向为音域推荐**，无后端、不保存原始音频、不引入 TFLite。
5. **竞品空隙**："唱几句后推荐适合音域的歌"直接竞争产品少且不充分，提出 5 个差异化方向。

## 5. 遗留风险（已记录，进入 M0/M1 前需知晓）

| # | 风险 | 归属 |
|---|---|---|
| R-1 | M-1.4 真机运行验证未完成（麦克风/前后台/中断/CPU/内存） | M3 前必须补测 |
| R-2 | 真实人声音高检测未实测（合成信号已验证） | M1 后设备矩阵补测 |
| R-3 | 舒适音区无统一客观定义 | M0 SPEC 定义 |
| R-4 | 系统默认 java=1.8 与 Kotlin 2.1/JDK17 工具链冲突；无 Gradle Wrapper | M1 |
| R-5 | 系统 Gradle 8.5 init.d 注入与 FAIL_ON_PROJECT_REPOS 冲突（Android 工程需用缓存 8.9 dist 或独立 wrapper） | M1 |
| R-6 | 歌曲音域数据来源未定 | M6 |

## 6. 验收结论

**M-1 里程碑未完全通过**：5/6 任务 DONE，M-1.4 运行验证 BLOCKED（环境限制，非代码问题）。

按 PLAN §3.4 质量门禁："当前 Milestone 的必须任务全部完成"未满足。**建议：** 连接 Android 真机（或创建 AVD）完成 M-1.4 运行验证后解除 BLOCKED 并复核本记录；在此之前不进入 M0 正式建设。若用户同意，可将 M-1.4 运行验证显式 DEFERRED 至 M1（Android 工程基线）阶段，本记录同步更新。
