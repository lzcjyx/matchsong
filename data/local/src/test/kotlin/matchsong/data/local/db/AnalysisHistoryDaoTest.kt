package matchsong.data.local.db

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import matchsong.data.local.analysisHistoryEntity
import matchsong.data.local.db.dao.AnalysisHistoryDao
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 分析历史 DAO 集成测试（M8.4-1，Room In-Memory + Robolectric）。
 *
 * 覆盖：插入/读取往返（含可空音域字段）、计数、按时间倒序观察、
 * 单条删除、清空、同 ID 重复插入幂等（REPLACE）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalysisHistoryDaoTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var dao: AnalysisHistoryDao

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        dao = db.analysisHistoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `插入后按 ID 读回全字段`() =
        runBlocking {
            val entity =
                analysisHistoryEntity(
                    historyId = "h1",
                    createdAtMs = 1_000,
                    stableLowestMidi = 48.0,
                    stableHighestMidi = 69.0,
                    comfortLowestMidi = null,
                    comfortHighestMidi = null,
                    confidenceLevel = "MEDIUM",
                    algorithmVersion = "2.0.0",
                    recommendationRefsJson = """{"songIds":["song-1","song-2"],"weightsVersion":"1.0.0"}""",
                    voicedFrameCount = 300,
                    qualityUsable = true,
                )

            dao.insert(entity)

            val loaded = dao.getById("h1")!!
            assertEquals(1_000, loaded.createdAtMs)
            assertEquals(48.0, loaded.stableLowestMidi!!, 0.001)
            assertEquals(69.0, loaded.stableHighestMidi!!, 0.001)
            assertNull(loaded.comfortLowestMidi)
            assertNull(loaded.comfortHighestMidi)
            assertEquals("MEDIUM", loaded.confidenceLevel)
            assertEquals("2.0.0", loaded.algorithmVersion)
            assertTrue(loaded.recommendationRefsJson!!.contains("song-1"))
            assertEquals(300, loaded.voicedFrameCount)
            assertTrue(loaded.qualityUsable)
        }

    @Test
    fun `重复插入同 ID 幂等覆盖`() =
        runBlocking {
            dao.insert(analysisHistoryEntity(historyId = "h1", createdAtMs = 1_000, voicedFrameCount = 300))
            dao.insert(analysisHistoryEntity(historyId = "h1", createdAtMs = 2_000, voicedFrameCount = 500))

            assertEquals(1, dao.count())
            assertEquals(2_000, dao.getById("h1")!!.createdAtMs)
            assertEquals(500, dao.getById("h1")!!.voicedFrameCount)
        }

    @Test
    fun `观察列表按记录时间倒序`() =
        runBlocking {
            dao.insert(analysisHistoryEntity(historyId = "h1", createdAtMs = 1_000))
            dao.insert(analysisHistoryEntity(historyId = "h2", createdAtMs = 3_000))
            dao.insert(analysisHistoryEntity(historyId = "h3", createdAtMs = 2_000))

            val ids = dao.observeAllDesc().first().map { it.historyId }
            assertEquals(listOf("h2", "h3", "h1"), ids)
            assertEquals(listOf("h2", "h3", "h1"), dao.getAll().map { it.historyId })
        }

    @Test
    fun `删除单条与清空`() =
        runBlocking {
            dao.insert(analysisHistoryEntity(historyId = "h1"))
            dao.insert(analysisHistoryEntity(historyId = "h2"))
            assertEquals(2, dao.count())

            dao.deleteById("h1")
            assertNull(dao.getById("h1"))
            assertEquals(1, dao.count())

            dao.clearAll()
            assertEquals(0, dao.count())
            assertTrue(dao.observeAllDesc().first().isEmpty())
        }
}
