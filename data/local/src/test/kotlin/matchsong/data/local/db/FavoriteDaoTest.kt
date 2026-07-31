package matchsong.data.local.db

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.dao.SongDao
import matchsong.data.local.db.entity.FavoriteEntity
import matchsong.data.local.songEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 收藏 DAO 集成测试（M6.4-1/3，Room In-Memory + Robolectric）。
 *
 * 覆盖：收藏增删查、重复收藏幂等（时间戳刷新）、按收藏时间倒序、
 * 歌曲删除级联清理收藏（M6.4-1 级联策略明确）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoriteDaoTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var songDao: SongDao
    private lateinit var favoriteDao: FavoriteDao

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        songDao = db.songDao()
        favoriteDao = db.favoriteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `收藏增删查与状态`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "song-1")))

            favoriteDao.insert(FavoriteEntity(songId = "song-1", favoritedAtMs = 1_000))
            assertTrue(favoriteDao.isFavorite("song-1"))
            assertEquals(1_000, favoriteDao.getById("song-1")!!.favoritedAtMs)

            favoriteDao.delete("song-1")
            assertFalse(favoriteDao.isFavorite("song-1"))
            assertNull(favoriteDao.getById("song-1"))
        }

    @Test
    fun `重复收藏幂等并刷新时间戳`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "song-1")))

            favoriteDao.insert(FavoriteEntity(songId = "song-1", favoritedAtMs = 1_000))
            favoriteDao.insert(FavoriteEntity(songId = "song-1", favoritedAtMs = 2_000))

            assertEquals(1, favoriteDao.observeAll().first().size)
            assertEquals(2_000, favoriteDao.getById("song-1")!!.favoritedAtMs)
        }

    @Test
    fun `收藏列表按收藏时间倒序`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "s1"), songEntity(songId = "s2"), songEntity(songId = "s3")))
            favoriteDao.insert(FavoriteEntity(songId = "s1", favoritedAtMs = 100))
            favoriteDao.insert(FavoriteEntity(songId = "s2", favoritedAtMs = 300))
            favoriteDao.insert(FavoriteEntity(songId = "s3", favoritedAtMs = 200))

            assertEquals(listOf("s2", "s3", "s1"), favoriteDao.observeAll().first().map { it.songId })
            assertEquals(listOf("s2", "s3", "s1"), favoriteDao.observeFavoriteSongIds().first())
        }

    @Test
    fun `删除歌曲级联删除其收藏`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "gone"), songEntity(songId = "kept")))
            favoriteDao.insert(FavoriteEntity(songId = "gone", favoritedAtMs = 1_000))
            favoriteDao.insert(FavoriteEntity(songId = "kept", favoritedAtMs = 2_000))

            songDao.deleteNotIn(listOf("kept"))

            assertNull("删除歌曲必须级联清理收藏（CASCADE）", favoriteDao.getById("gone"))
            assertNotNull(favoriteDao.getById("kept"))
        }

    @Test
    fun `不存在的歌曲收藏受外键约束拒绝`() {
        // 外键指向不存在的 songId：插入必须失败（Room 默认启用 foreign_keys）
        org.junit.jupiter.api.Assertions.assertThrows(Exception::class.java) {
            runBlocking {
                favoriteDao.insert(FavoriteEntity(songId = "no-such-song", favoritedAtMs = 1_000))
            }
        }
    }
}
