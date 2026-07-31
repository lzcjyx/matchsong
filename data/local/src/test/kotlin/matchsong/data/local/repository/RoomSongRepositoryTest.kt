package matchsong.data.local.repository

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import matchsong.core.common.time.Clock
import matchsong.data.local.catalogJson
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.songEntity
import matchsong.data.local.songJson
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

/** 测试时钟：收藏时间戳可控。 */
private class TestClock(var now: Long = 1_000L) : Clock {
    override fun nowMillis(): Long = now

    override fun nowNanos(): Long = 0
}

/**
 * RoomSongRepository 集成测试（M6.4-2/3，Room In-Memory + Robolectric）。
 *
 * 覆盖：domain Port 方法（getAll/getById → SongInfo 映射）、搜索/筛选扩展查询、
 * 收藏增删查与收藏歌曲关联（FR-HX-2 数据侧）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomSongRepositoryTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var clock: TestClock
    private lateinit var repository: RoomSongRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        clock = TestClock()
        repository = RoomSongRepository(db.songDao(), db.favoriteDao(), clock)
        runBlocking {
            db.songDao().insertAll(
                listOf(
                    songEntity(songId = "s1", title = "晴天", artist = "周杰伦", language = "zh", genre = "流行"),
                    songEntity(
                        songId = "s2",
                        title = "Yesterday",
                        artist = "The Beatles",
                        language = "en",
                        genre = "摇滚",
                    ),
                    songEntity(songId = "s3", title = "雪之华", artist = "中岛美嘉", language = "ja", genre = "流行"),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- domain Port（M6.4-2） ----

    @Test
    fun `Port getAll 返回全部歌曲映射`() =
        runBlocking {
            val all = repository.getAll()
            assertEquals(listOf("s1", "s2", "s3"), all.map { it.songId })
            assertEquals("晴天", all.first().title)
            assertEquals("The Beatles", all[1].artist)
            assertEquals("zh", all.first().language)
        }

    @Test
    fun `Port getById 命中与未命中`() =
        runBlocking {
            val hit = repository.getById("s2")
            assertNotNull(hit)
            assertEquals("Yesterday", hit!!.title)
            assertNull(repository.getById("missing"))
        }

    // ---- 搜索/筛选（M6.4-3） ----

    @Test
    fun `搜索命中标题与歌手`() =
        runBlocking {
            assertEquals(listOf("s1"), repository.search("晴天").first().map { it.songId })
            assertEquals(listOf("s2"), repository.search("beatles").first().map { it.songId })
            assertTrue(repository.search("不存在").first().isEmpty())
        }

    @Test
    fun `组合筛选语言与风格`() =
        runBlocking {
            assertEquals(listOf("s1", "s3"), repository.filter(null, "流行").first().map { it.songId })
            assertEquals(listOf("s3"), repository.filter("ja", "流行").first().map { it.songId })
            assertEquals(3, repository.filter(null, null).first().size)
        }

    @Test
    fun `音域过滤返回重叠歌曲`() =
        runBlocking {
            // 三条 fixture 默认音域 55~72，目标 [50, 60] 全部重叠
            assertEquals(3, repository.getByRange(lowMidi = 50.0, highMidi = 60.0).first().size)
            // 目标 [10, 20] 无重叠
            assertTrue(repository.getByRange(lowMidi = 10.0, highMidi = 20.0).first().isEmpty())
        }

    // ---- 收藏关系（M6.4-3 / FR-HX-2 数据侧） ----

    @Test
    fun `收藏增删查与状态`() =
        runBlocking {
            assertFalse(repository.isFavorite("s1"))

            repository.addFavorite("s1")
            assertTrue(repository.isFavorite("s1"))
            assertEquals(listOf("s1"), repository.observeFavoriteSongIds().first())

            repository.removeFavorite("s1")
            assertFalse(repository.isFavorite("s1"))
            assertTrue(repository.observeFavoriteSongIds().first().isEmpty())
        }

    @Test
    fun `收藏歌曲关联查询返回完整元数据`() =
        runBlocking {
            repository.addFavorite("s2")
            repository.addFavorite("s1")

            val favorites = repository.observeFavoriteSongs().first()
            assertEquals(setOf("s1", "s2"), favorites.map { it.songId }.toSet())
            assertEquals("Yesterday", favorites.first { it.songId == "s2" }.title)
        }

    @Test
    fun `重复收藏刷新时间戳`() =
        runBlocking {
            repository.addFavorite("s1")
            clock.now = 9_999L
            repository.addFavorite("s1")

            val rows = db.favoriteDao().observeAll().first()
            assertEquals(1, rows.size)
            assertEquals(9_999L, rows.first().favoritedAtMs)
        }

    @Test
    fun `收藏不存在的歌曲被外键拒绝`() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception::class.java) {
            runBlocking { repository.addFavorite("no-such-song") }
        }
    }

    @Test
    fun `导入数据后 Port 可读`() =
        runBlocking {
            val importRepo = SongImportRepository(db, db.songDao(), clock)
            importRepo.import(
                catalogJson(listOf(songJson("seed-1"), songJson("seed-2"), songJson("seed-3"))),
            ).getOrThrow()

            // 版本升级：v2.0.0 目录全量替换 v1.0.0 数据（M6.4-2 替换语义）
            importRepo
                .import(
                    catalogJson(
                        listOf(
                            songJson("imported-1", dataVersion = "2.0.0"),
                            songJson("imported-2", dataVersion = "2.0.0"),
                        ),
                    ),
                )
                .getOrThrow()

            val all = repository.getAll()
            assertEquals(2, all.size)
            assertEquals(listOf("imported-1", "imported-2"), all.map { it.songId })
            assertNotNull(repository.getById("imported-1"))
        }
}
