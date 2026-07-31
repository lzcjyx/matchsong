# M-1.1 仓库审计报告

- **任务：** M-1.1 仓库审计
- **状态：** DONE
- **审计日期：** 2026-07-30
- **审计人：** Coding Agent
- **仓库：** https://github.com/lzcjyx/matchsong.git
- **本地路径：** `D:/vibecoding/matchsong`

---

## 1. 仓库定性结论

**当前仓库是一个全新的、最小化的 Kotlin/JVM 脚手架项目，不是 Android 工程，也不是原型或现有产品。**

- 远程仓库 `https://github.com/lzcjyx/matchsong.git` 在本次连接前为**空仓库**（无任何 ref）。
- 本地目录在连接前**未初始化 git**（无 `.git`），仅包含一个最小 Kotlin/JVM Gradle 工程骨架与 `PLAN.md`。
- 本次 M-1.1 审计时已完成 `git init`、添加 `origin`、首次提交并推送到 `origin/main`。

因此，M-1.1 的核心结论是：**没有可复用模块，没有高风险旧代码，不存在已有音频/后端代码，项目处于真正的零起点。** 所有正式产品功能将在 M0/M1 起从零构建。

---

## 2. 目录结构（审计时点）

```text
D:/vibecoding/matchsong/
├── .git/                      # 本次审计中初始化
├── .gitignore                 # 本次审计中新增
├── .gradle/                   # Gradle 缓存（已 gitignore）
├── .lsp/                      # 本地 Kotlin LSP 工具（已 gitignore，非项目源码）
├── PLAN.md                    # 项目总计划（39KB，2301 行，M-1..M11）
├── build.gradle.kts           # 根构建脚本
├── gradle.properties          # Gradle 配置
├── settings.gradle.kts        # 项目设置
└── src/main/kotlin/matchsong/
    └── Main.kt                # 占位入口
```

本次审计新增的 `docs/`、`experiments/` 目录在审计完成后产生，不计入“已有代码”审计范围。

---

## 3. 是否已存在 Android 工程

**否。**

- 没有 `app/` 模块，没有 `AndroidManifest.xml`，没有 `build.gradle.kts` 中的 `com.android.application` / `com.android.library` 插件。
- 当前 `build.gradle.kts` 仅应用 `kotlin("jvm") version "2.1.0"`，是一个**纯 JVM** 工程，不是 Android 工程。
- 没有 `res/`、`drawable/`、`values/`、`mipmap/` 等资源目录。
- 没有 Activity、Fragment、Compose、Hilt、Room 等 Android 依赖。

**含义：** PLAN.md §2.2 默认技术栈（Jetpack Compose / Hilt / Room / AudioRecord / Foreground Service 等）在当前仓库中**完全未落地**，需要在 M1（Android 工程基线）中从零搭建 Android Gradle 工程。当前 JVM 脚手架仅适合做 M-1 阶段的算法可行性 Spike（如纯 Kotlin 的音高检测），不适合直接演化为 Android 产品工程。

---

## 4. 构建工具版本

| 项目 | 版本 | 来源 |
|---|---|---|
| Gradle | 8.5 | 系统安装 `D:\env\gradle-8.5`（无 `gradlew` wrapper） |
| Android Gradle Plugin | 未配置 | 无 Android 插件 |
| Kotlin | 2.1.0 | `build.gradle.kts` plugin 声明 |
| Gradle 自带 Kotlin | 1.9.20 | `gradle --version` |
| Groovy | 3.0.17 | `gradle --version` |
| JVM (运行 Gradle) | 17.0.20 Eclipse Adoptium | `JAVA_HOME=D:\scoop\apps\temurin17-jdk\current` |
| JVM (系统默认 java) | 1.8.0_202 | `where java` 第一个命中 |

### 关键风险：JDK 双版本冲突

