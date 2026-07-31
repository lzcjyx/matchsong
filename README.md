# matchsong

> 音域推荐 Android 应用：用户录制约 15-30 秒演唱，应用在端侧分析本次录音的稳定演唱音区、舒适音区与音高稳定性，推荐音域适合用户演唱的歌曲，并给出可解释的推荐理由（SPEC.md §1）。

- **当前里程碑：** M1（Android 工程基线）进行中
- **状态：** 工程基线阶段，**无业务功能实现**（PLAN §7.3 退出条件「没有业务功能实现」）
- **产品规格：** 见 `SPEC.md`（MVP 范围已冻结）

## 技术栈

| 类别 | 选型 | 版本 |
|---|---|---|
| 语言 | Kotlin | 2.1.0 |
| UI | Jetpack Compose（Material 3，BOM） | 2024.12.01 |
| 构建 | AGP / Gradle Wrapper | 8.7.3 / 8.9 |
| JDK | Temurin | 17 |
| DI | Hilt | 2.52 |
| 存储 | Room / DataStore | 2.6.1 / 1.1.1 |
| 导航 | Navigation Compose | 2.8.5 |
| 音频采集 / 分析 | AudioRecord（ADR-002）/ 纯 Kotlin YIN（ADR-003） | — |
| 质量工具 | Android Lint / Detekt / Ktlint / JaCoCo / OWASP Dependency-Check | 版本已声明，配置随 M1.2 落地 |

平台：**minSdk 26**（Android 8.0），**targetSdk / compileSdk 36**。依赖版本统一管理于 `gradle/libs.versions.toml`。

## 模块结构（8 个 Gradle 模块）

| 模块 | 职责 |
|---|---|
| `:app` | UI（Compose）+ 导航 + DI 装配；feature:onboarding / recording / analysis / recommendation / history / settings 与 design、di 为包级逻辑边界 |
| `:domain` | 领域层（纯 Kotlin）：recording / analysis / recommendation 三域，用例 + 状态机 + 统计算法 + Port 接口 |
| `:data:local` | Room + DataStore + 文件缓存，实现 domain 的 Port 接口 |
| `:data:songs` | 歌曲数据集导入 / 校验 / 版本（纯 Kotlin） |
| `:core:common` | 错误模型、DispatcherProvider、Clock、Logger 接口（纯 Kotlin） |
| `:core:model` | 纯 Kotlin 数据模型（无依赖） |
| `:core:audio` | 音频引擎：api / algorithm 子包纯 Kotlin + android 实现（AudioRecord、YIN、质量检测、前台服务） |
| `:core:testing` | 测试工具（Fake 数据工厂、音频夹具），仅 debug / test 引用，绝不进入 Release（FR-SHELL-3） |

依赖方向：`feature → domain → core`，`data` 实现 domain 接口；UI 不依赖 Audio Engine 实现类。模块依赖图见 `ARCHITECTURE.md` §4.2，模块划分与合并理由见 §3。

## 环境要求

- **JDK 17（必须）**：系统默认 java 为 1.8，Kotlin 2.1 要求 JDK 17+。构建前设置 `JAVA_HOME`：
  - 本机：`JAVA_HOME=D:/scoop/apps/temurin17-jdk/current`
- **Android SDK 36**：本机位于 `D:/androidsdk`，已写入 `local.properties` 的 `sdk.dir`（该文件不入库，见 `.gitignore`）。
- **Gradle**：Wrapper 8.9 已内置（`gradlew.bat` / `gradlew`），无需系统安装 Gradle。

## 常用命令

Windows 使用 `./gradlew.bat`，macOS/Linux 使用 `./gradlew`：

| 命令 | 说明 |
|---|---|
| `./gradlew.bat :app:assembleDebug` | 构建 Debug APK |
| `./gradlew.bat :app:assembleRelease` | 构建 Release APK（当前 debug 签名占位，M11.1 正式化） |
| `./gradlew.bat testDebugUnitTest` | 运行 JVM 单元测试（首个冒烟测试随 M1.2-2 落地） |
| `./gradlew.bat checkQuality` | 静态检查聚合命令（= lintDebug + detekt + ktlintCheck），随 M1.2-1 落地 |
| `./gradlew.bat dependencyUpdates` | 依赖版本检查（M1.2-3 落地） |
| `./gradlew.bat dependencyCheckAnalyze` | 依赖漏洞扫描 OWASP Dependency-Check（M1.2-3 落地） |
| `./gradlew.bat jacocoTestReport` | JaCoCo 覆盖率报告（M1.2-2 落地） |

> 标注「M1.2-x 落地」的命令当前尚未配置，落地后本条说明移除。

## 文档导航

| 文档 | 内容 |
|---|---|
| `SPEC.md` | 产品需求规格：功能需求（FR-*）、17 条验收条件（ACC-*）、隐私（§10）、非功能（§11）、置信度（§13） |
| `ARCHITECTURE.md` | 架构设计：模块划分（§3）、依赖图（§4.2）、日志脱敏（§13）、线程调度（§14） |
| `TESTING.md` | 测试策略与分层 |
| `PLAN.md` | 里程碑计划（M-1 ~ M11）与执行规则 |
| `docs/plans/task-breakdown.md` | 126 个细化任务 + Backlog |
| `docs/architecture/data-model.md` | 15 个数据模型：字段属性、存储映射、敏感数据处理（§4） |
| `docs/decisions/` | ADR-001..003 技术决策 |
| `docs/testing/` | 测试夹具清单 / 设备矩阵 / 手工测试清单 / 回归套件 |
| `docs/bugs/` | Bug 日志（PLAN §18 模板） |
| `docs/milestones/` | 里程碑验收记录（M-1 / M0 已完成，M1 模板待验收） |
| `docs/research/` · `docs/experiments/` · `experiments/` | 研究与 Spike 工程（M-1 产出） |
| `PRIVACY.md` · `SECURITY.md` · `CHANGELOG.md` | 隐私说明（初稿） / 安全说明（初稿） / 变更日志 |

## 状态说明

当前处于 M1 工程基线阶段：Gradle 工程骨架（8 模块）、版本目录、标准目录与基础文档已建立；业务功能（录音 / 分析 / 推荐）尚未实现，自 M2 起按 `docs/plans/task-breakdown.md` 逐步落地。
