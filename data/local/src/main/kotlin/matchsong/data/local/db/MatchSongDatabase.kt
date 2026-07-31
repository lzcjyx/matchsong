package matchsong.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import matchsong.data.local.db.dao.AnalysisHistoryDao
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.dao.FeedbackDao
import matchsong.data.local.db.dao.SongDao
import matchsong.data.local.db.entity.AnalysisHistoryEntity
import matchsong.data.local.db.entity.FavoriteEntity
import matchsong.data.local.db.entity.FeedbackEntity
import matchsong.data.local.db.entity.SongMetadataEntity
import matchsong.data.local.db.entity.SongRangeProfileEntity

/**
 * 歌曲 Room 数据库（M6.4-1，data-model §3.2）。
 *
 * 表：`song_metadata`（歌曲元数据）、`song_range_profile`（1:1 派生画像）、
 * `favorite`（N — N 收藏关联）、`analysis_history`（历史摘要，M8.4-1）、
 * `user_feedback`（六类反馈，M8.5-1）。
 * 版本 3（v2 = analysis_history，v3 = user_feedback；迁移见 [Migrations]）；
 * 表结构变更时递增版本号并提供 Migration（M6.5-2）。
 * exportSchema=true：schema JSON 导出至 data/local/schemas/（Migration 测试依赖）。
 *
 * Hilt 单例由 [matchsong.data.local.di.DatabaseModule] 提供。
 */
@Database(
    entities = [
        SongMetadataEntity::class,
        SongRangeProfileEntity::class,
        FavoriteEntity::class,
        AnalysisHistoryEntity::class,
        FeedbackEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MatchSongDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun analysisHistoryDao(): AnalysisHistoryDao

    abstract fun feedbackDao(): FeedbackDao

    companion object {
        /** 数据库文件名（应用私有存储）。 */
        const val DB_NAME = "matchsong.db"
    }
}
