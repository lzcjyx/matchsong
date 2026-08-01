# Internal Testing 检查表（M11.3）

- **构建：** `app-release.aab`（3.0MB，release 签名 CN=matchsong）+ `app-release.apk`（1.34MB，R8 + 资源收缩）
- **执行环境：** spike_avd（API 36，x86_64）+ 本机已执行项；**真机项待 M10.3 设备矩阵到位后补测**
- **版本：** 0.1.0 (1)

## 1. 本机已执行（release 构建冒烟，2026-08-01）

| 检查项 | 结果 | 说明 |
|---|---|---|
| 签名 | ✅ | AAB jar 签名验证通过；APK apksigner 验证：证书 CN=matchsong（SHA-256 77122bf0…） |
| 安装（全新） | ✅ | release APK 安装成功 |
| 更新（同签名覆盖） | ✅ | `install -r` 覆盖安装成功，冷启动 1153ms |
| 首次启动 | ✅ | Onboarding 正常渲染（"欢迎使用 MatchSong"/"同意并继续"），无崩溃（R8 规则正确） |
| 麦克风权限 | ✅ | `pm grant RECORD_AUDIO` 成功；权限状态机由仪器测试覆盖（debug 构建，同代码） |
| 崩溃/ANR | ✅ | 启动 + 安装/更新流程 logcat 无 FATAL/ANR |
| Mapping 文件 | ✅ | `app/build/outputs/mapping/release/`：mapping.txt/configuration.txt/seeds.txt/usage.txt/resources.txt |
| Release 日志 | ✅ | `AndroidLogger.d()` 经 BuildConfig.DEBUG 常量折叠移除（FR-PRIV-4） |
| Debug 功能隔离 | ✅ | core:testing 仅 debugImplementation；release 不含 Fake（FR-SHELL-3） |

## 2. 待真机执行（M11.3 前置：M10.3 设备矩阵）

| 检查项 | 说明 |
|---|---|
| 安装（真机全设备类型） | 低端/中端/Pixel/Samsung/中国厂商 |
| 首次启动（真机） | 厂商权限弹窗差异、后台限制 |
| 录音通知 | 前台服务通知可见性（厂商省电策略） |
| 分析→推荐（真实麦克风） | 真实人声链路端到端（模拟器虚拟麦克风不代表真实采集） |
| 删除数据 | 全删除 → 重新 Onboarding（ACC-15，真机） |
| 崩溃/ANR（真机） | Play Console 崩溃面板 + 系统 ANR 对话框 |
| 设备兼容 | Android 8.0 (API 26) ~ 16 (API 36)（BUG-011：API 26/31/34 AVD 代理未建） |

## 3. 更新策略

- 同签名覆盖安装 ✅（本机验证）；debug→release 跨签名更新预期失败（签名不匹配），正式发布路径为 Play 同签名更新。
- 版本演进：versionCode 单调递增（当前 1），versionName 语义化（当前 0.1.0）。

## 4. 结论

**M11.3 状态：部分完成**——签名/安装/更新/首启/权限/崩溃项已在 release 构建上验证；真机项因设备矩阵硬件阻塞（BUG-004）待补，为 M11 正式发布前置条件。
