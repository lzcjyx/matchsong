# TESTING.md — matchsong MVP 测试策略

- **版本：** 0.1.0
- **日期：** 2026-07-31
- **里程碑：** M0.4 建立测试策略（PLAN.md §6.2）
- **状态：** DRAFT → 待批准

## 依据

| 文档 | 用途 |
|---|---|
| SPEC.md §11 非功能需求、§12 ACC-1..17、§13 置信度 | 测试目标与验收指标 |
| PLAN.md §2.2 测试栈、§3.4 质量门禁、M1.2/M1.3/M3.7/M4.6/M5.8/M6.5/M7.6/M8.7/M10.1-10.6/M11.3-11.4 | 工具栈、门禁与各里程碑测试要求 |
| docs/experiments/pitch-detection-results.md | 合成音频夹具（可复用信号）与 YIN 实测基线 |
| docs/experiments/audio-recording-spike-results.md | 模拟器 spike_avd 可用性事实与录音链路验证 |

配套文档：`docs/testing/test-fixture-manifest.md`、`docs/testing/device-matrix.md`、`docs/testing/manual-test-checklist.md`、`docs/testing/regression-suite.md`。

---

## 1. 测试目标与原则

### 1.1 目标

- SPEC §12 的 ACC-1..17 每一条都有可执行的测试或人工检查项；
- 关键路径（录音 → 质量检测 → 分析 → 推荐 → 删除）具备自动化防线；
- 算法层测试零真机依赖（合成/记录夹具跑纯 JVM）；
- 真机只承担自动化覆盖不了的场景：厂商权限差异、真实麦克风质量、硬件性能。

### 1.2 原则

1. **不可靠音频不得生成正式结果**（SPEC §7.4）——质量失败路径（ACC-7/8/9）必须有测试，与正常路径同等重要；
2. **算法精度只在单元层断言**，UI 只断言状态与交互，系统行为只在仪器层断言——各层不重复覆盖；
3. **修复 Bug 必须先添加失败测试**（PLAN M10.6）；
4. **不得通过关闭规则掩盖问题**（PLAN M1.2）——静态检查豁免必须记录理由并评审；
5. **性能先基准后优化**（PLAN M10.1）——不得在没有基准数据时盲目优化；
6. 测试数据必须与生产数据边界明确（FR-SHELL-3），Fake 数据不得进入 Release。

---

## 2. 测试分层

| 层 | 运行位置 | 主要技术 | 覆盖范围 | 明确不做什么 |
|---|---|---|---|---|
| 单元测试 | JVM（本机/CI） | JUnit5 + MockK + Turbine | 纯逻辑：YIN 音高检测、音高后处理、稳定音域/舒适音区估计、质量阈值判定、录音状态机、推荐评分/过滤/变调/排序/解释、歌曲数据校验、各 ViewModel 状态逻辑 | 不启动 Android 组件；不访问真实麦克风/文件系统/数据库 |
| 集成测试 | JVM（本机/CI） | JUnit5 + Room In-Memory | Room DAO/迁移/回滚、DataStore、歌曲导入工具全链路、领域用例与 Repository 装配、错误恢复 | 不覆盖 UI；不依赖真实设备 |
| UI 测试（Compose） | 模拟器 spike_avd | Compose UI Test + Fake Repository | 首次启动/Onboarding/页面导航/Loading/Empty/Error/Permission/质量警告状态/删除确认弹窗（PLAN M2.5）；全部 MVP 页面 | 不依赖真实麦克风与真实分析结果（数据由 Fake 提供） |
| 仪器测试（Instrumented） | 模拟器 spike_avd | AndroidX Test | Android 框架行为：麦克风权限状态机、AudioRecord 真实采集、前台服务与通知、进程重建、后台/来电行为（M3.7 自动部分） | 不做算法精度断言（精度在 JVM 层用夹具断言） |
| E2E（Fake Audio） | 模拟器 spike_avd | AndroidX Test + Fake Audio Stream | 全流程：首次启动→Onboarding→授权→录音→质量通过→分析→推荐→收藏→反馈→历史→删除（PLAN M8.7） | 不验证真实音频质量（属人工清单，manual-test-checklist.md） |

**防重复约定：** 同一事实只在归属层断言一次——例如"YIN 对 440Hz 正弦输出 440.02Hz"只在单元层断言；"录音失败后 UI 显示重试"只在 UI/E2E 层断言；"AudioRecord 能采到 PCM"只在仪器层断言。

---

## 3. 工具栈（PLAN §2.2）

