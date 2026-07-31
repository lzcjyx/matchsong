package matchsong.domain.port

import kotlinx.coroutines.flow.Flow
import matchsong.domain.analysis.ConfidenceLevel

/**
 * 分析历史仓库 Port（ARCHITECTURE.md §7.1 analysis_history 表，FR-HX-1/FR-HX-4）。
 *
 * M8.4-1 由 Room 的 [matchsong.data.local.repository.RoomAnalysisHistoryRepository]
 * 实现；Fake 实现见 core:testing（FR-SHELL-3）。只保存分析摘要，不含原始音频
 * 与逐帧轨迹（FR-HX-1/ACC-14）。
 */
interface AnalysisHistoryRepository {
    /** 返回全部历史（确定性排序：按分析时间倒序）。 */
    suspend fun getAll(): List<AnalysisSummary>

    suspend fun getById(analysisId: String): AnalysisSummary?

    suspend fun add(summary: AnalysisSummary)

    suspend fun delete(analysisId: String)

    /** 删除全部历史（FR-HX-4 删除全部数据）。 */
    suspend fun clear()

    /** 观察历史列表（按分析时间倒序；M8.4-2 历史页数据源）。 */
    fun observeHistory(): Flow<List<AnalysisSummary>>
}

/**
 * 分析历史摘要（不含原始音频，FR-HX-1；M8.4-1 由 M2 占位模型细化为摘要全集，
 * 字段对齐 data-model §3.2 analysis_history）。
 *
 * 可空字段语义：稳定音域/舒适区在样本不足时为空（FR-ANAL-8）；
 * [recommendationRefsJson] 为 Top 推荐引用（songId 列表 + 权重版本）的 JSON
 * 序列化（task-breakdown M8.4-1 风险项：MVP 用 JSON 字符串），null = 无推荐（ACC-9）。
 */
data class AnalysisSummary(
    val analysisId: String,
    /** 会话 ID（M2 占位字段；M8.4 实体未持久化，读回为 null，M8.1 装配会话后细化）。 */
    val sessionId: String? = null,
    /** 记录时间（epoch 毫秒）。 */
    val analyzedAtMs: Long,
    /** 稳定最低音（MIDI，P5）；样本不足时为 null。 */
    val stableLowestMidi: Double? = null,
    /** 稳定最高音（MIDI，P95）；样本不足时为 null。 */
    val stableHighestMidi: Double? = null,
    /** 舒适最低音（MIDI）；样本不足时为 null。 */
    val comfortLowestMidi: Double? = null,
    /** 舒适最高音（MIDI）；样本不足时为 null。 */
    val comfortHighestMidi: Double? = null,
    /** 置信度分档（SPEC §13）；null = 占位未记录。 */
    val confidenceLevel: ConfidenceLevel? = null,
    /** 算法版本（FR-ANAL-6/FR-HX-1）。 */
    val algorithmVersion: String = "",
    /** 推荐引用 JSON（songId 列表 + 权重版本；null = 无推荐结果）。 */
    val recommendationRefsJson: String? = null,
    /** 有效帧数。 */
    val voicedFrameCount: Int = 0,
    /** 质量检测是否可用（FR-QUAL-3）。 */
    val qualityUsable: Boolean = false,
)
