package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata
import matchsong.domain.analysis.ComfortRangeEstimate
import matchsong.domain.analysis.PitchStabilityMetricsResult
import matchsong.domain.analysis.VocalRangeEstimate
import matchsong.domain.port.UserSettings

/**
 * M7.3-1 六特征评分（FR-RECM-3，SPEC §7.2）。
 *
 * 每特征输出 0~100；映射为线性分段 [推测]，M7.6 场景测试标定后记录版本。
 * 输入为变调评估后的歌曲（transposed 音域）。
 */
object FeatureScoring {
    /** 特征分数（0~100）。 */
    data class FeatureScores(
        val rangeFit: Double,
        val tessituraFit: Double,
        val highNoteBurdenFit: Double,
        val difficultyFit: Double,
        val pitchStabilityFit: Double,
        val preferenceFit: Double,
    ) {
        fun get(feature: ScoreFeature): Double =
            when (feature) {
                ScoreFeature.RANGE_FIT -> rangeFit
                ScoreFeature.TESSITURA_FIT -> tessituraFit
                ScoreFeature.HIGH_NOTE_BURDEN_FIT -> highNoteBurdenFit
                ScoreFeature.DIFFICULTY_FIT -> difficultyFit
                ScoreFeature.PITCH_STABILITY_FIT -> pitchStabilityFit
                ScoreFeature.PREFERENCE_FIT -> preferenceFit
            }
    }

    fun score(
        song: SongMetadata,
        transposedLowest: Int,
        transposedHighest: Int,
        userRange: VocalRangeEstimate?,
        comfortRange: ComfortRangeEstimate?,
        stability: PitchStabilityMetricsResult?,
        settings: UserSettings,
    ): FeatureScores {
        val userLow = userRange?.stableLowestMidi
        val userHigh = userRange?.stableHighestMidi

        // RangeFit：变调后音域与用户稳定音域重合度
        val rangeFit =
            if (userLow != null && userHigh != null) {
                val overlap = minOf(transposedHighest, userHigh.toInt()) - maxOf(transposedLowest, userLow.toInt())
                val span = (userHigh - userLow).toInt().coerceAtLeast(1)
                (overlap.toDouble() / span * 100).coerceIn(0.0, 100.0)
            } else {
                0.0
            }

        // TessituraFit：变调后主要音区与用户舒适区重合度
        val tessituraFit =
            if (comfortRange?.comfortLowestMidi != null && comfortRange.comfortHighestMidi != null) {
                val songTessLow = song.tessituraLowMidi + (transposedLowest - song.lowestMidi)
                val songTessHigh = song.tessituraHighMidi + (transposedLowest - song.lowestMidi)
                val overlap =
                    minOf(songTessHigh, comfortRange.comfortHighestMidi.toInt()) -
                        maxOf(songTessLow, comfortRange.comfortLowestMidi.toInt())
                val span = (comfortRange.comfortHighestMidi - comfortRange.comfortLowestMidi).toInt().coerceAtLeast(1)
                (overlap.toDouble() / span * 100).coerceIn(0.0, 100.0)
            } else {
                0.0
            }

        // HighNoteBurdenFit：歌曲高音负担 vs 用户高音稳定性（负担低 → 高分）
        val highNoteBurdenFit = ((1.0 - song.highNoteBurden) * 100).coerceIn(0.0, 100.0)

        // DifficultyFit：歌曲难度 vs 用户稳定性（难度低 → 高分；稳定性好可接受稍难）
        val stabilityFactor = stability?.let { (it.stableFrameRatio + 0.5).coerceIn(0.5, 1.5) } ?: 1.0
        val difficultyFit = ((1.0 - song.overallDifficulty) * 100 * stabilityFactor).coerceIn(0.0, 100.0)

        // PitchStabilityFit：跳进/长音负担 vs 用户稳定性
        val pitchStabilityFit = ((1.0 - song.leapDifficulty) * 100 * stabilityFactor).coerceIn(0.0, 100.0)

        // PreferenceFit：语言/风格偏好
        val preferenceFit =
            when {
                song.language == settings.language && settings.preferredGenres.contains(song.genre) -> 100.0
                song.language == settings.language -> 80.0
                settings.preferredGenres.contains(song.genre) -> 60.0
                else -> 40.0
            }

        return FeatureScores(
            rangeFit = rangeFit,
            tessituraFit = tessituraFit,
            highNoteBurdenFit = highNoteBurdenFit,
            difficultyFit = difficultyFit,
            pitchStabilityFit = pitchStabilityFit,
            preferenceFit = preferenceFit,
        )
    }

    /** 分数 → FitLevel（≥70 GOOD，≥40 PARTIAL，否则 POOR，[推测]）。 */
    fun fitLevel(score: Double): FitLevel =
        when {
            score >= 70.0 -> FitLevel.GOOD
            score >= 40.0 -> FitLevel.PARTIAL
            else -> FitLevel.POOR
        }
}
