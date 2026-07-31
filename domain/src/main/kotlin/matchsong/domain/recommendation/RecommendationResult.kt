package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata

/**
 * M7 推荐结果模型（data-model §2.12，SPEC §7.3）。
 */
data class RecommendationResult(
    /** 推荐项（Top 10，R-5）。 */
    val recommendations: List<RecommendationItem>,
    /** 总置信度（HIGH/MEDIUM；LOW 不生成正式推荐，ACC-9）。 */
    val totalConfidence: RecommendationConfidence,
    /** 空状态原因（无结果时非空，FR-RECM-5）。 */
    val emptyStateReason: String?,
    /** 候选歌曲总数（过滤后，供降级说明）。 */
    val candidateCount: Int,
    /** 权重版本（FR-RECM-3，ACC-13 复算依据）。 */
    val weightsVersion: String,
)

data class RecommendationItem(
    val song: SongMetadata,
    /** 总分 0~100。 */
    val score: Double,
    /** 变调建议（半音，负=降调）；null=不可调。 */
    val keyShiftSemitones: Int?,
    /** 推荐解释（≥1 条，ACC-11）。 */
    val explanation: List<String>,
    /** 特征分解（解释与分数一致性依据，ACC-16）。 */
    val fitBreakdown: Map<ScoreFeature, FitLevel>,
)

enum class RecommendationConfidence { HIGH, MEDIUM }

/**
 * M7.3-1 评分特征（SPEC §7.2）。
 */
enum class ScoreFeature {
    RANGE_FIT,
    TESSITURA_FIT,
    HIGH_NOTE_BURDEN_FIT,
    DIFFICULTY_FIT,
    PITCH_STABILITY_FIT,
    PREFERENCE_FIT,
}

/**
 * 特征匹配等级（POOR/PARTIAL/GOOD）。
 */
enum class FitLevel { POOR, PARTIAL, GOOD }
