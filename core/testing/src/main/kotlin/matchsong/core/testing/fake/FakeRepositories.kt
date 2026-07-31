package matchsong.core.testing.fake

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
) : SongRepository {
    private val songs = mutableMapOf<String, SongInfo>()

    init {
        initialSongs.forEach { songs[it.songId] = it }
    }

    override suspend fun getAll(): List<SongInfo> = songs.values.sortedBy { it.songId }

    override suspend fun getById(songId: String): SongInfo? = songs[songId]

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

/** 内存实现分析历史仓库（确定性排序：按分析时间倒序，再按 analysisId）。 */
class FakeAnalysisHistoryRepository(
    initialItems: List<AnalysisSummary> = emptyList(),
) : AnalysisHistoryRepository {
    private val items = mutableMapOf<String, AnalysisSummary>()

    init {
        initialItems.forEach { items[it.analysisId] = it }
    }

    override suspend fun getAll(): List<AnalysisSummary> =
        items.values.sortedWith(compareByDescending<AnalysisSummary> { it.analyzedAtMs }.thenBy { it.analysisId })

    override suspend fun getById(analysisId: String): AnalysisSummary? = items[analysisId]

    override suspend fun add(summary: AnalysisSummary) {
        items[summary.analysisId] = summary
    }

    override suspend fun delete(analysisId: String) {
        items.remove(analysisId)
    }

    override suspend fun clear() {
        items.clear()
    }
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
}

/** 内存实现收藏仓库（确定性：插入序）。 */
class FakeFavoritesRepository(
    initialSongIds: List<String> = emptyList(),
) : FavoritesRepository {
    private val favorites: MutableSet<String> = LinkedHashSet(initialSongIds)

    override suspend fun getAll(): List<String> = favorites.toList()

    override suspend fun isFavorite(songId: String): Boolean = favorites.contains(songId)

    override suspend fun add(songId: String) {
        favorites.add(songId)
    }

    override suspend fun remove(songId: String) {
        favorites.remove(songId)
    }

    override suspend fun clear() {
        favorites.clear()
    }
}

/** 内存实现反馈仓库。 */
class FakeFeedbackRepository(
    initialItems: List<FeedbackItem> = emptyList(),
) : FeedbackRepository {
    private val items = mutableListOf<FeedbackItem>().apply { addAll(initialItems) }

    override suspend fun getAll(): List<FeedbackItem> = items.toList()

    override suspend fun submit(feedback: FeedbackItem) {
        items.add(feedback)
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
}
