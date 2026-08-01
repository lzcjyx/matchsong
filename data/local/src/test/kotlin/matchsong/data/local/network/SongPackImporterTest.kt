package matchsong.data.local.network

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import matchsong.core.common.time.SystemClock
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.repository.SongImportRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

/**
 * BUG-018 歌曲包导入器测试：Fake 下载器 + Room In-Memory。
 *
 * 覆盖：替换语义（新版本事务替换）、失败不落库（下载失败/解析失败保持原库）。
 */
@RunWith(RobolectricTestRunner::class)
class SongPackImporterTest {
    private lateinit var db: MatchSongDatabase
    private lateinit var importRepository: SongImportRepository

    private val builtinJson =
        // 内置数据集第 1 首的简化合法条目（dataVersion 1.0.0）
        """
        [
          {
            "songId": "s-1", "title": "内置歌", "artist": "测试", "language": "zh", "genre": "流行",
            "originalKeyMidi": 60, "lowestMidi": 55, "highestMidi": 67,
            "tessituraLowMidi": 57, "tessituraHighMidi": 65, "rangeSpanSemitones": 12,
            "highNoteBurden": 0.1, "longNoteBurden": 0.4, "leapDifficulty": 0.5,
            "rhythmDifficulty": 0.4, "overallDifficulty": 0.3,
            "recommendedKeyShiftMin": -3, "recommendedKeyShiftMax": 3,
            "dataSource": "测试", "credibility": "MEDIUM", "dataVersion": "1.0.0"
          }
        ]
        """.trimIndent()

    private val packJson =
        """
        [
          {
            "songId": "p-1", "title": "包内歌", "artist": "周杰伦", "language": "zh", "genre": "流行",
            "originalKeyMidi": 66, "lowestMidi": 50, "highestMidi": 71,
            "tessituraLowMidi": 54, "tessituraHighMidi": 65, "rangeSpanSemitones": 21,
            "highNoteBurden": 0.2, "longNoteBurden": 0.4, "leapDifficulty": 0.5,
            "rhythmDifficulty": 0.4, "overallDifficulty": 0.35,
            "recommendedKeyShiftMin": -3, "recommendedKeyShiftMax": 3,
            "dataSource": "公开调性资料+听辨[推测]", "credibility": "MEDIUM", "dataVersion": "2.0.0"
          },
          {
            "songId": "p-2", "title": "包内歌二", "artist": "周杰伦", "language": "zh", "genre": "流行",
            "originalKeyMidi": 64, "lowestMidi": 48, "highestMidi": 69,
            "tessituraLowMidi": 52, "tessituraHighMidi": 63, "rangeSpanSemitones": 21,
            "highNoteBurden": 0.15, "longNoteBurden": 0.5, "leapDifficulty": 0.55,
            "rhythmDifficulty": 0.5, "overallDifficulty": 0.4,
            "recommendedKeyShiftMin": -3, "recommendedKeyShiftMax": 3,
            "dataSource": "公开调性资料+听辨[推测]", "credibility": "MEDIUM", "dataVersion": "2.0.0"
          }
        ]
        """.trimIndent()

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                MatchSongDatabase::class.java,
            ).allowMainThreadQueries().build()
        importRepository = SongImportRepository(db, db.songDao(), SystemClock)
        // 预置内置曲库
        runBlocking { importRepository.import(builtinJson) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private class FakeFetcher(
        var body: String? = null,
        var error: Exception? = null,
    ) : SongPackFetcher {
        override suspend fun fetch(url: String): String {
            error?.let { throw it }
            return body ?: throw IOException("空响应")
        }
    }

    @Test
    fun `pack import replaces builtin catalog`() =
        runBlocking {
            val importer = SongPackImporter(FakeFetcher(body = packJson), importRepository)

            val outcome = importer.importPack("https://example.com/pack.json").getOrThrow()

            assertEquals(2, outcome.importedCount)
            assertTrue(outcome.replaced)
            assertEquals(2L, db.songDao().count().toLong())
            // 内置歌被差量清理（删除不在新版本中的歌曲）
            assertTrue(db.songDao().countProfiles() <= 2L)
        }

    @Test
    fun `download failure keeps database unchanged`() =
        runBlocking {
            val importer = SongPackImporter(FakeFetcher(error = IOException("网络不可用")), importRepository)

            val result = importer.importPack("https://example.com/pack.json")

            assertTrue(result.isFailure)
            assertEquals("失败不应触碰数据库", 1L, db.songDao().count().toLong())
        }

    @Test
    fun `invalid pack json keeps database unchanged`() =
        runBlocking {
            val importer = SongPackImporter(FakeFetcher(body = "{not json"), importRepository)

            val result = importer.importPack("https://example.com/bad.json")

            assertTrue(result.isFailure)
            assertEquals(1L, db.songDao().count().toLong())
        }
}
