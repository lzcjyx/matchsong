package matchsong.data.local.repository

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import matchsong.core.common.time.Clock
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.songEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** 测试时钟：收藏时间戳可控（命名避开 RoomSongRepositoryTest 的 TestClock，防 JVM 类名冲突）。 */
private class FavoritesTestClock(var now: Long = 1_000L) : Clock {
    override fun nowMillis(): Long = now

    override fun nowNanos(): Long = 0
}

/**
 * RoomFavoritesRepository 集成测试（M8.3-1，Room In-Memory + Robolectric）。
 *
 * 覆盖：收藏/取消幂等（toggle）、收藏列表排序（收藏时间倒序）、
 * Flow 状态实时同步、重复收藏刷新时间戳、外键约束、删除歌曲级联清理（FR-HX-2 数据侧）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoritesRepositoryTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var clock: FavoritesTestClock
    private lateinit var repository: RoomFavoritesRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        clock = FavoritesTestClock()
        repository = RoomFavoritesRepository(db.favoriteDao(), clock)
        runBlocking {
            db.songDao().insertAll(
                listOf(
                    songEntity(songId = "s1", title = "晴天", artist = "周杰伦"),
                    songEntity(songId = "s2", title = "Yesterday", artist = "The Beatles"),
                    songEntity(songId = "s3", title = "雪之华", artist = "中岛美嘉"),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `收藏取消与 toggle 幂等`() =
        runBlocking {
            assertFalse(repository.isFavorite("s1"))

            repository.add("s1")
            assertTrue(repository.isFavorite("s1"))

            repository.toggle("s1")
            assertFalse("toggle 应取消已收藏", repository.isFavorite("s1"))

            repository.toggle("s1")
            assertTrue("toggle 应收藏未收藏", repository.isFavorite("s1"))

            repository.remove("s1")
            assertFalse(repository.isFavorite("s1"))
        }

    @Test
    fun `收藏列表按收藏时间倒序`() =
        runBlocking {
            repository.add("s2")
            clock.now = 2_000L
            repository.add("s1")
            clock.now = 3_000L
            repository.add("s3")

            assertEquals(listOf("s3", "s1", "s2"), repository.getAll())
        }

    @Test
    fun `observeFavoriteSongIds Flow 实时同步`() =
        runBlocking {
            assertTrue(repository.observeFavoriteSongIds().first().isEmpty())

            repository.add("s1")
            assertEquals(setOf("s1"), repository.observeFavoriteSongIds().first())

            repository.add("s2")
            assertEquals(setOf("s1", "s2"), repository.observeFavoriteSongIds().first())

            repository.remove("s1")
            assertEquals(setOf("s2"), repository.observeFavoriteSongIds().first())

            repository.clear()
            assertTrue(repository.observeFavoriteSongIds().first().isEmpty())
        }

    @Test
    fun `重复收藏刷新时间戳且不重复`() =
        runBlocking {
            repository.add("s1")
            clock.now = 9_999L
            repository.add("s1")

            val rows = db.favoriteDao().observeAll().first()
            assertEquals(1, rows.size)
            assertEquals(9_999L, rows.first().favoritedAtMs)
        }

    @Test
    fun `收藏不存在的歌曲被外键拒绝`() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception::class.java) {
            runBlocking { repository.add("no-such-song") }
        }
    }

    @Test
    fun `删除歌曲后收藏级联清理`() =
        runBlocking {
            repository.add("s1")
            repository.add("s2")
            assertEquals(setOf("s1", "s2"), repository.observeFavoriteSongIds().first())

            // 删除 s1、s3（保留 s2）：级联删除 s1 的收藏，s2 的收藏保留
            db.songDao().deleteNotIn(listOf("s2"))

            assertEquals(setOf("s2"), repository.observeFavoriteSongIds().first())
            assertFalse(repository.isFavorite("s1"))
            assertTrue(repository.isFavorite("s2"))
        }
}
