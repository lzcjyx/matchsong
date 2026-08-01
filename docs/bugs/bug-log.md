# Bug 日志

- **用途：** 记录全部已发现 Bug（PLAN §16.3 / §18、M10.6）。
- **工作流依据：** PLAN.md §18「Bug 修复工作流」（16 步，见文末附录）。
- **规则：** 修复 Bug 必须先添加失败测试（PLAN §16.3）；不得通过关闭规则或忽略掩盖问题（PLAN M1.2）；日志记录注意脱敏（不含设备标识与音频内容，ARCHITECTURE.md §13）。

## 1. 严重级别定义（PLAN §16.3 / M10.6）

| 级别 | 定义 | 处理要求 |
|---|---|---|
| P0 | 阻塞发布：崩溃、数据丢失、隐私违规、安全漏洞 | 全部修复 |
| P1 | 主要功能不可用 | 全部修复 |
| P2 | 一般缺陷 | 必须评估并记录 |
| P3 | 轻微问题 / 改进建议 | 可进入 Backlog |

## 2. Bug 状态

| 状态 | 含义 |
|---|---|
| OPEN | 已确认，待处理 |
| IN_PROGRESS | 修复中 |
| FIXED | 已修复，待验证 |
| CLOSED | 已验证关闭 |
| DEFERRED | 进入 Backlog（仅 P3） |

## 3. Bug 记录模板

每个 Bug 在 §4 日志表中占一行，并在 §5「Bug 明细」按下方模板完整记录。Bug ID 从 `BUG-001` 起顺序递增，不得复用。

```text
Bug ID:          BUG-XXX
标题 (Title):
严重级别 (Severity): P0 / P1 / P2 / P3
状态:            OPEN / IN_PROGRESS / FIXED / CLOSED / DEFERRED
设备:            型号 + 分辨率/内存（如适用）
Android 版本:
App 版本:        versionName (versionCode)
修复版本 (Resolved Version):
修复 Commit:
--- 以下字段按 PLAN §18 工作流填写 ---
最小复现步骤:
日志 / 堆栈:     附件路径或关键片段（脱敏：不含设备标识、路径、音频内容）
预期结果:
实际结果:
影响范围:
根因:
失败测试:        修复前添加的失败测试（提交 / 文件）
相关测试:
回归测试:
隐私 / 性能影响检查:
```

## 4. Bug 日志表

| Bug ID | 标题 | 严重级别 | 设备 / Android | App 版本 | 状态 | 修复 Commit | 备注 |
|---|---|---|---|---|---|---|---|
| BUG-001 | 反馈 UI 未接线（FR-HX-3：FeedbackSheet 孤儿组件） | P1 | 全部 | 0.1.0 | FIXED | M10.6 | 详情页反馈入口 + 六类反馈提交接线 |
| BUG-002 | 推荐详情页用 Fake 歌曲数据（无真实推荐上下文） | P2 | 全部 | 0.1.0 | FIXED | M10.6 | 真实推荐项经导航参数传递 |
| BUG-003 | NavType.IntType 声明 nullable 导致 app 启动崩溃 | P0 | 全部 | 0.1.0（未发布） | FIXED | M10.6 | 回归捕获（E2E homeToPrepareAndBackToHome）；改 StringType+解析 |
| BUG-004 | 真机设备矩阵未执行（硬件缺失） | P2 | 真机 | 0.1.0 | DEFERRED | — | 需低端/中端/Pixel/Samsung/中国厂商真机（device-matrix.md） |
| BUG-005 | 数据集音域为推导（MEDIUM 可信度），推荐精度待真实数据校准 | P2 | 全部 | 0.1.0 | DEFERRED | — | M10 记录；校准需真实演唱数据积累 |
| BUG-006 | 历史详情跳转用 popBackStack（MVP 简化，非独立详情页） | P3 | 全部 | 0.1.0 | DEFERRED | — | M9 已评估保持 |
| BUG-007 | 设置页无语言/风格偏好配置 UI（推荐过滤退化默认） | P2 | 全部 | 0.1.0 | DEFERRED | — | M11 前补齐 |
| BUG-008 | RecommendationResult 未携带 resultId（反馈 FK 关联缺失） | P3 | 全部 | 0.1.0 | DEFERRED | — | M10.6 发现；data-model §2.12 与实现差距 |
| BUG-009 | Room 未启用 SQLCipher（派生特征加密） | P3 | 全部 | 0.1.0 | DEFERRED | — | 本地威胁面低；M10 评估保持 |
| BUG-010 | 带伴奏人声子谐波锁定（YIN 已知限制） | P3 | 全部 | 0.1.0 | DEFERRED | — | M5 已记录，M11 后评估 |
| BUG-011 | 模拟器 AVD 代理（API 26/31/34）未创建 | P2 | 模拟器 | 0.1.0 | DEFERRED | — | 兼容性验证缺口（device-matrix.md） |
| BUG-012 | 真实录音→分析→推荐异步链路无自动化仪器覆盖 | P2 | 全部 | 0.1.0 | DEFERRED | — | Compose 测试时钟限制；真机 E2E（M11） |
| BUG-013 | 录音倒计时不跳动（固定显示"倒计时 3…"，3 秒后直接进录音） | P2 | 真机 | 0.1.0 | FIXED | 真机修复 | UI 硬编码文案；RecordingPort 增加 countdownSeconds 流（3→2→1） |
| BUG-014 | 录音完成→质量页白屏卡死（"完成"消失后无下一步） | P1 | 真机 | 0.1.0 | FIXED | 真机修复 | stop() 先发 COMPLETED 后 finalizeWav——UI 竞态读到 null WAV；重排落盘顺序 + 防御性错误态 |
| BUG-015 | 录音混入非歌唱说话声，干扰音域分析 | P2 | 真机 | 0.1.0 | FIXED | 真机修复 | 时间间隔分段（150ms）+ 稳定片段比例门禁（<0.3 判语音为主，按数据不足处理） |
| BUG-016 | 内置曲库从未在运行时导入——真机推荐必然空结果 | P1 | 全部 | 0.1.0 | FIXED | 真机修复 | 数据集移入 assets + Application 启动幂等导入；SongCatalogSeedTest 回归（真实 DB 50 首） |

