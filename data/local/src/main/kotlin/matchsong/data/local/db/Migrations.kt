package matchsong.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Room 数据库迁移（M8.5-1，data-model §5.2 Room schema 版本化）。
// 版本演进：v1→v2 历史记录表；v2→v3 用户反馈表。
// 新增表为纯增量变更，无需数据搬运；迁移语句与 [MatchSongDatabase] 版本号同步维护。

/**
 * v1 → v2：新增 `analysis_history` 表（M8.4-1，FR-HX-1 历史摘要）。
 *
 * 建表 SQL 与 AnalysisHistoryEntity 的 Room 生成 schema 一致
 * （列名/类型/NOT NULL/主键），保证迁移后 TableInfo 校验通过。
 */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `analysis_history` (" +
                    "`historyId` TEXT NOT NULL, " +
                    "`createdAtMs` INTEGER NOT NULL, " +
                    "`stableLowestMidi` REAL, " +
                    "`stableHighestMidi` REAL, " +
                    "`comfortLowestMidi` REAL, " +
                    "`comfortHighestMidi` REAL, " +
                    "`confidenceLevel` TEXT NOT NULL, " +
                    "`algorithmVersion` TEXT NOT NULL, " +
                    "`recommendationRefsJson` TEXT, " +
                    "`voicedFrameCount` INTEGER NOT NULL, " +
                    "`qualityUsable` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`historyId`))",
            )
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_feedback` (
                    `feedbackId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `songId` TEXT NOT NULL,
                    `resultId` TEXT,
                    `feedbackType` TEXT NOT NULL,
                    `createdAtMs` INTEGER NOT NULL,
                    `appVersion` TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_user_feedback_resultId_songId` ON `user_feedback` (" +
                    "`resultId`, `songId`)",
            )
        }
    }
