package matchsong.domain.recommendation

/**
 * M7.3-2 推荐权重集中配置（FR-RECM-3，SPEC §7.2 v1）。
 *
 * 权重变更必须新建版本对象（不原地修改）；版本随结果记录（ACC-13 复算依据）。
 */
data class RecommendationWeights(
    val version: String,
    val weights: Map<ScoreFeature, Double>,
    /** 最低推荐分（R-7，60 分 [推测]；低于则不推荐）。 */
    val minRecommendationScore: Double = 60.0,
) {
    init {
        // 权重和 = 1.0（除 ConfidenceAdjustment 乘子外）
        require(kotlin.math.abs(weights.values.sum() - 1.0) < 0.001) {
            "权重和必须为 1.0，实际 ${weights.values.sum()}"
        }
    }

    fun weightOf(feature: ScoreFeature): Double = weights.getValue(feature)

    companion object {
        /** v1：SPEC §7.2 默认权重。 */
        val V1 =
            RecommendationWeights(
                version = "1.0.0",
                weights =
                    mapOf(
                        ScoreFeature.RANGE_FIT to 0.30,
                        ScoreFeature.TESSITURA_FIT to 0.25,
                        ScoreFeature.HIGH_NOTE_BURDEN_FIT to 0.15,
                        ScoreFeature.DIFFICULTY_FIT to 0.10,
                        ScoreFeature.PITCH_STABILITY_FIT to 0.10,
                        ScoreFeature.PREFERENCE_FIT to 0.10,
                    ),
            )
    }
}
