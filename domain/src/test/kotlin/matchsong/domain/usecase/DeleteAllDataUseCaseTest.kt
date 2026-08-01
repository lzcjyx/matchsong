package matchsong.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import matchsong.core.common.error.AppError
import matchsong.core.common.result.OperationResult
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.AnalysisSummary
import matchsong.domain.port.ConsentRepository
import matchsong.domain.port.FavoritesRepository
import matchsong.domain.port.FeedbackItem
import matchsong.domain.port.FeedbackRepository
import matchsong.domain.port.RecordingFileCleaner
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.UserSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M9.3 删除全部数据用例测试（FR-PRIV-5：删除流程完整可测；ACC-15 重置语义）。
 *
 * 用内联 Fake Port 验证：全量清空（历史/收藏/反馈/设置/同意/缓存）、
 * 单步失败时其余步骤仍执行（隐私最小化）、空数据幂等成功。
 */
class DeleteAllDataUseCaseTest {
    private class FakeHistory(
        var items: MutableList<AnalysisSummary> = mutableListOf(),
        var throwOnClear: Boolean = false,
    ) : AnalysisHistoryRepository {
        override suspend fun getAll(): List<AnalysisSummary> = items

        override suspend fun getById(analysisId: String): AnalysisSummary? =
            items.firstOrNull { it.analysisId == analysisId }

        override suspend fun add(summary: AnalysisSummary) {
            items.add(summary)
        }

        override suspend fun delete(analysisId: String) {
            items.removeAll { it.analysisId == analysisId }
        }

        override suspend fun clear() {
            if (throwOnClear) error("db down")
            items.clear()
        }

        override fun observeHistory(): Flow<List<AnalysisSummary>> = MutableStateFlow(items)
    }

    private class FakeFavorites(
        var ids: MutableList<String> = mutableListOf(),
    ) : FavoritesRepository {
        override suspend fun getAll(): List<String> = ids

        override fun observeFavoriteSongIds(): Flow<Set<String>> = MutableStateFlow(ids.toSet())

        override suspend fun isFavorite(songId: String): Boolean = songId in ids

        override suspend fun add(songId: String) {
            if (songId !in ids) ids.add(songId)
        }

        override suspend fun remove(songId: String) {
            ids.remove(songId)
        }

        override suspend fun toggle(songId: String) {
            if (songId in ids) remove(songId) else add(songId)
        }

        override suspend fun clear() {
            ids.clear()
        }
    }

    private class FakeFeedback(
        var items: MutableList<FeedbackItem> = mutableListOf(),
    ) : FeedbackRepository {
        override suspend fun submit(feedback: FeedbackItem) {
            items.add(feedback)
        }

        override suspend fun getAll(): List<FeedbackItem> = items

        override suspend fun clear() {
            items.clear()
        }
    }

    private class FakeSettings(
        var settings: UserSettings = UserSettings(language = "en", preferredGenres = listOf("pop")),
        var onboardingDone: Boolean = true,
    ) : SettingsRepository {
        override suspend fun getSettings(): UserSettings = settings

        override suspend fun saveSettings(s: UserSettings) {
            settings = s
        }

        override suspend fun isOnboardingCompleted(): Boolean = onboardingDone

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            onboardingDone = completed
        }

        override suspend fun clear() {
            settings = UserSettings()
            onboardingDone = false
        }
    }

    private class FakeConsent(
        var acceptedVersion: String? = "1.0",
    ) : ConsentRepository {
        override suspend fun getAcceptedVersion(): String? = acceptedVersion

        override suspend fun isAccepted(version: String): Boolean = acceptedVersion == version

        override suspend fun accept(version: String) {
            acceptedVersion = version
        }

        override suspend fun revoke() {
            acceptedVersion = null
        }
    }

    private class FakeCleaner(
        var files: MutableSet<String> = mutableSetOf(),
    ) : RecordingFileCleaner {
        override suspend fun cleanStale(
            olderThanMs: Long,
            activeSessionIds: Set<String>,
        ): Int = 0

        override suspend fun clearAll(): Int {
            val count = files.size
            files.clear()
            return count
        }
    }

    private data class Harness(
        val history: FakeHistory,
        val favorites: FakeFavorites,
        val feedback: FakeFeedback,
        val settings: FakeSettings,
        val consent: FakeConsent,
        val cleaner: FakeCleaner,
        val useCase: DeleteAllDataUseCase,
    )

    private fun harness(): Harness {
        val history = FakeHistory(mutableListOf(summary("h1")))
        val favorites = FakeFavorites(mutableListOf("song-1"))
        val feedback = FakeFeedback(mutableListOf(feedbackItem()))
        val settings = FakeSettings()
        val consent = FakeConsent(acceptedVersion = "1.0")
        val cleaner = FakeCleaner(mutableSetOf("s1.pcm", "s1.wav"))
        val useCase =
            DeleteAllDataUseCase(
                historyRepository = history,
                favoritesRepository = favorites,
                feedbackRepository = feedback,
                settingsRepository = settings,
                consentRepository = consent,
                fileCleaner = cleaner,
            )
        return Harness(history, favorites, feedback, settings, consent, cleaner, useCase)
    }

    private fun summary(id: String) = AnalysisSummary(analysisId = id, analyzedAtMs = 1_000, algorithmVersion = "1.0")

    private fun feedbackItem() =
        FeedbackItem(
            feedbackId = "f1",
            resultId = "r1",
            songId = "song-1",
            feedbackType = matchsong.domain.port.FeedbackType.SUITABLE,
            createdAtMs = 1_000,
            appVersion = "1.0.0",
        )

    @Test
    fun `删除全部数据清空历史收藏反馈设置同意与缓存`() =
        runTest {
            val h = harness()

            val result = h.useCase()

            assertTrue(result is OperationResult.Success, "期望成功，实际 $result")
            assertTrue(h.history.items.isEmpty())
            assertTrue(h.favorites.ids.isEmpty())
            assertTrue(h.feedback.items.isEmpty())
            assertEquals(UserSettings(), h.settings.settings, "设置应恢复默认")
            assertTrue(!h.settings.onboardingDone, "Onboarding 标记应清除（重新展示）")
            assertNull(h.consent.acceptedVersion, "同意记录应撤销")
            assertTrue(h.cleaner.files.isEmpty())
        }

    @Test
    fun `单步失败时其余步骤仍执行并返回失败`() =
        runTest {
            val h = harness()
            h.history.throwOnClear = true

            val result = h.useCase()

            assertTrue(result is OperationResult.Failure, "期望失败，实际 $result")
            val failure = result as OperationResult.Failure
            assertTrue(failure.error is AppError.DatabaseError, "期望 DatabaseError，实际 ${failure.error}")
            // 其余步骤不受影响仍执行（隐私最小化）
            assertTrue(h.favorites.ids.isEmpty())
            assertTrue(h.feedback.items.isEmpty())
            assertEquals(UserSettings(), h.settings.settings)
            assertNull(h.consent.acceptedVersion)
            assertTrue(h.cleaner.files.isEmpty())
        }

    @Test
    fun `空数据时幂等成功`() =
        runTest {
            val h = harness()
            h.history.items.clear()
            h.favorites.ids.clear()
            h.feedback.items.clear()
            h.settings.clear()
            h.consent.revoke()
            h.cleaner.files.clear()

            val result = h.useCase()

            assertTrue(result is OperationResult.Success)
        }
}
