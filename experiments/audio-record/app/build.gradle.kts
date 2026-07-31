// M-1.4 Spike: AudioRecord vs MediaRecorder 最小实验
// 编译目标：验证两种 API 的工程配置与代码可编译（assembleDebug）
// 运行目标：需 Android 真机/模拟器（见 docs/experiments/audio-recording-spike-results.md）
plugins {
    id("com.android.application") version "8.7.3"
    kotlin("android") version "2.1.0"
}

android {
    namespace = "matchsong.spike.audiorecord"
    compileSdk = 36

    defaultConfig {
        applicationId = "matchsong.spike.audiorecord"
        minSdk = 26 // AudioRecord 在 26+ 行为一致；覆盖绝大多数存量设备
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-spike"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
