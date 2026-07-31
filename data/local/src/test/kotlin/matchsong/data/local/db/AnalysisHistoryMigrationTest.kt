package matchsong.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import matchsong.data.local.analysisHistoryEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Room Migration 集成测试（M8.4-1，M6.5-2 约定）。
 *
 * 手工构造 v1 数据库（DDL 取自导出的 schemas/1.json），写入 v1 数据后经
 * MIGRATION_1_2 + MIGRATION_2_3 打开到当前版本：
 * - 旧数据（song_metadata/favorite）完整保留；
 * - 新表 analysis_history（M8.4-1）与 user_feedback（M8.5-1）可用。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalysisHistoryMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        dbFile(MIGRATION_TEST_DB).delete()
    }

    private fun dbFile(name: String): File = context.getDatabasePath(name)

    /** 手工构造 v1 数据库（DDL 与 schemas/matchsong.data.local.db.MatchSongDatabase/1.json 一致）。 */
    private fun createV1Database(name: String) {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(V1_SONG_METADATA_DDL)
                            db.execSQL(V1_SONG_RANGE_PROFILE_DDL)
                            db.execSQL(V1_FAVORITE_DDL)
                            db.execSQL(
                                "INSERT INTO song_metadata (songId, title, artist, language, genre, originalKeyMidi, " +
                                    "lowestMidi, highestMidi, tessituraLowMidi, tessituraHighMidi, " +
                                    "rangeSpanSemitones, " +
                                    "highNoteBurden, longNoteBurden, leapDifficulty, rhythmDifficulty, " +
                                    "overallDifficulty, " +
                                    "recommendedKeyShiftMin, recommendedKeyShiftMax, audioUrl, dataSource, " +
                                    "credibility, " +
                                    "dataVersion, importBatchId) VALUES ('s1', '晴天', '周杰伦', 'zh', '流行', 60, 55, 72, " +
                                    "57, 69, 17, 0.3, 0.2, 0.4, 0.5, 0.4, -4, 3, NULL, 'test-dataset', " +
                                    "'HIGH', '1.0.0', 'batch-1')",
                            )
                            db.execSQL("INSERT INTO favorite (songId, favoritedAtMs) VALUES ('s1', 1000)")
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build()
        factory.create(config).writableDatabase.close()
    }

    @Test
    fun `v1 升级到当前版本保留旧数据且 analysis_history 可用`() =
        runBlocking {
            dbFile(MIGRATION_TEST_DB).delete()
            createV1Database(MIGRATION_TEST_DB)

            val db =
                Room.databaseBuilder(context, MatchSongDatabase::class.java, MIGRATION_TEST_DB)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .allowMainThreadQueries()
                    .build()
            try {
                // v1 数据完整保留（迁移禁止丢数据，M6.5-2）
                val song = db.songDao().getById("s1")!!
                assertEquals("晴天", song.title)
                assertEquals(1_000, db.favoriteDao().getById("s1")!!.favoritedAtMs)

                // 新表 analysis_history 可用（M8.4-1）
                db.analysisHistoryDao().insert(analysisHistoryEntity(historyId = "h1", createdAtMs = 1_000))
                assertEquals(1, db.analysisHistoryDao().count())
                assertEquals(48.0, db.analysisHistoryDao().getById("h1")!!.stableLowestMidi!!, 0.001)
                assertEquals(500, db.analysisHistoryDao().getById("h1")!!.voicedFrameCount)
            } finally {
                db.close()
            }
        }

    private companion object {
        const val MIGRATION_TEST_DB = "migration-test.db"

        const val V1_SONG_METADATA_DDL =
            "CREATE TABLE IF NOT EXISTS `song_metadata` (`songId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`artist` TEXT NOT NULL, `language` TEXT NOT NULL, `genre` TEXT NOT NULL, " +
                "`originalKeyMidi` INTEGER NOT NULL, `lowestMidi` INTEGER NOT NULL, `highestMidi` INTEGER NOT NULL, " +
                "`tessituraLowMidi` INTEGER NOT NULL, `tessituraHighMidi` INTEGER NOT NULL, " +
                "`rangeSpanSemitones` INTEGER NOT NULL, `highNoteBurden` REAL NOT NULL, " +
                "`longNoteBurden` REAL NOT NULL, `leapDifficulty` REAL NOT NULL, `rhythmDifficulty` REAL NOT NULL, " +
                "`overallDifficulty` REAL NOT NULL, `recommendedKeyShiftMin` INTEGER NOT NULL, " +
                "`recommendedKeyShiftMax` INTEGER NOT NULL, `audioUrl` TEXT, `dataSource` TEXT NOT NULL, " +
                "`credibility` TEXT NOT NULL, `dataVersion` TEXT NOT NULL, `importBatchId` TEXT NOT NULL, " +
                "PRIMARY KEY(`songId`))"

        const val V1_SONG_RANGE_PROFILE_DDL =
            "CREATE TABLE IF NOT EXISTS `song_range_profile` (`songId` TEXT NOT NULL, " +
                "`originalRangeLowMidi` INTEGER NOT NULL, `originalRangeHighMidi` INTEGER NOT NULL, " +
                "`tessituraPosition` REAL NOT NULL, `burdenHeadroom` REAL NOT NULL, " +
                "`keyShiftRangeMin` INTEGER NOT NULL, `keyShiftRangeMax` INTEGER NOT NULL, " +
                "`profileVersion` TEXT NOT NULL, PRIMARY KEY(`songId`), " +
                "FOREIGN KEY(`songId`) REFERENCES `song_metadata`(`songId`) ON UPDATE NO ACTION ON DELETE CASCADE )"

        const val V1_FAVORITE_DDL =
            "CREATE TABLE IF NOT EXISTS `favorite` (`songId` TEXT NOT NULL, `favoritedAtMs` INTEGER NOT NULL, " +
                "PRIMARY KEY(`songId`), FOREIGN KEY(`songId`) REFERENCES `song_metadata`(`songId`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
    }
}