| 工具 | 用途 | 说明 |
|---|---|---|
| JUnit5（Jupiter） | 单元/集成测试框架 | 全部 JVM 测试 |
| MockK | Mock/桩/协程测试 | 接口隔离、Dispatcher 注入、StateFlow 模拟 |
| Turbine | Flow/StateFlow 断言 | 状态机与 UI 状态流测试 |
| Room In-Memory Database | DAO/迁移集成测试 | 不触碰真实 DB 文件 |
| Compose UI Test | UI 测试 | createComposeRule / createAndroidComposeRule |
| AndroidX Test | 仪器测试基础 | test runner、ActivityScenario、规则 |
| MockWebServer | 预留 | **MVP 无网络模块**（SPEC §10.3：无网络权限、无后端），仅在引入网络模块时引入（PLAN §2.2），当前不依赖 |
| Macrobenchmark | 性能基准 | androidx.benchmark.macro，M10 起（见 §7） |
| Android Lint / Detekt / Ktlint | 静态检查 | `lintDebug` / `detekt` / `ktlintCheck`（M1.2）；外加依赖版本检查与依赖漏洞扫描（M1.2，CI 条件允许时） |
| JaCoCo | 行覆盖率报告 | 见 §4 |

---

## 4. 覆盖率目标

| 范围 | 目标（行覆盖率） | 依据 |
|---|---|---|
| 核心逻辑：`domain:*`、`core:audio`、`core:model` | **≥ 80%** | SPEC §11 可测试性（硬性指标） |
| UI 层：`app`、`feature:*` | **≥ 60%** | [推测] SPEC 未规定 UI 层阈值；Compose 声明式 UI 中大量为样式/布局样板，边际收益递减，60% 为底线。权限状态机、质量失败提示、删除流程等关键状态逻辑不受此限，必须单独覆盖 |

- 统计方式：JaCoCo 合并各模块 JVM 测试（`testDebugUnitTest`）覆盖率；仪器测试覆盖率可选统计，不作为门禁。
- 门禁：CI 每 PR 计算；核心模块低于 80% 或 UI 层低于 60% 阻止合并。
- 豁免：核心算法模块不豁免；纯导航/模板样板可豁免但须在覆盖率报告中记录理由（遵守 PLAN M1.2"不允许通过关闭规则掩盖问题"）。

---

## 5. 音频测试策略

### 5.1 原则

- **单元测试零真机依赖**：全部音频算法测试使用合成 WAV + 记录 WAV 夹具，跑在 JVM；
- 仪器测试使用模拟器 `spike_avd`（android-36 x86_64，已验证可用，见 audio-recording-spike-results.md）；
- 真实麦克风采集质量只在真机人工清单验证（M10 设备矩阵）；
- 每条夹具必须有**来源和预期**（PLAN M4.6 验收条件）。

### 5.2 夹具组成

完整清单与生成流程见 `docs/testing/test-fixture-manifest.md`：

- **合成 WAV**（复用 experiments/pitch-detection 已验证信号）：纯音 130/220/440/880/1046Hz、音阶 C3-E3-G3-C4、静音、白噪声、削波 440Hz、talkLike 150Hz；
- **计划记录夹具**（待录制）：安静说话、大声说话、部分静音、过短录音、男声、女声；
- **假流（Fake Frame Source / Fake Audio Stream）**（FR-QUAL-4）：程序化生成 PCM 帧流，供质量检测与录音状态机测试；
- **模拟器录音**（已有实验产物）：audio-record spike 的 15s 录音，用于仪器层真实采集链路验证。

### 5.3 断言基线

- 纯音夹具：频率相对误差 ≤ 1%，或 MIDI 音分偏差 < 50 音分（半音感知门限）[推测：SPIKE 实测纯音误差 < 0.03%，此处给算法测试留出安全边界]；
- 拒绝路径：静音/噪声/削波/过短按 SPEC §6 断言 `isUsable=false` 且原因精确匹配（ACC-7/8）；
- 数据不足：有效帧不足时不得输出音域与推荐（ACC-9）；
- 稳定性：相同输入两次分析结果一致（FR-RECM-7 / ACC-13）。

---

## 6. 测试执行分层映射（CI）

映射到 PLAN M1.3 CI 任务（至少执行：`assembleDebug`、`testDebugUnitTest`、`lintDebug`、`detekt`、`ktlintCheck`）：

