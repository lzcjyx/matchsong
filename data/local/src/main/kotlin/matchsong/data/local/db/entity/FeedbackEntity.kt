package matchsong.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户反馈 Room 实体（M8.5-1，data-model §2.13 / §3.2 表 `user_feedback`，FR-HX-3）。
 *
 * 六类反馈（SUITABLE/TOO_HIGH/TOO_LOW/TOO_HARD/DISLIKE_STYLE/INACCURATE_REASON）
 * 以枚举名字符串存储（与 [matchsong.domain.port.FeedbackType].name 一致，避免 TypeConverter）。
 * [resultId] 可空：来源页无推荐结果（如歌曲列表入口）时允许仅按歌曲反馈。
 *
 * 索引：重复提交按（resultId + songId）查重更新，故建联合索引（M8.5-1 [推测] 更新策略）。
 * 非敏感数据（用户偏好类，仍受 FR-PRIV-5 删除流程约束）；无自由文本字段（FR-HX-3 未定义）。
 */
@Entity(
    tableName = "user_feedback",
    indices = [Index("resultId", "songId")],
)
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val feedbackId: Long = 0,
    /** 被反馈歌曲 ID。 */
    val songId: String,
    /** 关联推荐结果 ID（可空，无结果来源时为 null）。 */
    val resultId: String?,
    /** 反馈类型枚举名（FeedbackType.name）。 */
    val feedbackType: String,
    /** 提交时间（epoch 毫秒）。 */
    val createdAtMs: Long,
    /** 提交时应用版本（语义化版本）。 */
    val appVersion: String,
)
