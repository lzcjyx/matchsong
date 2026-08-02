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

- **M10 稳定性与性能优化**
  - 性能基准（M10.1）：`docs/experiments/m10-baselines.md` + `PerfBaselineTest`（设备端 30s 分析耗时/内存）；模拟器实测：30s 分析 2795ms（目标 ≤10s）、PSS 136MB（目标 ≤200MB）、冷启动 ~2.0s（目标 ≤3s）、release APK 1.58MB。
  - 音频性能优化（M10.2）：VolumeMeter 实例复用、PCM 批量编码写盘（缓冲复用）、ZCR 均值主循环累加；分析耗时 2866→2795ms；评估无需 NDK（PLAN §2.2 门禁不触发）。
  - 稳定性测试（M10.4）：`StabilityTest` 快速导航 10 轮 + 屏幕旋转；回归捕获并修复 P0（导航参数 nullable Int 启动崩溃）。
  - Bug 清零（M10.6）：`docs/bugs/bug-log.md` 全量三分类（P0×1/P1×1 修复；P2/P3×10 记录）；**反馈 UI 接线（BUG-001）与详情页真实数据（BUG-002）修复**——详情页由 M2 占位 Fake 数据切换为真实推荐项（导航参数传递），新增反馈入口（六类反馈，仅保存不调权重）。
  - 设备矩阵（M10.3）：模拟器行实测更新；真机矩阵硬件阻塞（BUG-004 DEFERRED，M11 发布前补测）。

- **M11 Beta 与 Google Play 发布**
  - Release 配置（M11.1）：release 签名（keystore.properties + matchsong-release.keystore，均不入库；缺配置回退 debug 签名）+ R8 规则（kotlinx-serialization）+ `isShrinkResources` + Mapping 保存；AAB 3.0MB 签名验证通过。
  - Release 冒烟（M11.3）：安装/同签名更新/首次启动（Onboarding 正常渲染，R8 无崩溃）/权限/崩溃/ANR 验证；检查表 docs/testing/internal-testing-checklist.md。
  - 商店材料（M11.2）：应用名/描述/权限用途/数据安全/删除方式（play-store-materials.md §7-8）；合规红线：不宣传未经验证的准确率。
  - Closed Beta 指标方案（M11.4）：docs/release/closed-beta-metrics.md（9 项指标 + 不采集原始音频红线）。
  - 发布决策文档（M11.5）：docs/release/release-readiness.md（NOT_READY，阻塞项 4 条）/ known-issues.md（KI-1..11）/ rollback-plan.md（Play 轨道回滚 + Room 降级策略）。
  - 验收：docs/milestones/M11-acceptance.md（附条件通过——真机矩阵与发布决定为产品/硬件责任）。

- **真机反馈修复（2026-08-01，BUG-013/014/015）**
  - 倒计时修复：RecordingPort 新增 countdownSeconds 流，UI 渲染真实 3→2→1（原硬编码"倒计时 3…"）。
  - 白屏修复：stop() 先落盘再宣布 COMPLETED（消除 lastWavFile 竞态）+ 质量页 wavFile 缺失防御性错误态。
  - 语音干扰过滤：后处理时间间隔分段（150ms）+ 稳定片段比例门禁（<0.3 判语音为主，按数据不足处理，ACC-9）；合成测试 + MIR-1K 真实人声回归通过。
  - release APK 重建（1.34MB）并冒烟通过。

- **BUG-016 曲库运行时装载（2026-08-01，P1 修复）**
  - 发现：内置曲库 JSON 从未在运行时导入——真机推荐必然空结果（此前被 BUG-014 白屏掩盖）。
  - 修复：数据集移入 `data/songs/src/main/assets/` + `MatchSongApplication` 启动幂等导入（版本比对/事务替换）；失败仅记日志不阻塞。
  - 验证：`SongCatalogSeedTest`（真实 DB 50 首）+ release 冒烟 logcat"歌曲目录就绪：50 首"。

- **BUG-017 录音文件不可用 + BUG-018 联网歌曲包（2026-08-01）**
  - BUG-017（P1）：服务销毁时 `release()` 置空实例并删除录音文件（M9.2 误加）→ 必现"录音文件不可用"；改为幂等收尾，单例可复用、文件删除归分析流程。
  - BUG-018：联网歌曲包——HTTPS 下载 JSON（15s 超时/5MB 上限/零新依赖）→ 复用导入管线（版本事务替换）；设置页歌曲包区块；启动内置导入仅空库执行；示例周杰伦包（song-packs/）。
  - 验证：MockWebServer/导入器测试 + 模拟器端到端（下载→校验→"导入成功：8 首（替换原曲库）"）。

- **BUG-019/020/021 录音链路根治（2026-08-01，子代理评审 + 复现测试）**
  - BUG-019（P1）：录音机从未启动（recorder.start 无调用方）→ 零音频帧空 WAV；runner.start 补调用 + 失败映射 + 落盘收尾归采集协程（消除并发写文件竞态）。
  - BUG-020（P1）：FlowSessionViewModel 实际未 Activity 作用域（每路由独立实例 → wavFile 跨页不可见）；二次录音被 IDLE 门禁拦截 + 终态回放误触发；已修复（hiltViewModel(Activity) + 重启门禁放宽 + 防回放守卫）。
  - BUG-021（P2）：重试 popUpTo 失效致重复压栈 + 准备页自动前进弹回；统一 popUpTo(PREPARE){inclusive} + 授权后显式按钮。
  - 验证：RecordingHandoffTest（真实 runner 链路：有效 WAV + 二次重启）+ 仪器测试 24/24 + release 端到端冒烟（录音→停止→质量页正常，模拟器无音频正确报"没有检测到声音"）。

### 安全（Security）

- 新增 `PRIVACY.md` 初稿：明确 MVP 无网络权限、无后端、无 API Key；不上传原始音频与声音特征；录音分析完成后删除（FR-PRIV-1/3）；删除流程全链路可测（FR-PRIV-5）。M9.1 数据清单落地后定稿。
- 新增 `SECURITY.md` 初稿：漏洞报告渠道、依赖扫描策略（OWASP Dependency-Check，M1.2-3 落地）、Release 日志脱敏原则（FR-PRIV-4，ARCHITECTURE.md §13）。
- 合规细化计划：M9（数据清单与同意管理）、M11（Play Store 数据安全表单、隐私政策、麦克风权限用途说明）。
- **M9（2026-08-01）**：`PRIVACY.md` 定稿 v1.0；`SECURITY.md` 补 M9.4 安全检查记录（Exported Component/Intent/FileProvider/日志/备份/网络配置/依赖漏洞逐项结论）；`allowBackup=false` + 禁明文网络配置；音频/应用日志统一经 `LogRedactor` 脱敏（含异常堆栈）。

### 已修改（Changed）

- （当前无——预发布阶段无历史版本可改）
