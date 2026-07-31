package matchsong.data.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import matchsong.data.local.db.MIGRATION_1_2
import matchsong.data.local.db.MIGRATION_2_3
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.db.dao.AnalysisHistoryDao
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.dao.FeedbackDao
import matchsong.data.local.db.dao.SongDao
import matchsong.data.local.repository.RoomAnalysisHistoryRepository
import matchsong.data.local.repository.RoomFavoritesRepository
import matchsong.data.local.repository.RoomSongRepository
import matchsong.data.local.settings.DataStoreSettingsRepository
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.FavoritesRepository
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.SongRepository
import javax.inject.Singleton

/**
 * 歌曲 Room 存储 DI 装配（M6.4-1/2 + M7.6-2，data:local 内聚装配）。
 *
 * 提供：[MatchSongDatabase] 单例、两个 DAO、domain SongRepository Port 绑定
 * （实现为 [RoomSongRepository]）、SettingsRepository（DataStore，M7 推荐消费）。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMatchSongDatabase(
        @ApplicationContext context: Context,
    ): MatchSongDatabase =
        Room.databaseBuilder(
            context,
            MatchSongDatabase::class.java,
            MatchSongDatabase.DB_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

    @Provides
    fun provideSongDao(db: MatchSongDatabase): SongDao = db.songDao()

    @Provides
    fun provideFavoriteDao(db: MatchSongDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideAnalysisHistoryDao(db: MatchSongDatabase): AnalysisHistoryDao = db.analysisHistoryDao()

    @Provides
    fun provideFeedbackDao(db: MatchSongDatabase): FeedbackDao = db.feedbackDao()

    @Provides
    @Singleton
    fun provideSongRepository(repo: RoomSongRepository): SongRepository = repo

    /** M8.3-1 收藏仓库 Port 绑定（Room 实现，状态 Flow 实时同步）。 */
    @Provides
    @Singleton
    fun provideFavoritesRepository(repo: RoomFavoritesRepository): FavoritesRepository = repo

    /** M8.4-1 分析历史仓库 Port 绑定（Room 实现，FR-HX-1 数据侧）。 */
    @Provides
    @Singleton
    fun provideAnalysisHistoryRepository(repo: RoomAnalysisHistoryRepository): AnalysisHistoryRepository = repo

    @Provides
    @Singleton
    fun provideSettingsRepository(repo: DataStoreSettingsRepository): SettingsRepository = repo
}
