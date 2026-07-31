plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// core:testing — 测试工具模块：仅供 debugImplementation/testImplementation 引入，绝不放行到 Release（FR-SHELL-3）
android {
    namespace = "matchsong.core.testing"
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
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:audio"))
    // FakeRepositories 实现 domain Port 接口（M1.4-5）
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    // TestDispatcherProvider（main source set 测试工具）需要 coroutines-test；整个模块仅 debug/test 引入，不进 Release
    implementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
