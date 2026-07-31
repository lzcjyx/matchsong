package matchsong.domain.port

/**
 * 分析历史仓库 Port（ARCHITECTURE.md §7.1 analysis_history 表，FR-HX-1/FR-HX-4）。
 * M6 由 Room AnalysisHistoryDao 实现；Fake 实现见 core:testing（FR-SHELL-3）。
 */
interface AnalysisHistoryRepository {
    /** 返回全部历史（确定性排序：按分析时间倒序）。 */
    suspend fun getAll(): List<AnalysisSummary>

    suspend fun getById(analysisId: String): AnalysisSummary?

    suspend fun add(summary: AnalysisSummary)

    suspend fun delete(analysisId: String)

    /** 删除全部历史（FR-HX-4 删除全部数据）。 */
    suspend fun clear()
}

/** 分析历史摘要最小占位模型（不含原始音频，FR-HX-1；M2/M6 细化）。 */
data class AnalysisSummary(
    val analysisId: String,
    val sessionId: String,
    val analyzedAtMs: Long,
    val stableLowestMidi: Double? = null,
    val stableHighestMidi: Double? = null,
    val algorithmVersion: String = "",
)
