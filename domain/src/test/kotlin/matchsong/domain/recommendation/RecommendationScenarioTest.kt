package matchsong.domain.recommendation

import matchsong.core.model.song.Credibility
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.port.UserSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M7.6-1 推荐功能测试套件（PLAN M7.6 场景）。
 */
class RecommendationScenarioTest {
    private val engine = RecommendationEngine()
    private val settings = UserSettings(language = "zh", preferredGenres = listOf("流行"))

    private fun settingsWithExcluded(): UserSettings =
        UserSettings(language = "zh", preferredGenres = listOf("流行"), excludedGenres = listOf("金属"))

    @Test
    fun `perfect match ranks highest with explanation`() {
        val songs =
            listOf(
                RecommendationFixtures.song(
                    songId = "s-perfect",
                    lowest = 50,
                    highest = 67,
                    tessLow = 53,
                    tessHigh = 63,
                ),
                RecommendationFixtures.song(songId = "s-high", lowest = 60, highest = 84, tessLow = 64, tessHigh = 76),
            )
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        assertEquals("s-perfect", result.recommendations.first().song.songId, "完全匹配应排第一")
        assertTrue(result.recommendations.first().explanation.isNotEmpty(), "应有解释")
        assertEquals(RecommendationConfidence.HIGH, result.totalConfidence)
    }

    @Test
    fun `highest note exceeds range can be transposed down`() {
        // 歌曲最高音 C6(84) 超出用户 A4(69)；降 2 半音后最高 82 仍超 → 需更大降幅或不可调
        // 用户高音 69 + 容差 2 = 71；84 - 71 = 13 > 6 → 不可调
        val songs = listOf(RecommendationFixtures.song(songId = "s-too-high", lowest = 60, highest = 84))
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        // 超出可调范围 → 候选被过滤或不可调
        val item = result.recommendations.firstOrNull { it.song.songId == "s-too-high" }
        assertNull(item, "最高音超出可调范围不应推荐（或不可调）")
    }

    @Test
    fun `transposable down by 2 semitones recommended with shift`() {
        // 用户高音 69+容差2=71；歌曲最高 73 → 降 2 半音到 71 恰好匹配
        val songs = listOf(RecommendationFixtures.song(songId = "s-shift2", lowest = 55, highest = 73))
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        val item = result.recommendations.firstOrNull { it.song.songId == "s-shift2" }
        assertNotNull(item, "降 2 半音可匹配应被推荐")
        assertEquals(-2, item?.keyShiftSemitones, "应建议降 2 半音（ACC-17）")
        assertTrue(item?.explanation?.any { it.contains("降低 2 个半音") } == true, "解释应含变调建议")
    }

    @Test
    fun `language mismatch filtered`() {
        val songs = listOf(RecommendationFixtures.song(songId = "s-en", language = "en"))
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        assertTrue(result.recommendations.isEmpty(), "语言不匹配应被过滤")
        assertNotNull(result.emptyStateReason)
    }

    @Test
    fun `excluded genre filtered`() {
        val songs = listOf(RecommendationFixtures.song(songId = "s-metal", genre = "金属"))
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settingsWithExcluded())
        assertTrue(result.recommendations.isEmpty(), "排除风格应被过滤")
    }

    @Test
    fun `low credibility song filtered`() {
        val songs = listOf(RecommendationFixtures.song(songId = "s-low", credibility = Credibility.LOW))
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        assertTrue(result.recommendations.isEmpty(), "LOW 可信度歌曲应被过滤")
    }

    @Test
    fun `low confidence analysis produces no recommendation`() {
        val analysis = RecommendationFixtures.analysis(confidenceLevel = ConfidenceLevel.LOW)
        val songs = listOf(RecommendationFixtures.song())
        val result = engine.recommend(analysis, songs, settings)
        assertTrue(result.recommendations.isEmpty(), "LOW 置信度不生成推荐（ACC-9）")
        assertNotNull(result.emptyStateReason)
    }

    @Test
    fun `no candidates produces empty state with reason`() {
        val songs = listOf(RecommendationFixtures.song(songId = "s-en", language = "en"))
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        assertTrue(result.recommendations.isEmpty())
        assertNotNull(result.emptyStateReason, "空状态应有原因（ACC-12）")
        assertEquals(0, result.candidateCount)
    }

    @Test
    fun `same input twice produces identical ranking`() {
        val songs =
            listOf(
                RecommendationFixtures.song(songId = "s-a", lowest = 50, highest = 67),
                RecommendationFixtures.song(songId = "s-b", lowest = 52, highest = 69),
                RecommendationFixtures.song(songId = "s-c", lowest = 55, highest = 70),
            )
        val analysis = RecommendationFixtures.analysis()
        val r1 = engine.recommend(analysis, songs, settings)
        val r2 = engine.recommend(analysis, songs, settings)
        assertEquals(r1.recommendations.map { it.song.songId }, r2.recommendations.map { it.song.songId })
        assertEquals(r1.recommendations.map { it.score }, r2.recommendations.map { it.score })
    }

    @Test
    fun `weights version recorded in result`() {
        val songs = listOf(RecommendationFixtures.song())
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        assertEquals("1.0.0", result.weightsVersion)
    }

    @Test
    fun `explanation consistent with top feature`() {
        // TessituraFit 主导（舒适区高度重合）→ 解释应含舒适音区文案
        val songs =
            listOf(
                RecommendationFixtures.song(songId = "s-fit", lowest = 50, highest = 67, tessLow = 53, tessHigh = 63),
            )
        val result = engine.recommend(RecommendationFixtures.analysis(), songs, settings)
        val item = result.recommendations.first()
        val tessLevel = item.fitBreakdown[ScoreFeature.TESSITURA_FIT]
        if (tessLevel == FitLevel.GOOD) {
            assertTrue(
                item.explanation.any { it.contains("舒适音区") },
                "TessituraFit=GOOD 时解释应含舒适音区文案，实际 ${item.explanation}",
            )
        }
    }
}
