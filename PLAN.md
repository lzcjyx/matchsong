# PLAN.md

# Android Vocal Profile & Song Recommendation App

## 1. 文档目的

本计划用于指导 Coding Agent 分阶段完成一款 Android 原生应用。

应用允许用户录制约 15～30 秒的演唱音频，分析用户的基础声音特征、稳定演唱音区和录音质量，并根据歌曲音域、主要音区、演唱难度和用户偏好，推荐更适合用户演唱的歌曲。

本计划强调：

- 先调研，后开发；
- 先验证技术风险，后建设正式架构；
- 优先实现可解释的音域推荐；
- 优先采用本地音频处理；
- 每个 Milestone 必须独立验收；
- 未通过当前 Milestone 的质量门禁，不得进入下一阶段；
- 每次只执行一个足够小、可以测试和回滚的任务。

---

# 2. 产品与技术基线

## 2.1 MVP 产品定位

MVP 默认采用：

> 基于用户稳定演唱音区、舒适音区、音高稳定性和歌曲演唱负担的可解释歌曲推荐。

MVP 不以“准确识别用户音色类型”为主要卖点。

MVP 可以展示部分基础声音特征，但不得在缺乏研究依据的情况下生成以下类型的绝对结论：

- 你的声音很高级；
- 你是专业男高音；
- 你的声音适合成为职业歌手；
- 你的嗓音存在疾病；
- 你的音色和某位歌手完全一致。

第一版推荐的核心依据应是：

1. 用户稳定最低音；
2. 用户稳定最高音；
3. 用户舒适音区；
4. 音高轨迹稳定性；
5. 歌曲最低音和最高音；
6. 歌曲主要旋律音区；
7. 高音持续时间；
8. 旋律跳进和演唱难度；
9. 推荐升调或降调范围；
10. 用户语言、风格和歌手偏好。

---

## 2.2 默认技术栈

除非技术 Spike 得出相反结论，默认使用：

### Android 客户端

- Kotlin；
- Gradle Kotlin DSL；
- Jetpack Compose；
- Material 3；
- Navigation Compose；
- ViewModel；
- Kotlin Coroutines；
- StateFlow；
- Hilt；
- Room；
- DataStore；
- AudioRecord；
- Foreground Service；
- Jetpack Media3；
- Kotlinx Serialization。

### 音频分析

MVP 优先采用纯 Kotlin 或 JVM 兼容实现：

- PCM 音频流；
- WAV 测试文件；
- 分帧；
- 窗函数；
- RMS；
- 峰值和削波检测；
- 静音检测；
- YIN 音高检测；
- 音高轨迹平滑；
- 频率和音符转换；
- 音域与舒适音区估计。

在性能测试证明 Kotlin 实现不足前，不引入：

- C++；
- Android NDK；
- JNI；
- Oboe；
- 大型端侧模型；
- 云端实时音频分析。

### 测试

- JUnit；
- MockK；
- Turbine；
- Room In-Memory Database；
- Compose UI Test；
- AndroidX Test；
- MockWebServer，仅在引入网络模块时使用；
- Macrobenchmark；
- 静态分析使用 Android Lint、Detekt 和 Ktlint。

---

# 3. Coding Agent 全局执行规则

## 3.1 每次任务的工作循环

Coding Agent 每次只能执行一个 Task ID，或一组强相关且总规模足够小的 Task。

执行顺序必须是：

1. 阅读 `PLAN.md`；
2. 确认当前 Milestone；
3. 检查任务依赖；
4. 阅读相关源代码、测试和文档；
5. 输出本次任务目标；
6. 列出预计修改的文件；
7. 先添加或更新测试；
8. 实施最小必要修改；
9. 运行相关测试；
10. 运行静态检查；
11. 更新文档；
12. 更新任务状态；
13. 说明遗留风险；
14. 指出下一个可执行任务。

---

## 3.2 禁止行为

Coding Agent 不得：

- 在研究阶段直接建设完整产品；
- 一次完成多个 Milestone；
- 未经说明重写整个项目；
- 删除失败测试；
- 禁用 Lint 或其他严格检查；
- 使用空 `catch` 隐藏错误；
- 使用固定假数据伪装真实算法结果；
- 将实验代码直接复制进生产模块；
- 在没有依据时宣称算法准确；
- 默认永久保存原始录音；
- 未经用户明确同意上传原始音频；
- 将 API Key、Token 或密码提交到仓库；
- 为了通过测试而修改正确的测试预期；
- 未运行测试却声称测试已经通过；
- 在存在阻塞性失败时进入下一个 Milestone。

---

## 3.3 任务状态