- `build.gradle.kts` 声明 `kotlin { jvmToolchain(17) }`，要求 JDK 17。
- 系统默认 `java` 命令指向 **JDK 1.8**（`D:\Program Files\Java\jdk1.8.0_202\bin\java.exe`），但 `JAVA_HOME` 已正确指向 **temurin17**（`D:\scoop\apps\temurin17-jdk\current`）。
- 实测：在 `JAVA_HOME` 指向 temurin17 时，`gradle tasks` **BUILD SUCCESSFUL**（45s）。
- 风险：若任何工具/IDE 不继承 `JAVA_HOME` 而走 `PATH`，会拿到 JDK 1.8，导致 Kotlin 2.1.0 编译失败（Kotlin 2.x 编译需要 JDK 17+）。
- **建议（M1 落地时）：** 统一团队 JDK 17，并在 Android 工程中引入 `gradlew` wrapper 锁定 Gradle 版本，避免依赖系统全局 Gradle。

### 没有 Gradle Wrapper

当前仓库**没有** `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`。构建依赖系统全局 `gradle`。这不符合可复现构建要求，应在 M1 补齐 `gradle wrapper --gradle-version 8.5`。

---

## 5. Kotlin / Compose 配置

- **Kotlin：** 2.1.0（`build.gradle.kts` plugin）。
- **Compose：** **未配置**。没有 `org.jetbrains.kotlin.plugin.compose` 插件，没有 `androidx.compose.*` 依赖，没有 Compose Compiler 选项。
- 当前为 JVM 工程，Compose（Android only）无法在此脚手架上运行。

---

## 6. 已有模块

| 模块 | 存在 | 说明 |
|---|---|---|
| `app` (Android application) | 否 | 需 M1 创建 |
| `:core:audio` | 否 | 需 M3/M5 创建 |
| `:core:recommend` | 否 | 需 M7 创建 |
| `:data:song` | 否 | 需 M6 创建 |
| 任何 library module | 否 | — |

当前只有一个未命名的 root 工程（`rootProject.name = "matchsong"`），`src/main/kotlin/matchsong/Main.kt` 为唯一源文件，内容仅 `println("matchsong")`。

---

## 7. 已有测试

**无。**

- 没有 `src/test/` 目录。
- 没有任何测试依赖（JUnit、MockK、Turbine 等均未声明）。
- `gradle test` 任务存在但无测试可执行。

**含义：** PLAN.md §2.2 测试栈（JUnit / MockK / Turbine / Room In-Memory / Compose UI Test / AndroidX Test / MockWebServer / Macrobenchmark / Lint / Detekt / Ktlint）全部需在 M1 起逐步引入。

---

## 8. CI 配置

**无。**

- 没有 `.github/workflows/` 目录。
- 没有 `.gitlab-ci.yml`、`bitrise.yml`、`circle.yml` 等。
- 没有 `Dangerfile`、`renovate.json`、`dependabot.yml`。

**含义：** CI/CD 应在 M1（Android 工程基线）建立，至少包含：编译、单元测试、Lint/Detekt/Ktlint。远程仓库为 GitHub，建议使用 GitHub Actions。

---

## 9. Git 状态

| 项 | 值 |
|---|---|
| 初始状态 | 未初始化 git（审计前） |
| 审计后 `git init` | 已完成 |
| 默认分支 | `main` |
| 远程 `origin` | `https://github.com/lzcjyx/matchsong.git` |
| 首次提交 | `cb54ca4` "Initial project scaffold..." |
| 已推送到 `origin/main` | 是 |
| 未提交修改 | 审计后产生的 `docs/`、`experiments/`（属 M-1 交付物，单独提交） |
| Git 用户 | Li Chaoyi `<lizhichaojiyingxiong@gmail.com>` |
| Git 版本 | 2.52.0.windows.1 |

---

## 10. 已存在的音频代码

**无。**

- 没有 `AudioRecord`、`MediaRecorder`、`Oboe`、`TarsosDSP`、`JTransforms` 等任何音频相关 import 或依赖。
- 没有 PCM/WAV 处理代码。
- 没有麦克风权限相关代码。

---

