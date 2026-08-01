import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "matchsong.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "matchsong.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // M11.1 Release 签名：keystore.properties（不入库，含密码）+ matchsong-release.keystore（不入库）。
    // 生产密钥库由产品负责人安全保管（Backup/Rotation 见 docs/release/release-readiness.md）；
    // 本仓库生成的是本地开发/内部测试用密钥库。缺配置时回退 debug 签名以便开发构建。
    signingConfigs {
        create("release") {
            val props =
                Properties().apply {
                    val f = rootProject.file("keystore.properties")
                    if (f.exists()) {
                        f.inputStream().use { load(it) }
                    }
                }
            storeFile =
                rootProject.file(props.getProperty("KEYSTORE_FILE", "matchsong-release.keystore"))
                    .takeIf { it.exists() }
            storePassword = props.getProperty("STORE_PASSWORD")
            keyAlias = props.getProperty("KEY_ALIAS")
            keyPassword = props.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            // Debug 保留完整日志与测试工具（core:testing 仅 debugImplementation 引入）
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // M11.1 正式签名（keystore.properties 存在时）；缺配置回退 debug 签名保证可构建
            signingConfig =
                if (signingConfigs.getByName("release").storeFile != null) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // AndroidLogger 的 Debug/Release 分支（ARCHITECTURE.md §13，FR-PRIV-4）需要 BuildConfig.DEBUG
        buildConfig = true
    }
}

dependencies {
    // 模块依赖（ARCHITECTURE.md §3.3 依赖方向）
    implementation(project(":domain"))
    implementation(project(":data:local"))
    implementation(project(":data:songs"))
    implementation(project(":core:audio"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    debugImplementation(project(":core:testing"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // 测试
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
