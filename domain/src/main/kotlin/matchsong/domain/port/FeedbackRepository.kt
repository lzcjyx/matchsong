package matchsong.domain.port

/**
 * 反馈仓库 Port（ARCHITECTURE.md §7.1 feedback 表，FR-HX-3）。
 * M6 由 Room FeedbackDao 实现；Fake 实现见 core:testing（FR-SHELL-3）。
 */
interface FeedbackRepository {
    suspend fun submit(feedback: FeedbackItem)

    suspend fun getAll(): List<FeedbackItem>

    /** 清空反馈（FR-HX-4 删除全部数据）。 */
    suspend fun clear()
}

/** 反馈记录最小占位模型（M2/M6 细化；type 将收敛为受控枚举）。 */
data class FeedbackItem(
    val feedbackId: String,
    val analysisId: String,
    val songId: String,
    val type: String,
    val createdAtMs: Long,
)