| 测试类型 | 命令 | 运行环境 | 频率 | 对应 PLAN |
|---|---|---|---|---|
| 单元测试 + 静态检查（必选） | `assembleDebug` + `testDebugUnitTest` + `lintDebug` + `detekt` + `ktlintCheck` | CI runner | 每个 PR，失败阻止合并，结果可追踪 | M1.3 |
| 覆盖率门禁 | JaCoCo 报告 + 阈值检查 | CI runner | 每个 PR | SPEC §11 / M1.2 |
| Compose UI 测试 | `connectedDebugAndroidTest`（UI 套件） | 模拟器 job（spike_avd） | 每个 PR（M2 起）或合并前 | M1.3 / M2.5 |
| 仪器测试 | `connectedDebugAndroidTest`（仪器套件） | 模拟器 job（spike_avd） | 每个 PR（M3 起）或合并前 | M1.3 / M3.7 |
| E2E（Fake Audio） | E2E 套件（仪器） | 模拟器 job（spike_avd） | M8 起：每个 PR 或 nightly | M8.7 |
| Release Build + 依赖扫描 | `assembleRelease`、依赖版本/漏洞扫描 | CI | 发布前（M11） | M1.3 / M11 |
| Macrobenchmark | macrobenchmark 模块 | 真机；无真机时 spike_avd（数据仅参考）[推测] | M10 起：nightly 或门禁前 | M10.1 |
| 门禁回归 | 见 regression-suite.md | 本地 + CI | 每个 Milestone 结束 | PLAN §3.4 |

---

## 7. 性能目标测试

目标值全部来自 SPEC §11，M10 用 Macrobenchmark 验证：

| 指标 | 目标 | 测量方法 | 基线参考 |
|---|---|---|---|
| 冷启动到首页 | ≤ 3s | StartupTimingMetric（cold start，CompilationMode.Full），多次取中位数 | M10 首次建立 |
| 30s 音频 YIN 分析 | ≤ 10s（中端设备） | 分析全流程计时（质量→YIN→后处理→音域估计），输入为 30s 男声/女声夹具 | spike 桌面实测单帧 ~1.04ms；30s@44.1kHz、hop 1024 ≈ 1292 帧 → 纯 YIN 理论 ~1.3s，SPEC 预算 10s 约 7 倍余量（M5 后以真机为准） |
| 分析峰值内存 | ≤ 200MB | MemoryMetric（分析 30s 夹具） | 帧缓冲固定（2048 样本 ≈ 16KB/帧），M10 真机 |
| 单次完整流程耗电 | ≤ 1% | BatteryMetrics（真机） | M10 真机 |
| 录音期间 UI 流畅 | 无卡顿（音量反馈更新 ≤ 10Hz） | 单元测试断言节流 ≤10Hz + 人工体验 | FR-REC-4 |
| 首屏时间 / 录音期间 CPU / 分析期间 CPU / APK·AAB 大小 / 数据库启动时间 | M10.1 全量测量并记录 | Macrobenchmark + 构建产物检查 | 不得无基准优化（M10.2 按序优化，只有 Kotlin 无法达标才经 ADR 引入 NDK） |

---

## 8. 回归策略

- **时机：** 每个 Milestone 门禁前（PLAN §3.4）执行回归；M10.5 执行完整回归（发布前唯一全量回归）；大改动后按需执行[推测]。
- **内容：** 完整回归清单与每里程碑子集见 `docs/testing/regression-suite.md`。
- **门禁条件（PLAN §3.4）：** 当前 Milestone 必须任务全部完成、可构建、单元/集成测试通过、静态检查通过、无未解释严重错误、文档同步、遗留风险已记录、生成 `docs/milestones/M{n}-acceptance.md` 验收记录。
- **性能回归（M10 起）：** Macrobenchmark 基准对比，任一指标劣化 > 20% 视为回归[推测阈值]。

---

## 9. 真机测试要求

- **设备矩阵：** `docs/testing/device-matrix.md`。覆盖（SPEC §11 兼容 + PLAN M10.3）：低端/中端/Pixel/接近原生/Samsung/中国厂商（Xiaomi、OPPO、vivo）各 ≥ 1 台，Android 8.0（API 26，minSdk）至 16（API 36，targetSdk）版本跨度，有线耳机/蓝牙耳机/内置麦克风三种采集方式。
- **本环境现状：** 模拟器 `spike_avd`（android-36，x86_64，Pixel 5 配置）可用，承担仪器/E2E/开发验证；真机待补充（M10.3 前集齐）。
- **人工清单：** `docs/testing/manual-test-checklist.md`。覆盖权限全状态（授予/拒绝/永久拒绝/设置返回/使用中撤销）、录音场景（M3.7：Pixel/Samsung/Xiaomi、有线/BT、外放伴奏、来电/通知中断、前后台切换）、质量失败 UX（每种原因）、分析、推荐、删除流程、旋转/进程重建、低存储。
- **可靠性门禁：** 无 P0/P1 Bug（SPEC §11）；正式发布前已知崩溃率 0。

