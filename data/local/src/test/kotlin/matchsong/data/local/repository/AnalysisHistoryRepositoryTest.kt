package matchsong.data.local.repository

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.db.entity.AnalysisHistoryEntity
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.port.AnalysisSummary
import matchsong.domain.recommendation.RecommendationRefs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * RoomAnalysisHistoryRepository 集成测试（M8.4-1，Room In-Memory + Robolectric）。
 *
 * 覆盖：domain Port 方法（add/getAll/getById/delete/clear）映射往返、
 * observeHistory 观察流倒序与实时更新、推荐引用 JSON 可解析（task-breakdown
 * M8.4-1 测试步骤）、FR-HX-1 实体不含音频字段断言。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalysisHistoryRepositoryTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var repository: RoomAnalysisHistoryRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        repository = RoomAnalysisHistoryRepository(db.analysisHistoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun summary(
        analysisId: String,
        analyzedAtMs: Long,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        refsJson: String? = null,
    ): AnalysisSummary =
        AnalysisSummary(
            analysisId = analysisId,
            analyzedAtMs = analyzedAtMs,
            stableLowestMidi = 48.0,
            stableHighestMidi = 69.0,
            comfortLowestMidi = 52.0,
            comfortHighestMidi = 64.0,
            confidenceLevel = confidence,
            algorithmVersion = "1.0.0",
            recommendationRefsJson = refsJson,
            voicedFrameCount = 500,
            qualityUsable = true,
        )

    @Test
    fun `add 后 getAll 与 getById 映射往返`() =
        runBlocking {
            repository.add(summary(analysisId = "h1", analyzedAtMs = 1_000, confidence = ConfidenceLevel.MEDIUM))

            val loaded = repository.getById("h1")!!
            assertEquals("h1", loaded.analysisId)
            assertEquals(1_000, loaded.analyzedAtMs)
            assertEquals(48.0, loaded.stableLowestMidi!!, 0.001)
            assertEquals(69.0, loaded.stableHighestMidi!!, 0.001)
            assertEquals(52.0, loaded.comfortLowestMidi!!, 0.001)
            assertEquals(64.0, loaded.comfortHighestMidi!!, 0.001)
            assertEquals(ConfidenceLevel.MEDIUM, loaded.confidenceLevel)
            assertEquals("1.0.0", loaded.algorithmVersion)
            assertEquals(500, loaded.voicedFrameCount)
            assertTrue(loaded.qualityUsable)
            assertEquals(listOf("h1"), repository.getAll().map { it.analysisId })
        }

    @Test
    fun `getAll 按分析时间倒序`() =
        runBlocking {
            repository.add(summary(analysisId = "h1", analyzedAtMs = 1_000))
            repository.add(summary(analysisId = "h2", analyzedAtMs = 3_000))
            repository.add(summary(analysisId = "h3", analyzedAtMs = 2_000))

            assertEquals(listOf("h2", "h3", "h1"), repository.getAll().map { it.analysisId })
        }

    @Test
    fun `delete 与 clear 生效`() =
        runBlocking {
            repository.add(summary(analysisId = "h1", analyzedAtMs = 1_000))
            repository.add(summary(analysisId = "h2", analyzedAtMs = 2_000))

            repository.delete("h1")
            assertNull(repository.getById("h1"))
            assertEquals(listOf("h2"), repository.getAll().map { it.analysisId })

            repository.clear()
            assertTrue(repository.getAll().isEmpty())
            assertTrue(repository.observeHistory().first().isEmpty())
        }

    @Test
    fun `observeHistory 倒序且增删实时更新`() =
        runBlocking {
            repository.add(summary(analysisId = "h1", analyzedAtMs = 1_000))
            repository.add(summary(analysisId = "h2", analyzedAtMs = 3_000))
            assertEquals(listOf("h2", "h1"), repository.observeHistory().first().map { it.analysisId })

            repository.add(summary(analysisId = "h3", analyzedAtMs = 2_000))
            assertEquals(listOf("h2", "h3", "h1"), repository.observeHistory().first().map { it.analysisId })

            repository.delete("h2")
            assertEquals(listOf("h3", "h1"), repository.observeHistory().first().map { it.analysisId })
        }

    @Test
    fun `推荐引用 JSON 可解析为 RecommendationRefs`() =
        runBlocking {
            val refs = RecommendationRefs(songIds = listOf("song-1", "song-2"), weightsVersion = "1.0.0")
            repository.add(summary(analysisId = "h1", analyzedAtMs = 1_000, refsJson = Json.encodeToString(refs)))

            val loaded = repository.getById("h1")!!
            val parsed = Json.decodeFromString<RecommendationRefs>(loaded.recommendationRefsJson!!)
            assertEquals(listOf("song-1", "song-2"), parsed.songIds)
            assertEquals("1.0.0", parsed.weightsVersion)
        }

    @Test
    fun `实体不含原始音频字段`() {
        // FR-HX-1/ACC-14 数据侧：Entity 无 wavPath/audio 字段（反射断言，防未来误加）
        val fields = AnalysisHistoryEntity::class.java.declaredFields.map { it.name }
        assertFalse("实体不得含音频路径字段：$fields", fields.any { it.contains("wav", ignoreCase = true) })
        assertFalse("实体不得含路径字段：$fields", fields.any { it.contains("path", ignoreCase = true) })
        assertFalse("实体不得含音频字段：$fields", fields.any { it.contains("audio", ignoreCase = true) })
    }
}
