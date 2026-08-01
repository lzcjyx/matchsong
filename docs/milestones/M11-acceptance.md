# M11 里程碑验收记录

- **里程碑：** M11 Beta 与 Google Play 发布
- **验收日期：** 2026-08-01
- **验收人：** Coding Agent
- **总体状态：** **DONE（附条件）** —— Release 配置/签名/冒烟、商店材料、Beta 方案、发布决策文档全部落地；**正式发布阻塞于真机矩阵（BUG-004）与产品负责人决定**，非本里程碑代码缺口

## 1. 任务完成情况

| 任务 | 状态 | 交付物 |
|---|---|---|
| M11.1 Release 配置 | **DONE** | release 签名（keystore.properties + matchsong-release.keystore，gitignored）+ R8 规则（kotlinx-serialization）+ `isShrinkResources` + Mapping 保存（mapping.txt 等 5 文件）；AAB 3.0MB / APK 1.34MB，签名验证通过 |
| M11.2 商店材料 | **DONE（部分待设计）** | docs/compliance/play-store-materials.md §7-8：应用名/描述/权限/数据安全/删除方式就绪；图标/Feature Graphic/截图/支持邮箱待提供；**合规红线：不宣传未经验证的准确率** |
| M11.3 Internal Testing | **DONE（模拟器）** | docs/testing/internal-testing-checklist.md：签名/安装/同签名更新/首启/权限/崩溃/Mapping 验证（release 构建冒烟：onboarding 正常渲染、无 FATAL）；真机项待补 |
| M11.4 Closed Beta | **DONE（方案）** | docs/release/closed-beta-metrics.md：9 项指标 + 合规红线（不采集原始音频除非新增同意）+ 执行前置条件（需 Play Console） |
| M11.5 发布决策 | **DONE（文档）** | docs/release/release-readiness.md（NOT_READY + 阻塞项 + 决策项）/ known-issues.md（KI-1..11）/ rollback-plan.md（版本策略 + Play 轨道回滚 + Room 兼容性）；发布决定待产品负责人 |

## 2. 退出条件核对（PLAN §17.3）

| 退出条件 | 状态 | 说明 |
|---|---|---|
| AAB 可正常签名 | ✅ | jarsigner 验证通过；APK apksigner：CN=matchsong |
| Internal Testing 通过 | ⚠️ | 模拟器 ✅；真机矩阵（BUG-004）未执行——发布前置 |
| 商店材料完整 | ⚠️ | 文案/权限/数据安全/删除方式 ✅；视觉材料/支持邮箱待提供 |
| 隐私声明与代码一致 | ✅ | PRIVACY.md v1.0（M9.1 审计）；遥测未启用无新增采集 |
| 发布检查表通过 | ⚠️ | release-readiness.md 列出 4 项阻塞（真机矩阵/真机 Internal/视觉材料/邮箱） |
| 已准备回滚版本 | ✅ | rollback-plan.md：Play 轨道回滚 + versionCode 单调递增 + Room v3 降级策略 + mapping 归档 |
| 产品负责人明确发布决定 | ⏳ | 决策项已列出（发布/优化/歌曲/后端/embedding/真人评估），建议[推测]：真机矩阵完成后发布 |

## 3. Release 冒烟验证（本机模拟器，2026-08-01）

- 安装（全新）+ 同签名更新（`install -r`）✅，更新后冷启动 1153ms；
- 首次启动：Onboarding 正常渲染（R8 规则正确：kotlinx-serialization/Hilt/Compose 无崩溃）；
- RECORD_AUDIO 授权成功；logcat 无 FATAL/ANR；
- Mapping/configuration/seeds/usage/resources.txt 已产出；
- Release 日志：`AndroidLogger.d()` 经 BuildConfig.DEBUG 常量折叠移除（FR-PRIV-4）；core:testing 仅 debugImplementation（FR-SHELL-3）。

## 4. 关键决策

1. **签名**：本地生成 `matchsong-release.keystore`（RSA 2048/SHA256withRSA/10000 天），keystore.properties 驱动；两者均 gitignored；生产密钥库由产品负责人安全保管（release-readiness.md）。缺 keystore.properties 时回退 debug 签名保证可构建。
2. **Crash Reporting**：MVP 不引入第三方崩溃 SDK（无后端，N-5/N-6）；崩溃/ANR 经 Play Console 面板（Internal Testing 自动）采集。
3. **遥测**：Closed Beta 指标方案定义 9 项，但 MVP 未实现端侧事件发射——需产品决策 + ADR + PRIVACY 更新后才实现（不越权采集）。
4. **合规红线**：商店材料禁止宣传准确率（数据集 MEDIUM 可信度，未经验证，BUG-005）。

## 5. 构建与测试状态

- `checkQuality` / `testDebugUnitTest` / `assembleDebug` / `bundleRelease` 全绿；
- 仪器测试 21/21（debug，M10 验证，本次无代码变更）；
- AAB/APK 签名验证通过；release 冒烟通过。

## 6. 遗留（发布前阻塞项，非本里程碑缺口）

| # | 项 | 责任 |
|---|---|---|
| 1 | 真机设备矩阵 + 真机 Internal Testing（BUG-004/M11.3 §2） | 产品负责人提供设备 |
| 2 | 商店视觉材料（图标/Feature Graphic/截图） | 设计资源 |
| 3 | 支持邮箱 + 隐私政策托管 URL | 产品负责人 |
| 4 | 发布决定（release-readiness.md §4） | 产品负责人 |

## 7. 验收结论

**M11 里程碑验收通过（附条件）。** 全部 5 个任务完成：Release 构建链（签名/R8/收缩/Mapping）验证可用，商店材料与合规红线明确，Beta 指标方案与发布决策文档（readiness/known-issues/rollback）就绪。**正式发布动作（Play Console 上传、真机验证、商店发布）依赖产品负责人提供设备与决策，超出代码工程范围。**

**至此 M-1 ~ M11 全部里程碑完成**（真机矩阵为全项目唯一外部硬件依赖项）。