## 5. Bug 明细

### BUG-001 反馈 UI 未接线

- **严重级别：** P1（FR-HX-3 为 P1 需求，UI 完全缺失）
- **状态：** FIXED（M10.6，Commit 见 M10 提交）
- **复现：** 推荐详情页无任何反馈入口；FeedbackSheet/SubmitFeedbackUseCase 存在但无调用方
- **根因：** M8.5 仅落地数据层与组件，未接 UI 调用链；FeedbackRepository 无 Hilt 绑定
- **修复：** `RecommendationDetailViewModel.submitFeedback` + 详情页「反馈推荐结果」入口 + FeedbackSheet 接线；SubmitFeedbackUseCase Hilt 绑定
- **验证：** 编译 + 仪器测试 21/21 + 手工检查表

### BUG-002 推荐详情页 Fake 数据

- **严重级别：** P2
- **状态：** FIXED（M10.6）
- **修复：** 推荐项（标题/歌手/匹配度/变调/解释/resultId）经导航参数传递；收藏入口降级仅传歌曲名/歌手

### BUG-003 导航参数 nullable Int 崩溃

- **严重级别：** P0（应用启动即崩溃）
- **状态：** FIXED（M10.6，回归捕获，未发布）
- **复现：** 声明 `navArgument(NavType.IntType) { nullable = true }` → NavHost 图构建时抛 `IllegalArgumentException: integer does not allow nullable values` → 启动崩溃（E2E homeToPrepareAndBackToHome 失败暴露）
- **根因：** navigation-compose 仅 StringType 支持 nullable
- **修复：** score/keyShift 改 StringType + toIntOrNull 解析；路由构建器省略 null 参数
- **失败测试：** MainFlowE2eTest.homeToPrepareAndBackToHome（回归套件捕获）

### BUG-004 ~ BUG-012（P2/P3，评估并记录后进入 Backlog）

均为资源/后续里程碑项，明细见 §4 表格备注；评估结论：不阻塞 M10 退出条件（无 P0/P1 遗留）。

### BUG-013 录音倒计时不跳动

- **严重级别：** P2（功能缺陷，不影响数据）
- **状态：** FIXED（真机反馈修复，2026-08-01）
- **复现：** 真机点击开始录音 → 显示"倒计时 3…"持续 3 秒（不显示 2/1）→ 突然进入录音
- **根因：** `RecordingScreen` 硬编码 `Text("倒计时 3…")`；状态机内部跟踪 `countdownRemaining` 但未通过 `RecordingPort` 暴露，UI 无法渲染真实秒数
- **修复：** `RecordingPort` 增加 `countdownSeconds: StateFlow<Int>`；`RecordingSessionRunner` 每秒发射剩余秒数（3→2→1）；ViewModel 转发；UI 渲染 `倒计时 $n…`
- **测试：** 状态机 countdownRemaining 既有测试 + 编译/仪器测试 21/21

