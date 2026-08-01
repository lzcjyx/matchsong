plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Room schema 导出目录（exportSchema=true；M6.5-2 Migration 测试依赖历史 schema JSON）
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// data:local — Room/DataStore/文件缓存实现（实现 domain Port，ARCHITECTURE.md §3.3）
android {
    namespace = "matchsong.data.local"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // M9.4 修复：库模块默认 legacy runner（android.test.InstrumentationTestRunner）已在 API 28+
        // 移除，API 36 上 instrumentation 启动即崩溃——统一使用 androidx AndroidJUnitRunner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric 需要 Android 资源/清单环境（Room In-Memory JVM 测试，ARCHITECTURE.md §18）
            isIncludeAndroidResources = true
        }
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data:songs"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.junit5.engine)
    // Robolectric JVM 测试（Room In-Memory）：JUnit4 runner 经 vintage engine 在 JUnit Platform 下执行
    testImplementation(libs.junit4)
    testImplementation(libs.junit5.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    // BUG-018 歌曲包下载器测试（MockWebServer，仅网络模块使用）
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // M9.4 修复：AndroidJUnitRunner 为独立 artifact（ext.junit 不再传递引入），
    // 库模块 connected 测试需要显式声明，否则 runner 类缺失启动崩溃
    androidTestImplementation(libs.androidx.test.runner)
}
