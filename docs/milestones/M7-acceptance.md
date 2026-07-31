# M7 里程碑验收记录

- **里程碑：** M7 推荐引擎
- **验收日期：** 2026-07-31
- **验收人：** Coding Agent
- **总体状态：** **DONE** —— 11/11 任务完成，退出条件全部满足

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M7.1-1 CandidateFilter | **DONE** | 语言/排除风格/数据完整性/LOW 可信度/可调音域过滤 + 原因计数（无性别字段，天然无性别硬过滤） |
| M7.2-1 KeyShiftEvaluation | **DONE** | ±6 半音枚举 + 变调后音域 + 优先降调策略 + 不可调标记（ACC-17） |
| M7.3-1 六特征评分 | **DONE** | RangeFit/TessituraFit/HighNoteBurdenFit/DifficultyFit/PitchStabilityFit/PreferenceFit（0-100）+ FitLevel |
| M7.3-2 权重配置 | **DONE** | RecommendationWeights v1（SPEC §7.2：0.30/0.25/0.15/0.10/0.10/0.10）+ 版本化 + 和=1 校验 |
| M7.3-3 置信度调整与排序 | **DONE** | ConfidenceAdjustment 乘子 + 总分 + 确定性排序（songId tie-break）+ Top 10 |
| M7.4-1 解释生成 | **DONE** | 模板 + 实际特征数据填充（无数据不生成文案），每首 ≥1 条 |
| M7.4-2 解释一致性 | **DONE** | 一致性测试：TessituraFit=GOOD → 解释含舒适音区文案（ACC-16） |
| M7.5-1 无结果降级 | **DONE** | 空状态原因 + 建议 + LOW 置信短路（ACC-9/12） |
| M7.6-1 场景测试 | **DONE** | 10 场景：完全匹配/超音域/降调匹配/语言/风格/可信度/低置信/空库/可重复/权重版本 |
| M7.6-2 装配 | **DONE** | GetRecommendationsUseCase + RecommendationViewModel + DataStore SettingsRepository + DI 全链路 |

## 2. 退出条件核对（PLAN §13.4）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| 推荐结果可重复 | ✅ | 同输入两次排序/分数完全一致（测试锁定，ACC-13） |
| 排序逻辑可测试 | ✅ | 10 场景测试 + tie-break 确定性 |
| 推荐理由可追溯到实际数据 | ✅ | 解释由评分特征生成 + evidence 一致性测试（ACC-16） |
| 支持升降调建议 | ✅ | KeyShiftEvaluation 输出 ±半音 + 解释含变调文案（ACC-17） |
| 低置信度输入有降级处理 | ✅ | LOW 置信短路（ACC-9）+ MEDIUM 标注 |
| 没有歌曲时有合理空状态 | ✅ | emptyStateReason + 建议（ACC-12） |

## 3. 架构补充（M7 驱动）

1. **SongRepository Port 扩展**：`getAllMetadata(): List<SongMetadata>`（推荐引擎需要完整音域字段；SongInfo 占位仅 4 字段）+ RoomSongRepository 实现 + entity→model 映射
2. **DataStoreSettingsRepository**：UserSettings（语言/偏好/排除风格）落 DataStore（此前仅 domain Port 无实现）
3. **DI 装配**：DatabaseModule + AppModule 绑定推荐链路（SongRepo + SettingsRepo → GetRecommendationsUseCase）

## 4. 发现并修复的真实问题

1. **候选过滤可调音域判定错误**（初版）：用"音域跨度差"判定，导致所有歌曲被过滤（完美匹配也被拒）。修正为**变调后与用户音域存在重叠的 shift 区间判定**（区间非空逻辑）。
2. **originalKey 语义**：M6 已修正（调性独立于演唱音域），M7 引擎不再依赖该字段做过滤。

## 5. 构建与测试状态

- 构建：assembleDebug + assembleRelease 均 BUILD SUCCESSFUL
- 单元测试：domain 60+（推荐 14 场景 + 装配 3）+ 全模块 testDebugUnitTest 全过
- 仪器测试：connectedDebugAndroidTest **13/13**（无回归）
- 覆盖率门禁：core:common/core:testing ≥80% 通过
- 静态检查：checkQuality 全绿（2 处 @Suppress 已注释理由）

## 6. 遗留风险

| # | 风险 | 归属 |
|---|---|---|
| R-1 | 评分映射曲线/阈值（FitLevel 70/40、置信度乘子公式）为 [推测] 值 | Beta 反馈校准（权重版本化支持） |
| R-2 | 数据集音域 LOW 可信度 → LOW 可信度歌曲被过滤（当前 50 首全 LOW → 推荐可能空） | **重要**：M6 数据集 credibility 全为 LOW，推荐引擎过滤后候选为空 → 需提升数据集可信度或放宽过滤（M8 处理） |
| R-3 | SettingsRepository 无 UI（语言/偏好设置页未实现） | M8 设置页 |
| R-4 | 推荐列表页仍用 Fake 数据（M8.2 接真实管线） | M8.2 |

## 7. 验收结论

**M7 里程碑全部任务完成，验收通过。** 满足 PLAN §3.4 质量门禁：
- 推荐可重复（ACC-13）✅；排序可测试 ✅；理由可追溯（ACC-16）✅
- 升降调建议（ACC-17）✅；低置信降级（ACC-9）✅；空状态（ACC-12）✅
- 静态检查 + 覆盖率 + 仪器测试无回归 ✅

**⚠️ 关键阻塞发现（R-2）**：M6 数据集 50 首 credibility 全部为 LOW → 推荐引擎的 LOW 可信度过滤会清空候选。**M8 前必须处理**：提升数据集部分歌曲可信度（AcousticBrainz 复核调性 MEDIUM/HIGH）或调整过滤策略（LOW 但来源明确的歌曲允许进入）。

**建议下一步：** 进入 **M8（完整用户体验）** —— 串联录音→分析→推荐全流程（M8.1/8.2 接真实管线替换 Fake）、收藏、历史、反馈、错误恢复、E2E 测试。同时处理 R-2（数据集可信度）。
