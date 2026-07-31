# M-1 里程碑验收记录

- **里程碑：** M-1 仓库检查与研究
- **验收日期：** 2026-07-31（2026-07-30 完成研究/Spike 代码，07-31 完成 M-1.4 模拟器运行验证）
- **验收人：** Coding Agent
- **总体状态：** **DONE** —— 6/6 任务全部完成

---

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M-1.1 仓库审计 | **DONE** | `docs/research/repository-audit.md` |
| M-1.2 学术与技术研究 | **DONE** | `docs/research/academic-research.md`、`android-technical-feasibility.md`、`source-register.md`（29 条来源） |
| M-1.3 竞品研究 | **DONE** | `docs/research/competitor-research.md`（17 个产品）、`product-opportunities.md` |
| M-1.4 AudioRecord/MediaRecorder Spike | **DONE**（编译 + 模拟器运行实测） | `experiments/audio-record/`、`docs/experiments/audio-recording-spike-results.md` |
| M-1.5 音高检测 Spike | **DONE**（合成信号实测） | `experiments/pitch-detection/`（11 测试全过）、`docs/experiments/pitch-detection-results.md` |
| M-1.6 MVP 技术决策 | **DONE** | `docs/experiments/spike-results.md`、`mvp-technical-decision.md`、`docs/decisions/ADR-001..003` |

## 2. 里程碑退出条件核对（PLAN §5.4）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| 仓库审计完成 | ✅ | repository-audit.md |
| 研究文档完成 | ✅ | 学术+技术+竞品+来源登记 |
| AudioRecord Spike 完成 | ✅ | 编译 + 模拟器运行验证（权限/录音/前后台/来电/格式/CPU/内存） |
| 音高检测 Spike 完成 | ✅ | 合成信号全量实测（YIN/ACF/FFT），选型 YIN |
| MVP 方向确定 | ✅ | 音域推荐（ADR-001/002/003） |
| 高风险假设已有验证方案 | ✅ | 真机人声/录音验证方案已写入遗留风险表 |
| 未开始建设正式业务功能 | ✅ | 仅 experiments/ 实验代码与 docs/，无生产功能代码 |

## 3. 构建与测试状态

- 根工程（JVM 脚手架）：`gradle tasks` BUILD SUCCESSFUL
- experiments/pitch-detection：`gradle test` **11/11 通过**；`gradle run` 产出完整实测数据表
- experiments/audio-record：`:app:assembleDebug` **BUILD SUCCESSFUL**；APK 在模拟器（android-36 x86_64）实测运行
- 静态检查：Spike 工程未配置 Lint/Detekt/Ktlint（M1 引入，Spike 阶段不适用）

## 4. 关键研究/实验结论摘要

1. **仓库是空项目**（零起点，无历史包袱）；远程已连接，初始代码推送至 origin/main。
2. **音高检测选 YIN**：合成信号全频段误差 <0.03%，抗削波、拒静音/白噪声，~1ms/帧；ACF 有子谐波锁定缺陷，FFT 主峰法对谐波不可靠。
3. **采集 API 选 AudioRecord**：唯一可直读 PCM 的 API（实测确认）；前台服务 + microphone 类型在模拟器验证通过（后台/来电录音持续，无崩溃）。
4. **MVP 方向为音域推荐**，无后端、不保存原始音频、不引入 TFLite。
5. **竞品空隙**："唱几句后推荐适合音域的歌"直接竞争产品少且不充分，提出 5 个差异化方向。

## 5. 遗留风险（已记录，进入 M0/M1 前需知晓）

| # | 风险 | 归属 |
|---|---|---|
| R-1 | 录音/音高检测基于模拟器与合成信号，真实设备人声需真机复测 | M3/M5/M10 |
| R-2 | 音频焦点未在 Spike 实现（来电不自动暂停），正式产品 M3 必须实现 | M3 |
| R-3 | 舒适音区无统一客观定义 | M0 SPEC 定义 |
| R-4 | 系统默认 java=1.8 与 Kotlin 2.1/JDK17 工具链冲突；无 Gradle Wrapper | M1 |
| R-5 | 系统 Gradle 8.5 init.d 注入与 FAIL_ON_PROJECT_REPOS 冲突（Android 工程需用缓存 8.9 dist 或独立 wrapper） | M1 |
| R-6 | 歌曲音域数据来源未定 | M6 |

## 6. 验收结论

**M-1 里程碑全部任务完成，验收通过。** 满足 PLAN §3.4 质量门禁（当前 Milestone 必须任务全部完成；代码可构建；单元测试通过；文档同步；遗留风险已记录；验收记录已生成）。

**建议下一步：** 进入 M0（MVP 与架构冻结）—— 编写 SPEC、定义舒适音区/稳定性/演唱负担可计算指标、确定歌曲特征数据最小字段集、设计可解释推荐输出模板。
