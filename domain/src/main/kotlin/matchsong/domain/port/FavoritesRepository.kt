package matchsong.domain.port

/**
 * 收藏仓库 Port（ARCHITECTURE.md §7.1 favorites 表，FR-HX-2）。
 * M6 由 Room FavoriteDao 实现；Fake 实现见 core:testing（FR-SHELL-3）。
 */
interface FavoritesRepository {
    /** 返回全部收藏歌曲 ID（确定性：插入序）。 */
    suspend fun getAll(): List<String>

    suspend fun isFavorite(songId: String): Boolean

    suspend fun add(songId: String)

    suspend fun remove(songId: String)

    /** 清空收藏（FR-HX-4 删除全部数据）。 */
    suspend fun clear()
}
