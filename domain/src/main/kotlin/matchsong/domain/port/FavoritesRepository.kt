package matchsong.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * 收藏仓库 Port（ARCHITECTURE.md §7.1 favorites 表，FR-HX-2 / FR-SONG-4）。
 * 由 Room 实现（data:local）；Fake 实现见 core:testing（FR-SHELL-3）。
 *
 * 收藏状态经 [observeFavoriteSongIds] 以 Flow 实时同步（M8.3-1），
 * UI 侧单一数据源，避免详情页/列表页状态不一致。
 */
interface FavoritesRepository {
    /** 返回全部收藏歌曲 ID（确定性排序：Room 按收藏时间倒序，Fake 按插入序）。 */
    suspend fun getAll(): List<String>

    /** 收藏歌曲 ID 集合（按收藏时间倒序，最新在前）；任何增删即时发射。 */
    fun observeFavoriteSongIds(): Flow<Set<String>>

    suspend fun isFavorite(songId: String): Boolean

    suspend fun add(songId: String)

    suspend fun remove(songId: String)

    /** 收藏切换：已收藏则取消，未收藏则收藏（幂等）。 */
    suspend fun toggle(songId: String)

    /** 清空收藏（FR-HX-4 删除全部数据）。 */
    suspend fun clear()
}