## 11. 已存在的后端或 API

**无。**

- 没有任何 HTTP 客户端依赖（OkHttp / Retrofit / Ktor）。
- 没有服务端代码。
- 没有 API 定义。
- PLAN.md §2.2 明确 MVP 优先本地音频处理，不引入云端实时音频分析；本审计未发现任何偏离该原则的既有代码。

---

## 12. 可复用模块与高风险旧代码

- **可复用模块：** 无（项目为零起点）。
- **高风险旧代码：** 无。
- **唯一既有源文件** `Main.kt` 为占位符，无任何业务逻辑，无风险，将在 M1 Android 化时被替换或移除。
- **`PLAN.md`** 是项目唯一有价值的既有资产，作为后续所有里程碑的执行依据，不得擅自修改其里程碑结构与规则（任务状态字段除外，按 PLAN §3.3 维护）。

---

## 13. 与 PLAN.md 的差距分析

| PLAN.md 要求（M-1 阶段及之后） | 当前状态 | 差距 | 归属里程碑 |
|---|---|---|---|
| Kotlin / Gradle Kotlin DSL | 有 Kotlin DSL，但非 Android | 需转 Android Gradle 工程 | M1 |
| Jetpack Compose / Material 3 | 无 | 全部待建 | M1/M2 |
| Hilt / Room / DataStore | 无 | 全部待建 | M1 |
| AudioRecord / Foreground Service | 无 | M-1 Spike 验证 + M3 实现 | M-1.4 / M3 |
| YIN 音高检测 | 无 | M-1 Spike 验证 + M5 实现 | M-1.5 / M5 |
| 测试栈 (JUnit/MockK/Turbine/...) | 无 | M1 引入 | M1 |
| Detekt / Ktlint / Lint | 无 | M1 引入 | M1 |
| CI (GitHub Actions) | 无 | M1 引入 | M1 |
| Gradle Wrapper | 无 | M1 补齐 | M1 |
| `docs/` 交付物目录 | 本次审计新建 | 进行中 | M-1 |

---

## 14. 审计结论与遗留风险

### 结论

1. 仓库是**空项目**（零起点），不存在现有产品或原型。
2. **无可复用模块，无高风险旧代码**，不修改任何现有核心功能（无核心功能可改）。
3. 当前脚手架为**纯 Kotlin/JVM**，可用于 M-1 阶段的算法 Spike，但**不能**直接演化为 Android 产品，M1 需要重建为 Android Gradle 工程。
4. 远程仓库已连接，初始代码已推送至 `origin/main`。

### 遗留风险（记录供后续里程碑处理）

| 风险 | 影响 | 缓解 | 归属 |
|---|---|---|---|
| R-1.1 系统默认 `java` 为 JDK 1.8，与 Kotlin 2.1.0 / toolchain 17 冲突 | 若工具走 PATH 而非 JAVA_HOME，编译失败 | 统一 JDK 17；IDE/CI 显式设置 JAVA_HOME | M1 |
| R-1.2 无 Gradle Wrapper | 构建不可复现，依赖系统全局 Gradle | `gradle wrapper --gradle-version 8.5` | M1 |
| R-1.3 当前为 JVM 工程非 Android 工程 | PLAN 技术栈无法落地 | M1 重建为 Android Gradle 工程 | M1 |
| R-1.4 无 CI | 质量门禁无自动化 | M1 建立 GitHub Actions | M1 |
| R-1.5 本开发机无 Android SDK / 设备 / 麦克风采集能力 | M-1.4 AudioRecord Spike 无法在真机验证 | 见 M-1.4 可行性文档，需 Android 环境补做 | M-1.4 |

---

## 15. 验收条件核对

- [x] 明确仓库是空项目、原型还是现有产品 → **空项目（零起点脚手架）**
- [x] 明确可复用模块 → **无**
- [x] 明确高风险旧代码 → **无**
- [x] 不修改现有核心功能 → **无核心功能，未修改任何业务代码**

M-1.1 验收通过。
