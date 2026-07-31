package matchsong.domain.analysis

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import matchsong.core.common.time.Clock
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.AnalysisSummary
import matchsong.domain.recommendation.RecommendationRefs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * RecordAnalysisUseCase 测试（M8.4-1）。
 *
 * 覆盖：摘要字段映射（含可空音域）、推荐引用 JSON 序列化可解析、
 * analysisId 生成返回、多条记录按时间倒序。
 */
class RecordAnalysisUseCaseTest {
    private class TestClock(var now: Long) : Clock {
        override fun nowMillis(): Long = now

        override fun nowNanos(): Long = 0
    }

    private class InMemoryHistoryRepository : AnalysisHistoryRepository {
        private val items = mutableListOf<AnalysisSummary>()

        override suspend fun getAll(): List<AnalysisSummary> =
            items.sortedWith(compareByDescending<AnalysisSummary> { it.analyzedAtMs }.thenBy { it.analysisId })

        override suspend fun getById(analysisId: String): AnalysisSummary? =
            items.firstOrNull { it.analysisId == analysisId }

        override suspend fun add(summary: AnalysisSummary) {
            items.removeAll { it.analysisId == summary.analysisId }
            items.add(summary)
        }

        override suspend fun delete(analysisId: String) {
            items.removeAll { it.analysisId == analysisId }
        }

        override suspend fun clear() {
            items.clear()
        }

        override fun observeHistory(): Flow<List<AnalysisSummary>> =
            MutableStateFlow(
                items.sortedWith(compareByDescending<AnalysisSummary> { it.analyzedAtMs }.thenBy { it.analysisId }),
            )
    }

    private fun result(
        qualityUsable: Boolean = true,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        vocalRange: VocalRangeEstimate? =
            VocalRangeEstimate(
                stableLowestMidi = 48.0,
                stableHighestMidi = 69.0,
                rangeSpanSemitones = 21.0,
                coverage = 0.85,
                confidence = 0.8,
                sampleSufficiency = true,
                warning = AnalysisWarning.NONE,
            ),
        comfortRange: ComfortRangeEstimate? =
            ComfortRangeEstimate(
                comfortLowestMidi = 52.0,
                comfortHighestMidi = 64.0,
                primaryRangeLowMidi = 52.0,
                primaryRangeHighMidi = 64.0,
                confidence = 0.7,
                sampleSufficiency = true,
                estimateDisclaimer = "本次录音估计",
            ),
    ): VoiceAnalysisResult =
        VoiceAnalysisResult(
            qualityUsable = qualityUsable,
            qualityWarnings = emptyList(),
            vocalRange = vocalRange,
            comfortRange = comfortRange,
            stability = null,
            voicedFrameCount = 500,
            totalFrameCount = 650,
            confidenceLevel = confidence,
            warnings = emptyList(),
            algorithmVersion = "1.0.0",
        )

    @Test
    fun `记录映射 VoiceAnalysisResult 摘要字段`() =
        runTest {
            val repo = InMemoryHistoryRepository()
            val useCase = RecordAnalysisUseCase(repo, TestClock(now = 1_000), idGenerator = { "id-1" })

            val analysisId = useCase(result())

            assertEquals("id-1", analysisId)
            val saved = repo.getById("id-1")!!
            assertEquals(1_000, saved.analyzedAtMs)
            assertEquals(48.0, saved.stableLowestMidi!!, 0.001)
            assertEquals(69.0, saved.stableHighestMidi!!, 0.001)
            assertEquals(52.0, saved.comfortLowestMidi!!, 0.001)
            assertEquals(64.0, saved.comfortHighestMidi!!, 0.001)
            assertEquals(ConfidenceLevel.HIGH, saved.confidenceLevel)
            assertEquals("1.0.0", saved.algorithmVersion)
            assertEquals(500, saved.voicedFrameCount)
            assertTrue(saved.qualityUsable)
        }

    @Test
    fun `样本不足时音域字段为空`() =
        runTest {
            val repo = InMemoryHistoryRepository()
            val useCase = RecordAnalysisUseCase(repo, TestClock(now = 1_000), idGenerator = { "id-1" })

            useCase(
                result(
                    vocalRange = null,
                    comfortRange = null,
                    confidence = ConfidenceLevel.LOW,
                    qualityUsable = false,
                ),
            )

            val saved = repo.getById("id-1")!!
            assertNull(saved.stableLowestMidi)
            assertNull(saved.stableHighestMidi)
            assertNull(saved.comfortLowestMidi)
            assertNull(saved.comfortHighestMidi)
            assertEquals(ConfidenceLevel.LOW, saved.confidenceLevel)
            assertTrue(!saved.qualityUsable)
        }

    @Test
    fun `推荐引用序列化为 JSON 且可解析回`() =
        runTest {
            val repo = InMemoryHistoryRepository()
            val useCase = RecordAnalysisUseCase(repo, TestClock(now = 1_000), idGenerator = { "id-1" })

            useCase(
                result(),
                recommendationRefs = RecommendationRefs(songIds = listOf("song-1", "song-2"), weightsVersion = "2.0.0"),
            )

            val saved = repo.getById("id-1")!!
            val parsed = Json.decodeFromString<RecommendationRefs>(saved.recommendationRefsJson!!)
            assertEquals(listOf("song-1", "song-2"), parsed.songIds)
            assertEquals("2.0.0", parsed.weightsVersion)
        }

    @Test
    fun `无推荐时引用为 null`() =
        runTest {
            val repo = InMemoryHistoryRepository()
            val useCase = RecordAnalysisUseCase(repo, TestClock(now = 1_000), idGenerator = { "id-1" })

            useCase(result())

            assertNull(repo.getById("id-1")!!.recommendationRefsJson)
        }

    @Test
    fun `多条记录按分析时间倒序`() =
        runTest {
            val repo = InMemoryHistoryRepository()
            val clock = TestClock(now = 1_000)
            var counter = 0
            val useCase = RecordAnalysisUseCase(repo, clock, idGenerator = { "id-${++counter}" })

            useCase(result())
            clock.now = 3_000
            useCase(result())
            clock.now = 2_000
            useCase(result())

            assertEquals(listOf("id-2", "id-3", "id-1"), repo.getAll().map { it.analysisId })
        }
}
