package matchsong.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import matchsong.data.local.db.entity.FavoriteEntity

/**
 * 收藏 DAO（M6.4-1/3，FR-HX-2 数据侧）。
 *
 * 覆盖：插入（REPLACE 幂等）、删除、状态查询、收藏列表观察（按收藏时间倒序）。
 */
@Dao
interface FavoriteDao {
    /** 收藏（重复收藏按 REPLACE 幂等，时间戳更新）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE songId = :songId")
    suspend fun delete(songId: String)

    /** 收藏列表（按收藏时间倒序，最新在前）。 */
    @Query("SELECT * FROM favorite ORDER BY favoritedAtMs DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorite WHERE songId = :songId")
    suspend fun getById(songId: String): FavoriteEntity?

    /** 是否已收藏。 */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE songId = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    /** 收藏歌曲 ID 列表（按收藏时间倒序）。 */
    @Query("SELECT songId FROM favorite ORDER BY favoritedAtMs DESC")
    fun observeFavoriteSongIds(): Flow<List<String>>

    @Query("DELETE FROM favorite")
    suspend fun clearAll()
}
