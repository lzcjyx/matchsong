package matchsong.core.testing.fake

import kotlinx.coroutines.test.runTest
import matchsong.domain.port.AnalysisSummary
import matchsong.domain.port.FeedbackItem
import matchsong.domain.port.FeedbackType
import matchsong.domain.port.SongInfo
import matchsong.domain.port.UserSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * FakeRepositories 测试（M1.4-5 验收：增删查行为正确、返回确定性数据）。
 */
class FakeRepositoriesTest {
    // ---- FakeSongRepository ----

    @Test
    fun `歌曲仓库增查删行为`() =
        runTest {
            val repo = FakeSongRepository()
            repo.add(SongInfo("s1", "测试歌曲", "测试歌手", "zh"))
            repo.add(SongInfo("s2", "Test Song", "Test Artist", "en"))
            assertEquals(listOf("s1", "s2"), repo.getAll().map { it.songId }, "确定性排序：按 songId")
            assertEquals("测试歌曲", repo.getById("s1")?.title)
            assertNull(repo.getById("missing"))
            repo.remove("s1")
            assertEquals(listOf("s2"), repo.getAll().map { it.songId })
            repo.clear()
            assertTrue(repo.getAll().isEmpty())
        }

    @Test
    fun `歌曲仓库支持初始数据`() =
        runTest {
            val repo = FakeSongRepository(listOf(SongInfo("s1", "a", "b", "zh")))
            assertEquals(1, repo.getAll().size)
        }

    // ---- FakeAnalysisHistoryRepository ----

    @Test
    fun `历史仓库按时间倒序返回`() =
        runTest {
            val repo = FakeAnalysisHistoryRepository()
            repo.add(AnalysisSummary("a1", "s1", analyzedAtMs = 1_000, algorithmVersion = "1.0.0"))
            repo.add(AnalysisSummary("a2", "s2", analyzedAtMs = 3_000, algorithmVersion = "1.0.0"))
            repo.add(AnalysisSummary("a3", "s3", analyzedAtMs = 2_000, algorithmVersion = "1.0.0"))
            assertEquals(listOf("a2", "a3", "a1"), repo.getAll().map { it.analysisId })
            assertEquals("1.0.0", repo.getById("a1")?.algorithmVersion)
            repo.delete("a2")
            assertEquals(listOf("a3", "a1"), repo.getAll().map { it.analysisId })
            repo.clear()
            assertTrue(repo.getAll().isEmpty())
        }

    // ---- FakeSettingsRepository ----

    @Test
    fun `设置仓库保存读取与 Onboarding 标记`() =
        runTest {
            val repo = FakeSettingsRepository()
            assertFalse(repo.isOnboardingCompleted())
            assertTrue(repo.getSettings().excludedGenres.isEmpty())

            repo.saveSettings(UserSettings(language = "en", excludedGenres = listOf("rock")))
            assertEquals("en", repo.getSettings().language)
            assertEquals(listOf("rock"), repo.getSettings().excludedGenres)

            repo.setOnboardingCompleted(true)
            assertTrue(repo.isOnboardingCompleted())

            // M9.3 删除全部数据 → 恢复默认 + 标记清除（ACC-15）
            repo.clear()
            assertEquals("zh", repo.getSettings().language)
            assertTrue(repo.getSettings().excludedGenres.isEmpty())
            assertFalse(repo.isOnboardingCompleted())
        }

    // ---- FakeFavoritesRepository ----

    @Test
    fun `收藏仓库增删查行为`() =
        runTest {
            val repo = FakeFavoritesRepository()
            repo.add("song-1")
            repo.add("song-2")
            assertTrue(repo.isFavorite("song-1"))
            assertEquals(listOf("song-1", "song-2"), repo.getAll(), "确定性排序：插入序")
            repo.remove("song-1")
            assertFalse(repo.isFavorite("song-1"))
            assertTrue(repo.isFavorite("song-2"))
            repo.clear()
            assertTrue(repo.getAll().isEmpty())
        }

    // ---- FakeFeedbackRepository ----

    @Test
    fun `反馈仓库提交查询清空与重复提交更新`() =
        runTest {
            val repo = FakeFeedbackRepository()
            repo.submit(
                FeedbackItem(
                    feedbackId = "f1",
                    resultId = "result-1",
                    songId = "song-1",
                    feedbackType = FeedbackType.TOO_HIGH,
                    createdAtMs = 100,
                    appVersion = "0.1.0",
                ),
            )
            // 同一 resultId+songId 重复提交 → 更新原记录（不新增）
            repo.submit(
                FeedbackItem(
                    feedbackId = "f2",
                    resultId = "result-1",
                    songId = "song-1",
                    feedbackType = FeedbackType.SUITABLE,
                    createdAtMs = 200,
                    appVersion = "0.1.0",
                ),
            )
            assertEquals(listOf("f2"), repo.getAll().map { it.feedbackId })
            assertEquals(FeedbackType.SUITABLE, repo.getAll().single().feedbackType)
            // resultId 为 null 时仅按 songId 匹配，同样走更新
            repo.submit(
                FeedbackItem(
                    feedbackId = "f3",
                    resultId = null,
                    songId = "song-2",
                    feedbackType = FeedbackType.TOO_HARD,
                    createdAtMs = 300,
                    appVersion = "0.1.0",
                ),
            )
            repo.submit(
                FeedbackItem(
                    feedbackId = "f4",
                    resultId = null,
                    songId = "song-2",
                    feedbackType = FeedbackType.DISLIKE_STYLE,
                    createdAtMs = 400,
                    appVersion = "0.1.0",
                ),
            )
            assertEquals(listOf("f4", "f2"), repo.getAll().map { it.feedbackId })
            repo.clear()
            assertTrue(repo.getAll().isEmpty())
        }

    // ---- FakeConsentRepository ----

    @Test
    fun `同意仓库版本校验与撤销`() =
        runTest {
            val repo = FakeConsentRepository()
            assertNull(repo.getAcceptedVersion())
            assertFalse(repo.isAccepted("v1"))

            repo.accept("v1")
            assertEquals("v1", repo.getAcceptedVersion())
            assertTrue(repo.isAccepted("v1"))
            assertFalse(repo.isAccepted("v2"), "版本变更需重新同意")

            repo.accept("v2")
            assertTrue(repo.isAccepted("v2"))
            repo.revoke()
            assertNull(repo.getAcceptedVersion())
        }
}