---

## 10. 发布回归（M10.5 / M11）

1. **M10.5 完整回归**（PLAN M10.5 定义）：Unit + Integration + Compose UI + Instrumentation + E2E + Lint + Detekt + Ktlint + Release Build + 手工回归检查表，全部通过；配合 M10.6 Bug 清零（P0/P1 全部修复，P2 评估记录，P3 进 Backlog，记录至 `docs/bugs/bug-log.md`）。
2. **M11.3 Internal Testing 检查：** 安装、更新、首次启动、麦克风权限、录音通知、分析、推荐、删除数据、崩溃、ANR、设备兼容。
3. **M11.4 Closed Beta 指标：** 录音完成率、有效录音率、分析失败率、推荐点击率、用户适合度反馈、崩溃率、ANR、设备型号、结果解释理解程度。**不得采集原始音频用于研究**（除非新增明确同意流程）。
4. **发布决策（M11.5）：** 依据 Beta 数据决定发布/继续优化/增加歌曲/引入后端。

---

## 11. 各里程碑测试重点（简表）

| 里程碑 | 测试重点 | 关键验收 |
|---|---|---|
| M1 | 测试框架落地、静态检查全绿（Lint/Detekt/Ktlint）、CI、JaCoCo 覆盖率报告、core:testing 夹具工厂与 Fake 数据工厂 | 门禁：构建 + 单测 + 静态检查；统一检查命令（M1.2） |
| M2 | Compose UI 测试：首次启动、已完成 Onboarding、导航、Loading、Empty、Error、Fake 推荐列表、删除确认弹窗（M2.5） | Compose UI 测试通过，无麦克风/音频算法依赖（M2 退出条件） |
| M3 | 录音状态机单测（MockK）、Fake Audio Stream、仪器测试（权限/前台通知/后台/来电，spike_avd）、人工清单（M3.7 设备与音频附件组合） | 15-30s 稳定录制；通知全程可见；权限异常清晰反馈；资源正确释放 |
| M4 | 质量夹具 ≥ 9 种（M4.6 清单）、阈值集中配置测试、每种拒绝原因断言（ACC-7/8）、FR-QUAL-4 三种输入源（实时 PCM/WAV/假流） | 无效录音稳定拒绝；夹具有来源和预期；不合格音频不进入分析 |
| M5 | YIN 精度（纯音误差 < 0.03% 基线）、音阶分段、多采样率、噪声拒绝、边界频率（65/1046Hz）、八度跳变、极少有效帧、重复分析一致性（M5.8） | 音域非极值（P5/P95）；数据不足拒绝过度推断（ACC-9）；中端设备耗时 ≤ 10s/30s |
| M6 | Schema 校验、导入、重复 ID、无效音高、最低音高于最高音、缺失来源、Room Migration、版本回滚（M6.5） | MVP 数据集 50-200 首全量自动校验通过，每首有来源/可信度声明 |
| M7 | 完全匹配、最高/最低音超出、音区不匹配、可降调匹配、无候选、低置信降权、相同分数稳定排序、权重版本、解释与分数一致（M7.6） | ACC-12/13/16/17 全过；推荐可重复可追溯 |
| M8 | Fake Audio E2E 全流程（M8.7 的 11 步）、错误恢复、半成品结果不残留 | E2E 通过；失败可恢复；结果标注"本次录音估计" |
| M9 | 删除流程（单条/全部/缓存/重置，ACC-15）、原始音频生命周期（ACC-14）、Release 日志脱敏（FR-PRIV-4） | 分析完成后缓存无 PCM/WAV；删除后恢复首次启动状态 |
| M10 | Macrobenchmark（冷启动 ≤3s、分析 ≤10s、峰值内存 ≤200MB、电量 ≤1%）、设备矩阵（M10.3）、稳定性 11 项（M10.4）、完整回归（M10.5）、Bug 清零（M10.6） | SPEC 性能指标达成；矩阵通过；无 P0/P1；验收记录归档 |
| M11 | Internal Testing 检查清单、Closed Beta 指标监控、发布决策依据（M11.3-11.5） | 可发布构建；Beta 数据满足发布门槛 |

---

## 12. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| 0.1.0 | 2026-07-31 | 初稿（M0.4 交付物） |
