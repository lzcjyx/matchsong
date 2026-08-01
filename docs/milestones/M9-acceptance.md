# M9 里程碑验收记录

- **里程碑：** M9 隐私、安全与数据管理
- **验收日期：** 2026-08-01
- **验收人：** Coding Agent
- **总体状态：** **DONE** —— 5/5 任务完成，退出条件全部满足

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M9.1 数据清单 | **DONE** | 数据清单审计（Room 5 表 / DataStore 2 文件 / 文件缓存逐一核对）；`PRIVACY.md` 定稿 v1.0 |
| M9.2 原始录音生命周期 | **DONE** | `RecordingSessionRunner.cleanupSessionFiles()`（幂等 + 删除失败记安全错误）；AppNavHost 在分析完成/质量失败重录/声音结果重录/推荐重录时调用；`release()` 服务销毁兜底清理；启动残留清理沿用 M3.5-2（ACC-14） |
| M9.3 数据删除 | **DONE** | `DeleteAllDataUseCase`（历史/收藏/反馈/设置/同意/缓存尽力全清，任一步失败继续）+ `SettingsViewModel`/`SettingsScreen` 全操作 UI（粒度删除 4 项 + 重置应用）；`SettingsRepository.clear()`、`RecordingFileCleaner.clearAll()` Port 扩展；重置成功回 Splash 重新 Onboarding（ACC-15） |
| M9.4 安全检查 | **DONE** | 审计结论见 `SECURITY.md` §4（12 项逐项合规）；`allowBackup=false` + `network_security_config.xml` 禁明文；`AndroidLogLogger` 全量脱敏（消息+堆栈经 `LogRedactor`）；`RecordingSessionRunner`/`MatchSongApplication` 统一注入 Logger；错误消息源头去绝对路径（WavFileWriter/RecordingFileManager） |
| M9.5 Play Store 合规 | **DONE** | `docs/compliance/play-store-materials.md`：数据安全表单 / 权限声明 / 隐私政策 / 数据删除说明 / 未成年人声明 + M11 复核清单 |

## 2. 退出条件核对（PLAN §15.3）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| 没有静默录音路径 | ✅ | 录音必须伴随可见 UI + 前台通知（FR-REC-9，M3 验证）；本次未引入新录音入口 |
| 原始录音默认不永久保存 | ✅ | 分析完成即删（ACC-14 接线）+ 重录/失败/服务销毁清理 + 启动残留清理（24h 阈值） |
| 用户可以删除所有个人数据 | ✅ | 单条历史（M8.4）/全部历史/收藏/设置/缓存/重置应用（M9.3），重置后重新 Onboarding（ACC-15） |
| Release 日志不包含敏感信息 | ✅ | 音频路径全量经 `LogRedactor`（含堆栈序列化脱敏）；`AndroidLogLogger` 与 `AndroidLogger` 双实现均脱敏 |
| 安全审计无高严重度问题 | ✅ | 12 项检查结论合规；遗留仅 SQLCipher（M10 可选优化，本地威胁面低） |
| 隐私文档与实际代码一致 | ✅ | `PRIVACY.md` 定稿 v1.0，与实现逐一核对 |

## 3. 关键实现决策

1. **删除时机集中在导航层**：分析完成（ANALYZING Done）→ `cleanupSessionFiles()` + 清空会话 WAV 状态；重录路径（质量失败/声音结果/推荐列表）→ 清理后重置——与 `FlowSessionViewModel.reset()` 搭配，覆盖全部退出路径。
2. **`DeleteAllDataUseCase` 尽力执行语义**：6 个清空步骤全部尝试（部分删除优于拒绝删除——隐私最小化），任一步失败记录首个 `AppError` 并继续，返回失败供 UI 提示；歌曲库为应用内置数据不删除。
3. **重置导航**：成功 → `popUpTo(graph.id) inclusive` 清空返回栈 → Splash 分流（同意已撤销）→ 重新 Onboarding。
4. **日志脱敏落地方式**：不向 `android.util.Log` 直接传 Throwable（原始异常链会泄漏未脱敏 message），改为堆栈序列化后整体脱敏输出。
5. **备份策略**：`allowBackup=false`（Room 含敏感派生特征，禁云备份/ADB 备份外泄），而非排除规则——单机本地优先应用的最简安全解。

## 4. 发现并修复的遗留问题（HEAD 即存在）

