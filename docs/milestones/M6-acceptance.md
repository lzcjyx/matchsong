# M6 里程碑验收记录

- **里程碑：** M6 歌曲数据系统
- **验收日期：** 2026-07-31
- **验收人：** Coding Agent
- **总体状态：** **DONE** —— 7/7 任务完成，退出条件全部满足

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M6.1-1 SongMetadata/SongRangeProfile | **DONE** | core:model 23 字段模型 + validate() + Genre 词表 + Credibility（10 测试） |
| M6.1-2 JSON Schema | **DONE** | song-schema.json + SongSchemaValidator（12 测试） |
| M6.2-1 JSON/CSV 解析器 | **DONE** | SongDataParser + CsvSongParser（10 测试，含 BOM/引号转义） |
| M6.2-2 数据校验器 | **DONE** | SongImportValidator：重复/音域/来源/版本（13 测试） |
| M6.2-3 ImportRunner | **DONE** | JVM main 入口 + ImportReport（5 测试），与 App 解耦 |
| M6.3-1 采集规范与来源登记 | **DONE** | dataset-guidelines.md + song-data-sources.md（来源分级/禁止虚构/推导标注） |
| M6.3-2 MVP 数据集 | **DONE** | **50 首**（zh 30/en 20，流行/摇滚/民谣/金属），ImportRunner 0 错误，回归测试锁定 |
| M6.4-1 Room 存储 | **DONE** | 3 实体 + SongDao/FavoriteDao + MatchSongDatabase v1（exportSchema）+ 12+5 测试 |
| M6.4-2 导入与版本 | **DONE** | SongImportRepository（幂等 + 事务升级 + 收藏保留）+ RoomSongRepository（9+10 测试） |
| M6.4-3 搜索/筛选/收藏 | **DONE** | LIKE 搜索 + 语言/风格筛选 + 音域过滤 + 收藏关系查询 |
| M6.5-1/2 数据测试 | **DONE** | 数据集回归 4 用例 + schema 导出 + Room In-Memory 36 测试 |

## 2. 退出条件核对（PLAN §12.4）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| 存在可用的 MVP 歌曲数据集 | ✅ | 50 首（zh/en 覆盖，4 风格） |
| 所有歌曲通过自动校验 | ✅ | ImportRunner 0 错误 + MvpDatasetTest 回归门禁 |
| 每条关键数据存在来源或可信度声明 | ✅ | dataSource 非空 + credibility 分级（数据集为 LOW [推测] 诚实标注，升级路径文档化） |
| 数据可以安全升级 | ✅ | 事务导入 + 版本比对 + 收藏保留（测试锁定） |
| 推荐引擎无需读取硬编码歌曲列表 | ✅ | RoomSongRepository Port 实现（M7 消费） |

## 3. 数据集诚实性声明

- **调性**：公开记录事实（多来源交叉，MEDIUM 可信度）
- **音域/负担**：DERIVED 推导 [推测] + LOW 可信度（每首 dataSource 标注）；**未批量虚构**，推导方法文档化（dataset-guidelines.md）
- **升级路径**：逐首 AcousticBrainz API 复核调性 + 乐谱标注音域（song-data-sources.md §升级）

## 4. 语义修正记录（M6.3-2 发现）

**originalKey 音域检查移除**（SongImportValidator + SongSchemaValidator + 2 测试）：
- 原规则要求 originalKeyMidi 落在演唱音域内 —— **音乐上错误**（原调是伴奏调性，与演唱音域独立）
- 变调推荐正是为"原调高于/低于音域"设计
- 修正为仅校验 MIDI 范围 0..127

## 5. 构建与测试状态

- 构建：assembleDebug + assembleRelease 均 BUILD SUCCESSFUL
- 单元测试：全模块 testDebugUnitTest 全过（data:songs 44 + data:local 36 + core:model 10 + 其他）
- 仪器测试：connectedDebugAndroidTest **13/13**（无回归）
- 覆盖率门禁：core:common/core:testing ≥80% 通过
- 静态检查：checkQuality 全绿（3 处豁免/修正已注释理由）

## 6. 遗留风险

| # | 风险 | 归属 |
|---|---|---|
| R-1 | 数据集音域为推导值（LOW 可信度），推荐精度依赖其质量 | M7 后逐首升级（乐谱/AB 复核） |
| R-2 | MusicBrainz 网络不可达（本机屏蔽）→ AB 逐首复核受限 | 换网络后补 |
| R-3 | Room schema v1 无迁移（首个版本）；MIGRATION_1_2 待未来变更 | 后续版本 |
| R-4 | 中文搜索为子串匹配（无分词） | MVP 可接受，记录边界 |

## 7. 验收结论

**M6 里程碑全部任务完成，验收通过。** 满足 PLAN §3.4 质量门禁：
- MVP 数据集可用（50 首，0 错误校验）✅
- 每首有来源/可信度声明（无虚构）✅
- 数据安全升级（事务 + 测试）✅
- Room 存储 + 查询能力（M7 前置）✅
- 静态检查 + 覆盖率 + 仪器测试无回归 ✅

**建议下一步：** 进入 **M7（推荐引擎）** —— 按 task-breakdown.md M7.1-M7.6 实现候选过滤（复用 M6.4 搜索/音域查询）、变调计算、评分模型（RangeFit/TessituraFit/HighNoteBurdenFit/DifficultyFit/PitchStabilityFit/PreferenceFit + 置信度调整）、推荐解释（实际数据驱动）、无结果降级与推荐测试。首个任务 M7.1-1：候选过滤。