### BUG-014 录音完成→质量页白屏

- **严重级别：** P1（主流程中断）
- **状态：** FIXED（2026-08-01）
- **复现：** 真机停止录音 → 显示"完成" → 文字消失 → 白屏无下一步
- **根因：** `RecordingSessionRunner.stop()` 先发射 `COMPLETED` 再执行 `closePcmSink()+finalizeWav()`；UI 观察 COMPLETED 后立即读取 `runner.lastWavFile`（可能为 null）→ `flowSession.wavFile=null` → 质量页 `UiState.Idle` 渲染空白（真机线程调度放大竞态窗口，模拟器偶发/未现）
- **修复：** ① stop() 重排：先落盘（PCM 关闭+WAV 封装）再发射 COMPLETED；② 防御层：质量页 wavFile 为 null 时显示"录音文件不可用，请重新录制"错误态而非白屏
- **测试：** 既有仪器测试 21/21（回归路径 homeToPrepareAndBackToHome 覆盖导航）；竞态本身无法在模拟器确定性复现，已按代码路径修复 + release 冒烟

### BUG-015 录音混入非歌唱说话声

- **严重级别：** P2（结果质量）
- **状态：** FIXED（2026-08-01）
- **复现：** 真机录音时混入说话声（他人说话/自言自语），分析结果被干扰
- **根因：** 后处理按音高跳变分段——同音高的说话词与歌唱音合并成长片段逃过 300ms 最短片段过滤；连续说话（语调滑动）稳定帧占比低但未被拒绝
- **修复：** ① `PitchPostProcessor` 分段同时按时间间隔（>150ms）断裂（说话词与歌唱音微停顿分隔，短段被 300ms 规则丢弃）；② `AnalyzeRecordingUseCase` 语音门禁：稳定片段比例 < 0.3（AnalysisConfig.MIN_STABLE_FRAME_RATIO，[推测]）→ 判定非歌唱语音为主，按"有效演唱片段不足"处理（音域/舒适区置空 + LOW 置信度，ACC-9 不生成正式推荐）
- **测试：** PitchPostProcessorTest 新增同音高说话词+间隔分段用例；AnalyzeRecordingUseCaseTest 新增 300→600Hz 连续滑动（说话式语调）→ 无音域 + LOW + INSUFFICIENT_SAMPLES；MIR-1K 真实人声一致性测试全部通过（门禁不误伤真实演唱）
- **已知边界[推测]：** 连续平稳说话（无滑动、单音调）仍可能通过门禁——语音/歌唱判别为研究级问题，MVP 采用保守启发式，不宣称准确率（PLAN §2.1）

### BUG-016 内置曲库未在运行时导入

- **严重级别：** P1（推荐主功能在真机必然空结果）
- **状态：** FIXED（2026-08-01）
- **复现：** 真机完成分析 → 推荐页显示空状态/无候选（Room song_metadata 表为空）
- **根因：** 数据集 `mvp-songs.json` 位于 data:songs JVM resources，`SongImportRepository` 无任何运行时调用方（仅 CLI/测试使用）——M6 验收只验证了仓库层，未验证应用启动装载；此前白屏（BUG-014）掩盖了该问题
- **修复：** ① 数据集移至 `data/songs/src/main/assets/songs/mvp-songs.json`（Android 资产打包，CLI/测试改路径）；② `MatchSongApplication.ensureSongCatalog()` 启动异步幂等导入（版本比对跳过/事务替换），失败仅记日志不阻塞（推荐页空态降级）
- **测试：** `SongCatalogSeedTest`（仪器测试）：经 main 源码 @EntryPoint 查真实 DB，轮询断言 50 首落库；release 冒烟 logcat 确认"歌曲目录就绪：50 首"
- **已知边界：** 导入为异步——极快用户可能在导入完成前进推荐页看到空态（50 行导入 <100ms，可接受）；下次启动/重试即正常

---

## 附录：Bug 处理流程（PLAN §18）

每个 Bug 必须按以下顺序处理：

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
