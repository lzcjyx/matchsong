package matchsong.domain.port

/**
 * 设置与 Onboarding 标记仓库 Port（ARCHITECTURE.md §7.2 DataStore）。
 * M6 由 DataStore 实现；Fake 实现见 core:testing（FR-SHELL-3）。
 */
interface SettingsRepository {
    suspend fun getSettings(): UserSettings

    suspend fun saveSettings(settings: UserSettings)

    suspend fun isOnboardingCompleted(): Boolean

    suspend fun setOnboardingCompleted(completed: Boolean)
}

/** 用户设置最小占位模型（data-model.md §7.2 UserSettings；M2/M6 细化）。 */
data class UserSettings(
    val language: String = "zh",
    val preferredGenres: List<String> = emptyList(),
    val excludedGenres: List<String> = emptyList(),
)
