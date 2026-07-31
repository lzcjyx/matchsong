package matchsong.data.local.repository

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import matchsong.data.local.db.MatchSongDatabase
import matchsong.domain.port.FeedbackItem
import matchsong.domain.port.FeedbackType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * RoomFeedbackRepository 集成测试（M8.5-1，Room In-Memory + Robolectric）。
 *
 * 覆盖：提交/查询映射（枚举与可空 resultId 往返）、重复提交更新策略
 * （同 resultId+songId 更新而非新增，M8.5-1 [推测]）、null resultId 查重、清空。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedbackRepositoryTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var repository: RoomFeedbackRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        repository = RoomFeedbackRepository(db.feedbackDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `提交与查询往返映射`() =
        runBlocking {
            repository.submit(
                FeedbackItem(
                    feedbackId = "f1",
                    resultId = "result-1",
                    songId = "song-1",
                    feedbackType = FeedbackType.TOO_HIGH,
                    createdAtMs = 1_000,
                    appVersion = "0.1.0",
                ),
            )
            repository.submit(
                FeedbackItem(
                    feedbackId = "f2",
                    resultId = null,
                    songId = "song-2",
                    feedbackType = FeedbackType.DISLIKE_STYLE,
                    createdAtMs = 2_000,
                    appVersion = "0.1.0",
                ),
            )

            val all = repository.getAll()
            // feedbackId 由数据库自增主键生成（PK auto，M8.5-1），只保证非空唯一
            assertTrue("反馈 ID 由数据库生成且非空", all.all { it.feedbackId.isNotEmpty() })
            assertEquals("按提交时间倒序", listOf(2_000L, 1_000L), all.map { it.createdAtMs })
            val first = all.first()
            assertEquals(null, first.resultId)
            assertEquals(FeedbackType.DISLIKE_STYLE, first.feedbackType)
            assertEquals("0.1.0", first.appVersion)
            val second = all[1]
            assertEquals("result-1", second.resultId)
            assertEquals(FeedbackType.TOO_HIGH, second.feedbackType)
            assertEquals(1_000, second.createdAtMs)
        }

    @Test
    fun `同一结果同一歌曲重复提交更新而非新增`() =
        runBlocking {
            repository.submit(
                FeedbackItem(
                    feedbackId = "f1",
                    resultId = "result-1",
                    songId = "song-1",
                    feedbackType = FeedbackType.TOO_HIGH,
                    createdAtMs = 1_000,
                    appVersion = "0.1.0",
                ),
            )
            val idBefore = repository.getAll().single().feedbackId
            repository.submit(
                FeedbackItem(
                    feedbackId = "f2",
                    resultId = "result-1",
                    songId = "song-1",
                    feedbackType = FeedbackType.SUITABLE,
                    createdAtMs = 2_000,
                    appVersion = "0.2.0",
                ),
            )

            val all = repository.getAll()
            assertEquals("同 resultId+songId 只保留一条", 1, all.size)
            assertEquals("重复提交更新同一行，ID 不变", idBefore, all.single().feedbackId)
            assertEquals("类型刷新为新反馈", FeedbackType.SUITABLE, all.single().feedbackType)
            assertEquals("0.2.0", all.single().appVersion)
            assertEquals(2_000L, all.single().createdAtMs)
        }

    @Test
    fun `null resultId 同歌曲重复提交同样更新`() =
        runBlocking {
            repository.submit(
                FeedbackItem(
                    feedbackId = "f1",
                    resultId = null,
                    songId = "song-1",
                    feedbackType = FeedbackType.TOO_LOW,
                    createdAtMs = 1_000,
                    appVersion = "0.1.0",
                ),
            )
            repository.submit(
                FeedbackItem(
                    feedbackId = "f2",
                    resultId = null,
                    songId = "song-1",
                    feedbackType = FeedbackType.TOO_HARD,
                    createdAtMs = 2_000,
                    appVersion = "0.1.0",
                ),
            )
            // 不同歌曲不受影响
            repository.submit(
                FeedbackItem(
                    feedbackId = "f3",
                    resultId = null,
                    songId = "song-2",
                    feedbackType = FeedbackType.INACCURATE_REASON,
                    createdAtMs = 3_000,
                    appVersion = "0.1.0",
                ),
            )

            val all = repository.getAll()
            assertEquals("不同歌曲各保留一条，按时间倒序", listOf(3_000L, 2_000L), all.map { it.createdAtMs })
            assertEquals(
                listOf(FeedbackType.INACCURATE_REASON, FeedbackType.TOO_HARD),
                all.map { it.feedbackType },
            )
        }

    @Test
    fun `清空删除全部反馈`() =
        runBlocking {
            repository.submit(
                FeedbackItem(
                    feedbackId = "f1",
                    resultId = "result-1",
                    songId = "song-1",
                    feedbackType = FeedbackType.SUITABLE,
                    createdAtMs = 1_000,
                    appVersion = "0.1.0",
                ),
            )
            assertTrue(repository.getAll().isNotEmpty())

            repository.clear()

            assertTrue(repository.getAll().isEmpty())
        }
}
