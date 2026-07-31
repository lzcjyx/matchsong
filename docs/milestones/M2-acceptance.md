# M2 里程碑验收记录

- **里程碑：** M2 应用外壳与导航
- **验收日期：** 2026-07-31
- **验收人：** Coding Agent
- **总体状态：** **DONE** —— 12/12 任务完成，退出条件全部满足

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M2.1-1 路由定义与导航骨架 | **DONE** | Routes.kt（14 路由）、AppNavHost.kt、NavArgs.kt；全部 MVP 页面占位注册 |
| M2.1-2 返回栈、参数传递与状态恢复 | **DONE** | songId 类型安全参数、录音流程前进栈约定（popUpTo Prepare）、Splash 启动分流 |
| M2.1-3 导航测试套件 | **DONE** | NavigationTest（6 用例，并入 M2.5-2 执行） |
| M2.2-1 设计令牌 | **DONE** | MatchSongColors/Type/Spacing/Shape + Theme 组装（light/dark） |
| M2.2-2 基础组件库 | **DONE** | PrimaryButton、SongCard、AppTopBar（含无障碍语义） |
| M2.2-3 通用状态组件 | **DONE** | Loading/Empty/Error/Permission/QualityWarning + 质量失败文案映射（6 原因） |
| M2.3-1 Onboarding 页面 UI | **DONE** | 六项隐私说明 + 同意操作（不同意停留，ACC-2） |
| M2.3-2 同意状态持久化 | **DONE** | ConsentRepository Port + DataStore 实现 + AcceptConsentUseCase + GetOnboardingStatusUseCase + Splash 分流 + 版本常量 |
| M2.4-1 Fake Repository 全流程装配 | **DONE** | debug DI Map 多绑定（"fake" key）；Release 构建不含 Fake（assembleRelease 验证） |
| M2.4-2 模拟全流程串联 | **DONE** | 首页→录音准备→模拟录音→模拟分析→模拟结果→模拟推荐（Fake 歌曲 3 首 + 测试数据标记） |
| M2.5-1 Compose UI 测试基础设施 | **DONE** | androidTest 依赖 + createAndroidComposeRule 测试（Fake DI 自动注入） |
| M2.5-2 UI 测试用例集 | **DONE** | OnboardingFlowTest(3) + NavigationTest(6) + StateComponentsTest(4) = **13/13 通过** |

## 2. 退出条件核对（PLAN §8.4）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| 完整 UI 骨架可运行 | ✅ | Fake 全流程在模拟器实测通过 |
| 所有 MVP 页面可导航 | ✅ | 14 路由全注册，导航测试覆盖主流程 |
| Fake 数据与生产数据边界明确 | ✅ | debug 专属 DI（Map "fake" key）；Release 构建验证无 Fake 类 |
| Compose UI 测试通过 | ✅ | connectedDebugAndroidTest 13/13（AVD spike_avd） |
| 无麦克风和音频算法依赖 | ✅ | M2 无 AudioRecord/YIN 调用（占位页仅文案） |

## 3. 构建与测试状态

- 构建：`assembleDebug` + `assembleRelease` 均 BUILD SUCCESSFUL
- 单元测试：testDebugUnitTest 全过（core:common 27 + core:testing 28 + 新增 domain 用例）
- UI 测试：connectedDebugAndroidTest **13/13**（Onboarding 3 + Navigation 6 + StateComponents 4）
- 覆盖率门禁：core:common/core:testing ≥80% 通过
- 静态检查：checkQuality（lint + detekt + ktlint）全绿

## 4. 测试中发现的真实问题（已修复）

1. **Android DEX 不允许测试方法名含空格**（backtick 方法名 `home to prepare...` 报 DEX 040 错误）→ 统一驼峰命名；
2. **Hilt Singleton 的 FakeConsentRepository 跨测试共享状态** → 测试 @After 调 resetAll() 重置（Activity 在 Rule apply 时启动，早于 @Before，故 @After 才能保证下一测试干净）；
3. **AppTopBar 返回按钮文本 "←" 与测试期望 "返回" 不一致** → 统一为 "返回"；
4. **KSP 对同 FQN 注解类生成重复文件**（debug 覆盖 AppModule 方案不可行）→ 改用 Map 多绑定 + BuildConfig.DEBUG 选择器；
5. **ktlint 文件命名规则**（单顶层声明文件需与类同名）→ Type/Shape/Spacing 改名；
6. **detekt MatchingDeclarationName 对聚合文件过严**（枚举+组件、数据类+页面）→ 显式豁免并注释理由。

## 5. 遗留风险

| # | 风险 | 归属 |
|---|---|---|
| R-1 | Fake 歌曲数据为硬编码（M7 推荐引擎接入后替换） | M7 |
| R-2 | 同意记录在 DataStore，Room consent 表 M6 引入后需同步 | M6 |
| R-3 | Splash 分流在 init 读一次状态，进程重建后重新读取（SavedStateHandle 未覆盖 Splash） | M8.6 |
| R-4 | 录音/分析/质量页为占位，M3/M4/M5 逐个替换 | M3-M5 |
| R-5 | PermissionState 组件已建但未接入（M3 权限状态机） | M3 |

## 6. 验收结论

**M2 里程碑全部任务完成，验收通过。** 满足 PLAN §3.4 质量门禁：
- 完整 UI 骨架可运行（Fake 流程模拟器实测）✅
- 全部 MVP 页面可导航（14 路由）✅
- Fake 与生产边界明确（debug DI + Release 验证）✅
- Compose UI 测试 13/13 通过 ✅
- 无麦克风和音频算法依赖 ✅
- 静态检查 + 覆盖率门禁 ✅

**建议下一步：** 进入 **M3（录音系统）** —— 按 task-breakdown.md M3.1-M3.7（17 个任务）实现麦克风权限状态机、Recording Foreground Service、AudioRecord 封装（M1.4-5 接口落地）、录音状态机、PCM/WAV 存储、音量反馈与录音测试。首个任务 M3.1-1：权限状态机领域实现。
