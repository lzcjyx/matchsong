package matchsong.core.testing.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.AnalysisSummary
import matchsong.domain.port.ConsentRepository
import matchsong.domain.port.FavoritesRepository
import matchsong.domain.port.FeedbackItem
import matchsong.domain.port.FeedbackRepository
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.SongInfo
import matchsong.domain.port.SongRepository
import matchsong.domain.port.UserSettings
import java.util.LinkedHashSet

/**
 * Fake 数据工厂（ARCHITECTURE.md §16.1，FR-SHELL-3，M1.4-5）。
 *
 * 内存 Map/List/Set 实现的 domain Port 替身，返回确定性数据，
 * 供 M2 全流程串联（UI 演示）与全部测试层注入；仅 debug/test 引入，绝不进入 Release。
 * 实现为非线程安全（单线程测试约定）。
 */

class FakeSongRepository(
    initialSongs: List<SongInfo> = emptyList(),
    private val metadata: List<matchsong.core.model.song.SongMetadata> = emptyList(),
) : SongRepository {
    private val songs = mutableMapOf<String, SongInfo>()

    init {
        initialSongs.forEach { songs[it.songId] = it }
    }

    override suspend fun getAll(): List<SongInfo> = songs.values.sortedBy { it.songId }

    override suspend fun getById(songId: String): SongInfo? = songs[songId]

    /** 完整元数据（M7 推荐引擎测试注入）。 */
    override suspend fun getAllMetadata(): List<matchsong.core.model.song.SongMetadata> = metadata

    fun add(song: SongInfo) {
        songs[song.songId] = song
    }

    fun remove(songId: String) {
        songs.remove(songId)
    }

    fun clear() {
        songs.clear()
    }
}

/** 内存实现分析历史仓库（确定性排序：按分析时间倒序，再按 analysisId；M8.4-2 观察流实时同步）。 */
class FakeAnalysisHistoryRepository(
    initialItems: List<AnalysisSummary> = emptyList(),
) : AnalysisHistoryRepository {
    private val items =
        MutableStateFlow(
            initialItems.sortedWith(compareByDescending<AnalysisSummary> { it.analyzedAtMs }.thenBy { it.analysisId }),
        )

    override suspend fun getAll(): List<AnalysisSummary> = items.value

    override suspend fun getById(analysisId: String): AnalysisSummary? =
        items.value.firstOrNull { it.analysisId == analysisId }

    override suspend fun add(summary: AnalysisSummary) {
        items.value =
            (items.value.filterNot { it.analysisId == summary.analysisId } + summary)
                .sortedWith(compareByDescending<AnalysisSummary> { it.analyzedAtMs }.thenBy { it.analysisId })
    }

    override suspend fun delete(analysisId: String) {
        items.value = items.value.filterNot { it.analysisId == analysisId }
    }

    override suspend fun clear() {
        items.value = emptyList()
    }

    override fun observeHistory(): Flow<List<AnalysisSummary>> = items.asStateFlow()
}

/** 内存实现设置/Onboarding 仓库。 */
class FakeSettingsRepository(
    initialSettings: UserSettings = UserSettings(),
) : SettingsRepository {
    private var settings: UserSettings = initialSettings
    private var onboardingCompleted: Boolean = false

    override suspend fun getSettings(): UserSettings = settings

    override suspend fun saveSettings(newSettings: UserSettings) {
        settings = newSettings
    }

    override suspend fun isOnboardingCompleted(): Boolean = onboardingCompleted

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted = completed
    }

    /** M9.3 清空设置与 Onboarding 标记（删除全部数据，FR-HX-4/ACC-15）。 */
    override suspend fun clear() {
        settings = UserSettings()
        onboardingCompleted = false
    }
}

/**
 * 内存实现收藏仓库（确定性：插入序）。
 *
 * 状态经 MutableStateFlow 暴露（M8.3-1 Port 扩展）：add/remove/toggle/clear
 * 同步更新 Flow，订阅方实时收到收藏状态变更（单线程测试约定）。
 */
class FakeFavoritesRepository(
    initialSongIds: List<String> = emptyList(),
) : FavoritesRepository {
    private val favoritesState: MutableStateFlow<Set<String>> = MutableStateFlow(LinkedHashSet(initialSongIds))

    override suspend fun getAll(): List<String> = favoritesState.value.toList()

    override fun observeFavoriteSongIds(): Flow<Set<String>> = favoritesState.asStateFlow()

    override suspend fun isFavorite(songId: String): Boolean = songId in favoritesState.value

    override suspend fun add(songId: String) {
        favoritesState.value = favoritesState.value + songId
    }

    override suspend fun remove(songId: String) {
        favoritesState.value = favoritesState.value - songId
    }

    override suspend fun toggle(songId: String) {
        if (isFavorite(songId)) remove(songId) else add(songId)
    }

    override suspend fun clear() {
        favoritesState.value = emptySet()
    }
}

/** 内存实现反馈仓库（与 Room 实现一致的重复提交更新语义，M8.5-1）。 */
class FakeFeedbackRepository(
    initialItems: List<FeedbackItem> = emptyList(),
) : FeedbackRepository {
    private val items = mutableListOf<FeedbackItem>().apply { addAll(initialItems) }

    override suspend fun getAll(): List<FeedbackItem> =
        // 与 Room 实现一致：按提交时间倒序，再按 feedbackId（确定性排序）
        items.sortedWith(compareByDescending<FeedbackItem> { it.createdAtMs }.thenBy { it.feedbackId })

    override suspend fun submit(feedback: FeedbackItem) {
        val existing =
            items.indexOfFirst { it.resultId == feedback.resultId && it.songId == feedback.songId }
        if (existing >= 0) {
            items[existing] = feedback
        } else {
            items.add(feedback)
        }
    }

    override suspend fun clear() {
        items.clear()
    }
}

/** 内存实现同意记录仓库。 */
class FakeConsentRepository(
    initialVersion: String? = null,
) : ConsentRepository {
    private var acceptedVersion: String? = initialVersion

    override suspend fun getAcceptedVersion(): String? = acceptedVersion

    override suspend fun isAccepted(version: String): Boolean = acceptedVersion == version

    override suspend fun accept(version: String) {
        acceptedVersion = version
    }

    override suspend fun revoke() {
        acceptedVersion = null
    }

    companion object {
        /** 所有已创建实例（供测试重置；Hilt Singleton 跨测试共享状态）。 */
        private val instances = java.util.Collections.synchronizedList(mutableListOf<FakeConsentRepository>())

        /** 重置全部实例为未同意（M2.5 UI 测试 @Before 调用，保证测试隔离）。 */
        fun resetAll() {
            synchronized(instances) {
                instances.forEach { it.acceptedVersion = null }
            }
        }
    }

    init {
        instances.add(this)
    }
}
