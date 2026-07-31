# 回归测试套件（Regression Suite）

- **版本：** 0.1.0
- **日期：** 2026-07-31
- **里程碑：** M0.4（执行于每个 Milestone 门禁前；M10.5 完整回归）
- **依据：** PLAN §3.4 Milestone 质量门禁、PLAN M10.5 完整回归、M10.6 Bug 清零、SPEC §11
- **配套：** TESTING.md §6/§8/§10、docs/testing/manual-test-checklist.md、docs/testing/device-matrix.md

## 1. 目的与时机

- 目的：在进入下一个 Milestone 前证明**既有功能未被破坏**（回归），并满足 PLAN §3.4 质量门禁；
- 时机：
  1. 每个 Milestone 结束、门禁评审前（**门禁回归**）；
  2. M10.5 发布前（**完整回归**，发布前唯一全量回归）；
  3. 跨模块大改动后按需[推测]；
- 未通过不得进入下一 Milestone（PLAN §3.4）；修复 Bug 必须先添加失败测试（M10.6）。

## 2. 回归项定义（R-1..R-10）

| 编号 | 回归项 | 内容 | 执行方式 |
|---|---|---|---|
| R-1 | 单元测试 | 全模块 `testDebugUnitTest`：core:model、core:audio（YIN/质量/音高后处理/音域估计）、domain:recording（状态机）、domain:analysis、domain:recommendation（过滤/变调/评分/排序/解释）、data:songs（校验）、data:local（DAO 逻辑）、feature:*（ViewModel 状态） | `./gradlew testDebugUnitTest`（JVM，CI 每 PR 也跑） |
| R-2 | 集成测试 | Room In-Memory：DAO CRUD/迁移/版本回滚；歌曲导入工具全链路（Schema/重复 ID/无效音高/缺失来源，PLAN M6.5）；领域用例与 Repository 装配 | `testDebugUnitTest` 内集成套件（JVM） |
| R-3 | Compose UI 测试 | M2.5 全部 8 项（首次启动/已完成 Onboarding/导航/Loading/Empty/Error/Fake 推荐列表/删除确认弹窗）+ 各 feature 页面；Fake Repository 注入 | `connectedDebugAndroidTest` UI 套件（spike_avd） |
| R-4 | 仪器测试 | 麦克风权限状态机、AudioRecord 采集链路、前台服务与通知、后台/来电行为（M3.7 自动部分） | `connectedDebugAndroidTest` 仪器套件（spike_avd） |
| R-5 | E2E（Fake Audio） | 首次启动→Onboarding→授权→录音→质量通过→分析→推荐→收藏→反馈→历史→删除（PLAN M8.7 全流程） | E2E 套件（spike_avd，Fake Audio Stream） |
| R-6 | 静态检查 | `lintDebug`、`detekt`、`ktlintCheck`；依赖版本检查与漏洞扫描（M1.2；不允许关闭规则掩盖问题） | `./gradlew lintDebug detekt ktlintCheck` |
| R-7 | Release 构建 | `assembleRelease`（签名、脱敏日志检查 FR-PRIV-4） | 本地/CI |
| R-8 | 手工回归 | manual-test-checklist.md 关键项（按里程碑取子集，见 §4） | 真机 + spike_avd，人工执行 |
| R-9 | 性能回归 | Macrobenchmark：冷启动 ≤3s、30s 分析 ≤10s、峰值内存 ≤200MB、电量 ≤1%（SPEC §11）；与上次基准对比，劣化 > 20% 视为回归[推测阈值] | Macrobenchmark 模块（M10 起，真机；无真机用 spike_avd，数据仅参考） |
| R-10 | 覆盖率门禁 | JaCoCo：core 模块（domain/core:audio/core:model）≥ 80%、UI 层 ≥ 60% 行覆盖率[推测，见 TESTING.md §4] | CI 每 PR + 门禁时 |

## 3. 门禁映射（PLAN §3.4 ↔ 本套件）

