package matchsong.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import matchsong.data.local.db.dao.AnalysisHistoryDao
import matchsong.data.local.db.entity.AnalysisHistoryEntity
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.AnalysisSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * domain AnalysisHistoryRepository Port 的 Room 实现（M8.4-1，ARCHITECTURE.md §7.1）。
 *
 * 映射 [AnalysisSummary] ↔ [AnalysisHistoryEntity]；排序由 DAO 保证（createdAtMs 倒序）。
 * 只落库摘要字段，不保存原始音频（FR-HX-1）。
 */
@Singleton
class RoomAnalysisHistoryRepository
    @Inject
    constructor(
        private val dao: AnalysisHistoryDao,
    ) : AnalysisHistoryRepository {
        override suspend fun getAll(): List<AnalysisSummary> = dao.getAll().map { it.toSummary() }

        override suspend fun getById(analysisId: String): AnalysisSummary? = dao.getById(analysisId)?.toSummary()

        override suspend fun add(summary: AnalysisSummary) {
            dao.insert(summary.toEntity())
        }

        override suspend fun delete(analysisId: String) {
            dao.deleteById(analysisId)
        }

        override suspend fun clear() {
            dao.clearAll()
        }

        override fun observeHistory(): Flow<List<AnalysisSummary>> =
            dao.observeAllDesc().map { list -> list.map { it.toSummary() } }
    }

/**
 * [AnalysisSummary] → Room 实体。
 *
 * 注：confidenceLevel 为 null 的占位摘要按 LOW 落库——生产路径
 * （RecordAnalysisUseCase）恒提供非空值，null 仅出现在测试占位构造中。
 */
internal fun AnalysisSummary.toEntity(): AnalysisHistoryEntity =
    AnalysisHistoryEntity(
        historyId = analysisId,
        createdAtMs = analyzedAtMs,
        stableLowestMidi = stableLowestMidi,
        stableHighestMidi = stableHighestMidi,
        comfortLowestMidi = comfortLowestMidi,
        comfortHighestMidi = comfortHighestMidi,
        confidenceLevel = (confidenceLevel ?: ConfidenceLevel.LOW).name,
        algorithmVersion = algorithmVersion,
        recommendationRefsJson = recommendationRefsJson,
        voicedFrameCount = voicedFrameCount,
        qualityUsable = qualityUsable,
    )

/** Room 实体 → Port [AnalysisSummary]（sessionId 实体未持久化，读回为 null）。 */
internal fun AnalysisHistoryEntity.toSummary(): AnalysisSummary =
    AnalysisSummary(
        analysisId = historyId,
        sessionId = null,
        analyzedAtMs = createdAtMs,
        stableLowestMidi = stableLowestMidi,
        stableHighestMidi = stableHighestMidi,
        comfortLowestMidi = comfortLowestMidi,
        comfortHighestMidi = comfortHighestMidi,
        confidenceLevel = ConfidenceLevel.valueOf(confidenceLevel),
        algorithmVersion = algorithmVersion,
        recommendationRefsJson = recommendationRefsJson,
        voicedFrameCount = voicedFrameCount,
        qualityUsable = qualityUsable,
    )
