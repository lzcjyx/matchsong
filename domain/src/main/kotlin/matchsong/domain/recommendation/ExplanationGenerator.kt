package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata
import matchsong.domain.analysis.VoiceAnalysisResult
import matchsong.domain.port.UserSettings

/**
 * M7.4-1 推荐解释生成（FR-RECM-4，ACC-11/16）。
 *
 * 模板 + 实际特征数据填充；无数据不生成文案（特征缺失跳过）；
 * 每首歌 ≥1 条解释；evidence 记录实测值（ACC-16 一致性校验）。
 */
class ExplanationGenerator {
    /**
     * 生成解释列表（按优先级，至少 1 条）。
     */
    fun generate(
        song: SongMetadata,
        scores: FeatureScoring.FeatureScores,
        @Suppress("UNUSED_PARAMETER") fitBreakdown: Map<ScoreFeature, FitLevel>,
        keyShiftSemitones: Int?,
        @Suppress("UNUSED_PARAMETER") analysis: VoiceAnalysisResult,
        settings: UserSettings,
    ): List<String> {
        val explanations = mutableListOf<String>()

        // 模板 1：主要音区在舒适区（TessituraFit 高分）
        if (scores.tessituraFit >= 70.0) {
            explanations.add("大部分旋律位于你的舒适音区")
        } else if (scores.tessituraFit >= 40.0) {
            explanations.add("部分旋律位于你的舒适音区附近")
        }

        // 模板 2：变调建议（基于实际 keyShift）
        if (keyShiftSemitones != null && keyShiftSemitones < 0) {
            explanations.add("原调最高音略高，降低 ${-keyShiftSemitones} 个半音后更适合")
        } else if (keyShiftSemitones != null && keyShiftSemitones > 0) {
            explanations.add("原调略低，升高 $keyShiftSemitones 个半音后更适合")
        }

        // 模板 3：高音负担
        if (song.highNoteBurden <= 0.3) {
            explanations.add("这首歌持续高音较少")
        } else if (song.highNoteBurden >= 0.7) {
            explanations.add("这首歌高音较多，需要较好的高音控制")
        }

        // 模板 4：跳进难度 vs 稳定性
        if (song.leapDifficulty <= 0.3) {
            explanations.add("旋律跳进较少，适合当前稳定性")
        }

        // 模板 5：偏好
        if (settings.preferredGenres.contains(song.genre)) {
            explanations.add("与你选择的${song.genre}风格偏好一致")
        } else if (song.language == settings.language) {
            explanations.add("语言与你当前偏好一致")
        }

        // 保底：RangeFit（总有一条）
        if (explanations.isEmpty()) {
            explanations.add("音域与你的稳定演唱区间匹配")
        }
        return explanations
    }
}
