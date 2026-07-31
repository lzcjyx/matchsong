# M0 里程碑验收记录

- **里程碑：** M0 MVP 与架构冻结
- **验收日期：** 2026-07-31
- **验收人：** Coding Agent
- **总体状态：** **DONE** —— 5/5 任务完成，退出条件全部满足

---

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M0.1 编写 SPEC | **DONE** | `SPEC.md`（454 行，17 条 ACC 验收条件） |
| M0.2 编写架构文档 | **DONE** | `ARCHITECTURE.md`（665 行，8 个 Gradle 模块 + 逻辑边界） |
| M0.3 定义数据模型 | **DONE** | `docs/architecture/data-model.md`（15 模型 × 9 字段属性） |
| M0.4 建立测试策略 | **DONE** | `TESTING.md` + `docs/testing/`（test-fixture-manifest / device-matrix / manual-test-checklist / regression-suite） |
| M0.5 细化剩余任务 | **DONE** | `docs/plans/task-breakdown.md`（126 里程碑任务 + 16 Backlog，10 字段模板 100% 完整） |

## 2. 退出条件核对（PLAN §6.3）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| `SPEC.md` 已批准 | ✅ | 含产品目标/用户/非目标/流程/功能需求/异常流程/推荐定义/数据模型/架构/隐私/非功能/17 条 ACC |
| `ARCHITECTURE.md` 已批准 | ✅ | 覆盖全部 12 项必须定义（UI/Domain/Data/Audio Engine/Recording Service/Analysis Pipeline/Recommendation Engine/Storage/Error Model/日志/依赖方向/线程协程） |
| 数据模型完整 | ✅ | 15 个模型，每字段 9 项属性（类型/单位/范围/可空/来源/保存位置/敏感/保留时间）+ 存储映射 + 敏感处理 |
| 测试策略完整 | ✅ | 5 层测试 + 夹具清单 + 设备矩阵 + 手工清单 + 回归套件 |
| MVP 范围冻结 | ✅ | task-breakdown.md §MVP 范围冻结；M1-M11 为范围内，非 MVP 移入 Backlog（16 项 B-1..B-16） |
| 非 MVP 功能已移入 Backlog | ✅ | 16 项 Backlog 任务（B-1 音色推荐 … B-16），状态 DEFERRED |
| 不存在阻塞性架构问题 | ✅ | 无；仅 2 项非阻塞备注（SPEC §11 帧数笔误已修正、Media3 未使用已在 ARCHITECTURE 说明） |

## 3. 关键决策摘要

1. **模块划分**：8 个真实 Gradle 模块（app / domain / data:local / data:songs / core:common / core:model / core:audio / core:testing），feature:* 与 domain 三域为包级逻辑边界（M0.2 允许"减少模块但保持逻辑边界"）；
2. **依赖方向**：`feature → domain → core`，data 实现 domain Port；UI 不依赖 Audio Engine 实现类；
3. **音频焦点**：M3 必须实现 AudioFocusRequest（Spike 未实现，已列为风险）；
4. **权重版本化**：推荐权重 v1 集中配置并记录版本，置信度降权（SPEC §7.2）；
5. **分析性能核算**：30s 录音 ≈ 1292 帧（hop 1024），桌面实测 ~1.3-2s，SPEC 目标 ≤10s 余量充足。

## 4. 遗留风险（进入 M1 前需知晓）

| # | 风险 | 归属 |
|---|---|---|
| R-1 | 无 Gradle Wrapper；系统 Gradle 8.5 init.d 与 FAIL_ON_PROJECT_REPOS 冲突（Android 工程需用缓存 8.9 dist 或独立 wrapper） | M1.1 |
| R-2 | 系统默认 java=1.8 与 JDK17 工具链冲突 | M1.1 |
| R-3 | 真实设备人声/录音未测（模拟器已验证） | M3/M10 |
| R-4 | 舒适音区/质量阈值默认值为 [推测]，需 M4/M5 标定 | M4/M5 |
| R-5 | SPEC 阈值（性能/覆盖率）为产品目标，M10 验证 | M10 |

## 5. 验收结论

**M0 里程碑全部任务完成，验收通过。** 满足 PLAN §3.4 质量门禁。SPEC / ARCHITECTURE / 数据模型 / 测试策略 / 任务细化全部冻结。

**建议下一步：** 进入 **M1（Android 工程基线）** —— 按 task-breakdown.md M1.1-M1.5（16 个任务）建立 Android 工程、代码质量工具、CI、通用基础设施与文档。首个任务 M1.1-1：创建 Android Gradle 工程骨架（含 gradle wrapper 8.9 与版本目录）。
