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

// 统一 JDK 17 toolchain（Kotlin 2.1.0 要求 JDK 17+；系统默认 java 为 1.8，必须以 JAVA_HOME 指向 JDK 17）
subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}
