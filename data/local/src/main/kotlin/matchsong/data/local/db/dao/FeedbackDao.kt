package matchsong.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import matchsong.data.local.db.entity.FeedbackEntity

/**
 * 反馈 DAO（M8.5-1，FR-HX-3 数据侧）。
 *
 * 覆盖：插入、更新（重复提交）、全部查询（按提交时间倒序）、清空。
 * 写操作全部 suspend（Room 线程安全）。
 */
@Dao
interface FeedbackDao {
    /** 插入一条反馈，返回自增主键。 */
    @Insert
    suspend fun insert(entity: FeedbackEntity): Long

    /** 更新一条已有反馈（重复提交时刷新类型/时间/版本）。 */
    @Update
    suspend fun update(entity: FeedbackEntity)

    /**
     * 按（resultId + songId）查找已有反馈（重复提交查重用）。
     * 使用 SQLite `IS` 做空安全比较：resultId 为 null 时也能正确匹配。
     */
    @Query("SELECT * FROM user_feedback WHERE resultId IS :resultId AND songId = :songId LIMIT 1")
    suspend fun findByResultAndSong(
        resultId: String?,
        songId: String,
    ): FeedbackEntity?

    /** 全部反馈（按提交时间倒序，最新在前）。 */
    @Query("SELECT * FROM user_feedback ORDER BY createdAtMs DESC")
    suspend fun getAll(): List<FeedbackEntity>

    /** 清空全部反馈（FR-HX-4 删除全部数据）。 */
    @Query("DELETE FROM user_feedback")
    suspend fun clearAll()
}
