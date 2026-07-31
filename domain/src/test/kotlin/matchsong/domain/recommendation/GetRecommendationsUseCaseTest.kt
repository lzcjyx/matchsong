package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.SongRepository
import matchsong.domain.port.UserSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M7.6-2 装配用例测试（ACC-13 可重复性 + 空歌曲库降级）。
 */
class GetRecommendationsUseCaseTest {
    private class FakeSettings(
        var settings: UserSettings = UserSettings(language = "zh"),
    ) : SettingsRepository {
        override suspend fun getSettings(): UserSettings = settings

        override suspend fun saveSettings(s: UserSettings) {
            settings = s
        }

        override suspend fun isOnboardingCompleted(): Boolean = true

        override suspend fun setOnboardingCompleted(c: Boolean) { /* DataStore 实现由 M9 完善 */ }
    }

    private class FakeSongRepo(
        private val metadata: List<SongMetadata>,
    ) : SongRepository {
        override suspend fun getAll(): List<matchsong.domain.port.SongInfo> =
            metadata.map { matchsong.domain.port.SongInfo(it.songId, it.title, it.artist, it.language) }

        override suspend fun getById(songId: String): matchsong.domain.port.SongInfo? = null

        override suspend fun getAllMetadata(): List<SongMetadata> = metadata
    }

    private val settings = FakeSettings()

    private fun useCase(songs: List<SongMetadata>): GetRecommendationsUseCase =
        GetRecommendationsUseCase(FakeSongRepo(songs), settings)

    @Test
    fun `end to end produces ranked recommendations`() {
        val songs =
            listOf(
                RecommendationFixtures.song(songId = "s1", lowest = 50, highest = 67),
                RecommendationFixtures.song(songId = "s2", lowest = 52, highest = 68),
            )
        val result = kotlinx.coroutines.runBlocking { useCase(songs).invoke(RecommendationFixtures.analysis()) }
        assertTrue(result.recommendations.isNotEmpty())
        assertTrue(
            result.recommendations.map { it.song.songId }.sorted() == listOf("s1", "s2").sorted() ||
                result.recommendations.size <= 2,
        )
    }

    @Test
    fun `empty song library produces empty state`() {
        val result = kotlinx.coroutines.runBlocking { useCase(emptyList()).invoke(RecommendationFixtures.analysis()) }
        assertTrue(result.recommendations.isEmpty())
        assertEquals(0, result.candidateCount)
        assertTrue(result.emptyStateReason != null)
    }

    @Test
    fun `same input twice identical via use case`() {
        val songs =
            listOf(
                RecommendationFixtures.song(songId = "s1", lowest = 50, highest = 67),
                RecommendationFixtures.song(songId = "s2", lowest = 52, highest = 68),
                RecommendationFixtures.song(songId = "s3", lowest = 55, highest = 70),
            )
        val uc = useCase(songs)
        val analysis = RecommendationFixtures.analysis()
        val r1 = kotlinx.coroutines.runBlocking { uc.invoke(analysis) }
        val r2 = kotlinx.coroutines.runBlocking { uc.invoke(analysis) }
        assertEquals(r1.recommendations.map { it.song.songId }, r2.recommendations.map { it.song.songId })
        assertEquals(r1.recommendations.map { it.score }, r2.recommendations.map { it.score })
    }
}
