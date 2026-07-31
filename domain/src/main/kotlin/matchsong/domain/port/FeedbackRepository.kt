package matchsong.domain.port

/**
 * 反馈仓库 Port（ARCHITECTURE.md §7.1 user_feedback 表，FR-HX-3）。
 * Room 实现见 data:local RoomFeedbackRepository；Fake 实现见 core:testing（FR-SHELL-3）。
 */
interface FeedbackRepository {
    /** 提交一条反馈；同一 resultId+songId 重复提交时更新原记录（M8.5-1 [推测] 策略）。 */
    suspend fun submit(feedback: FeedbackItem)

    /** 全部反馈（按提交时间倒序）。 */
    suspend fun getAll(): List<FeedbackItem>

    /** 清空反馈（FR-HX-4 删除全部数据）。 */
    suspend fun clear()
}

/**
 * 用户反馈类型（FR-HX-3 六类，data-model §2.13）。
 * MVP 仅保存，不自动调整推荐权重（PLAN M8.5）。
 */
enum class FeedbackType {
    /** 适合唱。 */
    SUITABLE,

    /** 太高：歌曲整体音高偏高。 */
    TOO_HIGH,

    /** 太低：歌曲整体音高偏低。 */
    TOO_LOW,

    /** 太难：演唱难度超出预期。 */
    TOO_HARD,

    /** 不喜欢该风格。 */
    DISLIKE_STYLE,

    /** 推荐理由不准确。 */
    INACCURATE_REASON,
}

/** 反馈记录（data-model §2.13 UserFeedback；M8.5-1 由占位模型收敛为受控枚举）。 */
data class FeedbackItem(
    val feedbackId: String,
    /** 关联推荐结果 ID；来源页无推荐结果（如歌曲列表入口）时为空。 */
    val resultId: String?,
    val songId: String,
    val feedbackType: FeedbackType,
    /** 提交时间（epoch 毫秒）。 */
    val createdAtMs: Long,
    /** 提交时应用版本（语义化版本，便于后续反馈分析）。 */
    val appVersion: String,
)
