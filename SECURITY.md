# 安全说明（Security）

- **状态：** 初稿（随 M1 工程基线建立，随里程碑演进更新）
- **依据：** `SPEC.md` §10（隐私与安全）、`ARCHITECTURE.md` §13（日志策略）、`docs/architecture/data-model.md` §4（敏感数据处理）、`PLAN.md` §18（Bug 修复工作流）

## 1. 威胁模型概述

MVP 为**无后端、无 API Key** 的本地优先单机应用（SPEC N-8），无服务端攻击面；**联网面仅限歌曲包下载（BUG-018，HTTPS GET JSON，无上传）**。主要风险面：

- **依赖供应链**：第三方依赖库漏洞；
- **本地敏感数据**：录音临时文件、声音特征、历史摘要的访问与残留；
- **歌曲包下载**：恶意/被篡改的包 URL（HTTPS + 大小上限 5MB + 解析校验拒绝非法数据；风险=恶意数据注入曲库，缓解=数据经 SongImportValidator 校验、不执行任何代码）；
- **日志信息泄漏**：路径、设备标识、音频内容；
- **合规正确性**：隐私同意与数据删除流程的完整性（FR-PRIV-5、FR-ONB-2/3）。

## 2. 漏洞报告渠道

- 通过 GitHub 仓库提交 **Issue**（标注 security 相关标签，如可用），或直接提交包含修复的 **Pull Request**。
- 报告时请提供：受影响版本、复现步骤、预期行为与实际行为、设备与 Android 版本、日志/堆栈（**请勿在公开渠道附上任何录音、个人可识别信息**）。
- 处理流程遵循 PLAN.md §18「Bug 修复工作流」：分配 Bug ID → 记录环境 → 最小复现 → **失败测试先行** → 最小修复 → 相关/回归测试 → 更新 `docs/bugs/bug-log.md`。

## 3. 已知风险与缓解

| 风险 | 缓解措施 | 状态 |
|---|---|---|
| 依赖漏洞 | OWASP Dependency-Check（`dependencyCheckAnalyze`）+ Gradle Versions（`dependencyUpdates`）扫描（M1.2-3 落地）；原则：已知高危漏洞要么升级依赖、要么记录理由，不得掩盖（PLAN M1.2）；CI 接入（M1.3） | 已落地（M1.2-3/M1.3，CI 持续执行） |
| 日志信息泄漏 | Release 日志脱敏（FR-PRIV-4）：禁止输出文件路径、设备标识、任何原始音频样本与内容；仅允许聚合指标与错误类型；R8 移除 debug 日志调用（ARCHITECTURE.md §13） | **已落地（M9.4）**：AndroidLogLogger 全部输出经 LogRedactor 脱敏（含堆栈序列化后脱敏）；RecordingSessionRunner/MatchSongApplication 统一注入 Logger；WavFileWriter/RecordingFileManager 错误消息源头不再含绝对路径 |
| 敏感数据残留 | 原始音频仅存 cache 临时文件、分析完成即删（FR-PRIV-1）；逐帧音高轨迹不持久化；历史仅存派生摘要（FR-HX-1）；`allowBackup=false` 禁止 Room 敏感特征随云备份/ADB 备份外泄 | **已落地（M9.2/M9.4）**：分析完成/重录/失败/服务销毁均触发 `cleanupSessionFiles()`；启动清理残留（M3.5-2）；manifest `allowBackup=false` + networkSecurityConfig 禁明文 |
| 权限滥用 | 权限最小化：仅 RECORD_AUDIO + FOREGROUND_SERVICE(+_MICROPHONE)（SPEC §10.5）；录音必须伴随可见 UI + 前台通知，不静默录音（FR-REC-9） | 已落地（M2/M3 实现） |
| 密钥/令牌 | 应用内不存储任何 API Key 或令牌（MVP 无后端、无网络权限） | 适用 |
| 删除流程缺陷 | 全链路删除可测（FR-PRIV-5）：单条/全部历史、收藏、设置、缓存音频、重置应用；删除全部数据后恢复首次启动（ACC-15） | **已落地（M9.3）**：DeleteAllDataUseCase + SettingsViewModel/SettingsScreen 全操作接线；Robolectric/单测覆盖 DataStore clear、RecordingFileManager clearAll、Fake 契约 |

## 4. M9.4 安全检查记录（2026-08-01）

| 检查项 | 结论 | 说明 |
|---|---|---|
| Exported Component | ✅ 合规 | 仅 MainActivity（LAUNCHER，必需 exported=true）；RecordingService exported=false；无 Receiver/Provider |
| Intent 输入 | ✅ 合规 | 无外部 Intent 处理（无 exported 组件接收外部 Intent） |
| FileProvider | ✅ 不适用 | MVP 无文件分享/导出功能，未声明 FileProvider |
| PendingIntent | ✅ 不适用 | 仅前台服务通知「停止」动作（Activity 启动式，无隐式 PendingIntent 风险） |
| Service 权限 | ✅ 合规 | 前台服务仅麦克风类型，exported=false；录音必须伴随可见 UI + 前台通知（FR-REC-9） |
| 日志脱敏 | ✅ 已修复 | AndroidLogLogger 全量脱敏（含堆栈）；Runner/Application 统一注入 Logger；错误消息源头去绝对路径 |
| Debug 工具进 Release | ✅ 合规 | core:testing 仅 debugImplementation；Fake 绑定仅在 debug source set；Release 无测试/Fake 代码（FR-SHELL-3） |
| 数据库文件 | ✅ 已加固 | `allowBackup=false` 禁止备份外泄；文件位于应用私有目录（data/data） |
| 网络安全配置 | ✅ 已声明 | `network_security_config.xml` 显式禁明文；MVP 无网络权限 |
| 第三方 SDK | ✅ 合规 | 无广告/分析/网络 SDK（依赖清单见 gradle/libs.versions.toml） |
| API Key | ✅ 合规 | 仓库与运行时不存任何密钥（MVP 无后端） |
| 依赖漏洞 | ✅ CI 覆盖 | dependencyCheckAnalyze + Gradle Versions 于 CI 持续执行（M1.3） |

遗留：Room 未启用 SQLCipher（data-model §4.4 `[推测-实现建议]`）——MVP 本地威胁面低（无备份/无网络），列为 M10 可选优化项。

## 5. 安全实践

1. **失败测试先行**：修复 Bug 必须先添加失败测试（PLAN §16.3、§18 步骤 10）；
2. **最小修复**：按最小改动实施修复，不夹带无关变更（PLAN §18 步骤 11）；
3. **隐私影响检查**：每次变更评估隐私与性能影响（PLAN §18 步骤 14）；
4. **静态检查**：Lint / Detekt / Ktlint 统一命令 `checkQuality`（M1.2-1 落地），CI 门禁（M1.3）；
5. **脱敏默认**：Bug 记录与公开文档不含设备标识与音频内容（PLAN §18 步骤 5、FR-PRIV-4）。

## 6. 相关文档

- 日志脱敏策略：`ARCHITECTURE.md` §13
- 敏感数据处理：`docs/architecture/data-model.md` §4
- 隐私说明：`PRIVACY.md`
- Bug 工作流：`PLAN.md` §18、`docs/bugs/bug-log.md`
- 依赖扫描任务：`docs/plans/task-breakdown.md` M1.2-3