| PLAN §3.4 门禁项 | 对应回归项 |
|---|---|
| 当前 Milestone 必须任务全部完成 | 里程碑任务清单（独立核对） |
| 当前代码可以构建 | R-7（含 assembleDebug） |
| 相关单元测试通过 | R-1 |
| 相关集成测试通过 | R-2 |
| 静态检查通过 | R-6 |
| 没有未解释的严重错误 | R-1..R-8 失败项全部修复或有已记录解释 + Bug 清零（P0/P1=0，P2 记录，P3 backlog，M10.6） |
| 文档已同步 | 变更后重读 SPEC/PLAN/TESTING 配套 |
| 已记录遗留风险 | 更新 risk 记录（各 Milestone 遗留风险表） |
| 已生成 Milestone 验收记录 | 生成 `docs/milestones/M{n}-acceptance.md` |

## 4. 每里程碑回归子集

门禁回归子集 = 满足该里程碑既有能力不被破坏的最小集合；M10.5 为全量。

| 里程碑 | 回归子集 | 说明 |
|---|---|---|
| M1 | R-1（框架冒烟）、R-6、R-7、R-10 | 基线建立：构建/单测/静态/覆盖率报告 |
| M2 | 上 + R-3 | UI 骨架开始受保护 |
| M3 | 上 + R-4、R-8（录音人工项 MT-C、MT-B） | 录音系统进回归 |
| M4 | R-1（质量全量：所有夹具 FIX-* 拒绝/通过断言）、R-2 | 质量层进回归 |
| M5 | R-1（分析全量：纯音/音阶/噪声/边界/一致性）、R-2 | 分析层进回归 |
| M6 | R-2（数据全量：导入/迁移/回滚）、R-1（data:songs） | 数据层进回归 |
| M7 | R-1（推荐全量：过滤/变调/评分/降级/重复性）、R-3 | 推荐层进回归 |
| M8 | 上 + R-5（E2E 全流程） | E2E 进回归 |
| M9 | R-1（删除流程/缓存生命周期）、R-3（删除弹窗）、R-8（MT-G 组） | 隐私与删除进回归 |
| M10 | **R-1..R-10 全量** + 设备矩阵（device-matrix.md 全真机 × 采集方式）+ 稳定性人工项（MT-H 组） | M10.5 完整回归 + M10.6 Bug 清零 |
| M11 | R-6、R-7、R-8（MT-I 组 Internal Testing 检查）、R-9 | 发布回归；Beta 指标监控（M11.4） |

## 5. 完整回归（M10.5 定义，发布前唯一全量）

1. R-1 单元测试（全模块）
2. R-2 集成测试（Room/导入/用例）
3. R-3 Compose UI 测试
4. R-4 仪器测试
5. R-5 E2E（Fake Audio）
6. R-6 Lint / Detekt / Ktlint
7. R-7 Release Build（签名）
8. R-8 手工回归检查表（manual-test-checklist.md 全表 + 设备矩阵全真机）
9. R-9 性能基准（SPEC §11 四指标达成）
10. R-10 覆盖率门禁
11. 稳定性：连续录制/分析、旋转、进程重建、低内存、快速点击、焦点中断（M10.4 清单，MT-H 组）
12. Bug 清零：P0/P1 = 0，P2 全部评估记录，P3 进 Backlog（M10.6，`docs/bugs/bug-log.md`）
13. 生成 `docs/milestones/M10-acceptance.md` 验收记录

## 6. 执行与失败处理

- 统一入口：`./gradlew` 组合命令（M1.2 定义的统一检查命令，CI 同一套）；回归报告保留为 CI artifact 或本地日志；
- 任一项失败：修复 → 重跑该项及其依赖项 → 全绿后才可通过门禁；不得跳过、不得以"已知失败"放行（除非记录为已评估 P2 且不涉及 P0/P1 语义）；
- 回归发现的缺陷：按 M10.6 流程（先加失败测试再修复）写入 `docs/bugs/bug-log.md`。

## 7. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| 0.1.0 | 2026-07-31 | 初稿（门禁回归子集 + M10.5 完整回归定义） |
