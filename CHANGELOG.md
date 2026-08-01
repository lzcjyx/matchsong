# 变更日志（Changelog）

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 风格。当前处于预发布阶段（App `versionName` 0.1.0，未发布任何版本），全部变更集中在 `[Unreleased]`；正式发布后按版本归档。

## [Unreleased]

### 新增（Added）

- **M-1 仓库检查与研究**
  - 仓库审计：`docs/research/repository-audit.md`（空项目零起点结论）。
  - 学术与技术研究：`docs/research/academic-research.md`、`android-technical-feasibility.md`、`source-register.md`（29 条来源）。
  - 竞品研究：`docs/research/competitor-research.md`（17 个产品）、`product-opportunities.md`。
  - AudioRecord / MediaRecorder Spike：`experiments/audio-record/` + `docs/experiments/audio-recording-spike-results.md`（选型 AudioRecord，ADR-002）。
  - 音高检测 Spike：`experiments/pitch-detection/`（11 项测试全过）+ `docs/experiments/pitch-detection-results.md`（选型 YIN，ADR-003）。
  - MVP 技术决策：`docs/experiments/spike-results.md`、`mvp-technical-decision.md`、`docs/decisions/ADR-001..003`。

- **M0 MVP 与架构冻结**
  - `SPEC.md`：产品需求规格，17 条 ACC 验收条件。
  - `ARCHITECTURE.md`：8 个 Gradle 模块 + 逻辑边界、依赖方向、日志策略、线程调度。
  - `docs/architecture/data-model.md`：15 个数据模型（类型/单位/范围/敏感/保留时间等 9 项属性）。
  - `TESTING.md` + `docs/testing/`：测试策略、夹具清单、设备矩阵、手工测试清单、回归套件。
  - `docs/plans/task-breakdown.md`：126 个细化任务 + 16 项 Backlog。

- **M1 Android 工程基线**
  - Gradle 工程骨架：Wrapper 8.9、Version Catalog（AGP 8.7.3 / Kotlin 2.1.0 / KSP 2.1.0-1.0.29）、JDK 17 工具链、8 个模块（app / domain / data:local / data:songs / core:common / core:model / core:audio / core:testing）。
  - `:app` 空 Compose 应用（单 Activity、Material 3、minSdk 26 / targetSdk 36、Hilt / KSP）。
  - 标准目录与基础文档：`docs/bugs/bug-log.md`（PLAN §18 模板）、`docs/milestones/M1-acceptance.md`（验收模板）、`README.md`、`CHANGELOG.md`、`PRIVACY.md`、`SECURITY.md`。
  - 质量工具版本已声明于版本目录（Detekt / Ktlint / JaCoCo / OWASP Dependency-Check / Gradle Versions），配置与统一检查命令 `checkQuality` 随 M1.2 落地。

- **M9 隐私、安全与数据管理**
  - 数据清单审计完成，`PRIVACY.md` 定稿 v1.0（与 Room 5 表 / DataStore 2 文件 / 文件缓存逐一核对）。
  - 原始录音生命周期接通（FR-PRIV-1/ACC-14）：分析完成、重录、取消、失败、服务销毁均触发 `RecordingSessionRunner.cleanupSessionFiles()`；启动残留清理沿用 M3.5-2。
  - 数据删除全链路（FR-HX-4/FR-PRIV-5/ACC-15）：`DeleteAllDataUseCase`（历史/收藏/反馈/设置/同意/缓存全清，尽力执行）+ `SettingsViewModel`/`SettingsScreen` 全部删除操作 UI（粒度删除 + 重置应用回首次启动）。
  - `SettingsRepository.clear()`（DataStore 清空）与 `RecordingFileCleaner.clearAll()`（缓存目录清空）Port 扩展及实现。
  - 安全检查（M9.4）：`allowBackup=false`（Room 敏感特征禁备份外泄）+ `network_security_config.xml` 禁明文；`AndroidLogLogger` 全量脱敏（含堆栈）；`RecordingSessionRunner`/`MatchSongApplication` 统一注入 Logger；修复 M8 遗留的破损默认包 import 与 ktlint 违规（HistoryItem/FavoriteToggleViewModel）。
  - Play Store 合规材料初稿：`docs/compliance/play-store-materials.md`（数据安全表单、权限声明、隐私政策、删除说明、未成年人声明）。
  - `FeedbackRepository` 补 Hilt 绑定（M8.5 数据层存在但 UI 未接线——孤儿 `FeedbackSheet` 列为遗留风险）。

### 安全（Security）

- 新增 `PRIVACY.md` 初稿：明确 MVP 无网络权限、无后端、无 API Key；不上传原始音频与声音特征；录音分析完成后删除（FR-PRIV-1/3）；删除流程全链路可测（FR-PRIV-5）。M9.1 数据清单落地后定稿。
- 新增 `SECURITY.md` 初稿：漏洞报告渠道、依赖扫描策略（OWASP Dependency-Check，M1.2-3 落地）、Release 日志脱敏原则（FR-PRIV-4，ARCHITECTURE.md §13）。
- 合规细化计划：M9（数据清单与同意管理）、M11（Play Store 数据安全表单、隐私政策、麦克风权限用途说明）。
- **M9（2026-08-01）**：`PRIVACY.md` 定稿 v1.0；`SECURITY.md` 补 M9.4 安全检查记录（Exported Component/Intent/FileProvider/日志/备份/网络配置/依赖漏洞逐项结论）；`allowBackup=false` + 禁明文网络配置；音频/应用日志统一经 `LogRedactor` 脱敏（含异常堆栈）。

### 已修改（Changed）

- （当前无——预发布阶段无历史版本可改）
