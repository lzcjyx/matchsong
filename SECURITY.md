# 安全说明（Security）

- **状态：** 初稿（随 M1 工程基线建立，随里程碑演进更新）
- **依据：** `SPEC.md` §10（隐私与安全）、`ARCHITECTURE.md` §13（日志策略）、`docs/architecture/data-model.md` §4（敏感数据处理）、`PLAN.md` §18（Bug 修复工作流）

## 1. 威胁模型概述

MVP 为**无网络权限、无后端、无 API Key** 的本地单机应用（SPEC §10.3、N-8），无服务端攻击面。主要风险面：

- **依赖供应链**：第三方依赖库漏洞；
- **本地敏感数据**：录音临时文件、声音特征、历史摘要的访问与残留；
- **日志信息泄漏**：路径、设备标识、音频内容；
- **合规正确性**：隐私同意与数据删除流程的完整性（FR-PRIV-5、FR-ONB-2/3）。

## 2. 漏洞报告渠道

- 通过 GitHub 仓库提交 **Issue**（标注 security 相关标签，如可用），或直接提交包含修复的 **Pull Request**。
- 报告时请提供：受影响版本、复现步骤、预期行为与实际行为、设备与 Android 版本、日志/堆栈（**请勿在公开渠道附上任何录音、个人可识别信息**）。
- 处理流程遵循 PLAN.md §18「Bug 修复工作流」：分配 Bug ID → 记录环境 → 最小复现 → **失败测试先行** → 最小修复 → 相关/回归测试 → 更新 `docs/bugs/bug-log.md`。

## 3. 已知风险与缓解

| 风险 | 缓解措施 | 状态 |
|---|---|---|
| 依赖漏洞 | OWASP Dependency-Check（`dependencyCheckAnalyze`）+ Gradle Versions（`dependencyUpdates`）扫描（M1.2-3 落地）；原则：已知高危漏洞要么升级依赖、要么记录理由，不得掩盖（PLAN M1.2）；CI 接入（M1.3） | 计划（M1.2-3） |
| 日志信息泄漏 | Release 日志脱敏（FR-PRIV-4）：禁止输出文件路径、设备标识、任何原始音频样本与内容；仅允许聚合指标与错误类型；R8 移除 debug 日志调用（ARCHITECTURE.md §13） | 设计已冻结（M1.4-3 实现） |
| 敏感数据残留 | 原始音频仅存 cache 临时文件、分析完成即删（FR-PRIV-1）；逐帧音高轨迹不持久化；历史仅存派生摘要（FR-HX-1）；建议 Room 启用 SQLCipher 加密派生特征（data-model.md §4.4，`[推测-实现建议]`） | 设计已冻结（M9 细化） |
| 权限滥用 | 权限最小化：仅 RECORD_AUDIO + FOREGROUND_SERVICE(+_MICROPHONE)（SPEC §10.5）；录音必须伴随可见 UI + 前台通知，不静默录音（FR-REC-9） | 设计已冻结（M2/M3 实现） |
| 密钥/令牌 | 应用内不存储任何 API Key 或令牌（MVP 无后端、无网络权限） | 适用 |
| 删除流程缺陷 | 全链路删除可测（FR-PRIV-5）：单条/全部历史、收藏、设置、缓存音频、重置应用；删除全部数据后恢复首次启动（ACC-15） | 设计已冻结（M3+ 实现） |

## 4. 安全实践

1. **失败测试先行**：修复 Bug 必须先添加失败测试（PLAN §16.3、§18 步骤 10）；
2. **最小修复**：按最小改动实施修复，不夹带无关变更（PLAN §18 步骤 11）；
3. **隐私影响检查**：每次变更评估隐私与性能影响（PLAN §18 步骤 14）；
4. **静态检查**：Lint / Detekt / Ktlint 统一命令 `checkQuality`（M1.2-1 落地），CI 门禁（M1.3）；
5. **脱敏默认**：Bug 记录与公开文档不含设备标识与音频内容（PLAN §18 步骤 5、FR-PRIV-4）。

## 5. 相关文档

- 日志脱敏策略：`ARCHITECTURE.md` §13
- 敏感数据处理：`docs/architecture/data-model.md` §4
- 隐私说明：`PRIVACY.md`
- Bug 工作流：`PLAN.md` §18、`docs/bugs/bug-log.md`
- 依赖扫描任务：`docs/plans/task-breakdown.md` M1.2-3
