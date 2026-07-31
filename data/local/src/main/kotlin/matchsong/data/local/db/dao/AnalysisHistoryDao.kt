package matchsong.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import matchsong.data.local.db.entity.AnalysisHistoryEntity

/**
 * 分析历史 DAO（M8.4-1，FR-HX-1 数据侧）。
 *
 * 覆盖：插入（REPLACE 幂等）、按 ID 查询、全量查询与观察（按时间倒序）、
 * 单条删除（M9.3 联动）、清空（FR-HX-4）、计数。
 */
@Dao
interface AnalysisHistoryDao {
    /** 记录历史摘要（同 ID 重复记录按 REPLACE 覆盖）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnalysisHistoryEntity)

    @Query("SELECT * FROM analysis_history WHERE historyId = :historyId")
    suspend fun getById(historyId: String): AnalysisHistoryEntity?

    /** 全部历史（按记录时间倒序，最新在前）。 */
    @Query("SELECT * FROM analysis_history ORDER BY createdAtMs DESC")
    fun observeAllDesc(): Flow<List<AnalysisHistoryEntity>>

    /** 全部历史一次性查询（按记录时间倒序）。 */
    @Query("SELECT * FROM analysis_history ORDER BY createdAtMs DESC")
    suspend fun getAll(): List<AnalysisHistoryEntity>

    @Query("DELETE FROM analysis_history WHERE historyId = :historyId")
    suspend fun deleteById(historyId: String)

    @Query("DELETE FROM analysis_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM analysis_history")
    suspend fun count(): Int
}