每个任务只能处于以下状态之一：

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`
- `DEFERRED`

只有满足任务的全部验收条件后，才能标记为 `DONE`。

---

## 3.4 Milestone 质量门禁

进入下一个 Milestone 前，必须满足：

- 当前 Milestone 的必须任务全部完成；
- 当前代码可以构建；
- 相关单元测试通过；
- 相关集成测试通过；
- 静态检查通过；
- 没有未解释的严重错误；
- 文档已同步；
- 已记录遗留风险；
- 已生成 Milestone 验收记录。

验收记录保存至：

```text
docs/milestones/M{编号}-acceptance.md
```

---

# 4. Milestone 总览

| Milestone | 名称 | 核心目标 |
|---|---|---|
| M-1 | 仓库检查与研究 | 确认产品和技术是否可行 |
| M0 | MVP 与架构冻结 | 完成 SPEC、架构和任务拆分 |
| M1 | Android 工程基线 | 建立可持续开发和测试环境 |
| M2 | 应用外壳与导航 | 完成 Compose 基础用户界面 |
| M3 | 录音系统 | 稳定完成麦克风授权和音频录制 |
| M4 | 音频质量检测 | 拒绝静音、过短、削波等无效录音 |
| M5 | 音高与音域分析 | 输出可信的音高轨迹和舒适音区 |
| M6 | 歌曲数据系统 | 建立可验证、可更新的歌曲特征数据 |
| M7 | 推荐引擎 | 生成可解释的歌曲适唱度推荐 |
| M8 | 完整用户体验 | 串联录音、分析、推荐和反馈 |
| M9 | 隐私、安全与数据管理 | 满足敏感音频数据保护要求 |
| M10 | 稳定性与性能优化 | 完成设备矩阵、性能和回归测试 |
| M11 | Beta 与 Google Play 发布 | 形成可发布构建和商店材料 |

推荐严格按照顺序执行。

M6 的部分数据准备工作可以与 M4、M5 并行，但不得影响音频分析主流程的验证。

---

# 5. M-1：仓库检查与研究

## 5.1 目标

在编写正式产品代码前，确认：

- 当前仓库状态；
- Android 录音方案；
- 音高分析方案；
- 产品竞争环境；
- MVP 能力边界；
- 需要通过 Spike 验证的高风险假设。

---

## 5.2 进入条件

- 已获得项目仓库访问权限；
- 可以读取全部现有代码；
- 可以运行 Gradle 和 Android 测试环境；
- 可以访问外部研究资料，或者明确记录离线限制。

---

## 5.3 任务

### M-1.1 仓库审计

**状态：** `DONE`（2026-07-30，交付 docs/research/repository-audit.md）

检查：

- 当前目录结构；
- 是否已经存在 Android 工程；
- Gradle 和 Android Gradle Plugin 版本；
- Kotlin 版本；
- Compose 配置；
- 已有模块；
- 已有测试；
- CI 配置；
- Git 状态；
- 未提交修改；
- 已存在的音频代码；
- 已存在的后端或 API。

交付：

```text
docs/research/repository-audit.md
```

验收条件：

- 明确仓库是空项目、原型还是现有产品；
- 明确可复用模块；
- 明确高风险旧代码；
- 不修改现有核心功能。

---

### M-1.2 学术与技术研究

**状态：** `DONE`（2026-07-30，交付 docs/research/academic-research.md、android-technical-feasibility.md、source-register.md）

研究：

- AudioRecord；
- MediaRecorder；
- Foreground Service；
- Android 后台录音限制；
- 采样率和设备差异；
- YIN、pYIN、CREPE；
- 音域和舒适音区估计；
- 音高稳定性；
- 移动端噪声处理；
- 端侧推理限制；
- 原始音频隐私风险。

交付：

```text
docs/research/academic-research.md
docs/research/android-technical-feasibility.md
docs/research/source-register.md
```

验收条件：

- 关键结论均有来源；
- 明确区分研究事实和工程推测；
- 不得虚构论文或测试结果；
- 明确哪些功能可以在端侧完成；
- 明确哪些功能暂不可靠。

---

### M-1.3 竞品研究

**状态：** `DONE`（2026-07-30，交付 docs/research/competitor-research.md、product-opportunities.md）

调查：

- Smule；
- StarMaker；
- WeSing；
- Voloco；
- SingSharp；
- 音域测试应用；
- AI 声乐教练应用；
- 唱歌评分应用；
- 歌曲推荐产品。

交付：

```text
docs/research/competitor-research.md
docs/research/product-opportunities.md
```

验收条件：

- 至少覆盖直接竞品和间接竞品；
- 明确每个产品的输入、输出和核心流程；
- 明确本产品与唱歌评分产品的差异；
- 明确“唱几句后推荐歌曲”是否存在直接竞争产品；
- 提出至少三个差异化方向。

---

### M-1.4 AudioRecord 与 MediaRecorder Spike

**状态：** `DONE`（2026-07-31，代码 + 编译 + 模拟器运行验证全部完成，交付 experiments/audio-record/ 与 docs/experiments/audio-recording-spike-results.md）

分别实现最小实验：

```text
experiments/audio-record/
experiments/media-recorder/
```

测试：

- 麦克风权限；
- 开始和停止；
- 15～30 秒录音；
- 前后台切换；
- 来电或音频焦点中断；
- 文件格式；
- PCM 数据访问；
- 错误恢复；
- 文件大小；
- CPU 和内存占用。

验收条件：

- 记录两种 API 的实际结果；
- 明确正式产品使用哪一种；
- 默认优先选择能够访问 PCM 的 AudioRecord；
- 实验代码不得直接进入生产模块。

---

### M-1.5 音高检测 Spike

**状态：** `DONE`（2026-07-30，交付 experiments/pitch-detection/ 与 docs/experiments/pitch-detection-results.md；真实人声验证列为遗留风险，待 M1 后补测）

比较：

- YIN；
- 简单自相关方法；
- FFT 基线；
- 可行时比较轻量 TFLite 模型。

输入至少包括：

- 正弦波；
- 固定音阶；
- 男声；
- 女声；
- 静音；
- 白噪声；
- 说话声；
- 环境噪声；
- 削波音频。

测量：

- 音高误差；
- 未识别率；
- 错误识别率；
- 每帧处理时间；
- CPU 使用；
- 内存使用；
- 中端设备可行性。

交付：

```text
experiments/pitch-detection/
docs/experiments/pitch-detection-results.md
```

验收条件：

- 选定 MVP 音高检测方法；
- 给出选型理由；
- 定义适用频率范围；
- 定义无效帧过滤规则；
- 明确算法不能判断的内容。

---

### M-1.6 MVP 技术决策

**状态：** `DONE`（2026-07-30，交付 docs/experiments/spike-results.md、mvp-technical-decision.md 与 docs/decisions/ADR-001..003）

比较三种方向：

1. 音色推荐；
2. 音域推荐；
3. 综合推荐。

交付：

```text
docs/experiments/spike-results.md
docs/experiments/mvp-technical-decision.md
docs/decisions/ADR-001-mvp-recommendation-direction.md
docs/decisions/ADR-002-audio-recording-api.md
docs/decisions/ADR-003-pitch-detection-method.md
```

验收条件：

- 选择一个 MVP 主方向；
- 明确 MVP 非目标；
- 明确是否需要后端；
- 明确是否保存原始音频；
- 明确是否引入 TFLite；
- 明确主要技术风险。

---

## 5.4 M-1 退出条件

只有满足以下条件才能进入 M0：

- 仓库审计完成；
- 研究文档完成；
- AudioRecord Spike 完成；
- 音高检测 Spike 完成；
- MVP 方向确定；
- 高风险假设已经有验证方案；
- 暂未开始建设正式业务功能。

---

# 6. M0：MVP 与架构冻结

## 6.1 目标

把研究结果转化为明确、可测试的产品需求和工程架构。

---

## 6.2 任务

### M0.1 编写 SPEC

**状态：** `DONE`（2026-07-31，交付 SPEC.md v0.1.0，含 17 条 Given/When/Then 验收条件）

创建：

```text
SPEC.md
```

必须包含：

- 产品目标；
- 目标用户；
- 非目标；
- 用户流程；
- 功能需求；
- 异常流程；
- 推荐系统定义；
- 数据模型；
- Android 架构；
- 隐私和安全；
- 非功能需求；
- Given/When/Then 验收条件。

验收条件：

- 每个 MVP 功能都具有可测试验收标准；
- 明确结果置信度和失败状态；
- 明确不可靠音频不得生成正式结果；
- 明确原始音频的保存和删除策略。

---

### M0.2 编写架构文档

**状态：** `DONE`（2026-07-31，交付 ARCHITECTURE.md，8 个真实 Gradle 模块 + 逻辑边界）

创建：

```text
ARCHITECTURE.md
```

建议模块：

```text
app
core:common
core:model
core:audio
core:testing
data:local
data:songs
domain:recording
domain:analysis
domain:recommendation
feature:onboarding
feature:recording
feature:analysis
feature:recommendation
feature:history
feature:settings
```

小型 MVP 可以减少 Gradle Module 数量，但必须保持逻辑边界。

架构至少定义：

- UI 层；
- Domain 层；
- Data 层；
- Audio Engine；
- Recording Service；
- Analysis Pipeline；
- Recommendation Engine；
- Storage；
- Error Model；
- 日志策略；
- 依赖方向；
- 线程和协程调度策略。

---

### M0.3 定义数据模型

**状态：** `DONE`（2026-07-31，交付 docs/architecture/data-model.md，15 个模型 × 9 项字段属性）

至少定义：

- `RecordingSession`
- `RecordingConfig`
- `AudioQualityReport`
- `PitchFrame`
- `PitchTrack`
- `VocalRangeEstimate`
- `VoiceFeatureVector`
- `SongMetadata`
- `SongRangeProfile`
- `RecommendationScore`
- `RecommendationExplanation`
- `RecommendationResult`
- `UserFeedback`
- `UserSettings`
- `ConsentRecord`

每个字段必须记录：

- 类型；
- 单位；
- 合法范围；
- 是否可为空；
- 数据来源；
- 保存位置；
- 是否敏感；
- 保留时间。

---

### M0.4 建立测试策略

**状态：** `DONE`（2026-07-31，交付 TESTING.md + docs/testing/ 4 份文档）

创建：

```text
TESTING.md
docs/testing/test-fixture-manifest.md
docs/testing/device-matrix.md
docs/testing/manual-test-checklist.md
docs/testing/regression-suite.md
```

定义：

- 单元测试范围；
- 集成测试范围；
- UI 测试范围；
- 音频测试夹具；
- 真机测试要求；
- 性能目标；
- 发布回归测试。

---

### M0.5 细化剩余任务

**状态：** `DONE`（2026-07-31，交付 docs/plans/task-breakdown.md，126 个里程碑任务 + 16 个 Backlog 任务）

Coding Agent 根据最终 SPEC，对本计划中的任务进行二次细化。

每个新增任务必须包含：

```text
Task ID
状态
目标
前置依赖
涉及文件
实施步骤
测试步骤
验收标准
风险
回滚方式
```

不得改变已经批准的 Milestone 顺序，除非创建 ADR 说明原因。

---

## 6.3 M0 退出条件

- `SPEC.md` 已批准；
- `ARCHITECTURE.md` 已批准；
- 数据模型完整；
- 测试策略完整；
- MVP 范围冻结；
- 非 MVP 功能已移入 Backlog；
- 不存在阻塞性架构问题。

---

# 7. M1：Android 工程基线

## 7.1 目标

建立可以稳定构建、测试和持续集成的 Android 工程。

---

## 7.2 任务

### M1.1 初始化工程配置

**状态：** `DONE`（2026-07-31，8 模块 Gradle 工程 + Version Catalog + wrapper 8.9；Debug/Release 构建成功；模拟器冒烟通过）

配置：

- Kotlin；
- Gradle Kotlin DSL；
- Android Gradle Plugin；
- Version Catalog；
- Compose；
- Material 3；
- Java/Kotlin Toolchain；
- `minSdk`；
- `targetSdk`；
- Debug 和 Release Build Type。

验收条件：

- Debug 构建成功；
- Release 构建成功；
- 空应用可以在模拟器和真机启动。

---

### M1.2 建立代码质量工具

**状态：** `DONE`（2026-07-31，Lint/Detekt/Ktlint 统一 checkQuality 命令 + JaCoCo 覆盖率门禁 + 依赖版本检查/漏洞扫描）

配置：

- Android Lint；
- Detekt；
- Ktlint；
- 单元测试；
- 覆盖率报告；
- 依赖版本检查；
- 依赖漏洞扫描。

验收条件：

- 本地存在统一检查命令；
- CI 能执行同一套检查；
- 不允许通过关闭规则掩盖问题。

---

### M1.3 建立 CI

**状态：** `DONE`（2026-07-31，.github/workflows/ci.yml：PR 必选五项 + 覆盖率门禁 + 模拟器 job + Release + 依赖扫描）

CI 至少执行：

```text
assembleDebug
testDebugUnitTest
lintDebug
detekt
ktlintCheck
```

条件允许时增加：

- Instrumentation Test；
- Compose UI Test；
- Release Build；
- 依赖扫描。

验收条件：

- Pull Request 上自动执行；
- 失败会阻止合并；
- 结果可追踪。

---

### M1.4 建立通用基础设施

**状态：** `DONE`（2026-07-31，OperationResult/AppError/DispatcherProvider/Clock/Logger+脱敏/core:testing 工具/Fake 工厂；55 个单元测试通过）

实现：

- 统一错误模型；
- `Result` 或应用级 Operation Result；
- Dispatcher Provider；
- 时钟抽象；
- Logger 接口；
- Debug 日志；
- Release 日志脱敏；
- 测试工具模块；
- Fake 数据工厂。

---

### M1.5 建立目录与文档

**状态：** `DONE`（2026-07-31，docs/bugs/bug-log.md、M1-acceptance.md 模板、README/CHANGELOG/PRIVACY/SECURITY）

创建：

```text
docs/bugs/
docs/testing/
docs/decisions/
docs/milestones/
experiments/
```

创建：

```text
README.md
CHANGELOG.md
PRIVACY.md
SECURITY.md
```

---

## 7.3 M1 退出条件

- Debug 和 Release 均可构建；
- CI 通过；
- 静态检查通过；
- 单元测试框架可运行；
- 基础模块边界确定；
- 没有业务功能实现。

---

# 8. M2：应用外壳与导航

## 8.1 目标

实现不依赖真实音频算法的完整 UI 骨架。

---

## 8.2 页面范围

MVP 页面：

1. 启动页；
2. Onboarding；
3. 隐私和录音说明；
4. 首页；
5. 录音准备页；
6. 录音页；
7. 音频质量结果页；
8. 分析中页面；
9. 声音结果页；
10. 推荐列表；
11. 推荐详情；
12. 收藏；
13. 历史记录；
14. 设置；
15. 数据删除确认页。

---

## 8.3 任务

### M2.1 Navigation Compose

**状态：** `DONE`（2026-07-31，14 路由全注册 + 参数传递 + 返回栈约定 + Splash 分流；导航测试通过）

实现：

- 路由定义；
- 参数传递；
- Deep Link 策略，如需要；
- 返回栈；
- 恢复状态。

测试：

- 正常导航；
- 返回键；
- 重建 Activity；
- 无效参数。

---

### M2.2 Design System

**状态：** `DONE`（2026-07-31，色板/排版/间距/形状令牌 + 基础组件 + 五类状态组件）

实现：

- Typography；
- Spacing；
- Shape；
- Component；
- Loading；
- Empty State；
- Error State；
- Permission State；
- Audio Quality Warning。

不得在业务页面中重复硬编码样式。

---

### M2.3 Onboarding 与隐私说明

**状态：** `DONE`（2026-07-31，六项隐私说明 + 同意持久化 DataStore + 启动分流；ACC-1/2 测试通过）

展示：

- 为什么需要麦克风；
- 录音用于什么；
- 是否上传音频；
- 是否保存音频；
- 用户如何删除数据；
- 结果不是医学或专业诊断。

本阶段不请求真实权限，权限流程在 M3 完成。

---

### M2.4 Fake 数据流程

**状态：** `DONE`（2026-07-31，debug DI Map 多绑定注入 Fake；全流程页面串联；测试数据标记；Release 不含 Fake）

使用 Fake Repository 串联页面：

```text
首页
→ 录音准备
→ 模拟录音
→ 模拟分析
→ 模拟声音结果
→ 模拟推荐
```

Fake 数据必须明确标记为测试数据，不得进入 Release 构建。

---

### M2.5 UI 测试

**状态：** `DONE`（2026-07-31，13/13 通过：Onboarding 流程、导航、状态组件、删除确认）

覆盖：

- 首次启动；
- 已完成 Onboarding；
- 页面导航；
- Loading；
- Empty；
- Error；
- Fake 推荐列表；
- 数据删除确认弹窗。

---

## 8.4 M2 退出条件

- 完整 UI 骨架可运行；
- 所有 MVP 页面可导航；
- Fake 数据与生产数据边界明确；
- Compose UI 测试通过；
- 无麦克风和音频算法依赖。

---

# 9. M3：录音系统

## 9.1 目标

在支持的 Android 设备上稳定录制可用于后续分析的 PCM 音频。

---

## 9.2 任务

### M3.1 麦克风权限状态机

**状态：** `DONE`（2026-07-31，PermissionStateMachine 六状态 + UI 集成：请求/拒绝/永久拒绝/设置返回）

状态至少包括：

- NotRequested；
- Requesting；
- Granted；
- Denied；
- PermanentlyDenied；
- Unavailable。

处理：

- 首次请求；
- 拒绝；
- “不再询问”；
- 从系统设置返回；
- 权限在使用过程中被撤销。

---

### M3.2 Recording Foreground Service

**状态：** `DONE`（2026-07-31，前台服务+通知+停止动作；RecordingPort 通信桥；AudioFocus 焦点处理；后台录音验证通过）

实现：

- 启动；
- 停止；
- 通知；
- 服务绑定；
- App 切后台；
- Activity 重建；
- 服务异常；
- 用户从通知停止录音。

不得静默录音。

---

### M3.3 AudioRecord 封装

**状态：** `DONE`（2026-07-31，AndroidAudioRecorder（VOICE_RECOGNITION/44.1k/mono）+ 采样率降级链 + 错误映射 + FakeAudioRecorder）

实现：

- AudioRecord 初始化；
- 采样率选择；
- 单声道；
- PCM 编码；
- Buffer 读取；
- 开始和停止；
- 资源释放；
- 错误映射；
- 设备兼容降级。

封装为可替换接口：

```text
AudioRecorder
AndroidAudioRecorder
FakeAudioRecorder
```

---

### M3.4 录音状态机

**状态：** `DONE`（2026-07-31，RecordingStateMachine 八状态 + 倒计时 + 自动停止 + 中断标记）

状态建议：

```text
Idle
Preparing
Countdown
Recording
Paused
Stopping
Completed
Failed
```

根据实际产品决定是否保留 Pause。

所有状态变化必须可测试。

---

### M3.5 PCM/WAV 存储

**状态：** `DONE`（2026-07-31，WavFileWriter/Reader（与夹具同格式）+ RecordingFileManager + 启动残留清理）

实现：

- 临时 PCM；
- WAV Header；
- 时长计算；
- 文件大小限制；
- Cache 存储；
- 正常关闭；
- 异常清理；
- App 崩溃后的残留文件清理。

默认不永久保存原始录音。

---

### M3.6 音量反馈

**状态：** `DONE`（2026-07-31，VolumeMeter + 集中阈值 + ≤10Hz 节流 + 录音页音量条/削波/过低提示）

实时展示：

- 当前音量；
- 是否过低；
- 是否削波；
- 麦克风是否有输入。

UI 更新频率必须节流，不得每个采样点触发 Compose 重组。

---

### M3.7 录音测试

**状态：** `DONE`（2026-07-31，单元测试 100+ 通过；仪器测试 13/13；模拟器人工验证：录音+前台通知+后台5s+停止；真机矩阵待补充）

自动测试：

- 状态机；
- Fake Audio Stream；
- 正常停止；
- 初始化失败；
- 读取失败；
- 权限撤销；
- 服务异常。

人工测试：

- Pixel；
- Samsung；
- Xiaomi 或其他主流设备；
- 有线耳机；
- 蓝牙耳机；
- 外放伴奏；
- 来电或通知中断；
- 前后台切换。

---

## 9.3 M3 退出条件

- 可以稳定录制 15～30 秒；
- 录音过程中始终有前台通知；
- 权限异常有清晰反馈；
- 原始音频默认存入临时目录；
- 录音结束后资源正确释放；
- 录音测试和人工检查通过；
- 尚未向用户展示正式分析结果。

---

# 10. M4：音频质量检测

## 10.1 目标

在进入音高分析之前识别明显无效或低质量录音。

---

## 10.2 检测指标

MVP 至少包含：

- 录音时长；
- 静音比例；
- 平均 RMS；
- 峰值；
- 削波比例；
- 有效声音比例；
- 近似噪声水平；
- 可能的人声活动区间；
- 可分析帧数量。

---

## 10.3 任务

### M4.1 Audio Frame Pipeline

**状态：** `DONE`（2026-07-31，帧分割 2048/1024 + 帧统计（RMS/峰值/削波游程/ZCR）+ WavFileSource 主输入）

实现：

```text
PCM Input
→ Frame Split
→ Window
→ Frame Statistics
→ Quality Aggregation
```

必须支持从：

- 实时 PCM；
- WAV 测试文件；
- Fake Frame Source

读取数据。

---

### M4.2 静音和低音量检测

**状态：** `DONE`（2026-07-31，QualityConfig 集中阈值 Q-1~Q-5 + 静音/低音量/有效声音判定）

定义：

- 静音阈值；
- 低音量阈值；
- 最小有效声音时长；
- 最小有效帧比例。

阈值必须集中配置，不能散落在代码中。

---

### M4.3 削波检测

**状态：** `DONE`（2026-07-31，连续满幅≥3 判削波帧 + 削波比例门禁 + 短时峰值容忍）

检测：

- 连续满幅样本；
- 削波帧比例；
- 严重削波；
- 可接受的短时峰值。

---

### M4.4 质量报告

**状态：** `DONE`（2026-07-31，AudioQualityReport 全字段 + 六类警告门禁 + 优先级判定 + confidence）

输出：

```text
AudioQualityReport
```

包含：

- `isUsable`
- `confidence`
- `duration`
- `silenceRatio`
- `clippingRatio`
- `averageRms`
- `warnings`
- `recommendedAction`

---

### M4.5 质量失败 UX

**状态：** `DONE`（2026-07-31，QualityResultScreen 可用/失败双态 + 警告→文案映射 + 重录引导）

对应提示：

- 录音过短；
- 没有检测到声音；
- 声音太小；
- 环境过于嘈杂；
- 麦克风削波；
- 有效演唱片段不足；
- 建议重新录制。

不得对不可用录音继续生成正式推荐。

---

### M4.6 测试

**状态：** `DONE`（2026-07-31，10 个合成夹具 + 元数据 + 清单校验；55 测试全过；阈值标定文档）

夹具至少包括：

- 静音；
- 低音量；
- 正常音量；
- 白噪声；
- 严重削波；
- 过短录音；
- 部分静音；
- 纯说话；
- 测试音阶。

---

## 10.4 M4 退出条件

- 无效录音可以被稳定拒绝；
- 质量阈值可配置；
- 每种拒绝状态有明确提示；
- 测试夹具拥有来源和预期；
- 不合格音频不会进入正式分析。

---

# 11. M5：音高与音域分析

## 11.1 目标

从合格录音中生成可解释、有置信度的音高轨迹和音域估计。

---

## 11.2 任务

### M5.1 YIN 音高检测生产实现

**状态：** `DONE`（2026-07-31，YinPitchDetector 生产化：高通预滤波 + Double 精度 + 阈值 0.25 + 批量/取消；人声标定驱动修复）

将 Spike 中验证通过的实现重构为生产模块。

要求：

- 独立接口；
- 无 Android UI 依赖；
- 支持批处理；
- 可配置频率范围；
- 可配置置信度阈值；
- 支持取消；
- 支持测试注入。

---

### M5.2 音高后处理

**状态：** `DONE`（2026-07-31，PitchPostProcessor：过滤/八度修正/中值滤波/跳变过滤/最短片段 + PitchNotation 转换）

实现：

- 无效帧过滤；
- 低置信度过滤；
- Octave Error 近似处理；
- 中值滤波；
- 短时跳变过滤；
- 最短稳定音高片段；
- 频率转 MIDI Note；
- MIDI Note 转音名。

---

### M5.3 稳定音域估计

**状态：** `DONE`（2026-07-31，RangeStatistics P5/P95 分位 + VocalRangeEstimator（样本充足门禁 + 置信度））

不得直接把所有帧中的最小值和最大值作为用户音域。

实现：

- 异常值剔除；
- 分位数范围；
- 最低稳定音；
- 最高稳定音；
- 录音覆盖范围；
- 分析置信度；
- 样本是否足够。

---

### M5.4 舒适音区估计

**状态：** `DONE`（2026-07-31，ComfortRangeEstimator：分布/停留权重/稳定比例/边缘检查 + 裁剪 ⊆ 稳定区间）

基于：

- 音高分布；
- 停留时间；
- 稳定音符比例；
- 音量稳定性；
- 边缘音区样本数量。

输出：

- 舒适最低音；
- 舒适最高音；
- 主要演唱音区；
- 估计置信度。

必须明确：

> 这是本次录音中的估计，不代表用户完整生理音域。

---

### M5.5 音高稳定性

**状态：** `DONE`（2026-07-31，PitchStabilityMetrics：稳定片段比例/波动/长音波动/有效帧比例，无唱功分数）

计算：

- 稳定片段比例；
- 音高波动；
- 长音波动；
- 有效帧比例。

MVP 不直接输出“唱功分数”。

---

### M5.6 分析结果模型

**状态：** `DONE`（2026-07-31，VoiceAnalysisResult + AnalyzeRecordingUseCase 流水线编排（质量门禁短路 + 置信度分档 + 版本））

输出：

```text
VoiceAnalysisResult
```

包含：

- 录音质量；
- 检测音高帧；
- 稳定最低音；
- 稳定最高音；
- 舒适音区；
- 主要音区；
- 稳定性指标；
- 分析置信度；
- 警告；
- 模型或算法版本。

---

### M5.7 结果页面

**状态：** `DONE`（2026-07-31，VoiceResultScreen：音域/舒适区/稳定性通俗展示 + 本次录音估计声明 + 数据不足提示 + 置信度徽标）

向用户展示：

- 本次稳定音域；
- 本次舒适音区；
- 音高分布；
- 结果置信度；
- 数据不足提示；
- 重新录制入口；
- 通俗解释。

避免展示无法被普通用户理解的裸算法指标。

---

### M5.8 测试

**状态：** `DONE`（2026-07-31，合成全场景 + MIR-1K 真实人声（男/女声+真值标签，开源下载）+ 一致性 + 性能；已知限制：带伴奏人声子谐波锁定记录为 M10 优化项）

测试：

- 纯音；
- 音阶；
- 多个采样率；
- 男声；
- 女声；
- 噪声；
- 跑调；
- 八度跳变；
- 极少有效帧；
- 边界频率；
- 重复分析一致性。

---

## 11.3 M5 退出条件

- 已验证音频可输出稳定音高轨迹；
- 音域不是简单极值；
- 结果包含置信度；
- 数据不足时拒绝过度推断；
- 中端设备处理耗时满足 SPEC；
- 测试和人工样本验证通过。

---

# 12. M6：歌曲数据系统

## 12.1 目标

建立可靠、可验证、可版本化的歌曲特征数据。

---

## 12.2 MVP 歌曲字段

每首歌曲至少包含：

- Song ID；
- 歌曲名；
- 歌手；
- 语言；
- 风格；
- 原调；
- 最低音；
- 最高音；
- 主要音区；
- 音域跨度；
- 高音持续负担；
- 长音负担；
- 跳进难度；
- 节奏难度；
- 总体难度；
- 推荐变调范围；
- 试听或外部链接；
- 数据来源；
- 数据可信度；
- 数据版本。

---

## 12.3 任务

### M6.1 数据 Schema

使用 Kotlin 数据类和可验证的 JSON Schema 或等价校验方式。

所有音高字段统一使用内部标准，例如 MIDI Note。

---

### M6.2 数据导入工具

实现：

- JSON 或 CSV 导入；
- 字段校验；
- 重复检查；
- 音高范围检查；
- 数据来源检查；
- 版本检查；
- 错误报告。

导入工具和 App Runtime 解耦。

---

### M6.3 MVP 数据集

建立小型高质量数据集。

早期建议：

- 50～200 首；
- 覆盖不同音区；
- 覆盖男女歌手；
- 覆盖不同语言和风格；
- 优先准确，不追求数量。

不得在没有可靠来源时批量虚构歌曲音域。

---

### M6.4 Room 存储

实现：

- Song Entity；
- DAO；
- 数据版本；
- 初始数据导入；
- 数据升级；
- 搜索；
- 筛选；
- 收藏关系。

---

### M6.5 数据测试

覆盖：

- Schema；
- 导入；
- 重复 ID；
- 无效音高；
- 最低音高于最高音；
- 缺失来源；
- Room Migration；
- 数据版本回滚策略。

---

## 12.4 M6 退出条件

- 存在可用的 MVP 歌曲数据集；
- 所有歌曲通过自动校验；
- 每条关键数据存在来源或可信度声明；
- 数据可以安全升级；
- 推荐引擎无需读取硬编码歌曲列表。

---

# 13. M7：推荐引擎

## 13.1 目标

基于用户分析结果和歌曲特征生成透明、可测试、可解释的推荐。

---

## 13.2 推荐流程

```text
VoiceAnalysisResult
+ UserPreferences
+ SongMetadata
→ Candidate Filter
→ Key Shift Evaluation
→ Feature Scoring
→ Ranking
→ Explanation Generation
→ RecommendationResult
```

---

## 13.3 任务

### M7.1 候选过滤

过滤：

- 语言；
- 用户排除的风格；
- 明显超出可调整音域的歌曲；
- 数据不完整歌曲；
- 不支持的歌曲版本。

不得以歌手性别作为硬性过滤条件。

---

### M7.2 变调计算

对每首候选歌曲计算：

- 原调匹配度；
- 降半音数量；
- 升半音数量；
- 变调后的最低音；
- 变调后的最高音；
- 变调后的主要音区；
- 是否超过合理变调范围。

---

### M7.3 评分模型

MVP 建议包含：

- `RangeFit`
- `TessituraFit`
- `HighNoteBurdenFit`
- `DifficultyFit`
- `PitchStabilityFit`
- `PreferenceFit`
- `ConfidenceAdjustment`

所有权重集中配置并记录版本。

不得将低置信度用户分析当作高精度输入。

---

### M7.4 推荐解释

解释必须由实际评分特征生成，例如：

- 大部分旋律位于你的舒适音区；
- 原调最高音略高，降低 1 个半音后更适合；
- 这首歌持续高音较少；
- 旋律跳进较少，适合当前稳定性；
- 这首歌与你选择的语言和风格偏好一致。

禁止无数据依据的文案。

---

### M7.5 无结果降级

当不存在高匹配歌曲时：

- 扩大风格范围；
- 建议合理变调；
- 展示“接近匹配”；
- 说明为什么匹配度有限；
- 不伪造高分推荐。

---

### M7.6 推荐测试

覆盖：

- 完全匹配；
- 最高音超出；
- 最低音超出；
- 主要音区不匹配；
- 可通过降调匹配；
- 无可用候选；
- 低置信度分析；
- 相同分数稳定排序；
- 权重版本；
- 推荐解释与分数一致。

---

## 13.4 M7 退出条件

- 推荐结果可重复；
- 排序逻辑可测试；
- 推荐理由可追溯到实际数据；
- 支持升降调建议；
- 低置信度输入有降级处理；
- 没有歌曲时有合理空状态。

---

# 14. M8：完整用户体验

## 14.1 目标

把录音、分析、推荐、收藏、历史和反馈串联成完整 MVP。

---

## 14.2 任务

### M8.1 录音到分析流程

实现：

```text
准备
→ 权限
→ 倒计时
→ 录音
→ 质量检测
→ 分析
→ 结果
```

要求：

- 支持取消；
- 支持重录；
- 支持分析失败后恢复；
- 防止重复提交；
- 支持 Activity 重建。

---

### M8.2 分析到推荐流程

实现：

- 查看声音结果；
- 查看推荐；
- 查看推荐理由；
- 查看建议变调；
- 返回分析结果；
- 重新测试。

---

### M8.3 收藏

实现：

- 收藏歌曲；
- 取消收藏；
- 收藏列表；
- 收藏状态同步；
- 数据库测试。

---

### M8.4 历史记录

默认只保存：

- 分析摘要；
- 时间；
- 音域结果；
- 推荐结果引用；
- 算法版本。

除非用户主动选择，否则不保存原始音频。

---

### M8.5 用户反馈

允许反馈：

- 适合唱；
- 太高；
- 太低；
- 太难；
- 不喜欢该风格；
- 推荐理由不准确。

第一版可以保存反馈，但不得在没有设计和测试时自动大幅修改算法权重。

---

### M8.6 错误恢复

覆盖：

- 权限失败；
- 录音失败；
- 文件写入失败；
- 质量失败；
- 分析取消；
- 分析崩溃；
- 数据库失败；
- 推荐数据为空；
- App 被系统重建；
- 存储不足。

---

### M8.7 E2E 测试

使用 Fake Audio Input 完成：

- 首次启动；
- Onboarding；
- 授权；
- 录音；
- 质量通过；
- 分析；
- 推荐；
- 收藏；
- 反馈；
- 历史记录；
- 删除记录。

---

## 14.3 M8 退出条件

- 用户可以完整完成一次测试；
- 失败后可以恢复；
- 无阻塞性 UX 问题；
- Fake Audio E2E 测试通过；
- 真机手工流程通过；
- 所有结果均明确为本次录音估计。

---

# 15. M9：隐私、安全与数据管理

## 15.1 目标

确保音频和声音特征按敏感数据进行保护。

---

## 15.2 任务

### M9.1 数据清单

记录：

- 收集的数据；
- 处理目的；
- 保存位置；
- 保留时间；
- 删除方式；
- 是否上传；
- 是否与第三方共享。

更新：

```text
PRIVACY.md
```

---

### M9.2 原始录音生命周期

实现：

- 默认临时保存；
- 分析完成后删除；
- 失败后清理；
- App 启动时清理过期缓存；
- 用户主动保存时明确提示；
- 删除失败时记录安全错误。

---

### M9.3 数据删除

用户可以：

- 删除单条历史记录；
- 删除全部历史记录；
- 删除收藏；
- 删除设置；
- 删除缓存音频；
- 重置应用数据。

删除流程必须有测试。

---

### M9.4 安全检查

检查：

- Exported Component；
- Intent 输入；
- FileProvider；
- PendingIntent；
- Service 权限；
- 日志脱敏；
- Debug 工具是否进入 Release；
- 数据库文件；
- 网络安全配置；
- 第三方 SDK；
- API Key；
- 依赖漏洞。

---

### M9.5 Play Store 合规

准备：

- 麦克风权限说明；
- Foreground Service 用途；
- 数据安全表单信息；
- 隐私政策；
- 数据删除说明；
- 不面向未成年人的默认声明，如适用。

---

## 15.3 M9 退出条件

- 没有静默录音路径；
- 原始录音默认不永久保存；
- 用户可以删除所有个人数据；
- Release 日志不包含敏感信息；
- 安全审计无高严重度问题；
- 隐私文档与实际代码一致。

---

# 16. M10：稳定性与性能优化

## 16.1 目标

确保应用可以在目标 Android 设备上稳定运行。

---

## 16.2 任务

### M10.1 性能基准

测量：

- 冷启动；
- 首屏时间；
- 录音期间 CPU；
- 分析期间 CPU；
- 峰值内存；
- 30 秒音频分析时间；
- 电量消耗；
- APK/AAB 大小；
- 数据库启动时间。

不得在没有基准数据时盲目优化。

---

### M10.2 音频性能优化

按顺序优化：

1. 减少对象分配；
2. 复用 Buffer；
3. 调整 Frame Size 和 Hop Size；
4. 使用批处理；
5. 调整协程调度；
6. 避免 UI 高频刷新；
7. 必要时评估 C++/NDK。

只有 Kotlin 方案无法达到 SPEC 时，才能通过 ADR 引入 NDK。

---

### M10.3 设备矩阵测试

至少覆盖：

- 一个低端或较旧设备；
- 一个中端设备；
- 一个 Pixel 或接近原生 Android 的设备；
- 一个 Samsung；
- 一个中国厂商设备，如 Xiaomi、OPPO 或 vivo；
- 不同 Android 版本；
- 有线耳机；
- 蓝牙设备；
- 手机内置麦克风。

---

### M10.4 稳定性测试

测试：

- 连续录制；
- 连续分析；
- 反复进入退出；
- 屏幕旋转；
- 低内存恢复；
- 进程重建；
- 存储不足；
- 长时间后台；
- 权限动态撤销；
- 快速重复点击；
- 音频焦点中断。

---

### M10.5 完整回归

运行：

- Unit Tests；
- Integration Tests；
- Compose UI Tests；
- Instrumentation Tests；
- E2E；
- Lint；
- Detekt；
- Ktlint；
- Release Build；
- 手工回归检查表。

---

### M10.6 Bug 清零

发布阻塞级别：

- P0：全部修复；
- P1：全部修复；
- P2：必须评估并记录；
- P3：可进入 Backlog。

所有 Bug 保存至：

```text
docs/bugs/bug-log.md
```

修复 Bug 必须先添加失败测试。

---

## 16.3 M10 退出条件

- 达到 SPEC 性能指标；
- 目标设备矩阵通过；
- 无 P0/P1 Bug；
- Release 构建稳定；
- 完整回归通过；
- 已记录所有剩余 P2/P3 问题。

---

# 17. M11：Beta 与 Google Play 发布

## 17.1 目标

形成可供内部测试、封闭测试和正式发布的 Android App Bundle。

---

## 17.2 任务

### M11.1 Release 配置

完成：

- Application ID；
- Version Code；
- Version Name；
- 签名配置；
- ProGuard/R8；
- Resource Shrinking；
- Release 日志关闭；
- Debug 功能隔离；
- Crash Reporting 配置，如采用；
- Mapping 文件保存。

---

### M11.2 商店材料

准备：

- 应用名称；
- 简短描述；
- 完整描述；
- 图标；
- Feature Graphic；
- 手机截图；
- 隐私政策；
- 麦克风权限用途；
- 数据安全说明；
- 内容分级；
- 支持邮箱；
- 删除数据方式。

不得宣传未经验证的准确率。

---

### M11.3 Internal Testing

检查：

- 安装；
- 更新；
- 首次启动；
- 麦克风权限；
- 录音通知；
- 分析；
- 推荐；
- 删除数据；
- 崩溃；
- ANR；
- 设备兼容。

---

### M11.4 Closed Beta

收集：

- 录音完成率；
- 有效录音率；
- 分析失败率；
- 推荐点击率；
- 用户适合度反馈；
- 崩溃率；
- ANR；
- 设备型号；
- 用户对结果解释的理解程度。

不得采集原始音频用于研究，除非新增明确同意流程。

---

### M11.5 发布决策

根据 Beta 数据判断：

- 是否正式发布；
- 是否继续优化音域推荐；
- 是否加入更多歌曲；
- 是否引入后端；
- 是否研究音色 embedding；
- 是否开展真人算法评估。

创建：

```text
docs/release/release-readiness.md
docs/release/known-issues.md
docs/release/rollback-plan.md
```

---

## 17.3 M11 退出条件

- AAB 可正常签名；
- Internal Testing 通过；
- 商店材料完整；
- 隐私声明与代码一致；
- 发布检查表通过；
- 已准备回滚版本；
- 产品负责人作出明确发布决定。

---

# 18. Bug 修复工作流

每个 Bug 必须按照以下顺序处理：

1. 分配 Bug ID；
2. 记录设备和 Android 版本；
3. 记录 App 版本；
4. 写出最小复现步骤；
5. 保存错误日志和堆栈；
6. 确定预期结果；
7. 确定实际结果；
8. 判断影响范围；
9. 定位根因；
10. 在修复前添加失败测试；
11. 实施最小修复；
12. 运行相关测试；
13. 运行回归测试；
14. 检查隐私和性能影响；
15. 更新 Bug 日志；
16. 必要时更新 SPEC、PLAN 或 ADR。

Bug 模板：

```text
Bug ID:
Title:
Severity:
Environment:
App Version:
Device:
Android Version:
Preconditions:
Steps to Reproduce:
Expected:
Actual:
Logs:
Root Cause:
Fix:
Changed Files:
Tests Added:
Regression Tests:
Privacy Impact:
Performance Impact:
Resolved Version:
```

---

# 19. Definition of Done

一个 Task 只有满足以下条件才可以标记为 `DONE`：

- 功能符合 SPEC；
- 代码已经完成；
- 代码结构符合架构边界；
- 单元测试已添加；
- 相关集成或 UI 测试已添加；
- 所有相关测试通过；
- 静态检查通过；
- 没有吞掉异常；
- 错误状态有用户反馈；
- 日志不包含敏感信息；
- 文档已更新；
- `PLAN.md` 状态已更新；
- 没有未说明的临时实现；
- 回滚方式明确；
- 已说明人工验证需求。

---

# 20. Coding Agent 每次输出格式

每次工作开始前输出：

## Current Milestone

当前 Milestone 和完成比例。

## Current Task

- Task ID；
- 任务名称；
- 当前状态；
- 依赖是否完成。

## Goal

说明本次只完成什么。

## Pre-check

- 将读取的文件；
- 将修改的文件；
- 将运行的测试；
- 已知风险。

执行完成后输出：

## Implementation

- 实际完成内容；
- 修改文件；
- 技术决策；
- 与原计划的差异。

## Test Results

列出真实执行的命令和结果。

禁止只写“测试通过”，必须列出实际测试命令。

## Documentation Updates

列出更新的文档。

## Risks and Remaining Issues

说明：

- 尚未验证的部分；
- 设备相关风险；
- 算法限制；
- 技术债。

## Plan Update

更新当前 Task 状态。

## Next Task

只给出一个满足依赖关系的下一个 Task。

---

# 21. 首次执行指令

Coding Agent 首次收到本计划时，必须从 `M-1.1` 开始。

首次执行仅允许完成：

1. 检查仓库；
2. 输出仓库结构；
3. 确认现有 Android 配置；
4. 检查已有代码和测试；
5. 识别未提交修改；
6. 创建 `docs/research/repository-audit.md`；
7. 更新 `M-1.1` 状态。

首次执行不得：

- 初始化新的完整架构；
- 修改业务代码；
- 开发录音功能；
- 引入大型依赖；
- 创建推荐算法；
- 创建正式数据库；
- 将 M-1 的所有任务一次完成；
- 跳到 M1 或之后的 Milestone。

完成 `M-1.1` 后，下一个任务应为 `M-1.2 学术与技术研究`。

---

# 22. Backlog：MVP 之后再评估

以下功能不属于默认 MVP：

- 专业声部分类；
- 医学嗓音检测；
- 声带健康诊断；
- 歌手身份识别；
- 与明星声音相似度排名；
- 实时唱歌评分；
- 实时音准纠正；
- 实时伴奏变调；
- 自动分离人声和伴奏；
- 云端保存完整录音；
- 社交社区；
- 用户翻唱发布；
- AI 声音克隆；
- 复杂音色 embedding 推荐；
- 协同过滤；
- Learning to Rank；
- 付费订阅；
- 广告系统；
- iOS 版本；
- Flutter 或 Kotlin Multiplatform 重构。

任何 Backlog 功能进入开发前，必须：

1. 明确用户价值；
2. 明确数据和隐私要求；
3. 编写 SPEC 变更；
4. 创建 ADR；
5. 增加新的 Milestone；
6. 获得明确批准。
