pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "matchsong"

// 8 个真实 Gradle 模块（ARCHITECTURE.md §3.2）
include(":app")
include(":core:common")
include(":core:model")
include(":core:audio")
include(":core:testing")
include(":data:local")
include(":data:songs")
include(":domain")
