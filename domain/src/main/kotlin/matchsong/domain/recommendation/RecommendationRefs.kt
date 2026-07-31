package matchsong.domain.recommendation

import kotlinx.serialization.Serializable

/**
 * 推荐结果引用（M8.4-1，data-model §5.2 权重版本化、task-breakdown M8.4-1 风险项）。
 *
 * 随历史摘要持久化（JSON 字符串），使历史结果可追溯（ACC-13）：
 * 记录当时的 Top 推荐歌曲 ID 与权重版本；不存储分数明细（分数可随代码重算，
 * 引用仅用于历史详情回溯跳转）。
 */
@Serializable
data class RecommendationRefs(
    /** Top 推荐歌曲 ID（顺序 = 推荐排序，M7）。 */
    val songIds: List<String>,
    /** 推荐权重版本（M7.3，语义化）。 */
    val weightsVersion: String,
)
