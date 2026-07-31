package matchsong.domain.analysis

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import matchsong.core.common.time.Clock
import matchsong.core.common.time.SystemClock
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.AnalysisSummary
import matchsong.domain.recommendation.RecommendationRefs
import java.util.UUID

/**
 * M8.4-1 记录分析历史用例（FR-HX-1 数据侧，data-model §3.2 analysis_history）。
 *
 * 分析完成时调用（M8.2 装配）：把 [VoiceAnalysisResult] 摘要 + 推荐引用落库为
 * 历史记录。只保存摘要，不含原始音频与逐帧轨迹（FR-HX-1/ACC-14）。
 *
 * @return 生成的 historyId（供调用方跳转历史详情/推荐）。
 */
class RecordAnalysisUseCase(
    private val historyRepository: AnalysisHistoryRepository,
    private val clock: Clock = SystemClock,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(
        result: VoiceAnalysisResult,
        recommendationRefs: RecommendationRefs? = null,
    ): String {
        val analysisId = idGenerator()
        historyRepository.add(
            AnalysisSummary(
                analysisId = analysisId,
                analyzedAtMs = clock.nowMillis(),
                stableLowestMidi = result.vocalRange?.stableLowestMidi,
                stableHighestMidi = result.vocalRange?.stableHighestMidi,
                comfortLowestMidi = result.comfortRange?.comfortLowestMidi,
                comfortHighestMidi = result.comfortRange?.comfortHighestMidi,
                confidenceLevel = result.confidenceLevel,
                algorithmVersion = result.algorithmVersion,
                recommendationRefsJson = recommendationRefs?.let { Json.encodeToString(it) },
                voicedFrameCount = result.voicedFrameCount,
                qualityUsable = result.qualityUsable,
            ),
        )
        return analysisId
    }
}
