package matchsong.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import matchsong.core.common.time.Clock
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.entity.FavoriteEntity
import matchsong.domain.port.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * domain FavoritesRepository Port 的 Room 实现（M8.3-1，ARCHITECTURE.md §7.1）。
 *
 * 收藏状态同步：observeFavoriteSongIds 直接转发 [FavoriteDao] 的 Flow 查询
 * （Room 表变更即时发射，UI 实时刷新，单一数据源）。
 * 排序：按收藏时间倒序（最新在前），由 DAO 查询保证。
 */
@Singleton
class RoomFavoritesRepository
    @Inject
    constructor(
        private val favoriteDao: FavoriteDao,
        private val clock: Clock,
    ) : FavoritesRepository {
        override suspend fun getAll(): List<String> = favoriteDao.observeAll().first().map { it.songId }

        override fun observeFavoriteSongIds(): Flow<Set<String>> =
            favoriteDao.observeFavoriteSongIds().map { it.toSet() }

        override suspend fun isFavorite(songId: String): Boolean = favoriteDao.isFavorite(songId)

        override suspend fun add(songId: String) {
            favoriteDao.insert(FavoriteEntity(songId = songId, favoritedAtMs = clock.nowMillis()))
        }

        override suspend fun remove(songId: String) {
            favoriteDao.delete(songId)
        }

        override suspend fun toggle(songId: String) {
            if (isFavorite(songId)) remove(songId) else add(songId)
        }

        override suspend fun clear() {
            favoriteDao.clearAll()
        }
    }
