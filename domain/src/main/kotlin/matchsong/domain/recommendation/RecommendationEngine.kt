package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.analysis.VoiceAnalysisResult
import matchsong.domain.port.UserSettings

/**
 * M7 推荐引擎（FR-RECM-1..7，SPEC §7.1 流水线）。
 *
 * 流水线：候选过滤 → 变调评估 → 特征评分 → 加权总分 → 置信度调整 → 排序 → 解释生成。
 * 确定性：总分降序 + songId 字典序 tie-break（ACC-13）；无随机扰动。
 * LOW 置信度输入不生成正式推荐（ACC-9，引擎短路）。
 */
class RecommendationEngine(
    private val candidateFilter: CandidateFilter = CandidateFilter(),
    private val keyShift: KeyShiftEvaluation = KeyShiftEvaluation(),
    private val weights: RecommendationWeights = RecommendationWeights.V1,
    private val explanationGenerator: ExplanationGenerator = ExplanationGenerator(),
) {
    @Suppress("LongMethod") // 推荐流水线编排（过滤→变调→评分→排序→降级），顺序清晰优于拆散
    fun recommend(
        analysis: VoiceAnalysisResult,
        songs: List<SongMetadata>,
        settings: UserSettings,
    ): RecommendationResult {
        // LOW 置信度：不生成正式推荐（ACC-9）
        if (analysis.confidenceLevel == ConfidenceLevel.LOW || !analysis.qualityUsable) {
            return RecommendationResult(
                recommendations = emptyList(),
                totalConfidence = RecommendationConfidence.MEDIUM,
                emptyStateReason = "分析置信度过低，无法生成可靠推荐",
                candidateCount = 0,
                weightsVersion = weights.version,
            )
        }

        val userRange = analysis.vocalRange
        val comfortRange = analysis.comfortRange
        val stability = analysis.stability

        // 1. 候选过滤
        val filterResult = candidateFilter.filter(songs, userRange, settings)
        if (filterResult.candidates.isEmpty()) {
            return RecommendationResult(
                recommendations = emptyList(),
                totalConfidence = RecommendationConfidence.MEDIUM,
                emptyStateReason = "没有符合条件（语言/风格/音域）的歌曲",
                candidateCount = 0,
                weightsVersion = weights.version,
            )
        }

        // 2-5. 变调 + 评分 + 加权 + 置信度
        val items =
            filterResult.candidates.mapNotNull { song ->
                if (userRange == null) return@mapNotNull null
                val shift = keyShift.evaluate(song, userRange)
                if (!shift.inRange || shift.keyShiftSemitones == null) return@mapNotNull null

                val scores =
                    FeatureScoring.score(
                        song = song,
                        transposedLowest = shift.transposedLowestMidi,
                        transposedHighest = shift.transposedHighestMidi,
                        userRange = userRange,
                        comfortRange = comfortRange,
                        stability = stability,
                        settings = settings,
                    )
                val total = weightedTotal(scores) * confidenceMultiplier(analysis)
                if (total < weights.minRecommendationScore) return@mapNotNull null

                val fitBreakdown = ScoreFeature.entries.associateWith { FeatureScoring.fitLevel(scores.get(it)) }
                val explanation =
                    explanationGenerator.generate(
                        song = song,
                        scores = scores,
                        fitBreakdown = fitBreakdown,
                        keyShiftSemitones = shift.keyShiftSemitones,
                        analysis = analysis,
                        settings = settings,
                    )
                RecommendationItem(
                    song = song,
                    score = total.coerceIn(0.0, 100.0),
                    keyShiftSemitones = shift.keyShiftSemitones,
                    explanation = explanation,
                    fitBreakdown = fitBreakdown,
                )
            }

        // 6. 排序（总分降序 + songId 字典序 tie-break，确定性）
        val ranked =
            items.sortedWith(
                compareByDescending<RecommendationItem> { it.score }.thenBy { it.song.songId },
            ).take(MAX_RECOMMENDATIONS)

        // 7. 无结果降级（FR-RECM-5）
        if (ranked.isEmpty()) {
            return RecommendationResult(
                recommendations = emptyList(),
                totalConfidence = RecommendationConfidence.MEDIUM,
                emptyStateReason = "没有歌曲达到最低匹配分数，建议重新录制或扩大风格偏好",
                candidateCount = filterResult.candidates.size,
                weightsVersion = weights.version,
            )
        }

        return RecommendationResult(
            recommendations = ranked,
            totalConfidence =
                if (analysis.confidenceLevel == ConfidenceLevel.HIGH) {
                    RecommendationConfidence.HIGH
                } else {
                    RecommendationConfidence.MEDIUM
                },
            emptyStateReason = null,
            candidateCount = filterResult.candidates.size,
            weightsVersion = weights.version,
        )
    }

    private fun weightedTotal(scores: FeatureScoring.FeatureScores): Double =
        ScoreFeature.entries.sumOf { feature ->
            scores.get(feature) * weights.weightOf(feature)
        }

    /** 置信度乘子（SPEC §7.2）：confidence ≥ 0.5 → 1；< 0.5 显著降权 [推测]。 */
    private fun confidenceMultiplier(analysis: VoiceAnalysisResult): Double {
        val confidence = analysis.vocalRange?.confidence ?: return 0.5
        return if (confidence >= 0.5) 1.0 else (0.3 + 0.4 * confidence).coerceIn(0.3, 1.0)
    }

    companion object {
        const val MAX_RECOMMENDATIONS = 10
    }
}
