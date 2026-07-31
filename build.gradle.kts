// 根构建脚本：统一插件声明（apply false）与公共配置
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.gradle.versions) apply false
    alias(libs.plugins.dependency.check) apply false
}

// JaCoCo 为 Gradle 核心插件，不声明版本（版本随 Gradle 8.9）
apply(plugin = "org.gradle.jacoco")

// 依赖版本检查（M1.2-3）：dependencyUpdates 报告过时依赖
apply(plugin = "com.github.ben-manes.versions")

// 依赖漏洞扫描（M1.2-3）：dependencyCheckAnalyze
apply(plugin = "org.owasp.dependencycheck")

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    // NVD 数据源需要 API key 才能稳定下载；无 key 时任务可运行但数据可能过期（CI 记录限制）
    failBuildOnCVSS = 9.0f
    suppressionFile = "config/dependency-check/suppressions.xml"
}

// Detekt 应用于所有子项目（纯 Kotlin + Android 模块），使用统一配置
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // 统一 JDK 17 toolchain（系统默认 java 为 1.8，必须以 JAVA_HOME 指向 JDK 17）
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

// Detekt 统一配置（需在插件应用后通过 extensions 配置）
subprojects {
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
    }
}

// ---- JaCoCo 覆盖率（M1.2-2）----
// 每个子项目显式应用 jacoco（核心插件，随 Gradle 版本）
subprojects {
    apply(plugin = "org.gradle.jacoco")
    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    tasks.register("jacocoTestReport", JacocoReport::class) {
        group = "verification"
        description = "生成 JaCoCo 覆盖率报告（单模块）"
        val testTaskName = if (plugins.hasPlugin("com.android.library") || plugins.hasPlugin("com.android.application")) {
            "testDebugUnitTest"
        } else {
            "test"
        }
        dependsOn(testTaskName)

        val sourceDirs = files("src/main/kotlin")
        val classDirs = files(
            layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
            layout.buildDirectory.dir("classes/kotlin/main"),
        )
        val execData = files(
            layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"),
            layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"),
            layout.buildDirectory.file("jacoco/test.exec"),
        )

        sourceDirectories.setFrom(sourceDirs)
        classDirectories.setFrom(classDirs)
        executionData.setFrom(execData.filter { it.exists() })
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }

    // Android 模块默认不注入 JaCoCo agent；对所有 Test 任务显式启用
    tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            isEnabled = true
        }
    }
}

// 覆盖率门禁（M1.2-2）：核心逻辑模块行覆盖率 ≥ 80%（TESTING.md §4）
// 当前仅 core:common / core:testing 有实际逻辑与测试（91% / 98%）；
// domain / core:model / core:audio / data:songs 为占位与接口（0%），
// 属 M1 阶段预期，待 M3+ 业务逻辑落地后逐个启用门禁（不静默关闭，启用时在 CI 记录）。
val coverageVerifiedModules = listOf(
    ":core:common",
    ":core:testing",
)

tasks.register("jacocoCoverageVerification") {
    group = "verification"
    description = "核心模块覆盖率门禁（≥80% 行覆盖率）"
    dependsOn(
        coverageVerifiedModules.map { "$it:testDebugUnitTest" },
        coverageVerifiedModules.map { "$it:jacocoTestReport" },
    )
    doLast {
        var failed = false
        for (module in coverageVerifiedModules) {
            val modulePath = module.removePrefix(":").replace(':', '/') // ":core:common" → "core/common"
            val xmlFile = rootProject.file("$modulePath/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
            if (!xmlFile.exists()) {
                println("WARN: $module 覆盖率报告缺失，跳过校验")
                continue
            }
            val text = xmlFile.readText()
            // 解析 <counter type="LINE" missed="n" covered="n"/>
            val lineCounter = Regex("""<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""")
            var missed = 0L
            var covered = 0L
            for (m in lineCounter.findAll(text)) {
                missed += m.groupValues[1].toLong()
                covered += m.groupValues[2].toLong()
            }
            val pct = covered * 100.0 / (covered + missed)
            println("$module: 行覆盖率 ${String.format("%.1f", pct)}%")
            if (pct < 80.0) {
                println("FAIL: $module 覆盖率 ${String.format("%.1f", pct)}% < 80%")
                failed = true
            }
        }
        if (failed) {
            throw GradleException("覆盖率门禁未通过：核心模块需 ≥80% 行覆盖率")
        }
    }
}

// 聚合覆盖率报告任务（全模块）
tasks.register("jacocoFullReport") {
    group = "verification"
    description = "生成全部模块的 JaCoCo 覆盖率报告"
    dependsOn(
        subprojects.map { "${it.path}:jacocoTestReport" },
    )
}

// 统一质量检查命令：Lint + Detekt + Ktlint（PLAN M1.2）
tasks.register("checkQuality") {
    group = "verification"
    description = "统一静态检查：Android Lint + Detekt + Ktlint"
    dependsOn(
        ":app:lintDebug",
        subprojects.map { "${it.path}:detekt" },
        subprojects.map { "${it.path}:ktlintCheck" },
    )
}
