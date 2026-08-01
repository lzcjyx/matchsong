# Closed Beta 指标采集方案（M11.4）

- **状态：** 方案定稿；**执行需 Play Console 权限 + 产品负责人启动**
- **目标：** 采集 MVP 核心漏斗与质量指标，支撑 M11.5 发布决策
- **合规红线：** **不得采集原始音频用于研究，除非新增明确同意流程**（PLAN M11.4）；本方案仅采集匿名聚合指标与端侧派生特征，不上传原始录音

## 1. 指标定义

| 指标 | 定义 | 数据来源 | 采集方式 |
|---|---|---|---|
| 录音完成率 | 完成录音会话 / 发起录音 | RecordingSession 状态 | 端侧聚合事件（含 sessionId 去重） |
| 有效录音率 | 质量合格 / 完成录音 | AudioQualityReport.isUsable | 端侧聚合 |
| 分析失败率 | 分析失败或数据不足 / 分析发起 | AnalysisError / confidenceLevel | 端侧聚合 |
| 推荐点击率 | 点击推荐歌曲 / 展示推荐 | RecommendationDetail 进入事件 | 端侧聚合 |
| 用户适合度反馈 | 六类反馈分布 | user_feedback 表（本地，不自动上传） | 问卷/反馈导出（用户主动提交） |
| 崩溃率 | 崩溃会话 / 活跃会话 | Play Console 崩溃面板 | Play SDK 自动 |
| ANR | ANR 次数 | Play Console | Play SDK 自动 |
| 设备型号 | 设备分布 | Play Console | Play SDK 自动 |
| 解释理解度 | 用户对推荐理由的理解 | 问卷（Beta 结束） | 问卷 |

## 2. MVP 采集原则

- **无原始音频**：仅聚合计数与派生摘要（音域/置信度分档分布），不上传录音；
- **本地优先**：历史/反馈数据存本地（FR-HX-1），导出需用户主动操作（M11.4 反馈导出流程待定，Backlog）；
- **无第三方分析 SDK**：MVP 不引入分析 SDK（N-5/N-6）；指标经 Play Console 崩溃/ANR 面板 + 端侧事件日志（脱敏聚合，FR-PRIV-4）采集；
- **版本对齐**：所有事件携带 appVersion（versionName 0.1.0），供分布分析。

## 3. 端侧事件日志（脱敏聚合，[推测] 实现建议）

```text
MatchSong:Telemetry 事件类型=record_started/record_completed/quality_ok/quality_fail/analysis_ok/analysis_fail/recommendation_shown/recommendation_clicked
字段=事件类型, 时间戳(天粒度), appVersion, 聚合计数；不含设备标识与音频内容
```

> 当前 MVP **未实现**遥测事件发射（无后端接收端）；本方案为 M11.4 启动前的最小实现指引——如产品决定采集，需新增 ADR 明确数据流与隐私影响（PLAN §18 步骤 14）。

## 4. 执行前置条件

- [ ] Play Console 封闭测试轨道配置（Google Play 账号）
- [ ] 产品负责人确认指标口径与隐私说明更新（遥测需同步 PRIVACY.md + 同意版本）
- [ ] M10.3 真机矩阵完成（Beta 设备多样性）
- [ ] 支持邮箱（M11.2）就绪
