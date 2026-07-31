package matchsong.data.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.dao.SongDao
import matchsong.data.local.repository.RoomSongRepository
import matchsong.domain.port.SongRepository
import javax.inject.Singleton

/**
 * 歌曲 Room 存储 DI 装配（M6.4-1/2，data:local 内聚装配）。
 *
 * 提供：[MatchSongDatabase] 单例、两个 DAO、domain SongRepository Port 绑定
 * （实现为 [RoomSongRepository]）。
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
        ).build()

    @Provides
    fun provideSongDao(db: MatchSongDatabase): SongDao = db.songDao()

    @Provides
    fun provideFavoriteDao(db: MatchSongDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    @Singleton
    fun provideSongRepository(repo: RoomSongRepository): SongRepository = repo
}
