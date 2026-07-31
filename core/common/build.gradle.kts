plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// core:common — 通用基础设施（错误模型/调度器/时钟/日志）
// 构建形态为 android library（被 Android 模块依赖），代码保持零 Android import（ARCHITECTURE.md P3）
android {
    namespace = "matchsong.core.common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