| # | 问题 | 修复 |
|---|---|---|
| 1 | `AppNavHost.kt` 使用默认包 import（`import AnalyzingViewModel` / `import QualityResultViewModel`）——HEAD 提交无法编译 | 改为完整包名 import |
| 2 | `HistoryItem.kt` 注释残留（重复文本 + 孤立 `*/`）触发 ktlint | 清理注释 |
| 3 | `FavoriteToggleViewModel` `_songId`/`isFavorite` 命名违规 ktlint | 改名 `songIdFlow`/`favoriteState` |
| 4 | `FeedbackRepository` 无 Hilt 绑定（M8.5 数据层存在但 UI 未接线，`FeedbackSheet` 为孤儿组件） | DatabaseModule 补绑定（删除用例消费）；UI 接线列为遗留风险 |
| 5 | `:data:local` connected 测试崩溃（`Process crashed`）：库模块默认 legacy runner（`android.test.InstrumentationTestRunner`，API 28+ 已移除）且 `AndroidJUnitRunner` 为独立 artifact 未显式声明 | 配置 `testInstrumentationRunner` + 补 `androidx.test:runner` 依赖；`connectedDebugAndroidTest` 全模块通过 |

> 说明：M8 验收记录声称"checkQuality 全绿 + 构建成功"，但 HEAD 提交实际含编译/ktlint 失败项——本里程碑修复后全部门禁通过。

## 5. 构建与测试状态

- 构建：`assembleDebug` + `assembleRelease` 均 BUILD SUCCESSFUL
- 单元测试：全模块 `testDebugUnitTest` 通过（含新增：DeleteAllDataUseCaseTest 3 场景、DataStoreSettingsRepositoryTest（Robolectric）、RecordingFileManagerTest clearAll 2 用例、FakeRecordingFileCleanerTest 2 用例、FakeSettingsRepository clear 断言）
- 覆盖率门禁：`jacocoCoverageVerification`（core ≥80%）通过
- 静态检查：`checkQuality`（Lint/Detekt/Ktlint）全绿
- 仪器测试：**本地模拟器验证通过**（spike_avd, API 36）：`connectedDebugAndroidTest` 全模块 GREEN，app 18/18 通过（Navigation/E2E/状态组件），含设置页删除流程相关断言；`:data:local` 空套件 runner 修复后通过

## 6. 遗留风险

| # | 风险 | 归属 |
|---|---|---|
| R-1 | 反馈 UI 未接线：`FeedbackSheet` 为孤儿组件（M8.5 仅数据层落地），用户无法提交反馈 | M9/M11 可增强 |
| R-2 | 真实录音→分析→推荐异步链路未在仪器测试覆盖（Compose 时钟限制，M3 已知） | M10 真机 E2E |
| R-3 | ACC-14 文件删除的自动化断言仅覆盖 FileManager 层；导航层接线由代码审查 + 模拟器人工验证 | M10 真机验证 |
| R-4 | Room 未启用 SQLCipher（data-model §4.4 `[推测-实现建议]`）——无备份/无网络下本地威胁面低 | M10 可选优化 |
| R-5 | 数据集音域为推导（MEDIUM 可信度），推荐精度待真实数据校准 | M10 |
| R-6 | 历史详情跳转用 popBackStack（MVP 简化） | M9 已评估保持 |
| R-7 | 设置页无语言/风格偏好配置 UI（偏好固定默认，推荐过滤退化为默认） | M9/M11 |

## 7. 验收结论

**M9 里程碑全部任务完成，验收通过。** 满足 PLAN §3.4 质量门禁：
- 数据清单与隐私文档定稿（M9.1）✅
- 原始录音分析完成即删 + 全路径清理（M9.2/ACC-14）✅
- 删除流程完整可测 + 重置回首次启动（M9.3/ACC-15/FR-PRIV-5）✅
- 安全检查 12 项合规 + 日志全量脱敏（M9.4/FR-PRIV-4）✅
- Play Store 合规材料初稿（M9.5）✅
- 构建/单测/覆盖率/静态检查全绿 ✅

**建议下一步：** 进入 **M10（稳定性与性能优化）** —— 性能基准（冷启动/录音 CPU/分析耗时/内存）、真机设备矩阵（含 R-2/R-3 真机录音 E2E 验证）、可选 SQLCipher 评估。
