package matchsong.data.local.repository

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import matchsong.core.common.time.Clock
import matchsong.data.local.catalogJson
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.dao.SongDao
import matchsong.data.local.db.entity.FavoriteEntity
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

/** 测试时钟：批次号合成依赖 nowMillis。 */
private class ManualClock(var now: Long = 1_000L) : Clock {
    override fun nowMillis(): Long = now

    override fun nowNanos(): Long = 0
}

/**
 * 导入仓库集成测试（M6.4-2，Room In-Memory + Robolectric）。
 *
 * 覆盖：导入行数与数据集规模一致、画像派生落库、同版本重复导入幂等、
 * 版本升级全量替换且保留存量歌曲收藏、多版本共存、批次号合成、
 * 畸形 JSON 不触碰数据库。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SongImportRepositoryTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var songDao: SongDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var clock: ManualClock
    private lateinit var repository: SongImportRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        songDao = db.songDao()
        favoriteDao = db.favoriteDao()
        clock = ManualClock()
        repository = SongImportRepository(db, songDao, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `导入后行数与数据集规模一致且画像落库`() =
        runBlocking {
            val songs = listOf(songJson(songId = "s1"), songJson(songId = "s2"), songJson(songId = "s3"))

            val result = repository.import(catalogJson(songs))

            assertTrue(result.isSuccess)
            val outcome = result.getOrThrow()
            assertTrue(outcome.replaced)
            assertEquals(setOf("1.0.0"), outcome.dataVersions)
            assertEquals(3, outcome.importedCount)
            assertEquals(3, songDao.count())
            assertEquals("每首歌应派生一条画像", 3, songDao.countProfiles())
        }

    @Test
    fun `同版本重复导入幂等跳过`() =
        runBlocking {
            val songs = listOf(songJson(songId = "s1"), songJson(songId = "s2"))
            val json = catalogJson(songs)

            val first = repository.import(json).getOrThrow()
            val second = repository.import(json).getOrThrow()

            assertTrue(first.replaced)
            assertFalse("同版本集合应幂等跳过", second.replaced)
            assertEquals(2, songDao.count())
        }

    @Test
    fun `版本升级全量替换且保留存量歌曲收藏`() =
        runBlocking {
            // v1：s1、s2；s1、s2 均被收藏
            repository.import(catalogJson(listOf(songJson("s1"), songJson("s2"))))
            favoriteDao.insert(FavoriteEntity(songId = "s1", favoritedAtMs = 1_000))
            favoriteDao.insert(FavoriteEntity(songId = "s2", favoritedAtMs = 2_000))

            // v2：s1（保留）、s2（移除）、s3（新增）
            val outcome =
                repository.import(
                    catalogJson(listOf(songJson("s1", dataVersion = "2.0.0"), songJson("s3", dataVersion = "2.0.0"))),
                ).getOrThrow()

            assertTrue(outcome.replaced)
            assertEquals(setOf("2.0.0"), outcome.dataVersions)
            assertEquals(listOf("s1", "s3"), songDao.getAll().map { it.songId })
            // 存量歌曲 s1 的收藏保留（upsert 不触发级联），被移除的 s2 的收藏级联清理
            assertNotNull(favoriteDao.getById("s1"))
            assertNull("被移除歌曲的收藏应随级联删除", favoriteDao.getById("s2"))
            assertEquals("存量歌曲数据版本应更新", "2.0.0", songDao.getById("s1")!!.dataVersion)
        }

    @Test
    fun `批次内版本不一致导入失败且不落库`() =
        runBlocking {
            // SongImportValidator 要求批次内 dataVersion 一致（M6.2-2）
            val songs =
                listOf(
                    songJson("s1", dataVersion = "1.0.0"),
                    songJson("s2", dataVersion = "2.0.0"),
                )

            val result = repository.import(catalogJson(songs))

            assertTrue(result.isFailure)
            assertEquals(0, songDao.count())
        }

    @Test
    fun `importBatchId 缺省时合成当前导入批次`() =
        runBlocking {
            clock.now = 42_000L
            repository.import(catalogJson(listOf(songJson("s1", importBatchId = null)))).getOrThrow()

            assertEquals("import-42000", songDao.getById("s1")!!.importBatchId)
        }

    @Test
    fun `导入失败后旧数据保持可用`() =
        runBlocking {
            repository.import(catalogJson(listOf(songJson("s1"))))
            val oldVersion = songDao.getById("s1")!!.dataVersion

            val result = repository.import("{\"dataVersion\":\"2.0.0\",\"songs\":[")

            assertTrue(result.isFailure)
            assertEquals("失败后旧数据保持可用", oldVersion, songDao.getById("s1")!!.dataVersion)
            assertEquals(1, songDao.count())
        }

    @Test
    fun `畸形 JSON 导入失败且不触碰数据库`() =
        runBlocking {
            val result = repository.import("not-json-at-all")

            assertTrue(result.isFailure)
            assertEquals(0, songDao.count())
        }

    @Test
    fun `空数据集导入失败`() =
        runBlocking {
            val result = repository.import(catalogJson(emptyList()))

            assertTrue(result.isFailure)
            assertEquals(0, songDao.count())
        }

    @Test
    fun `歌曲缺省 dataVersion 时解析失败`() =
        runBlocking {
            val json =
                """[{
                "songId": "s1", "title": "T", "artist": "A", "language": "zh", "genre": "pop",
                "originalKeyMidi": 60.0, "lowestMidi": 55.0, "highestMidi": 72.0,
                "tessituraLowMidi": 57.0, "tessituraHighMidi": 69.0, "rangeSpanSemitones": 17.0,
                "highNoteBurden": 0.3, "longNoteBurden": 0.2, "leapDifficulty": 0.4,
                "rhythmDifficulty": 0.5, "overallDifficulty": 0.4,
                "recommendedKeyShiftMin": -4, "recommendedKeyShiftMax": 3,
                "credibility": "HIGH", "importBatchId": "b1"
            }]"""
            // dataSource/dataVersion 为必填字段：缺省即解析失败（与 data:songs 契约一致）
            val result = repository.import(json)

            assertTrue(result.isFailure)
            assertEquals(0, songDao.count())
        }
}
