package matchsong.data.local.db

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.dao.SongDao
import matchsong.data.local.db.entity.FavoriteEntity
import matchsong.data.local.db.entity.SongRangeProfileEntity
import matchsong.data.local.songEntity
import org.junit.After
import org.junit.Assert.assertEquals
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
 * 歌曲 DAO 集成测试（M6.4-1/3，Room In-Memory + Robolectric，JVM 执行）。
 *
 * 覆盖：批量 upsert、按 ID 查询、搜索（标题/歌手，大小写/中文子串）、
 * 组合筛选（语言/风格）、音域范围过滤（M7 复用）、差量清理、外键级联。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SongDaoTest {
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

    private fun profileFor(songId: String) =
        SongRangeProfileEntity(
            songId = songId,
            originalRangeLowMidi = 55,
            originalRangeHighMidi = 72,
            tessituraPosition = 0.0,
            burdenHeadroom = 0.7,
            keyShiftRangeMin = -4,
            keyShiftRangeMax = 3,
            profileVersion = "1.0.0",
        )

    // ---- 插入 / 查询（M6.4-1） ----

    @Test
    fun `批量插入后全量查询行数一致且按 songId 排序`() =
        runBlocking {
            val songs =
                listOf(
                    songEntity(songId = "b", title = "B 歌"),
                    songEntity(songId = "a", title = "A 歌"),
                    songEntity(songId = "c", title = "C 歌"),
                )
            songDao.insertAll(songs)

            assertEquals(3, songDao.count())
            val all = songDao.getAll()
            assertEquals(listOf("a", "b", "c"), all.map { it.songId })
        }

    @Test
    fun `按 ID 查询命中与未命中`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "song-1"), songEntity(songId = "song-2")))

            val hit = songDao.getById("song-1")
            assertNotNull(hit)
            assertEquals("测试歌曲", hit!!.title)
            assertNull(songDao.getById("missing"))
        }

    @Test
    fun `upsert 更新已存在行且保留外键子行`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "song-1", title = "旧标题", dataVersion = "1.0.0")))
            favoriteDao.insert(FavoriteEntity(songId = "song-1", favoritedAtMs = 1_000))
            songDao.insertProfiles(listOf(profileFor("song-1")))

            // 同主键再次 upsert：行内容更新
            songDao.insertAll(listOf(songEntity(songId = "song-1", title = "新标题", dataVersion = "1.1.0")))
            assertEquals("新标题", songDao.getById("song-1")!!.title)

            // REPLACE 会「先删后插」级联删掉收藏/画像；UPSERT 必须保留它们（M6.4-2 收藏保留策略）
            assertNotNull("upsert 不得级联删除收藏", favoriteDao.getById("song-1"))
            assertEquals("upsert 不得级联删除画像", 1, songDao.countProfiles())
        }

    @Test
    fun `数据版本集合去重返回`() =
        runBlocking {
            songDao.insertAll(
                listOf(
                    songEntity(songId = "s1", dataVersion = "1.0.0"),
                    songEntity(songId = "s2", dataVersion = "1.0.0"),
                    songEntity(songId = "s3", dataVersion = "2.0.0"),
                ),
            )
            assertEquals(listOf("1.0.0", "2.0.0"), songDao.getDataVersions().sorted())
        }

    // ---- 搜索（M6.4-3） ----

    @Test
    fun `搜索按标题子串命中`() =
        runBlocking {
            songDao.insertAll(
                listOf(
                    songEntity(songId = "s1", title = "晴天"),
                    songEntity(songId = "s2", title = "雨天"),
                    songEntity(songId = "s3", title = "雷雨交加"),
                ),
            )
            assertEquals(listOf("s1"), songDao.search("晴").first().map { it.songId })
            assertEquals(listOf("s3"), songDao.search("雷雨").first().map { it.songId })
        }

    @Test
    fun `搜索按歌手命中且 ASCII 大小写不敏感`() =
        runBlocking {
            songDao.insertAll(
                listOf(
                    songEntity(songId = "s1", title = "Song", artist = "Adele"),
                    songEntity(songId = "s2", title = "Track", artist = "Beyonce"),
                ),
            )
            assertEquals(listOf("s1"), songDao.search("adele").first().map { it.songId })
            assertEquals(listOf("s2"), songDao.search("BEYONCE").first().map { it.songId })
        }

    @Test
    fun `搜索无命中返回空列表`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "s1", title = "晴天")))
            assertTrue(songDao.search("不存在的歌").first().isEmpty())
        }

    // ---- 筛选（M6.4-3） ----

    @Test
    fun `组合筛选语言与风格`() =
        runBlocking {
            songDao.insertAll(
                listOf(
                    songEntity(songId = "s1", language = "zh", genre = "流行"),
                    songEntity(songId = "s2", language = "zh", genre = "摇滚"),
                    songEntity(songId = "s3", language = "en", genre = "流行"),
                    songEntity(songId = "s4", language = "ja", genre = "流行"),
                ),
            )
            assertEquals(listOf("s1"), songDao.filter("zh", "流行").first().map { it.songId })
            assertEquals(listOf("s1", "s3", "s4"), songDao.filter(null, "流行").first().map { it.songId })
            assertEquals(listOf("s1", "s2"), songDao.filter("zh", null).first().map { it.songId })
            assertEquals(4, songDao.filter(null, null).first().size)
        }

    // ---- 音域范围（M7 候选过滤复用） ----

    @Test
    fun `音域范围过滤返回与目标区间重叠的歌曲`() =
        runBlocking {
            songDao.insertAll(
                listOf(
                    songEntity(songId = "low", lowestMidi = 40, highestMidi = 55),
                    songEntity(songId = "mid", lowestMidi = 52, highestMidi = 70),
                    songEntity(songId = "high", lowestMidi = 68, highestMidi = 85),
                ),
            )
            // 目标 [50, 60]：low（55 ∈ 区间）与 mid（52 ≤ 60 且 70 ≥ 50）命中；high 不命中
            val hit = songDao.getByRange(lowMidi = 50.0, highMidi = 60.0).first().map { it.songId }
            assertEquals(setOf("low", "mid"), hit.toSet())
        }

    // ---- 差量清理与级联（M6.4-2 导入差量 + M6.4-1 级联策略） ----

    @Test
    fun `deleteNotIn 仅删除不在集合中的歌曲并级联其收藏`() =
        runBlocking {
            songDao.insertAll(
                listOf(
                    songEntity(songId = "keep"),
                    songEntity(songId = "remove"),
                ),
            )
            favoriteDao.insert(FavoriteEntity(songId = "keep", favoritedAtMs = 1_000))
            favoriteDao.insert(FavoriteEntity(songId = "remove", favoritedAtMs = 2_000))

            songDao.deleteNotIn(listOf("keep"))

            assertEquals(listOf("keep"), songDao.getAll().map { it.songId })
            assertNotNull(favoriteDao.getById("keep"))
            assertNull("被移除歌曲的收藏应级联删除", favoriteDao.getById("remove"))
        }

    @Test
    fun `清空歌曲表级联清空画像与收藏`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "s1")))
            favoriteDao.insert(FavoriteEntity(songId = "s1", favoritedAtMs = 1_000))
            songDao.insertProfiles(listOf(profileFor("s1")))

            songDao.clearAll()

            assertEquals(0, songDao.count())
            assertEquals(0, songDao.countProfiles())
            assertNull(favoriteDao.getById("s1"))
        }

    @Test
    fun `观察流发射初始全量数据`() =
        runBlocking {
            songDao.insertAll(listOf(songEntity(songId = "a"), songEntity(songId = "b")))
            val emitted = songDao.observeAll().first()
            assertEquals(listOf("a", "b"), emitted.map { it.songId })
        }
}
